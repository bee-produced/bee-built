package com.beeproduced.bee.fetched

import com.beeproduced.bee.fetched.annotations.BeeFetched
import com.beeproduced.bee.fetched.codegen.*
import com.beeproduced.bee.generative.BeeGenerativeConfig
import com.beeproduced.bee.generative.BeeGenerativeFeature
import com.beeproduced.bee.generative.BeeGenerativeInput
import com.beeproduced.bee.generative.util.*
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.netflix.graphql.dgs.DgsDataLoader

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-04
 */
class BeeFetchedFeature : BeeGenerativeFeature {

    private lateinit var scanPackage: String
    private lateinit var packageName: String

    override fun order(): Int = Int.MAX_VALUE

    override fun multipleRoundProcessing(): Boolean = false

    override fun setup(options: Map<String, String>): BeeGenerativeConfig {
        scanPackage = options.getOption(BeeFetchedOption.scanPackage)
        packageName = options.getOption(BeeFetchedOption.packageName)
        return BeeGenerativeConfig(
            packages = setOf(scanPackage),
            annotatedBy = setOf(BeeFetchedOption.beeFetchedAnnotationName)
        )
    }

    override fun process(input: BeeGenerativeInput) {
        val dtos = input.symbols.packages.getValue(scanPackage).let(::transformDtos)
        val dataLoaders = input.symbols
            .annotatedBy
            .getValue(BeeFetchedOption.beeFetchedAnnotationName)
            .map(::transformDataLoader)
        input.logger.let { logger ->
            logger.warn("Found dtos: $dtos")
            logger.warn("Found data loaders: $dataLoaders")
        }

        val codegen = BeeFetchedCodegen(input.codeGenerator, input.dependencies, input.logger, dtos, packageName)
        dataLoaders.forEach { dl -> codegen.processDataLoader(dl) }
    }

    private fun transformDtos(classes: Set<KSClassDeclaration>): Map<String, DgsDto> {
        return classes.map { ksClass ->
            val properties = transformDtoDetails(ksClass)
            DgsDto(
                name = ksClass.qualifiedName?.asString() ?: "Unknown",
                properties = properties
            )
        }.associateBy { it.name }
    }

    private fun transformDtoDetails(ksClass: KSClassDeclaration) =
        ksClass.getAllProperties().map { property ->
            val type = property.type.resolve()
            if (type.declaration.qualifiedName?.asString() == "kotlin.collections.List" && type.arguments.size == 1) {
                PropertyDetails(
                    name = property.simpleName.asString(),
                    nonCollectionType = type.arguments.first().type?.resolve()?.declaration?.qualifiedName?.asString()
                        ?: "Unknown",
                    isCollection = true
                )
            } else {
                PropertyDetails(
                    name = property.simpleName.asString(),
                    nonCollectionType = type.declaration.qualifiedName?.asString() ?: "Unknown",
                    isCollection = false
                )
            }
        }.toList()

    private fun transformDataLoader(classDeclaration: KSClassDeclaration): DataLoaderDefinition {
        val autoFetcherAnnotation = classDeclaration.getAnnotation<BeeFetched>()
        val dgsDataLoaderAnnotation = classDeclaration.getAnnotation<DgsDataLoader>()
        val typeArgs = extractInterfaceTypeArguments(classDeclaration)
        val keyType = typeArgs[0]
        val dtoType = typeArgs[1]

        if (autoFetcherAnnotation == null || dgsDataLoaderAnnotation == null)
            throw IllegalArgumentException("Could not parse [$classDeclaration] as data loader")

        val autoFetcher = AutoFetcherDefinition(
            mappings = autoFetcherAnnotation.fetcherMappings(),
            ignore = autoFetcherAnnotation.fetcherIgnores(),
            safeMode = autoFetcherAnnotation.safeArgumentValue<Boolean>("safeMode", true)
        )
        val dgsDataLoader = dgsDataLoaderAnnotation.argumentValue("name") as? String ?: ""

        return DataLoaderDefinition(keyType, dtoType, autoFetcher, dgsDataLoader)
    }

    private fun extractInterfaceTypeArguments(ksClass: KSClassDeclaration): List<String> {
        val typeArgs = mutableListOf<String>()
        for (superType in ksClass.superTypes) {
            val s = superType.resolve()
            if (!s.declaration.packageName.asString().startsWith("org.dataloader"))
                continue

            typeArgs.addAll(s.arguments.mapNotNull {
                it.type?.let { typeArg ->
                    val resolvedType = typeArg.resolve().resolveTypeAlias()
                    resolvedType.declaration.qualifiedName?.asString()
                }
            })
        }

        return typeArgs
    }

    @Suppress("UNCHECKED_CAST")
    private fun KSAnnotation.fetcherMappings(): List<FetcherMappingDefinition> {
        val mappings = arguments.find { it.name?.asString() == "mappings" }?.value as Collection<KSAnnotation>
        return mappings.map { mapping ->
            val target = mapping.argumentValue<KSType>("target").resolveTypeAlias().declaration.qualifiedName!!.asString()
            val property = mapping.argumentValue<String>("property")
            val idProperty = mapping.argumentValue<String>("idProperty")
            FetcherMappingDefinition(target, property, idProperty)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun KSAnnotation.fetcherIgnores(): List<FetcherIgnoreDefinition> {
        val mappings = arguments.find { it.name?.asString() == "ignore" }?.value as Collection<KSAnnotation>
        return mappings.map { mapping ->
            val target = mapping.argumentValue<KSType>("target").resolveTypeAlias().declaration.qualifiedName!!.asString()
            val property = mapping.argumentValue<String>("property").let {
                it.ifEmpty { null }
            }
            FetcherIgnoreDefinition(target, property)
        }
    }

}