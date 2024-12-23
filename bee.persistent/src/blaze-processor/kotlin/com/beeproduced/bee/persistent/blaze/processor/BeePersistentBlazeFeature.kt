package com.beeproduced.bee.persistent.blaze.processor

import com.beeproduced.bee.generative.*
import com.beeproduced.bee.generative.processor.Options
import com.beeproduced.bee.generative.util.getOption
import com.beeproduced.bee.generative.util.resolveTypeAlias
import com.beeproduced.bee.persistent.blaze.BeeBlazeRepository
import com.beeproduced.bee.persistent.blaze.processor.codegen.*
import com.beeproduced.bee.persistent.blaze.processor.info.*
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATIONS_RELATION
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_BEE_REPOSITORY
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_EMBEDDED
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_EMBEDDED_ID
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_ENABLE_BEE_REPOSITORIES
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_ENTITY
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_GENERATED_VALUE
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_ID
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_INHERITANCE
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_LAZY_FIELD
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_TRANSIENT
import com.beeproduced.bee.persistent.blaze.processor.utils.buildUniqueClassName
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * @author Kacper Urbaniec
 * @version 2023-12-14
 */
typealias FullyQualifiedName = String

class BeePersistentBlazeFeature : BeeGenerativeFeature {
  override fun order(): Int = Int.MAX_VALUE

  override fun multipleRoundProcessing(): Boolean = false

  override fun setup(options: Options, shared: Shared): BeeGenerativeConfig {
    return BeeGenerativeConfig(
      packages = setOf(),
      annotatedBy =
        setOf(ANNOTATION_ENTITY, ANNOTATION_BEE_REPOSITORY, ANNOTATION_ENABLE_BEE_REPOSITORIES),
    )
  }

  private lateinit var logger: KSPLogger

  override fun process(input: BeeGenerativeInput) {
    logger = input.logger
    logger.info("Hey")
    logger.info(input.shared.keys.toString())

    val symbols = input.symbols

    val entityDeclarations = symbols.annotatedBy.getValue(ANNOTATION_ENTITY)
    val duplicateEntitySimpleNames = duplicateSimpleNames(entityDeclarations)

    val visitedEmbedded = mutableMapOf<String, EmbeddedInfo>()
    val duplicateEmbeddedNames = mutableSetOf<String>()

    val entitiesWithoutInheritanceInfo =
      entityDeclarations.map { entityDeclaration ->
        val entityName = entityDeclaration.simpleName.asString()
        val uniqueName =
          if (!duplicateEntitySimpleNames.contains(entityName)) entityName
          else buildUniqueClassName(entityDeclaration.packageName.asString(), entityName)

        val entityAnnotations = resolveAnnotations(entityDeclaration.annotations)
        // logger.info(entityDeclaration.simpleName.asString())
        // logger.info(entityDeclaration.packageName.asString())

        // Properties
        val propertyDeclarations =
          entityDeclaration.getAllProperties().filter { it.hasBackingField }.toList()

        // Placeholder is used for entities that inherit the id from
        // their superclass
        var idP: IdProperty = IdProperty.PLACEHOLDER
        val properties: MutableList<EntityProperty> = mutableListOf()
        val jpaProperties: MutableList<EntityProperty> = mutableListOf()
        val columns: MutableList<ColumnProperty> = mutableListOf()
        val lazyColumns: MutableList<ColumnProperty> = mutableListOf()
        val relations: MutableList<ColumnProperty> = mutableListOf()

        for (p in propertyDeclarations) {
          val propertyName = p.simpleName.asString()
          val annotations = resolveAnnotations(p.annotations)
          val type = p.type.resolve().resolveTypeAlias()

          val entityProperty = EntityProperty(p, type, annotations)
          properties.add(entityProperty)

          if (annotations.hasAnnotation(ANNOTATION_TRANSIENT)) continue

          jpaProperties.add(entityProperty)

          val innerValue = getInnerValue(type)
          val hasId = annotations.hasAnnotation(ANNOTATION_ID)
          val hasEmbeddedId = annotations.hasAnnotation(ANNOTATION_EMBEDDED_ID)
          if (hasId || hasEmbeddedId) {
            val isGenerated = annotations.hasAnnotation(ANNOTATION_GENERATED_VALUE)
            val embedded =
              if (hasEmbeddedId) getEmbeddedInfo(type, visitedEmbedded, duplicateEmbeddedNames)
              else null
            idP = IdProperty(p, type, annotations, innerValue, isGenerated, embedded)
            continue
          }

          val isEmbedded = annotations.hasAnnotation(ANNOTATION_EMBEDDED)
          val embedded =
            if (isEmbedded) getEmbeddedInfo(type, visitedEmbedded, duplicateEmbeddedNames) else null
          val columnProperty = ColumnProperty(p, type, annotations, innerValue, embedded)
          val hasRelation = annotations.hasAnnotation(ANNOTATIONS_RELATION)
          if (hasRelation) {
            if (!type.isMarkedNullable) {
              throw IllegalArgumentException(
                "Relation [$propertyName] of [$entityName] must be marked nullable"
              )
            }
            relations.add(columnProperty)
            continue
          }

          if (annotations.hasAnnotation(ANNOTATION_LAZY_FIELD)) lazyColumns.add(columnProperty)
          else columns.add(columnProperty)
        }

        EntityInfo(
          declaration = entityDeclaration,
          uniqueName = uniqueName,
          annotations = entityAnnotations,
          properties = properties,
          jpaProperties = jpaProperties,
          id = idP,
          columns = columns,
          lazyColumns = lazyColumns,
          relations = relations,
          superClass = null,
          subClasses = null,
        )
      }

    // Add inheritance info
    val inheritedEntities: MutableMap<FullyQualifiedName, EntityInfo> =
      entitiesWithoutInheritanceInfo.associateByTo(HashMap()) { it.qualifiedName }

    for (entityInfo in entitiesWithoutInheritanceInfo) {
      if (!entityInfo.annotations.hasAnnotation(ANNOTATION_INHERITANCE)) continue

      // Store subclasses for superclass
      val subClasses = entitiesWithoutInheritanceInfo.findSubclasses(entityInfo)
      val subClassNames = subClasses.mapTo(HashSet()) { it.qualifiedName }
      inheritedEntities[entityInfo.qualifiedName] = entityInfo.copy(subClasses = subClassNames)

      // Store superclass for subclass
      // Also set inherited id
      val superClassName = entityInfo.qualifiedName
      for (subClass in subClasses) {
        val columnsWithoutId = subClass.columns.filter { it.simpleName != entityInfo.id.simpleName }
        inheritedEntities[subClass.qualifiedName] =
          subClass.copy(id = entityInfo.id, columns = columnsWithoutId, superClass = superClassName)
      }
    }

    // Debug
    for (entityInfo in inheritedEntities.values) {
      // logger.info(entityInfo.toString())
      logger.info(entityInfo.simpleName)
      logger.info(entityInfo.id.toString())
      logger.info(entityInfo.columns.toString())
      logger.info(entityInfo.relations.toString())
      logger.info("${entityInfo.superClass} X ${entityInfo.subClasses}")
      logger.info("---")
    }

    // Validate
    if (inheritedEntities.values.isEmpty()) {
      logger.info("No entities found. Stopping codegen betimes.")
      return
    }
    for (entityInfo in inheritedEntities.values) {
      if (entityInfo.id === IdProperty.PLACEHOLDER) {
        throw IllegalArgumentException("Entity [${entityInfo.simpleName}] has no ID")
      }
    }

    val packageName = input.options.getOption(BeePersistentBlazeOptions.packageName)
    val viewPackageName =
      input.options.getOrDefault(BeePersistentBlazeOptions.subPackageView, "views").let {
        "$packageName.$it"
      }
    val repositoryPackageName =
      input.options
        .getOrDefault(BeePersistentBlazeOptions.subPackageRepository, "repositories")
        .let { "$packageName.$it" }
    val dlsPackageName =
      input.options.getOrDefault(BeePersistentBlazeOptions.subPackageDSL, "dsl").let {
        "$packageName.$it"
      }
    val builderPackageName =
      input.options.getOrDefault(BeePersistentBlazeOptions.subPackageBuilder, "builder").let {
        "$packageName.$it"
      }
    val depth = input.options.getOrDefault(BeePersistentBlazeOptions.depth, "2").toInt()

    val config =
      BeePersistentBlazeConfig(
        packageName,
        depth,
        viewPackageName,
        repositoryPackageName,
        dlsPackageName,
        builderPackageName,
      )
    val resources = ResourcesCodegen(input.codeGenerator)

    // Process
    val entities = inheritedEntities.values.toList()
    val analyser = BeePersistentAnalyser(input.logger, config)
    val views = analyser.processEntities(entities)

    val viewCodeGen =
      BeePersistentViewCodegen(
        input.codeGenerator,
        input.dependencies,
        input.logger,
        inheritedEntities.values.toList(),
        config,
      )

    viewCodeGen.processViews(views)

    val instantiatorCodegen =
      BeePersistentInstantiatorCodegen(
        input.codeGenerator,
        resources,
        input.dependencies,
        input.logger,
        config,
      )
    instantiatorCodegen.processViews(views, entities)

    val repoCodeGen =
      BeePersistentRepoCodegen(
        input.codeGenerator,
        input.dependencies,
        input.logger,
        inheritedEntities,
        views,
        config,
      )
    val repos = getRepoInfo(symbols)
    for (repo in repos) {
      repoCodeGen.processRepo(repo)
    }

    val builderCodegen =
      BeePersistentBuilderCodegen(
        input.codeGenerator,
        input.dependencies,
        input.logger,
        views,
        inheritedEntities,
        config,
      )
    builderCodegen.processRepoBuilder(repos)

    val dslCodegen =
      BeePersistentDSLCodegen(
        input.codeGenerator,
        resources,
        input.dependencies,
        input.logger,
        inheritedEntities.values.toList(),
        views,
        config,
      )
    dslCodegen.processRepoDSL(repos)

    resources.process()
  }

  private fun duplicateSimpleNames(entities: Set<KSClassDeclaration>): Set<String> {
    val processed = mutableSetOf<String>()
    val duplicates = mutableSetOf<String>()

    for (entityDeclaration in entities) {
      val simpleName = entityDeclaration.simpleName.asString()
      if (!processed.contains(simpleName)) processed.add(simpleName) else duplicates.add(simpleName)
    }
    return duplicates
  }

  private fun resolveAnnotations(annotations: Sequence<KSAnnotation>): List<ResolvedAnnotation> {
    return annotations
      .map { a ->
        val type = a.annotationType.resolve().resolveTypeAlias()
        val declaration = type.declaration
        ResolvedAnnotation(a, declaration, type)
      }
      .toList()
  }

  private fun List<ResolvedAnnotation>.hasAnnotation(annotation: String): Boolean {
    return any { it.qualifiedName == annotation }
  }

  private fun List<ResolvedAnnotation>.hasAnnotation(annotations: Set<String>): Boolean {
    return any { annotations.contains(it.qualifiedName) }
  }

  private fun getInnerValue(type: KSType): ResolvedValue? {
    val declaration = type.declaration
    if (
      declaration !is KSClassDeclaration ||
        !declaration.annotations.any { it.shortName.asString() == "JvmInline" }
    )
      return null

    val innerType =
      declaration.primaryConstructor?.parameters?.firstOrNull()?.type?.resolve()?.resolveTypeAlias()
        ?: return null

    val innerDeclaration = innerType.declaration
    return ResolvedValue(innerDeclaration, innerType)
  }

  private fun List<EntityInfo>.findSubclasses(superEntity: EntityInfo): Set<EntityInfo> {
    val subClasses = mutableSetOf<EntityInfo>()
    for (entity in this) {
      if (entity === superEntity) continue
      for (superType in entity.declaration.getAllSuperTypes()) {
        if (superType.declaration.qualifiedName?.asString() == superEntity.qualifiedName) {
          subClasses.add(entity)
        }
      }
    }
    return subClasses
  }

  private fun getEmbeddedInfo(
    eType: KSType,
    visitedEmbedded: MutableMap<String, EmbeddedInfo>,
    duplicateEmbeddedNames: MutableSet<String>,
  ): EmbeddedInfo {
    val declaration = eType.declaration as KSClassDeclaration
    val qualifiedName = requireNotNull(declaration.qualifiedName).asString()

    if (visitedEmbedded.containsKey(qualifiedName)) return visitedEmbedded.getValue(qualifiedName)

    val embedded = getEmbeddedInfo(declaration, duplicateEmbeddedNames)
    visitedEmbedded[qualifiedName] = embedded

    return embedded
  }

  private fun getEmbeddedInfo(
    declaration: KSClassDeclaration,
    duplicateEmbeddedNames: MutableSet<String>,
  ): EmbeddedInfo {
    val embeddedName = declaration.simpleName.asString()
    val uniqueName =
      if (!duplicateEmbeddedNames.contains(embeddedName)) embeddedName
      else buildUniqueClassName(declaration.packageName.asString(), embeddedName)

    val propertyDeclarations = declaration.getAllProperties().filter { it.hasBackingField }.toList()

    val properties: MutableList<EntityProperty> = mutableListOf()
    val jpaProperties: MutableList<EntityProperty> = mutableListOf()
    val columns: MutableList<ColumnProperty> = mutableListOf()
    val lazyColumns: MutableList<ColumnProperty> = mutableListOf()

    for (p in propertyDeclarations) {
      val annotations = resolveAnnotations(p.annotations)
      val type = p.type.resolve().resolveTypeAlias()
      val entityProperty = EntityProperty(p, type, annotations)
      properties.add(entityProperty)

      if (annotations.hasAnnotation(ANNOTATION_TRANSIENT)) continue
      jpaProperties.add(entityProperty)

      val columnProperty = ColumnProperty(p, type, annotations, null, null)
      if (annotations.hasAnnotation(ANNOTATION_LAZY_FIELD)) lazyColumns.add(columnProperty)
      else columns.add(columnProperty)
    }

    return EmbeddedInfo(
      declaration = declaration,
      uniqueName = uniqueName,
      properties = properties,
      jpaProperties = jpaProperties,
      columns = columns,
      lazyColumns = lazyColumns,
    )
  }

  private fun getRepoInfo(symbols: BeeGenerativeSymbols): List<RepoInfo> {
    val config = getRepoConfig(symbols)
    logger.info("RepoConfig: $config")

    val repoDeclarations = symbols.annotatedBy[ANNOTATION_BEE_REPOSITORY] ?: return emptyList()

    return repoDeclarations.map { repoDeclaration -> getRepoInfo(repoDeclaration, config) }
  }

  private val repoInterface = BeeBlazeRepository::class.qualifiedName!!

  private fun getRepoInfo(repoDeclaration: KSClassDeclaration, config: RepoConfig?): RepoInfo {
    var ksType: KSType? = null
    for (superT in repoDeclaration.superTypes) {
      val full = superT.resolve()
      if (full.declaration.qualifiedName?.asString() == repoInterface) {
        ksType = full
        break
      }
    }
    if (ksType == null) {
      val name = repoDeclaration.qualifiedName?.asString()
      throw IllegalArgumentException("Repository [$name] does not implement [BeeBlazeRepository].")
    }
    val typeArguments = ksType.arguments

    val entityType = requireNotNull(typeArguments[0].type).resolve().resolveTypeAlias()
    val idType = requireNotNull(typeArguments[1].type).resolve().resolveTypeAlias()
    logger.info("generics: entity $entityType, id $idType")

    val pack = entityType.declaration.packageName.asString()
    val applyConfig =
      if (config != null && config.basePackages.any { pack.startsWith(it) }) config else null

    return RepoInfo(repoDeclaration, entityType, idType, applyConfig)
  }

  private fun getRepoConfig(symbols: BeeGenerativeSymbols): RepoConfig? {
    val configs = symbols.annotatedBy[ANNOTATION_ENABLE_BEE_REPOSITORIES]
    if (configs != null && configs.count() > 1)
      logger.warn("Multiple [EnableBeeRepositories] annotations found. Possible misconfiguration.")
    val configClass = configs?.firstOrNull() ?: return null

    val annotation =
      configClass.annotations.firstOrNull { a ->
        val aType = a.annotationType.resolve()
        val name = aType.declaration.qualifiedName?.asString()
        name == ANNOTATION_ENABLE_BEE_REPOSITORIES
      } ?: return null

    val props = mutableMapOf<String, Any>()
    for (arg in annotation.arguments) {
      val propName = arg.name?.asString() ?: ""
      props[propName] = requireNotNull(arg.value)
    }

    @Suppress("UNCHECKED_CAST")
    return RepoConfig(
      basePackages = props["basePackages"] as ArrayList<String>,
      entityManagerFactoryRef = props["entityManagerFactoryRef"] as String,
      criteriaBuilderFactoryRef = props["criteriaBuilderFactoryRef"] as String,
      entityViewManagerRef = props["entityViewManagerRef"] as String,
    )
  }
}
