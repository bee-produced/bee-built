package com.beeproduced.bee.fetched.codegen

import com.beeproduced.bee.fetched.codegen.BeeFetchedCodegen.PoetConstants.COMPLETABLE_FUTURE
import com.beeproduced.bee.fetched.codegen.BeeFetchedCodegen.PoetConstants.DATA_FETCHING_ENVIRONMENT
import com.beeproduced.bee.fetched.codegen.BeeFetchedCodegen.PoetConstants.DATA_LOADER
import com.beeproduced.bee.fetched.codegen.BeeFetchedCodegen.PoetConstants.DTO
import com.beeproduced.bee.fetched.codegen.BeeFetchedCodegen.PoetConstants._ID_PROPERTY
import com.beeproduced.bee.fetched.codegen.BeeFetchedCodegen.PoetConstants._PROPERTY
import com.beeproduced.bee.fetched.codegen.BeeFetchedCodegen.PoetConstants.__DATA_LOADER_NAME
import com.beeproduced.bee.fetched.codegen.BeeFetchedCodegen.PoetConstants.__DTO_NAME
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

    private val poetMap: PoetMap = mutableMapOf()
    private fun FunSpec.Builder.addNStatement(format: String)
        = addNStatementBuilder(format, poetMap)
    @Suppress("ConstPropertyName")
    object PoetConstants {
        const val _ID_PROPERTY = "%idProperty:L"
        const val _PROPERTY = "%property:L"
        const val __DATA_LOADER_NAME = "%dataloadername:S"
        const val __DTO_NAME = "%dtoname:S"
        const val COMPLETABLE_FUTURE = "%completed:T"
        const val DATA_LOADER = "%dataloader:T"
        const val DTO = "%dto:T"
        const val DATA_FETCHING_ENVIRONMENT = "%dfe:T"
    }

    fun processDataLoader(dataLoader: DataLoaderDefinition) {
        logger.info("processDataLoader($dataLoader)")
        definition = dataLoader
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
        logger.info("processDto($name, $dto)")
        if (ignoreDto(name)) return this
        val properties = dto.properties.associateBy { it.name }
        logger.info("processDto|properties: $properties")
        for (property in dto.properties) {
            if (property.nonCollectionType != definition.dtoType) {
                logger.info("processDto|skipping property: ${property.nonCollectionType} != ${definition.dtoType}")
                continue
            }

            logger.info("processDto|processing property: $name; ${typedMappings}")
            val idNames = idNames(name, property)
            val idProperty = idNames.firstNotNullOfOrNull { properties[it] }
            if (idProperty == null) {
                logger.info("processDto|no id property found for $name - ${property.name}")
                continue
            }
            logger.info("processDto|idProperty: ${idProperty.name}")

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
        logger.info("buildNestedFetcher($dto, $property, $idProperty, $dataLoader, $safeMode)")
        val dtoName = dto.substringAfterLast(".")
        val dtoType = dto.toPoetClassName()

        val propertyType = property.toPoetTypename()
        val returnType = CompletableFuture::class.asClassName()
            .parameterizedBy(propertyType)

        val dfeType = DataFetchingEnvironment::class.asClassName()
        val propertyNonCollectionType = property.nonCollectionType.toPoetClassName()
        val idNonCollectionType = idProperty.nonCollectionType.toPoetClassName()
        val dataLoaderType = DataLoader::class.asClassName()
            .parameterizedBy(idNonCollectionType, propertyNonCollectionType)

        val completed = CompletableFuture::class.asClassName()

        val funcName = "${
            dtoName.replaceFirstChar {
                it.lowercase(Locale.getDefault())
            }
        }${
            property.name.replaceFirstChar {
                it.titlecase(Locale.getDefault())
            }
        }"

        poetMap.addMapping(_ID_PROPERTY, idProperty.name)
        poetMap.addMapping(_PROPERTY, property.name)
        poetMap.addMapping(COMPLETABLE_FUTURE, completed)
        poetMap.addMapping(DATA_LOADER, dataLoaderType)
        poetMap.addMapping(__DATA_LOADER_NAME, dataLoader)
        poetMap.addMapping(DTO, dtoType)
        poetMap.addMapping(__DTO_NAME, dtoName)
        poetMap.addMapping(DATA_FETCHING_ENVIRONMENT, dfeType)

        // TODO: What happens with internal types
        return FunSpec.builder(funcName)
            .addAnnotation(
                AnnotationSpec
                    .builder(DgsData::class)
                    .addMember("parentType = %S", dtoName)
                    .addMember("field = %S", property.name)
                    .build()
            )
            .addParameter("dfe", dfeType)
            .returns(returnType)
            .addNStatement("val data = dfe.getSource<$DTO>()")
            .apply {
                if (safeMode && property.isCollection)
                    addNStatement("if (!data.$_PROPERTY.isNullOrEmpty()) return $COMPLETABLE_FUTURE.completedFuture(data.$_PROPERTY)")
                else if (safeMode) // && !property.isCollection
                    addNStatement("if (data.$_PROPERTY != null) return $COMPLETABLE_FUTURE.completedFuture(data.$_PROPERTY)")
            }
            .addNStatement("val dataLoader: $DATA_LOADER = dfe.getDataLoader($__DATA_LOADER_NAME)")
            .apply {
                if (property.isCollection) {
                    addNStatement("val ids = data.$_ID_PROPERTY")
                    if (idProperty.isNullable)
                        addNStatement("if (ids.isNullOrEmpty()) return $COMPLETABLE_FUTURE.completedFuture(data.$_PROPERTY)")
                    addNStatement("return dataLoader.loadMany(ids)")
                } else {
                    addNStatement("val id = data.$_ID_PROPERTY")
                    if (idProperty.isNullable)
                        addNStatement("if (id == null) return $COMPLETABLE_FUTURE.completedFuture(data.$_PROPERTY)")
                    addNStatement("return dataLoader.load(id)")
                }
            }
            .build()
    }

    private fun ignoreDto(name: String): Boolean {
        logger.info("ignoreDto($name)")
        logger.info("ignoreDto|typedIgnore: $typedIgnore")
        val ignoreDefinitions = typedIgnore[name]
        val ignore = ignoreDefinitions != null &&
            ignoreDefinitions.all { it.property == null }
        logger.info("ignoreDto|ignore: $ignore")
        return ignore
    }

    private fun idNames(name: String, property: PropertyDetails): Set<String> {
        logger.info("idNames($name, $property)")
        val mappingDefinitions = typedMappings[name]
        logger.info("idNames|mappingDefinitions: $mappingDefinitions")
        val mapping = mappingDefinitions?.firstOrNull { it.property == property.name }
        if (mapping != null)
            return setOf(mapping.idProperty)
        // PropertyDetails("x", "String", false) => setOf("xId")
        // PropertyDetails("xs", "String", true) => setOf("xsIds", "xIds")
        // PropertyDetails("y", "String", true) => setOf("ysIds", "yIds")
        val idNames = when {
            property.isCollection && property.name.endsWith("s") -> setOf(
                "${property.name}Ids",
                "${property.name.dropLast(1)}Ids"
            )

            property.isCollection -> setOf("${property.name}sIds", "${property.name}Ids")
            else -> setOf("${property.name}Id")
        }
        logger.info("idNames|idNames: $idNames")
        return idNames
    }
}