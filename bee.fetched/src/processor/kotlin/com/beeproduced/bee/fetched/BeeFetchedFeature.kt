package com.beeproduced.bee.fetched

import com.beeproduced.bee.fetched.annotations.BeeFetched
import com.beeproduced.bee.fetched.codegen.*
import com.beeproduced.bee.generative.*
import com.beeproduced.bee.generative.processor.Options
import com.beeproduced.bee.generative.util.*
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.netflix.graphql.dgs.DgsDataLoader

/**
 * @author Kacper Urbaniec
 * @version 2023-10-04
 */
class BeeFetchedFeature : BeeGenerativeFeature {

  private lateinit var scanPackage: String
  private lateinit var packageName: String

  override fun order(): Int = Int.MAX_VALUE

  override fun multipleRoundProcessing(): Boolean = false

  override fun setup(options: Options, shared: Shared): BeeGenerativeConfig {
    scanPackage = options.getOption(BeeFetchedOptions.scanPackage)
    packageName = options.getOption(BeeFetchedOptions.packageName)
    return BeeGenerativeConfig(
      packages = setOf(scanPackage),
      annotatedBy = setOf(BeeFetchedOptions.beeFetchedAnnotationName),
      callbacks = listOf(::getInternalTypesSymbols),
    )
  }

  private fun getInternalTypesSymbols(
    symbols: BeeGenerativeSymbols,
    options: Options,
    shared: Shared,
  ): BeeGenerativeConfig {
    val internalTypes =
      symbols.annotatedBy
        .getValue(BeeFetchedOptions.beeFetchedAnnotationName)
        .asSequence()
        .mapNotNull { it.getAnnotation<BeeFetched>() }
        .map { it.fetcherInternalTypes() }
        .flatten()
        .map { it.internal }
        .toSet()
    shared[BeeFetchedOptions.internalTypes] = internalTypes
    return BeeGenerativeConfig(classes = internalTypes)
  }

  override fun process(input: BeeGenerativeInput) {
    val dtos = input.symbols.packages.getValue(scanPackage).let(::transformDtos)
    val internalTypes = input.shared.getTyped<Set<String>>(BeeFetchedOptions.internalTypes)
    val internalDtos =
      internalTypes.mapTo(HashSet()) { input.symbols.classes.getValue(it) }.let(::transformDtos)
    val dataLoaders =
      input.symbols.annotatedBy
        .getValue(BeeFetchedOptions.beeFetchedAnnotationName)
        .map(::transformDataLoader)
    input.logger.let { logger ->
      logger.info("Found data loaders: $dataLoaders")
      logger.info("Found dtos: $dtos")
      logger.info("Found internal dtos: $internalDtos")
    }

    val codegen =
      BeeFetchedCodegen(
        input.codeGenerator,
        input.dependencies,
        input.logger,
        dtos,
        internalDtos,
        packageName,
      )
    dataLoaders.forEach { dl -> codegen.processDataLoader(dl) }
  }

  private fun transformDtos(classes: Set<KSClassDeclaration>): Map<String, DgsDto> {
    return classes
      .map { ksClass ->
        val properties = transformDtoDetails(ksClass)
        DgsDto(name = ksClass.qualifiedName?.asString() ?: "Unknown", properties = properties)
      }
      .associateBy { it.name }
  }

  private fun transformDtoDetails(ksClass: KSClassDeclaration) =
    ksClass
      .getAllProperties()
      .map { property ->
        val type = property.type.resolve()
        val name = property.simpleName.asString()
        val isNullable = type.isMarkedNullable
        val (nonCollectionType, isCollection) =
          if (
            type.declaration.qualifiedName?.asString() == "kotlin.collections.List" &&
              type.arguments.size == 1
          ) {
            val nonCollectionType =
              type.arguments.first().type?.resolve()?.declaration?.qualifiedName?.asString()
            Pair(nonCollectionType ?: "Unknown", true)
          } else {
            val nonCollectionType = type.declaration.qualifiedName?.asString()
            Pair(nonCollectionType ?: "Unknown", false)
          }
        PropertyDetails(name, nonCollectionType, isCollection, isNullable)
      }
      .toList()

  private fun transformDataLoader(classDeclaration: KSClassDeclaration): DataLoaderDefinition {
    val autoFetcherAnnotation = classDeclaration.getAnnotation<BeeFetched>()
    val dgsDataLoaderAnnotation = classDeclaration.getAnnotation<DgsDataLoader>()
    val typeArgs = extractInterfaceTypeArguments(classDeclaration)
    val (keyType, nullableKey) = typeArgs[0]
    val (dtoType, nullableDto) = typeArgs[1]

    if (autoFetcherAnnotation == null || dgsDataLoaderAnnotation == null)
      throw IllegalArgumentException("Could not parse [$classDeclaration] as data loader")

    val autoFetcher =
      AutoFetcherDefinition(
        mappings = autoFetcherAnnotation.fetcherMappings(),
        internalTypes = autoFetcherAnnotation.fetcherInternalTypes(),
        ignore = autoFetcherAnnotation.fetcherIgnores(),
        safeMode = autoFetcherAnnotation.safeArgumentValue<Boolean>("safeMode", true),
        safeModeOverrides = autoFetcherAnnotation.fetcherSafeModeOverrides(),
      )
    val dgsDataLoader = dgsDataLoaderAnnotation.argumentValue("name") as? String ?: ""

    return DataLoaderDefinition(
      keyType,
      dtoType,
      nullableKey,
      nullableDto,
      autoFetcher,
      dgsDataLoader,
    )
  }

  private fun extractInterfaceTypeArguments(
    ksClass: KSClassDeclaration
  ): List<Pair<String, Boolean>> {
    val typeArgs = mutableListOf<Pair<String, Boolean>>()
    for (superType in ksClass.superTypes) {
      val s = superType.resolve()
      if (!s.declaration.packageName.asString().startsWith("org.dataloader")) continue

      typeArgs.addAll(
        s.arguments.mapNotNull {
          it.type?.let { typeArg ->
            val resolvedType = typeArg.resolve().resolveTypeAlias()
            val qualifiedName = resolvedType.declaration.qualifiedName?.asString()
            val isNullable = resolvedType.isMarkedNullable
            if (qualifiedName != null) Pair(qualifiedName, isNullable) else null
          }
        }
      )
    }

    return typeArgs
  }

  @Suppress("UNCHECKED_CAST")
  private fun KSAnnotation.fetcherMappings(): List<FetcherMappingDefinition> {
    val mappings =
      arguments.find { it.name?.asString() == BeeFetched::mappings.name }?.value
        as Collection<KSAnnotation>
    return mappings.map { mapping ->
      val target =
        mapping
          .argumentValue<KSType>(FetcherMappingDefinition::target.name)
          .resolveTypeAlias()
          .declaration
          .qualifiedName!!
          .asString()
      val property = mapping.argumentValue<String>(FetcherMappingDefinition::property.name)
      val idProperty = mapping.argumentValue<String>(FetcherMappingDefinition::idProperty.name)
      FetcherMappingDefinition(target, property, idProperty)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun KSAnnotation.fetcherIgnores(): List<FetcherIgnoreDefinition> {
    val mappings =
      arguments.find { it.name?.asString() == BeeFetched::ignore.name }?.value
        as Collection<KSAnnotation>
    return mappings.map { mapping ->
      val target =
        mapping
          .argumentValue<KSType>(FetcherIgnoreDefinition::target.name)
          .resolveTypeAlias()
          .declaration
          .qualifiedName!!
          .asString()
      val property =
        mapping.argumentValue<String>(FetcherIgnoreDefinition::property.name).let {
          it.ifEmpty { null }
        }
      FetcherIgnoreDefinition(target, property)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun KSAnnotation.fetcherInternalTypes(): List<FetcherInternalTypeDefinition> {
    val mappings =
      arguments.find { it.name?.asString() == BeeFetched::internalTypes.name }?.value
        as Collection<KSAnnotation>
    return mappings.map { mapping ->
      val target =
        mapping
          .argumentValue<KSType>(FetcherInternalTypeDefinition::target.name)
          .resolveTypeAlias()
          .declaration
          .qualifiedName!!
          .asString()
      val internal =
        mapping
          .argumentValue<KSType>(FetcherInternalTypeDefinition::internal.name)
          .resolveTypeAlias()
          .declaration
          .qualifiedName!!
          .asString()
      val property =
        mapping.argumentValue<String>(FetcherInternalTypeDefinition::property.name).let {
          it.ifEmpty { null }
        }
      FetcherInternalTypeDefinition(target, internal, property)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun KSAnnotation.fetcherSafeModeOverrides(): List<FetcherSafeModeOverrideDefinition> {
    val mappings =
      arguments.find { it.name?.asString() == BeeFetched::safeModeOverrides.name }?.value
        as Collection<KSAnnotation>
    return mappings.map { mapping ->
      val target =
        mapping
          .argumentValue<KSType>(FetcherSafeModeOverrideDefinition::target.name)
          .resolveTypeAlias()
          .declaration
          .qualifiedName!!
          .asString()
      val property = mapping.argumentValue<String>(FetcherSafeModeOverrideDefinition::property.name)
      val safeMode =
        mapping.argumentValue<Boolean>(FetcherSafeModeOverrideDefinition::safeMode.name)
      FetcherSafeModeOverrideDefinition(target, property, safeMode)
    }
  }
}
