package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.PoetMap.Companion.addNStatementBuilder
import com.beeproduced.bee.generative.util.toPoetClassName
import com.beeproduced.bee.persistent.blaze.processor.FullyQualifiedName
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentAnalyser.Companion.viewName
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentBuilderCodegen.Companion.infoField
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentBuilderCodegen.Companion.infoName
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.AUTOWIRED
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.BEE_SELECTION
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.CLAZZ
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.CRITERIA_BUILDER_FACTORY
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.DEFAULT_BEE_SELECTION
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.ENTITY_MANAGER
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.ENTITY_VIEW_MANAGER
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.ENTITY_VIEW_SETTING
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.QUALIFIER
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.SELECTION
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.SELECTION_INFO
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.SELECT_QUERY
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.SELECT_QUERY_BUILDER
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.TYPED_FIELD_NODE
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.VIEW_CLAZZ
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._BEE_CLONE_FN
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._CBF_PROP
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._CLAZZ_PROPERTY
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._EM_PROP
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._EVM_PROP
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._FETCH_SELECTION_FN
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._SELECTION_INFO_VAL
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._UNWRAP_FN
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._VIEW_CLAZZ_PROPERTY
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.__SELECTION_INFO_NAME
import com.beeproduced.bee.persistent.blaze.processor.info.*
import com.beeproduced.bee.persistent.blaze.processor.utils.reflectionGetterName
import com.beeproduced.bee.persistent.blaze.processor.utils.viewLazyColumnsWithSubclasses
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * @author Kacper Urbaniec
 * @version 2023-12-28
 */
class BeePersistentRepoCodegen(
  private val codeGenerator: CodeGenerator,
  private val dependencies: Dependencies,
  private val logger: KSPLogger,
  private val entities: Map<FullyQualifiedName, EntityInfo>,
  private val views: ViewInfo,
  private val config: BeePersistentBlazeConfig,
) {
  private val packageName = config.repositoryPackageName
  private val viewPackageName = config.viewPackageName
  private lateinit var className: String

  private lateinit var repoInterface: KSClassDeclaration
  private lateinit var view: EntityViewInfo
  private lateinit var entity: EntityInfo
  private var repoConfig: RepoConfig? = null

  private val poetMap: PoetMap = PoetMap()

  private fun FunSpec.Builder.addNamedStmt(format: String) = addNStatementBuilder(format, poetMap)

  private fun CodeBlock.Builder.addNamedStmt(format: String) = addNStatementBuilder(format, poetMap)

  @Suppress("ConstPropertyName")
  object PoetConstants {
    const val CLAZZ = "%clazz:T"
    const val _CLAZZ_PROPERTY = "%clazzproperty:L"
    const val VIEW_CLAZZ = "%viewclazz:T"
    const val _VIEW_CLAZZ_PROPERTY = "%viewclazzproperty:L"
    const val _ID_PROPERTY = "%idProperty:L"
    const val BEE_SELECTION = "%beeselection:T"
    const val SELECTION_INFO = "%selectioninfo:T"
    const val _SELECTION_INFO_VAL = "%selectioninfoval:L"
    const val __SELECTION_INFO_NAME = "%selectioninfoname:S"
    const val ENTITY_MANAGER = "%entitymanager:T"
    const val _EM_PROP = "%entitymanagerprop:L"
    const val CRITERIA_BUILDER_FACTORY = "%criteriabuilderfactory:T"
    const val _CBF_PROP = "%criteriabuilderfactoryprop:L"
    const val ENTITY_VIEW_MANAGER = "%entityviewmanager:T"
    const val _EVM_PROP = "%entityviewmanagerprop:L"
    const val AUTOWIRED = "%autowired:T"
    const val QUALIFIER = "%qualifier:T"
    const val _FETCH_SELECTION_FN = "%fetchselectionfn:M"
    const val ENTITY_VIEW_SETTING = "%entityviewsetting:T"
    const val TYPED_FIELD_NODE = "%typedfieldnode:T"
    const val DEFAULT_BEE_SELECTION = "%default_bee_selection:T"
    const val SELECT_QUERY = "%select_query:T"
    const val SELECT_QUERY_BUILDER = "%select_query_builder:T"
    const val SELECTION = "%selection:T"
    const val _BEE_CLONE_FN = "%beeclone:M"
    const val _UNWRAP_FN = "%unwrap:M"
  }

  init {
    poetMap.addMapping(_CLAZZ_PROPERTY, "clazz")
    poetMap.addMapping(_VIEW_CLAZZ_PROPERTY, "viewClazz")
    poetMap.addMapping(
      BEE_SELECTION,
      ClassName("com.beeproduced.bee.persistent.blaze.selection", "BeeSelection"),
    )
    poetMap.addMapping(
      SELECTION_INFO,
      ClassName("com.beeproduced.bee.persistent.blaze.meta", "SelectionInfo"),
    )
    poetMap.addMapping(ENTITY_MANAGER, ClassName("jakarta.persistence", "EntityManager"))
    poetMap.addMapping(_EM_PROP, "em")
    poetMap.addMapping(
      CRITERIA_BUILDER_FACTORY,
      ClassName("com.blazebit.persistence", "CriteriaBuilderFactory"),
    )
    poetMap.addMapping(_CBF_PROP, "cbf")
    poetMap.addMapping(
      ENTITY_VIEW_MANAGER,
      ClassName("com.blazebit.persistence.view", "EntityViewManager"),
    )
    poetMap.addMapping(_EVM_PROP, "evm")
    poetMap.addMapping(
      AUTOWIRED,
      ClassName("org.springframework.beans.factory.annotation", "Autowired"),
    )
    poetMap.addMapping(
      QUALIFIER,
      ClassName("org.springframework.beans.factory.annotation", "Qualifier"),
    )
    poetMap.addMapping(
      _FETCH_SELECTION_FN,
      MemberName("com.beeproduced.bee.persistent.blaze.repository", "fetchSelection"),
    )
    poetMap.addMapping(
      ENTITY_VIEW_SETTING,
      ClassName("com.blazebit.persistence.view", "EntityViewSetting"),
    )
    poetMap.addMapping(
      TYPED_FIELD_NODE,
      ClassName(
        "com.beeproduced.bee.persistent.blaze.selection.DefaultBeeSelection",
        "TypedFieldNode",
      ),
    )
    poetMap.addMapping(
      DEFAULT_BEE_SELECTION,
      ClassName("com.beeproduced.bee.persistent.blaze.selection", "DefaultBeeSelection"),
    )
    poetMap.addMapping(
      SELECT_QUERY,
      ClassName("com.beeproduced.bee.persistent.blaze.dsl.select", "SelectQuery"),
    )
    poetMap.addMapping(
      SELECT_QUERY_BUILDER,
      ClassName("com.beeproduced.bee.persistent.blaze.dsl.select", "SelectQueryBuilder"),
    )
    poetMap.addMapping(
      SELECTION,
      ClassName("com.beeproduced.bee.persistent.blaze.dsl.select", "Selection"),
    )
    poetMap.addMapping(
      _BEE_CLONE_FN,
      MemberName(config.builderPackageName, "beeClone", isExtension = true),
    )
    poetMap.addMapping(
      _UNWRAP_FN,
      MemberName(
        ClassName("com.beeproduced.bee.persistent.blaze.meta.dsl", "InlineValueUnwrappers"),
        "unwrap",
      ),
    )
  }

  fun processRepo(repo: RepoInfo) {
    logger.info("processRepo($repo)")
    repoInterface = repo.repoInterface
    view = findView(repo)
    entity = view.entity
    repoConfig = repo.config
    className = "Generated${entity.uniqueName}BlazeRepository"
    poetMap.addMapping(CLAZZ, entity.qualifiedName.toPoetClassName())
    poetMap.addMapping(VIEW_CLAZZ, "${viewPackageName}.${view.name}".toPoetClassName())

    FileSpec.builder(packageName, className)
      .buildRepo()
      .build()
      .writeTo(codeGenerator, dependencies)
  }

  private fun FileSpec.Builder.buildRepo(): FileSpec.Builder = apply {
    addType(
      TypeSpec.classBuilder(className)
        .addModifiers(KModifier.OPEN)
        .addAnnotation(ClassName("org.springframework.stereotype", "Component"))
        .addSuperinterface(repoInterface.toClassName())
        .buildConstructor()
        .addRepoProperties()
        .buildPersist()
        .buildUpdate()
        .buildSelect()
        .buildSelectById()
        .buildSelectionInfo()
        .build()
    )
  }

  private fun TypeSpec.Builder.buildConstructor(): TypeSpec.Builder = apply {
    val emProp =
      PropertySpec.builder(poetMap.literalMapping(_EM_PROP), poetMap.classMapping(ENTITY_MANAGER))
        .initializer(poetMap.literalMapping(_EM_PROP))
        .addModifiers(KModifier.OVERRIDE)
        .build()
    val cbfProp =
      PropertySpec.builder(
          poetMap.literalMapping(_CBF_PROP),
          poetMap.classMapping(CRITERIA_BUILDER_FACTORY),
        )
        .initializer(poetMap.literalMapping(_CBF_PROP))
        .addModifiers(KModifier.OVERRIDE)
        .build()
    val evmProp =
      PropertySpec.builder(
          poetMap.literalMapping(_EVM_PROP),
          poetMap.classMapping(ENTITY_VIEW_MANAGER),
        )
        .initializer(poetMap.literalMapping(_EVM_PROP))
        .addModifiers(KModifier.OVERRIDE)
        .build()
    addProperties(listOf(emProp, cbfProp, evmProp))

    val emParam =
      ParameterSpec.builder(poetMap.literalMapping(_EM_PROP), poetMap.classMapping(ENTITY_MANAGER))
        .apply {
          val annotation = buildConstructorAnnotation { entityManagerFactoryRef }
          addAnnotation(annotation)
        }
        .build()
    val cbfParam =
      ParameterSpec.builder(
          poetMap.literalMapping(_CBF_PROP),
          poetMap.classMapping(CRITERIA_BUILDER_FACTORY),
        )
        .apply {
          val annotation = buildConstructorAnnotation { criteriaBuilderFactoryRef }
          addAnnotation(annotation)
        }
        .build()
    val evmParam =
      ParameterSpec.builder(
          poetMap.literalMapping(_EVM_PROP),
          poetMap.classMapping(ENTITY_VIEW_MANAGER),
        )
        .apply {
          val annotation = buildConstructorAnnotation { entityViewManagerRef }
          addAnnotation(annotation)
        }
        .build()
    val constructorBuilder = FunSpec.constructorBuilder()
    constructorBuilder.addParameters(listOf(emParam, cbfParam, evmParam))
    primaryConstructor(constructorBuilder.build())
  }

  private fun buildConstructorAnnotation(selector: RepoConfig.() -> String): AnnotationSpec {
    if (repoConfig == null) return AnnotationSpec.builder(poetMap.classMapping(AUTOWIRED)).build()
    val annotationValue = repoConfig!!.selector()
    return AnnotationSpec.builder(poetMap.classMapping(QUALIFIER))
      .addMember("\"$annotationValue\"")
      .build()
  }

  private fun TypeSpec.Builder.addRepoProperties(): TypeSpec.Builder = apply {
    val clazzProperty =
      PropertySpec.builder(
          poetMap.literalMapping(_CLAZZ_PROPERTY),
          ClassName("java.lang", "Class").parameterizedBy(poetMap.classMapping(CLAZZ)),
        )
        .addModifiers(KModifier.PROTECTED)
        .initializer("%T::class.java", poetMap.classMapping(CLAZZ))
        .build()
    val viewClazzProperty =
      PropertySpec.builder(
          poetMap.literalMapping(_VIEW_CLAZZ_PROPERTY),
          ClassName("java.lang", "Class").parameterizedBy(poetMap.classMapping(VIEW_CLAZZ)),
        )
        .addModifiers(KModifier.PROTECTED)
        .initializer("%T::class.java", poetMap.classMapping(VIEW_CLAZZ))
        .build()

    addProperties(listOf(clazzProperty, viewClazzProperty))
  }

  private fun EntityInfo.defaultIdLiteral(): String {
    if (!id.isGenerated) return ""
    val type = id.type
    if (type.isMarkedNullable) return "null"
    val typeName =
      if (id.innerValue != null) id.innerValue.type.declaration.simpleName.asString()
      else type.declaration.simpleName.asString()
    return when (typeName) {
      "Double" -> "default.${id.simpleName}"
      "Float" -> "default.${id.simpleName}"
      "Long" -> "default.${id.simpleName}"
      "Int",
      "Short",
      "Byte" -> "default.${id.simpleName}"
      "Boolean" -> "default.${id.simpleName}"
      "Char" -> "default.${id.simpleName}"
      else -> "null"
    }
  }

  private fun TypeSpec.Builder.buildPersist(): TypeSpec.Builder = apply {
    val typeVariable = TypeVariableName("E", poetMap.classMapping(CLAZZ))
    val persistFun =
      FunSpec.builder("persist")
        .addModifiers(KModifier.OVERRIDE)
        .addTypeVariable(typeVariable)
        .addParameter("entity", typeVariable)
        .returns(typeVariable)
    val idLiteral = entity.defaultIdLiteral()

    if (entity.subClasses == null) {
      buildDefault(entity, idLiteral)
      persistFun.buildPersistBlock(entity, idLiteral)
    } else {
      val subEntities = entity.subClasses!!.map { entities.getValue(it) }
      buildDefault(subEntities.first(), idLiteral)
      for (subEntity in subEntities) {
        persistFun.addStatement("if (entity is %T) {", subEntity.declaration.toClassName())
        persistFun.buildPersistBlock(subEntity, idLiteral, padding = "  ")
        persistFun.addStatement("}")
      }
      persistFun.addStatement(
        "throw Exception(\"Unknown entity [\${entity.javaClass.canonicalName}]\")"
      )
    }
    persistFun.apply {}

    addFunction(persistFun.build())
  }

  private fun TypeSpec.Builder.buildDefault(e: EntityInfo, idLiteral: String) {
    if (!idLiteral.startsWith("default.")) return
    val defaultProperty =
      PropertySpec.builder("default", poetMap.classMapping(CLAZZ))
        .addModifiers(KModifier.PROTECTED)
        .initializer("%T::class.java.getConstructor().newInstance()", e.declaration.toClassName())
    addProperty(defaultProperty.build())
  }

  private fun FunSpec.Builder.buildPersistBlock(
    e: EntityInfo,
    idLiteral: String,
    padding: String = "",
  ) = apply {
    val idAssignment = if (idLiteral.startsWith("default.")) "${e.id.simpleName}=$idLiteral" else ""
    addNamedStmt("${padding}val e = entity.$_BEE_CLONE_FN { $idAssignment }")
    // TODO: Integrate into builder breaking nullability rules?
    if (idLiteral == "null") {
      val infoObjName = e.infoName()
      val infoField = e.id.infoField()
      val infoObjClass = ClassName(config.builderPackageName, infoObjName)
      addStatement("${padding}%T.${infoField}.set(e, null)", infoObjClass)
    }
    addNamedStmt("${padding}em.persist(e)")
    addNamedStmt("${padding}em.flush()")
    addNamedStmt("${padding}em.clear()")
    addNamedStmt("${padding}@Suppress(\"UNCHECKED_CAST\")")

    if (e.relations.isEmpty()) {
      addStatement("${padding}return e as E")
      return@apply
    }

    addNamedStmt("${padding}return e.$_BEE_CLONE_FN { ")
    for (relation in e.relations) {
      addStatement("$padding  ${relation.simpleName}=null")
    }
    addStatement("$padding} as E")
  }

  private fun TypeSpec.Builder.buildUpdate(): TypeSpec.Builder = apply {
    val typeVariable = TypeVariableName("E", poetMap.classMapping(CLAZZ))
    val persistFun =
      FunSpec.builder("update")
        .addModifiers(KModifier.OVERRIDE)
        .addTypeVariable(typeVariable)
        .addParameter("entity", typeVariable)
        .returns(typeVariable)
    if (entity.subClasses == null) {
      persistFun.buildUpdateBlock(entity)
    } else {
      val subEntities = entity.subClasses!!.map { entities.getValue(it) }
      for (subEntity in subEntities) {
        persistFun.addStatement("if (entity is %T) {", subEntity.declaration.toClassName())
        persistFun.buildUpdateBlock(subEntity, padding = "  ")
        persistFun.addStatement("}")
      }
      persistFun.addStatement(
        "throw Exception(\"Unknown entity [\${entity.javaClass.canonicalName}]\")"
      )
    }
    persistFun.apply {}

    addFunction(persistFun.build())
  }

  private fun FunSpec.Builder.buildUpdateBlock(e: EntityInfo, padding: String = "") = apply {
    val eClassName = e.declaration.toClassName()
    val columns = e.columns + e.lazyColumns
    if (columns.isEmpty()) {
      addStatement("${padding}return entity")
      return@apply
    }
    addStatement("${padding}cbf.update(em, %T::class.java)", eClassName)
    val infoObjName = e.infoName()
    val infoObjClass = ClassName(config.builderPackageName, infoObjName)
    for (column in columns) {
      val simpleName = column.simpleName
      if (column.isAccessible()) {
        val inner = column.innerValue
        if (inner == null) addStatement("$padding  .set(\"$simpleName\", entity.$simpleName)")
        else {
          val qualifiedName = column.qualifiedName
          val unwrapFn = poetMap.mappings.getValue(_UNWRAP_FN)
          val columnType = column.type.toTypeName()
          val innerType = inner.type.toTypeName()
          addStatement(
            "$padding  .set(\"$simpleName\", %M<%T, %T>(entity.$simpleName, \"$qualifiedName\"))",
            unwrapFn,
            columnType,
            innerType,
          )
        }
      } else {
        val infoField = column.infoField()
        val getterName = column.type.reflectionGetterName()
        addStatement(
          "$padding  .set(\"$simpleName\", %T.${infoField}.${getterName}(entity))",
          infoObjClass,
        )
      }
    }
    val idSimpleName = e.id.simpleName
    addStatement("$padding  .where(\"$idSimpleName\").eq(entity.$idSimpleName)")
    addStatement("$padding  .executeUpdate()")
    addStatement("${padding}return entity")
  }

  private fun TypeSpec.Builder.buildSelectById(): TypeSpec.Builder = apply {
    val entityClassName = entity.declaration.toClassName()
    val idClassname = entity.id.type.toTypeName()
    val dslClassname = ClassName(config.dslPackageName, "${entity.simpleName}DSL")
    val idName = entity.id.simpleName

    val selectFN =
      FunSpec.builder("selectById")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("id", idClassname)
        .addParameter("selection", poetMap.classMapping(BEE_SELECTION))
        .returns(entityClassName.copy(nullable = true))
    selectFN.apply {
      addNamedStmt("return select(selection) {")
      val eqStmt = if (!entity.id.isValueClass) "eq" else "eqInline"
      addStatement("  where(%T.$idName.$eqStmt(id))", dslClassname)
      addNamedStmt("}.firstOrNull()")
    }

    addFunction(selectFN.build())
  }

  private fun TypeSpec.Builder.buildSelect(): TypeSpec.Builder = apply {
    val entityClassName = entity.declaration.toClassName()
    val listOfEntity = ClassName("kotlin.collections", "List").parameterizedBy(entityClassName)
    val selectQueryClassName = poetMap.classMapping(SELECT_QUERY).parameterizedBy(entityClassName)
    val selectionClassName = poetMap.classMapping(SELECTION).parameterizedBy(entityClassName)
    val dsl = LambdaTypeName.get(receiver = selectQueryClassName, returnType = selectionClassName)
    val selectFn =
      FunSpec.builder("select")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("selection", poetMap.classMapping(BEE_SELECTION))
        .addParameter("dsl", dsl)
        .returns(listOfEntity)
    selectFn.apply {
      addNamedStmt("val setting = $ENTITY_VIEW_SETTING.create($_VIEW_CLAZZ_PROPERTY)")
      addNamedStmt("  .apply { $_FETCH_SELECTION_FN(selectionInfo, selection) }")
      addNamedStmt("val builder = $_CBF_PROP.create($_EM_PROP, $_CLAZZ_PROPERTY).let { builder ->")
      addNamedStmt("  val selectQuery = $SELECT_QUERY_BUILDER<$CLAZZ>()")
      addNamedStmt("  selectQuery.dsl()")
      addNamedStmt("  selectQuery.applyBuilder(builder)")
      addNamedStmt("}")
      addNamedStmt("val query = $_EVM_PROP.applySetting(setting, builder)")
      addNamedStmt("@Suppress(\"UNCHECKED_CAST\")")
      addStatement("return query.resultList as %T", listOfEntity)
    }
    addFunction(selectFn.build())
  }

  private fun findView(repo: RepoInfo): EntityViewInfo {
    val qualifiedName = repo.entityType.declaration.qualifiedName?.asString()
    return views.coreEntityViewsByQualifiedName[qualifiedName]
      ?: throw IllegalArgumentException("No view for [$qualifiedName] found")
  }

  private fun TypeSpec.Builder.buildSelectionInfo(): TypeSpec.Builder = apply {
    val initializerBlock = CodeBlock.builder()
    initializerBlock.addNamedStmt("run {")
    initializerBlock.traverseSelectionInfo(view)
    initializerBlock.addNamedStmt("  ${view.name.lowercase()}")
    initializerBlock.addNamedStmt("}")

    val selectionInfoProperty =
      PropertySpec.builder("selectionInfo", poetMap.classMapping(SELECTION_INFO))
        .initializer(initializerBlock.build())
        .build()
    val companionObject =
      TypeSpec.companionObjectBuilder().addProperty(selectionInfoProperty).build()
    addType(companionObject)
  }

  private fun CodeBlock.Builder.traverseSelectionInfo(
    entityView: EntityViewInfo,
    visitedEmbeddedViews: MutableSet<String> = mutableSetOf(),
  ): CodeBlock.Builder = apply {
    val entity = entityView.entity

    val (allRelations, allColumns, allLazyColumns) =
      viewLazyColumnsWithSubclasses(entityView, views)

    val relationMapInput = allRelations.map { (simpleName, viewName) -> Pair(simpleName, viewName) }
    val relationSI = traverseSubSelectionInfo(relationMapInput, visitedEmbeddedViews)

    val idSI = "\"${entity.id.simpleName}\""
    val (columns, embedded) = allColumns.partition { !it.isEmbedded }
    val columnSI = selectionInfoListValues(columns)
    val embeddedMapInput =
      embedded.map {
        val viewName = viewName(requireNotNull(it.embedded))
        Pair(it.simpleName, viewName)
      }
    val embeddedSI = traverseSubEmbeddedInfo(embeddedMapInput, visitedEmbeddedViews)

    val (lazyColumns, lazyEmbedded) = allLazyColumns.partition { !it.isEmbedded }
    val lazyColumnSI = selectionInfoListValues(lazyColumns)
    val lazyEmbeddedMapInput =
      lazyEmbedded.map {
        val viewName = viewName(requireNotNull(it.embedded))
        Pair(it.simpleName, viewName)
      }
    val lazyEmbeddedSI = traverseSubEmbeddedInfo(lazyEmbeddedMapInput, visitedEmbeddedViews)

    poetMap.addMapping(_SELECTION_INFO_VAL, entityView.name.lowercase())
    poetMap.addMapping(__SELECTION_INFO_NAME, entityView.name)
    addNamedStmt("  val $_SELECTION_INFO_VAL = $SELECTION_INFO(")
    addNamedStmt("    view = $__SELECTION_INFO_NAME,")
    addNamedStmt("    relations = mapOf($relationSI),")
    addNamedStmt("    id = $idSI,")
    addNamedStmt("    columns = setOf($columnSI),")
    addNamedStmt("    lazyColumns = setOf($lazyColumnSI),")
    addNamedStmt("    embedded = mapOf($embeddedSI),")
    addNamedStmt("    lazyEmbedded = mapOf($lazyEmbeddedSI)")
    addNamedStmt("  )")
  }

  private fun CodeBlock.Builder.traverseEmbeddedInfo(
    embeddedView: EmbeddedViewInfo
  ): CodeBlock.Builder = apply {
    val embedded = embeddedView.embedded
    val columnSI = selectionInfoListValues(embedded.columns)
    val lazyColumnSI = selectionInfoListValues(embedded.lazyColumns)

    poetMap.addMapping(_SELECTION_INFO_VAL, embeddedView.name.lowercase())
    poetMap.addMapping(__SELECTION_INFO_NAME, embeddedView.name)
    addNamedStmt("  val $_SELECTION_INFO_VAL = $SELECTION_INFO(")
    addNamedStmt("    view = $__SELECTION_INFO_NAME,")
    addNamedStmt("    relations = emptyMap(),")
    addNamedStmt("    id = null,")
    addNamedStmt("    columns = setOf($columnSI),")
    addNamedStmt("    lazyColumns = setOf($lazyColumnSI),")
    addNamedStmt("    embedded = emptyMap(),")
    addNamedStmt("    lazyEmbedded = emptyMap()")
    addNamedStmt("  )")
  }

  private fun CodeBlock.Builder.traverseSubSelectionInfo(
    simpleNameAndViewName: List<Pair<String, String>>,
    visitedEmbeddedViews: MutableSet<String>,
  ): String {
    for ((_, viewName) in simpleNameAndViewName) {
      val view = views.entityViews.getValue(viewName)
      traverseSelectionInfo(view, visitedEmbeddedViews)
    }
    return simpleNameAndViewName.joinToString(separator = ", ") { (p, v) ->
      "\"$p\" to ${v.lowercase()}"
    }
  }

  private fun CodeBlock.Builder.traverseSubEmbeddedInfo(
    simpleNameAndViewName: List<Pair<String, String>>,
    visitedEmbeddedViews: MutableSet<String>,
  ): String {
    for ((_, viewName) in simpleNameAndViewName) {
      if (visitedEmbeddedViews.contains(viewName)) continue
      visitedEmbeddedViews.add(viewName)
      val view = views.embeddedViews.getValue(viewName)
      traverseEmbeddedInfo(view)
    }
    return simpleNameAndViewName.joinToString(separator = ", ") { (p, v) ->
      "\"$p\" to ${v.lowercase()}"
    }
  }

  private fun selectionInfoListValues(columnProperties: List<ColumnProperty>): String =
    columnProperties.joinToString(separator = ", ") { "\"${it.simpleName}\"" }
}
