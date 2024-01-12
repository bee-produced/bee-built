package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.PoetMap.Companion.addNStatementBuilder
import com.beeproduced.bee.generative.util.toPoetClassName
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.BEE_SELECTION
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.CLAZZ
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.DEFAULT_BEE_SELECTION
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.TYPED_FIELD_NODE
import com.beeproduced.bee.persistent.blaze.processor.info.ColumnProperty
import com.beeproduced.bee.persistent.blaze.processor.info.EntityInfo
import com.beeproduced.bee.persistent.blaze.processor.info.RepoInfo
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */
class BeePersistentDSLCodegen(
    private val codeGenerator: CodeGenerator,
    private val dependencies: Dependencies,
    private val logger: KSPLogger,
    private val entities: List<EntityInfo>,
    private val views: ViewInfo,
    private val config: BeePersistentBlazeConfig
) {
    private val packageName = config.dslPackageName
    private lateinit var className: String

    private lateinit var view: EntityViewInfo
    private lateinit var entity: EntityInfo

    private val poetMap: PoetMap = PoetMap()
    private fun FunSpec.Builder.addNamedStmt(format: String)
        = addNStatementBuilder(format, poetMap)
    private fun CodeBlock.Builder.addNamedStmt(format: String)
        = addNStatementBuilder(format, poetMap)

    @Suppress("ConstPropertyName")
    object PoetConstants {
        const val CLAZZ = "%clazz:T"
        const val TYPED_FIELD_NODE = "%typedfieldnode:T"
        const val BEE_SELECTION = "%beeselection:T"
        const val DEFAULT_BEE_SELECTION = "%default_bee_selection:T"
    }

    init {
        poetMap.addMapping(TYPED_FIELD_NODE, ClassName("com.beeproduced.bee.persistent.blaze.selection.DefaultBeeSelection", "TypedFieldNode"))
        poetMap.addMapping(BEE_SELECTION, ClassName("com.beeproduced.bee.persistent.blaze.selection", "BeeSelection"))
        poetMap.addMapping(DEFAULT_BEE_SELECTION, ClassName("com.beeproduced.bee.persistent.blaze.selection", "DefaultBeeSelection"))
    }

    fun processRepoDSL(repo: RepoInfo) {
        logger.info("processRepoDSL($repo")
        view = findView(repo)
        entity = view.entity
        className = "${entity.uniqueName}DSL"
        poetMap.addMapping(CLAZZ, entity.qualifiedName.toPoetClassName())

        FileSpec
            .builder(packageName, className)
            .buildSelectionDSL()
            .build()
            .writeTo(codeGenerator, dependencies)
    }


    private fun FileSpec.Builder.buildSelectionDSL(): FileSpec.Builder = apply {
        val selectionDSLName = "${entity.simpleName}DSL"
        val selectionDSL = TypeSpec
            .objectBuilder(selectionDSLName)
        val name = selectionDSL.traverseDSL(view, selectionDSLName,)

        val viewDSL = ClassName("", name)
        val selectorLambda = LambdaTypeName.get(receiver = viewDSL, returnType = UNIT)
        val selectorFn = FunSpec.builder("select")
            .returns(poetMap.classMapping(BEE_SELECTION))
            .addParameter("selector", selectorLambda)
            .addStatement("val selectionBuilder = %T()", viewDSL)
            .addStatement("selectionBuilder.selector()")
            .addNamedStmt("return ${DEFAULT_BEE_SELECTION}(selectionBuilder.fields)")
        selectionDSL.addFunction(selectorFn.build())


        addType(selectionDSL.build())
    }

    private fun TypeSpec.Builder.traverseDSL(
        entityView: EntityViewInfo,
        dslName: String,
        visitedEmbeddedViews: MutableMap<String, String?> = mutableMapOf()
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
                .addNamedStmt("mutableSetOf(${TYPED_FIELD_NODE}(\"$idName\", \"$entitySimpleName\"))")
                .build()
        )
        viewDSL.addProperty(viewDSLFields.build())


        val allRelations: MutableMap<String, String> = entityView.relations.toMutableMap()
        val allColumns: MutableList<ColumnProperty> = entity.columns.toMutableList()
        val allLazyColumns: MutableList<ColumnProperty> = entity.lazyColumns.toMutableList()

        val subViews = views.subclassEntityViewsBySuperClass[entityView.name]
        subViews?.forEach { subView ->
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
                .addNamedStmt("fields.add(${TYPED_FIELD_NODE}(\"$simpleName\", \"$entitySimpleName\",")
                .addStatement("  selectionBuilder.fields")
                .addStatement("))")
            viewDSL.addFunction(selectorFn.build())
        }

        val lazyColumns = allLazyColumns.filter { !it.isEmbedded }
        for (lazyColumn in lazyColumns) {
            val simpleName = lazyColumn.simpleName
            val selectorFn = FunSpec.builder(simpleName)
                .addNamedStmt("fields.add(${TYPED_FIELD_NODE}(\"$simpleName\", \"$entitySimpleName\"))")
            viewDSL.addFunction(selectorFn.build())
        }

        // TODO: Streamline...
        for (embeddedColumn in allColumns.filter { it.isEmbedded }) {
            val simpleName = embeddedColumn.simpleName
            val embedded = requireNotNull(embeddedColumn.embedded)
            val name = traverseDSL(embeddedColumn, visitedEmbeddedViews, false)
            if (name == null) continue
            val subViewDSL = ClassName("", name)

            val selectorLambda = LambdaTypeName.get(receiver = subViewDSL, returnType = UNIT)
            val selectorFn = FunSpec.builder(simpleName)
                .addParameter("selector", selectorLambda)
                .addStatement("val selectionBuilder = %T()", subViewDSL)
                .addStatement("selectionBuilder.selector()")
                .addNamedStmt("fields.add(${TYPED_FIELD_NODE}(\"$simpleName\", \"$entitySimpleName\",")
                .addStatement("  selectionBuilder.fields")
                .addStatement("))")
            viewDSL.addFunction(selectorFn.build())
        }

        for (embeddedColumn in lazyColumns.filter { it.isEmbedded }) {
            val simpleName = embeddedColumn.simpleName
            val embedded = requireNotNull(embeddedColumn.embedded)
            val name = traverseDSL(embeddedColumn, visitedEmbeddedViews, true)
            if (name == null) continue
            val subViewDSL = ClassName("", name)

            val selectorLambda = LambdaTypeName.get(receiver = subViewDSL, returnType = UNIT)
            val selectorFn = FunSpec.builder(simpleName)
                .addParameter("selector", selectorLambda)
                .addStatement("val selectionBuilder = %T()", subViewDSL)
                .addStatement("selectionBuilder.selector()")
                .addNamedStmt("fields.add(${TYPED_FIELD_NODE}(\"$simpleName\", \"$entitySimpleName\",")
                .addStatement("  selectionBuilder.fields")
                .addStatement("))")
            viewDSL.addFunction(selectorFn.build())
        }

        addType(viewDSL.build())

        viewDSLName
    }



    private fun TypeSpec.Builder.traverseDSL(
        embeddedColumn: ColumnProperty,
        visitedEmbeddedViews: MutableMap<String, String?>,
        isLazy: Boolean
    ): String? {
        val embeddedInfo = requireNotNull(embeddedColumn.embedded)
        val embeddedSimpleName = embeddedInfo.simpleName
        val uniqueName =  embeddedInfo.uniqueName
        if (visitedEmbeddedViews.containsKey(uniqueName))
            return visitedEmbeddedViews.getValue(uniqueName)

        val viewName = BeePersistentAnalyser.viewName(embeddedInfo)
        val view = views.embeddedViews.getValue(viewName)
        val embedded = view.embedded

        val lazyColumns = embedded.lazyColumns
        if (lazyColumns.isEmpty() && !isLazy) {
            visitedEmbeddedViews[uniqueName] = null
            return null
        }

        val viewDSLName = "${view.name}__Selection"
        val viewDSL = TypeSpec.classBuilder(viewDSLName)
        val viewDSLFields = PropertySpec.builder(
            "fields", MUTABLE_SET.parameterizedBy(poetMap.classMapping(TYPED_FIELD_NODE))
        )

        val columns = embedded.columns
        val someProp = columns.firstOrNull()
        if (someProp != null) {
            val simpleName = someProp.simpleName
            viewDSLFields.initializer(
                CodeBlock.builder()
                    .addNamedStmt("mutableSetOf(${TYPED_FIELD_NODE}(\"$simpleName\", \"$embeddedSimpleName\"))")
                    .build()
            )
        } else {
            viewDSLFields.initializer(
                CodeBlock.builder()
                    .addNamedStmt("mutableSetOf()")
                    .build()
            )
        }

        viewDSL.addProperty(viewDSLFields.build())

        addType(viewDSL.build())
        return viewDSLName
    }

    private fun findView(repo: RepoInfo): EntityViewInfo {
        val qualifiedName = repo.entityType.declaration.qualifiedName?.asString()
        return views.coreEntityViewsByQualifiedName[qualifiedName]
            ?: throw IllegalArgumentException("No view for [$qualifiedName] found")
    }
}