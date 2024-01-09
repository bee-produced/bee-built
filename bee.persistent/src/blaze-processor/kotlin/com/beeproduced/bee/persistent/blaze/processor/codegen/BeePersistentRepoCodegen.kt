package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.PoetMap.Companion.addNStatementBuilder
import com.beeproduced.bee.generative.util.toPoetClassName
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentAnalyser.Companion.viewName
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.AUTOWIRED
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.BEE_SELECTION
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.CLAZZ
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.CRITERIA_BUILDER_FACTORY
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.ENTITY_MANAGER
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.ENTITY_VIEW_MANAGER
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.ENTITY_VIEW_SETTING
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.QUALIFIER
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.SELECTION_INFO
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.TYPED_FIELD_NODE
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.VIEW_CLAZZ
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._CBF_PROP
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._CLAZZ_PROPERTY
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._EM_PROP
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._EVM_PROP
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._FETCH_SELECTION_FN
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._SELECTION_INFO_VAL
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._VIEW_CLAZZ_PROPERTY
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.__SELECTION_INFO_NAME
import com.beeproduced.bee.persistent.blaze.processor.info.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-28
 */
class BeePersistentRepoCodegen(
    private val codeGenerator: CodeGenerator,
    private val dependencies: Dependencies,
    private val logger: KSPLogger,
    private val entities: List<EntityInfo>,
    private val views: ViewInfo,
    private val config: BeePersistentBlazeConfig
) {
    private val packageName = config.repositoryPackageName
    private val viewPackageName = config.viewPackageName
    private lateinit var className: String

    private lateinit var repoInterface: KSClassDeclaration
    private lateinit var view: EntityViewInfo
    private lateinit var entity: EntityInfo
    private var repoConfig: RepoConfig? = null

    private val poetMap: PoetMap = PoetMap()
    private fun FunSpec.Builder.addNamedStmt(format: String)
        = addNStatementBuilder(format, poetMap)
    private fun CodeBlock.Builder.addNamedStmt(format: String)
        = addNStatementBuilder(format, poetMap)

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
    }

    init {
        poetMap.addMapping(_CLAZZ_PROPERTY, "clazz")
        poetMap.addMapping(_VIEW_CLAZZ_PROPERTY, "viewClazz")
        poetMap.addMapping(BEE_SELECTION, ClassName("com.beeproduced.bee.persistent.blaze.selection", "BeeSelection"))
        poetMap.addMapping(SELECTION_INFO, ClassName("com.beeproduced.bee.persistent.blaze.meta", "SelectionInfo"))
        poetMap.addMapping(ENTITY_MANAGER, ClassName("jakarta.persistence", "EntityManager"))
        poetMap.addMapping(_EM_PROP, "em")
        poetMap.addMapping(CRITERIA_BUILDER_FACTORY, ClassName("com.blazebit.persistence", "CriteriaBuilderFactory"))
        poetMap.addMapping(_CBF_PROP, "cbf")
        poetMap.addMapping(ENTITY_VIEW_MANAGER, ClassName("com.blazebit.persistence.view", "EntityViewManager"))
        poetMap.addMapping(_EVM_PROP, "evm")
        poetMap.addMapping(AUTOWIRED, ClassName("org.springframework.beans.factory.annotation", "Autowired"))
        poetMap.addMapping(QUALIFIER, ClassName("org.springframework.beans.factory.annotation", "Qualifier"))
        poetMap.addMapping(_FETCH_SELECTION_FN, MemberName("com.beeproduced.bee.persistent.blaze.repository", "fetchSelection"))
        poetMap.addMapping(ENTITY_VIEW_SETTING, ClassName("com.blazebit.persistence.view", "EntityViewSetting"))
        poetMap.addMapping(TYPED_FIELD_NODE, ClassName("com.beeproduced.bee.persistent.blaze.selection.DefaultBeeSelection", "TypedFieldNode"))
    }

    fun processRepo(repo: RepoInfo) {
        logger.info("processRepo($repo)")
        repoInterface = repo.repoInterface
        view = findView(repo)
        entity = view.entity
        repoConfig = repo.config
        className = "${entity.uniqueName}Repository"
        poetMap.addMapping(CLAZZ, entity.qualifiedName.toPoetClassName())
        poetMap.addMapping(VIEW_CLAZZ, "${viewPackageName}.${view.name}".toPoetClassName())

        FileSpec
            .builder(packageName, className)
            .buildSelectionDSL()
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
                .buildSelect()
                .buildSelectionInfo()
                .build()
        )
    }

    private fun TypeSpec.Builder.buildConstructor(): TypeSpec.Builder = apply {
        val emProp = PropertySpec.builder(
            poetMap.literalMapping(_EM_PROP),
            poetMap.classMapping(ENTITY_MANAGER)
        )
            .initializer(poetMap.literalMapping(_EM_PROP))
            .addModifiers(KModifier.OVERRIDE)
            .build()
        val cbfProp = PropertySpec.builder(
            poetMap.literalMapping(_CBF_PROP),
            poetMap.classMapping(CRITERIA_BUILDER_FACTORY)
        )
            .initializer(poetMap.literalMapping(_CBF_PROP))
            .addModifiers(KModifier.OVERRIDE)
            .build()
        val evmProp = PropertySpec.builder(
            poetMap.literalMapping(_EVM_PROP),
            poetMap.classMapping(ENTITY_VIEW_MANAGER)
        )
            .initializer(poetMap.literalMapping(_EVM_PROP))
            .addModifiers(KModifier.OVERRIDE)
            .build()
        addProperties(listOf(emProp, cbfProp, evmProp))

        val emParam = ParameterSpec.builder(
            poetMap.literalMapping(_EM_PROP),
            poetMap.classMapping(ENTITY_MANAGER)
        ).apply {
            val annotation = buildConstructorAnnotation { entityManagerFactoryRef }
            addAnnotation(annotation)
        }.build()
        val cbfParam = ParameterSpec.builder(
            poetMap.literalMapping(_CBF_PROP),
            poetMap.classMapping(CRITERIA_BUILDER_FACTORY)
        ).apply {
            val annotation = buildConstructorAnnotation { criteriaBuilderFactoryRef }
            addAnnotation(annotation)
        }.build()
        val evmParam = ParameterSpec.builder(
            poetMap.literalMapping(_EVM_PROP),
            poetMap.classMapping(ENTITY_VIEW_MANAGER)
        ).apply {
            val annotation = buildConstructorAnnotation { entityViewManagerRef }
            addAnnotation(annotation)
        }.build()
        val constructorBuilder = FunSpec.constructorBuilder()
        constructorBuilder.addParameters(listOf(emParam, cbfParam, evmParam))
        primaryConstructor(constructorBuilder.build())
    }

    private fun buildConstructorAnnotation(selector: RepoConfig.()->String): AnnotationSpec {
        if (repoConfig == null) return AnnotationSpec.builder(poetMap.classMapping(AUTOWIRED)).build()
        val annotationValue = repoConfig!!.selector()
        return AnnotationSpec.builder(poetMap.classMapping(QUALIFIER))
            .addMember("\"$annotationValue\"")
            .build()
    }

    private fun TypeSpec.Builder.addRepoProperties(): TypeSpec.Builder = apply {
        val clazzProperty = PropertySpec
            .builder(
                poetMap.literalMapping(_CLAZZ_PROPERTY),
                ClassName("java.lang", "Class")
                    .parameterizedBy(poetMap.classMapping(CLAZZ))
            )
            .addModifiers(KModifier.PROTECTED)
            .initializer("%T::class.java", poetMap.classMapping(CLAZZ))
            .build()
        val viewClazzProperty = PropertySpec
            .builder(
                poetMap.literalMapping(_VIEW_CLAZZ_PROPERTY),
                ClassName("java.lang", "Class")
                    .parameterizedBy(poetMap.classMapping(VIEW_CLAZZ))
            )
            .addModifiers(KModifier.PROTECTED)
            .initializer("%T::class.java", poetMap.classMapping(VIEW_CLAZZ))
            .build()

        addProperties(listOf(clazzProperty, viewClazzProperty))
    }

    private fun TypeSpec.Builder.buildSelect(): TypeSpec.Builder = apply {
        val listOfEntity = ClassName("kotlin.collections", "List")
            .parameterizedBy(entity.declaration.toClassName())
        val selectFn = FunSpec.builder("select")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("selection", poetMap.classMapping(BEE_SELECTION))
            .returns(listOfEntity)
        selectFn.apply {
            addNamedStmt("val setting = $ENTITY_VIEW_SETTING.create($_VIEW_CLAZZ_PROPERTY)")
            addNamedStmt("  .apply { $_FETCH_SELECTION_FN(selectionInfo, selection) }")
            addNamedStmt("val builder = $_CBF_PROP.create($_EM_PROP, $_CLAZZ_PROPERTY)")
            addNamedStmt("val query = $_EVM_PROP.applySetting(setting, builder)")
            addNamedStmt("@Suppress(\"UNCHECKED_CAST\")")
            addStatement("return query.resultList as %T", listOfEntity)
        }
        addFunction(selectFn.build())
    }

    private fun findView(repo: RepoInfo): EntityViewInfo {
        val qualifiedName = repo.entityType.declaration.qualifiedName?.asString()
        val view = views.entityViews.values.firstOrNull { view ->
            view.name.endsWith("Core") && view.qualifiedName == qualifiedName
        } ?: throw IllegalArgumentException("No view for [$qualifiedName] found")
        return view
    }

    private fun TypeSpec.Builder.buildSelectionInfo(): TypeSpec.Builder = apply {
        val initializerBlock = CodeBlock.builder()
        initializerBlock.addNamedStmt("run {")
        initializerBlock.traverseSelectionInfo(view)
        initializerBlock.addNamedStmt("  ${view.name.lowercase()}")
        initializerBlock.addNamedStmt("}")

        val selectionInfoProperty = PropertySpec
            .builder("selectionInfo", poetMap.classMapping(SELECTION_INFO))
            .initializer(initializerBlock.build())
            .build()
        val companionObject = TypeSpec
            .companionObjectBuilder()
            .addProperty(selectionInfoProperty)
            .build()
        addType(companionObject)
    }

    private fun CodeBlock.Builder.traverseSelectionInfo(
        entityView: EntityViewInfo,
        visitedEmbeddedViews: MutableSet<String> = mutableSetOf()
    ): CodeBlock.Builder = apply {
        val entity = entityView.entity
        val allRelations: MutableMap<String, String> = entityView.relations.toMutableMap()
        val allColumns: MutableList<ColumnProperty> = entity.columns.toMutableList()
        val allLazyColumns: MutableList<ColumnProperty> = entity.lazyColumns.toMutableList()

        // TODO: Optimize this access?
        val subViews = views.entityViews.values
            .filter { it.superClassName == entityView.name }
        for (subView in subViews) {
            allRelations.putAll(subView.relations)
            allColumns.addAll(subView.entity.columns)
            allLazyColumns.addAll(subView.entity.lazyColumns)
        }

        val relationMapInput = allRelations.map {
            (simpleName, viewName) -> Pair(simpleName, viewName)
        }
        val relationSI = traverseSubSelectionInfo(relationMapInput, visitedEmbeddedViews)

        val idSI = "\"${entity.id.simpleName}\""
        val (columns, embedded)
            = allColumns.partition { !it.isEmbedded }
        val columnSI = selectionInfoListValues(columns)
        val embeddedMapInput = embedded.map {
           val viewName = viewName(requireNotNull(it.embedded))
           Pair(it.simpleName, viewName)
        }
        val embeddedSI = traverseSubEmbeddedInfo(embeddedMapInput, visitedEmbeddedViews)

        val (lazyColumns, lazyEmbedded)
            = allLazyColumns.partition { !it.isEmbedded }
        val lazyColumnSI = selectionInfoListValues(lazyColumns)
        val lazyEmbeddedMapInput = lazyEmbedded.map {
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
        visitedEmbeddedViews: MutableSet<String>
    ): String {
        for ((_, viewName) in simpleNameAndViewName) {
            val view = views.entityViews.getValue(viewName)
            traverseSelectionInfo(view, visitedEmbeddedViews)
        }
        return simpleNameAndViewName.joinToString(separator = ", ") {
            (p, v) -> "\"$p\" to ${v.lowercase()}"
        }
    }

    private fun CodeBlock.Builder.traverseSubEmbeddedInfo(
        simpleNameAndViewName: List<Pair<String, String>>,
        visitedEmbeddedViews: MutableSet<String>
    ): String {
        for ((_, viewName) in simpleNameAndViewName) {
            if (visitedEmbeddedViews.contains(viewName)) continue
            visitedEmbeddedViews.add(viewName)
            val view = views.embeddedViews.getValue(viewName)
            traverseEmbeddedInfo(view)
        }
        return simpleNameAndViewName.joinToString(separator = ", ") {
                (p, v) -> "\"$p\" to ${v.lowercase()}"
        }
    }

    private fun selectionInfoListValues(columnProperties: List<ColumnProperty>) : String
        = columnProperties.joinToString(separator = ", ") { "\"${it.simpleName}\"" }


    private fun FileSpec.Builder.buildSelectionDSL(): FileSpec.Builder = apply {
        // TODO: What with duplicate names?
        // Move to package as unique name?
        val selectionDSLName = "${entity.simpleName}DSL"
        val selectionDSL = TypeSpec
            .objectBuilder(selectionDSLName)
        selectionDSL.traverseDSL(view, selectionDSLName,)

        addType(selectionDSL.build())
    }

    private fun TypeSpec.Builder.traverseDSL(
        entityView: EntityViewInfo,
        dslName: String,
        visitedEmbeddedViews: MutableSet<String> = mutableSetOf()
    ): String = run {
        val entity = entityView.entity

        val viewDSLName = "${entityView.name}__Selection"
        val viewDSL = TypeSpec.classBuilder(viewDSLName)
        val viewDSLFields = PropertySpec.builder(
            "fields", MUTABLE_SET.parameterizedBy(poetMap.classMapping(TYPED_FIELD_NODE))
        )
        val idName = entity.id.simpleName
        val entitySimpleName = entity.simpleName
        viewDSLFields.initializer(
            CodeBlock.builder()
                .addNamedStmt("mutableSetOf($TYPED_FIELD_NODE(\"$idName\", \"$entitySimpleName\"))")
                .build()
        )
        viewDSL.addProperty(viewDSLFields.build())



        val allRelations: MutableMap<String, String> = entityView.relations.toMutableMap()
        val allColumns: MutableList<ColumnProperty> = entity.columns.toMutableList()
        val allLazyColumns: MutableList<ColumnProperty> = entity.lazyColumns.toMutableList()

        // TODO: Optimize this access?
        val subViews = views.entityViews.values
            .filter { it.superClassName == entityView.name }
        for (subView in subViews) {
            allRelations.putAll(subView.relations)
            allColumns.addAll(subView.entity.columns)
            allLazyColumns.addAll(subView.entity.lazyColumns)
        }

        val relationMapInput = allRelations.map {
                (simpleName, viewName) -> Pair(simpleName, viewName)
        }

        for ((simpleName, viewName) in relationMapInput) {
            val view = views.entityViews.getValue(viewName)
            val name = traverseDSL(view, dslName, visitedEmbeddedViews)
            // val subViewDSL = ClassName("$packageName.$dslName", name)
            val subViewDSL = ClassName("", name)

            val selectorLambda = LambdaTypeName.get(receiver = subViewDSL, returnType = UNIT)
            val selectorFn = FunSpec.builder(simpleName)
                .addParameter("selector", selectorLambda)
                .addStatement("val selectionBuilder = %T()", subViewDSL)
                .addStatement("selectionBuilder.selector()")
                .addNamedStmt("fields.add($TYPED_FIELD_NODE(\"$simpleName\", \"$entitySimpleName\",")
                .addStatement("  selectionBuilder.fields")
                .addStatement("))")
            viewDSL.addFunction(selectorFn.build())
        }

        // val relationSI = traverseSubSelectionInfo(relationMapInput, visitedEmbeddedViews)
        //
        // val idSI = "\"${entity.id.simpleName}\""
        // val (columns, embedded)
        //     = allColumns.partition { !it.isEmbedded }
        // val columnSI = selectionInfoListValues(columns)
        // val embeddedMapInput = embedded.map {
        //     val viewName = viewName(requireNotNull(it.embedded))
        //     Pair(it.simpleName, viewName)
        // }
        // val embeddedSI = traverseSubEmbeddedInfo(embeddedMapInput, visitedEmbeddedViews)
        //
        // val (lazyColumns, lazyEmbedded)
        //     = allLazyColumns.partition { !it.isEmbedded }
        // val lazyColumnSI = selectionInfoListValues(lazyColumns)
        // val lazyEmbeddedMapInput = lazyEmbedded.map {
        //     val viewName = viewName(requireNotNull(it.embedded))
        //     Pair(it.simpleName, viewName)
        // }
        // val lazyEmbeddedSI = traverseSubEmbeddedInfo(lazyEmbeddedMapInput, visitedEmbeddedViews)
        //
        // poetMap.addMapping(_SELECTION_INFO_VAL, entityView.name.lowercase())
        // poetMap.addMapping(__SELECTION_INFO_NAME, entityView.name)
        // addNamedStmt("  val $_SELECTION_INFO_VAL = $SELECTION_INFO(")
        // addNamedStmt("    view = $__SELECTION_INFO_NAME,")
        // addNamedStmt("    relations = mapOf($relationSI),")
        // addNamedStmt("    id = $idSI,")
        // addNamedStmt("    columns = setOf($columnSI),")
        // addNamedStmt("    lazyColumns = setOf($lazyColumnSI),")
        // addNamedStmt("    embedded = mapOf($embeddedSI),")
        // addNamedStmt("    lazyEmbedded = mapOf($lazyEmbeddedSI)")
        // addNamedStmt("  )")

        addType(viewDSL.build())

        viewDSLName
    }

    // private fun TypeSpec.Builder.traverseEmbeddedInfo(
    //     embeddedView: EmbeddedViewInfo
    // ): TypeSpec.Builder = apply {
    //     val embedded = embeddedView.embedded
    //     val columnSI = selectionInfoListValues(embedded.columns)
    //     val lazyColumnSI = selectionInfoListValues(embedded.lazyColumns)
    //
    //     poetMap.addMapping(_SELECTION_INFO_VAL, embeddedView.name.lowercase())
    //     poetMap.addMapping(__SELECTION_INFO_NAME, embeddedView.name)
    //     addNamedStmt("  val $_SELECTION_INFO_VAL = $SELECTION_INFO(")
    //     addNamedStmt("    view = $__SELECTION_INFO_NAME,")
    //     addNamedStmt("    relations = emptyMap(),")
    //     addNamedStmt("    id = null,")
    //     addNamedStmt("    columns = setOf($columnSI),")
    //     addNamedStmt("    lazyColumns = setOf($lazyColumnSI),")
    //     addNamedStmt("    embedded = emptyMap(),")
    //     addNamedStmt("    lazyEmbedded = emptyMap()")
    //     addNamedStmt("  )")
    // }
    //
    //
    // private fun CodeBlock.Builder.traverseSubEmbeddedInfo(
    //     simpleNameAndViewName: List<Pair<String, String>>,
    //     visitedEmbeddedViews: MutableSet<String>
    // ): String {
    //     for ((_, viewName) in simpleNameAndViewName) {
    //         if (visitedEmbeddedViews.contains(viewName)) continue
    //         visitedEmbeddedViews.add(viewName)
    //         val view = views.embeddedViews.getValue(viewName)
    //         traverseEmbeddedInfo(view)
    //     }
    //     return simpleNameAndViewName.joinToString(separator = ", ") {
    //             (p, v) -> "\"$p\" to ${v.lowercase()}"
    //     }
    // }
}