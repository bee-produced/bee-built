package com.beeproduced.bee.fetched

import com.beeproduced.bee.fetched.BeeFetchedFeature.DgsDto
import com.beeproduced.bee.fetched.BeeFetchedFeature.PropertyDetails
import com.beeproduced.bee.fetched.annotations.BeeFetched
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataLoader
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
class BeeFetchedVisitor(
    val codeGenerator: CodeGenerator,
    val dependencies: Dependencies,
    val logger: KSPLogger,
    val dtos: Map<String, DgsDto>,
    val packageName: String
) : KSVisitorVoid() {

    data class DataLoaderDefinition(
        val keyType: String,
        val dtoType: String,
        val autoFetcher: AutoFetcherDefinition,
        // Using annotations directly is not a good idea...
        // org.jetbrains.kotlin.backend.common.BackendException: Backend Internal error: Exception during IR lowering
        // File being compiled: C:/.../lib.data/src/dgs/kotlin/com/beeproduced/data/dgs/processor/AutoFetcherVisitor.kt
        // The root cause java.lang.NullPointerException was thrown at: org.jetbrains.kotlin.backend.jvm.lower.JvmAnnotationImplementationTransformer
        // val dataLoader: DgsDataLoader
        val dataLoader: String,
    )

    // Mimics annotations as instancing them here leads to troubles
    // E.g. KClass is hard to get from KSType and error-prone
    // https://github.com/google/ksp/issues/1038 (generated dgs code)
    data class AutoFetcherDefinition(
        val mappings: List<FetcherMappingDefinition>,
        val ignore: List<FetcherIgnoreDefinition>,
        val safeMode: Boolean,
    )

    data class FetcherMappingDefinition(
        val target: String,
        val property: String,
        val idProperty: String,
    )

    data class FetcherIgnoreDefinition(
        val target: String,
        val property: String?,
    )

    lateinit var fileName: String
    lateinit var definition: DataLoaderDefinition
    val typedIgnore: Map<String, List<FetcherIgnoreDefinition>> by lazy {
        definition.autoFetcher.ignore.groupBy { it.target }
    }
    val typedMappings: Map<String, List<FetcherMappingDefinition>> by lazy {
        definition.autoFetcher.mappings.groupBy { it.target }
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val def = transform(classDeclaration)
        if (def == null) {
            logger.warn("Unable to process ${classDeclaration.qualifiedName?.asString()}")
            return
        }
        definition = def
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
        val processedProperties = mutableSetOf<String>()
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
        val dtoType = dto.toClassName()
        val propertyNonCollectionType = property.nonCollectionType.toClassName()
        val idType = idProperty.nonCollectionType.toClassName()
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

    private fun transform(classDeclaration: KSClassDeclaration): DataLoaderDefinition? {
        val autoFetcherAnnotation = classDeclaration.getAnnotation<BeeFetched>()
        val dgsDataLoaderAnnotation = classDeclaration.getAnnotation<DgsDataLoader>()
        val typeArgs = extractInterfaceTypeArguments(classDeclaration)
        val keyType = typeArgs[0]
        val dtoType = typeArgs[1]

        if (autoFetcherAnnotation != null && dgsDataLoaderAnnotation != null) {
            val autoFetcher = AutoFetcherDefinition(
                mappings = autoFetcherAnnotation.fetcherMappings(),
                ignore = autoFetcherAnnotation.fetcherIgnores(),
                safeMode = autoFetcherAnnotation.argumentValue("safeMode") as? Boolean ?: true
            )
            val dgsDataLoader = dgsDataLoaderAnnotation.argumentValue("name") as? String ?: ""

            return DataLoaderDefinition(keyType, dtoType, autoFetcher, dgsDataLoader)
        }

        return null
    }

    private fun extractInterfaceTypeArguments(ksClass: KSClassDeclaration): List<String> {
        val typeArgs = mutableListOf<String>()

        for (superType in ksClass.superTypes) {
            val s = superType.resolve()
            if (s.declaration.packageName.asString().startsWith("org.dataloader")) {
                typeArgs.addAll(s.arguments.mapNotNull {
                    it.type?.resolve()?.declaration?.qualifiedName?.asString()
                })
            }
        }

        return typeArgs
    }

    private inline fun <reified T : Annotation> KSClassDeclaration.getAnnotation(): KSAnnotation? {
        return annotations.firstOrNull { it.shortName.asString() == T::class.simpleName }
    }

    private fun KSAnnotation.fetcherMappings(): List<FetcherMappingDefinition> {
        val mappings = arguments.find { it.name?.asString() == "mappings" }?.value as Collection<KSAnnotation>
        return mappings.map { mapping ->
            val target = (mapping.argumentValue("target") as KSType).declaration.qualifiedName!!.asString()
            val property = mapping.argumentValue("property") as String
            val idProperty = mapping.argumentValue("idProperty") as String
            FetcherMappingDefinition(target, property, idProperty)
        }
    }

    private fun KSAnnotation.fetcherIgnores(): List<FetcherIgnoreDefinition> {
        val mappings = arguments.find { it.name?.asString() == "ignore" }?.value as Collection<KSAnnotation>
        return mappings.map { mapping ->
            val target = (mapping.argumentValue("target") as KSType).declaration.qualifiedName!!.asString()
            val property = (mapping.argumentValue("property") as String).let {
                it.ifEmpty { null }
            }
            FetcherIgnoreDefinition(target, property)
        }
    }

    private fun KSAnnotation.argumentValue(argumentName: String): Any? {
        // Returns primitive values or more sophisticated KSP wrapper (KSType, KSAnnotation, ...)
        return arguments.find { it.name?.asString() == argumentName }?.value
    }

    private fun String.toClassName(): ClassName {
        val lastIndex = this.lastIndexOf(".")
        if (lastIndex == -1) {
            // This means the provided string doesn't have a package part.
            // It's just a class without a package or it's an error.
            return ClassName("", this)
        }
        val packageName = this.substring(0, lastIndex)
        val simpleName = this.substring(lastIndex + 1)
        return ClassName(packageName, simpleName)
    }
}