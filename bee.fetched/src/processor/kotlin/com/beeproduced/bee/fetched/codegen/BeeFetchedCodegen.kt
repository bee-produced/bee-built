package com.beeproduced.bee.fetched.codegen

import com.beeproduced.bee.generative.util.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoader
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-04
 */
class BeeFetchedCodegen(
    private val codeGenerator: CodeGenerator,
    private val dependencies: Dependencies,
    private val logger: KSPLogger,
    private val dtos: Map<String, DgsDto>,
    private val packageName: String
) {

    private lateinit var fileName: String
    private lateinit var definition: DataLoaderDefinition
    private val typedIgnore: Map<String, List<FetcherIgnoreDefinition>> by lazy {
        definition.autoFetcher.ignore.groupBy { it.target }
    }
    private val typedMappings: Map<String, List<FetcherMappingDefinition>> by lazy {
        definition.autoFetcher.mappings.groupBy { it.target }
    }

    fun processDataLoader(dataLoader: DataLoaderDefinition) {
        definition = dataLoader
        logger.warn("Def: $definition")
        fileName = "${definition.dataLoader}AutoFetcher"
        FileSpec
            .builder(packageName, fileName)
            .buildAutoFetcher()
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun FileSpec.Builder.buildAutoFetcher(): FileSpec.Builder {
        addType(
            TypeSpec.classBuilder(fileName)
                .addAnnotation(DgsComponent::class)
                .buildAutoFetcher()
                .build()
        )
        return this
    }

    private fun TypeSpec.Builder.buildAutoFetcher(): TypeSpec.Builder {
        for ((name, dto) in dtos)
            processDto(name, dto)
        return this
    }

    private fun TypeSpec.Builder.processDto(name: String, dto: DgsDto): TypeSpec.Builder {
        logger.warn("processDto($name, $dto)")
        if (ignoreDto(name)) return this
        val properties = dto.properties.associateBy { it.name }
        logger.warn("$properties")
        for (property in dto.properties) {
            logger.warn("${property.nonCollectionType} , ${definition.dtoType}")
            if (property.nonCollectionType != definition.dtoType)
                continue

            logger.warn("$name; ${typedMappings}")
            val idNames = idNames(name, property)
            logger.warn("$idNames")
            val idProperty = idNames.firstNotNullOfOrNull { properties[it] }
            if (idProperty == null) {
                logger.warn("No id property found for $name - ${property.name}")
                continue
            }
            logger.warn("Found ${idProperty.name}")

            addFunction(
                buildNestedFetcher(
                    name, property, idProperty,
                    definition.dataLoader, definition.autoFetcher.safeMode
                )
            )
        }

        return this
    }

    private fun buildNestedFetcher(
        dto: String, property: PropertyDetails, idProperty: PropertyDetails,
        dataLoader: String, safeMode: Boolean,
    ): FunSpec {
        val simpleName = dto.substringAfterLast(".")
        val dtoType = dto.toPoetClassName()
        val propertyNonCollectionType = property.nonCollectionType.toPoetClassName()
        val idType = idProperty.nonCollectionType.toPoetClassName()
        val propertyType = if (property.isCollection)
            ClassName("kotlin.collections", "List")
                .parameterizedBy(propertyNonCollectionType)
        else propertyNonCollectionType
        val returnType = CompletableFuture::class.asClassName()
            .parameterizedBy(propertyType)
        val dfeType = DataFetchingEnvironment::class.asClassName()
        val dataLoaderType = DataLoader::class.asClassName()
            .parameterizedBy(idType, propertyNonCollectionType)
        val completed = CompletableFuture::class.asClassName()

        val funcName = "${
            simpleName.replaceFirstChar {
                it.lowercase(Locale.getDefault())
            }
        }${
            property.name.replaceFirstChar {
                it.titlecase(Locale.getDefault())
            }
        }"

        // TODO: What happens on nullable ids?
        return FunSpec.builder(funcName)
            .addAnnotation(
                AnnotationSpec
                    .builder(DgsData::class)
                    .addMember("parentType = %S", simpleName)
                    .addMember("field = %S", property.name)
                    .build()
            )
            .addParameter("dfe", dfeType)
            .returns(returnType)
            .addStatement("val data = dfe.getSource<%T>()", dtoType)
            .apply {
                if (safeMode && property.isCollection) addStatement(
                    "if (!data.%L.isNullOrEmpty()) return %T.completedFuture(data.%L)",
                    property.name,
                    completed,
                    property.name
                ) else if (safeMode && !property.isCollection) addStatement(
                    "if (data.%L != null) return %T.completedFuture(data.%L)",
                    property.name,
                    completed,
                    property.name
                )
            }
            .addStatement("val dataLoader: %T = dfe.getDataLoader(%S)", dataLoaderType, dataLoader)
            .apply {
                if (property.isCollection) {
                    addStatement("val ids = data.%L", idProperty.name)
                    addStatement("return dataLoader.loadMany(ids)")
                } else {
                    addStatement("val id = data.%L", idProperty.name)
                    addStatement("return dataLoader.load(id)")
                }
            }
            .build()
    }

    private fun ignoreDto(name: String): Boolean {
        logger.warn("IGNORE_DTO")
        logger.warn(name)
        logger.warn("$typedIgnore")
        val ignoreDefinitions = typedIgnore[name]
        return ignoreDefinitions != null &&
            ignoreDefinitions.all { it.property == null }
    }

    private fun idNames(name: String, property: PropertyDetails): Set<String> {
        logger.warn("2 ${property.name}")
        val mappingDefinitions = typedMappings[name]
        logger.warn("$mappingDefinitions")
        val mapping = mappingDefinitions?.firstOrNull { it.property == property.name }
        if (mapping != null)
            return setOf(mapping.idProperty)
        // PropertyDetails("x", "String", false) => setOf("xId")
        // PropertyDetails("xs", "String", true) => setOf("xsIds", "xIds")
        // PropertyDetails("y", "String", true) => setOf("ysIds", "yIds")
        return when {
            property.isCollection && property.name.endsWith("s") -> setOf(
                "${property.name}Ids",
                "${property.name.dropLast(1)}Ids"
            )

            property.isCollection -> setOf("${property.name}sIds", "${property.name}Ids")
            else -> setOf("${property.name}Id")
        }
    }


}