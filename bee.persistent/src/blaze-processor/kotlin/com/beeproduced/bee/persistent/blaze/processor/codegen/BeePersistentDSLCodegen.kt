package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.PoetMap.Companion.addNStatementBuilder
import com.beeproduced.bee.generative.util.toPoetClassName
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.APPLICATION_LISTENER
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.APPLICATION_READY_EVENT
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.APPLICATION_STARTING_EVENT
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.BEE_SELECTION
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.CLAZZ
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.COMPONENT
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.DEFAULT_BEE_SELECTION
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.EVENT_LISTENER
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.EXP
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.INLINE_VALUE_CLAZZ
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.INLINE_VALUE_UNWRAPPERS
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.INNER_CLAZZ
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.PATH
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.SORTABLE_EXP
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.SORTABLE_VALUE_EXP
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.TYPED_FIELD_NODE
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.VALUE_EXP
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentDSLCodegen.PoetConstants.VALUE_PATH
import com.beeproduced.bee.persistent.blaze.processor.info.*
import com.beeproduced.bee.persistent.blaze.processor.utils.SubProperty
import com.beeproduced.bee.persistent.blaze.processor.utils.buildUniqueClassName
import com.beeproduced.bee.persistent.blaze.processor.utils.viewColumnsWithSubclasses
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import org.gradle.configurationcache.extensions.capitalized
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */
class BeePersistentDSLCodegen(
    private val codeGenerator: CodeGenerator,
    private val resourcesCodegen: ResourcesCodegen,
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

    private val inlineValues = mutableMapOf<String, Property>()

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
        const val PATH = "%path:T"
        const val VALUE_PATH = "%valuepath:T"
        const val EXP = "%expression:T"
        const val VALUE_EXP = "%valuexpression:T"
        const val SORTABLE_EXP = "%sortablexpression:T"
        const val SORTABLE_VALUE_EXP = "%sortablevalueexpression:T"
        const val COMPONENT = "%component:T"
        const val EVENT_LISTENER = "%eventlistener:T"
        const val APPLICATION_READY_EVENT = "%applicationreadyevent:T"
        const val INLINE_VALUE_UNWRAPPERS = "%inlinevalueunwrappers:T"
        const val INLINE_VALUE_CLAZZ = "%inlinevalueclazz:T"
        const val INNER_CLAZZ = "%innerclass:T"
        const val APPLICATION_LISTENER = "%applicationlistener:T"
        const val APPLICATION_STARTING_EVENT = "%applicationstartingevent:T"
    }

    init {
        poetMap.addMapping(TYPED_FIELD_NODE, ClassName("com.beeproduced.bee.persistent.blaze.selection.DefaultBeeSelection", "TypedFieldNode"))
        poetMap.addMapping(BEE_SELECTION, ClassName("com.beeproduced.bee.persistent.blaze.selection", "BeeSelection"))
        poetMap.addMapping(DEFAULT_BEE_SELECTION, ClassName("com.beeproduced.bee.persistent.blaze.selection", "DefaultBeeSelection"))
        poetMap.addMapping(PATH, ClassName("com.beeproduced.bee.persistent.blaze.dsl.path", "Path"))
        poetMap.addMapping(VALUE_PATH, ClassName("com.beeproduced.bee.persistent.blaze.dsl.path", "ValuePath"))
        poetMap.addMapping(PATH, ClassName("com.beeproduced.bee.persistent.blaze.dsl.path", "Path"))
        poetMap.addMapping(VALUE_PATH, ClassName("com.beeproduced.bee.persistent.blaze.dsl.path", "ValuePath"))
        poetMap.addMapping(EXP, ClassName("com.beeproduced.bee.persistent.blaze.dsl.expression", "Expression"))
        poetMap.addMapping(VALUE_EXP, ClassName("com.beeproduced.bee.persistent.blaze.dsl.expression", "ValueExpression"))
        poetMap.addMapping(SORTABLE_EXP, ClassName("com.beeproduced.bee.persistent.blaze.dsl.path", "SortableExpression"))
        poetMap.addMapping(SORTABLE_VALUE_EXP, ClassName("com.beeproduced.bee.persistent.blaze.dsl.path", "SortableValueExpression"))
        poetMap.addMapping(COMPONENT, ClassName("org.springframework.stereotype", "Component"))
        poetMap.addMapping(EVENT_LISTENER, ClassName("org.springframework.context.event", "EventListener"))
        poetMap.addMapping(APPLICATION_READY_EVENT, ClassName("org.springframework.boot.context.event", "ApplicationReadyEvent"))
        poetMap.addMapping(INLINE_VALUE_UNWRAPPERS, ClassName("com.beeproduced.bee.persistent.blaze.meta.dsl", "InlineValueUnwrappers"))
        poetMap.addMapping(APPLICATION_LISTENER, ClassName("org.springframework.context", "ApplicationListener"))
        poetMap.addMapping(APPLICATION_STARTING_EVENT, ClassName("org.springframework.boot.context.event", "ApplicationStartingEvent"))
    }


    fun processRepoDSL(repos: List<RepoInfo>) {
        for (repo in repos) processRepoDSL(repo)
        buildRegistration()
    }

    private fun processRepoDSL(repo: RepoInfo) {
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

        selectionDSL.traverseWhereDSL(view, selectionDSL, sort = true)

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

        for (embeddedColumn in allLazyColumns.filter { it.isEmbedded }) {
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

        for (lazyColumn in lazyColumns) {
            val simpleName = lazyColumn.simpleName
            val selectorFn = FunSpec.builder(simpleName)
                .addNamedStmt("fields.add(${TYPED_FIELD_NODE}(\"$simpleName\", \"$embeddedSimpleName\"))")
            viewDSL.addFunction(selectorFn.build())
        }

        addType(viewDSL.build())
        return viewDSLName
    }


    private fun TypeSpec.Builder.traverseWhereDSL(
        entityView: EntityViewInfo,
        viewDSL: TypeSpec.Builder,
        path: String = "",
        visitedEmbeddedViews: MutableMap<String, String?> = mutableMapOf(),
        sort: Boolean = false,
    ): TypeSpec.Builder {

        val entitySimpleName = entityView.entity.simpleName
        val (allRelations, allColumns)
            = viewColumnsWithSubclasses(entityView, views)

        val (columns, embedded) = allColumns.partition { !it.property.isEmbedded }
        for (column in columns) {
            viewDSL.addPath(column, path, entitySimpleName, sort)
        }

        for (column in embedded) {
            viewDSL.addPath(column, path, entitySimpleName, sort)
        }

        for ((simpleName, subRelation) in allRelations) {
            val innerView = views.entityViews.getValue(subRelation.relationView)
            if (innerView.isExtended) continue

            val whereDSLName = "${innerView.name}__Where"
            val innerDSL = TypeSpec.objectBuilder(whereDSLName)
            val subViewMapping = if (subRelation.subView == null) null
            else SubViewMapping(entitySimpleName, subRelation.subView)
            val newPath = buildPath(path, simpleName, subViewMapping)

            val relationProperty = PropertySpec.builder(simpleName, ClassName("", whereDSLName))
                .initializer("%L",  whereDSLName)
            viewDSL.addProperty(relationProperty.build())

            traverseWhereDSL(innerView, innerDSL, newPath, visitedEmbeddedViews)

            addType(innerDSL.build())
        }

        return this
    }

    private fun TypeSpec.Builder.addPath(
        subProperty: SubProperty, path: String, entitySimpleName: String,
        sort: Boolean, embeddedPropertyName: String? = null
    ) {
        val column = subProperty.property
        val propertyName = embeddedPropertyName ?: column.simpleName
        val subViewMapping = if (subProperty.subView == null) null
        else SubViewMapping(entitySimpleName, subProperty.subView)
        val newPath = buildPath(path, column.simpleName, subViewMapping)
        val inner = column.innerValue
        if (inner != null) {
            val innerType = inner.type.toTypeName().copy(nullable = false)
            val columnType = column.type.toTypeName().copy(nullable = false)
            val interfaceType = if (sort)
                poetMap.classMapping(SORTABLE_VALUE_EXP).parameterizedBy(columnType, innerType)
            else poetMap.classMapping(VALUE_EXP).parameterizedBy(columnType, innerType)
            val pathType = poetMap.classMapping(VALUE_PATH)

            val pathProperty = PropertySpec.builder(propertyName, interfaceType)
                .initializer(CodeBlock.Builder()
                    .addStatement("%T(%S, %T::class)", pathType, newPath, columnType)
                    .build()
                )
            addProperty(pathProperty.build())
            // Generate registration later
            inlineValues[column.qualifiedName!!] = column
            return
        }
        val columnType = column.type.toTypeName().copy(nullable = false)
        val interfaceType = if (sort)
            poetMap.classMapping(SORTABLE_EXP).parameterizedBy(columnType)
        else poetMap.classMapping(EXP).parameterizedBy(columnType)
        val pathType = poetMap.classMapping(PATH)
        val pathProperty = PropertySpec.builder(propertyName, interfaceType)
            .initializer(CodeBlock.Builder()
                .addStatement("%T(%S)", pathType, newPath)
                .build()
            )
        addProperty(pathProperty.build())

        val embedded = column.embedded ?: return
        for (embeddedColumn in embedded.jpaProperties) {
            val columnProp = ColumnProperty(embeddedColumn.declaration, embeddedColumn.type, embeddedColumn.annotations, null, null)
            val embeddedSubProperty = SubProperty(columnProp)
            val overrideName = "$propertyName${embeddedColumn.simpleName.capitalized()}"
            addPath(embeddedSubProperty, newPath, entitySimpleName, sort, overrideName)
        }
    }

    data class SubViewMapping(val entitySimpleName: String, val subEntitySimpleName: String)
    private fun buildPath(path: String, simpleName: String, subViewMapping: SubViewMapping?): String {
        return when {
            path.isEmpty() && subViewMapping == null -> { simpleName }
            path.isEmpty() && subViewMapping != null -> {
                // TREAT alias like CriteriaBuilder<T> create function from blaze: camel cased result of what Class.getSimpleName()
                val entityCamelCase = subViewMapping.entitySimpleName.aliasCamelCase()
                "TREAT($entityCamelCase as ${subViewMapping.subEntitySimpleName}).$simpleName"
            }
            subViewMapping == null -> { "$path.$simpleName" }
            else -> { "TREAT($path as ${subViewMapping.subEntitySimpleName}).$simpleName" }
        }
    }

    private fun String.aliasCamelCase() = replaceFirstChar { it.lowercase(Locale.getDefault()) }

    private fun buildRegistration() {
        if (inlineValues.isEmpty()) return

        val unwrapperClassName = "GeneratedInlineValueUnwrappers"
        FileSpec
            .builder(packageName, unwrapperClassName)
            .buildRegistration()
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun FileSpec.Builder.buildRegistration() = apply {
        val event = poetMap.classMapping(APPLICATION_STARTING_EVENT)
        val listenerInterface = poetMap.classMapping(APPLICATION_LISTENER)
            .parameterizedBy(event)

        val registrationName = buildUniqueClassName(packageName, "UnwrapperRegistration")
        val registration = TypeSpec
            .classBuilder(registrationName)
            .addSuperinterface(listenerInterface)

        val register = FunSpec
            .builder("onApplicationEvent")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("event", event)
        register.apply {
            // Use · to omit line breaks
            // See: https://github.com/square/kotlinpoet/issues/598#issuecomment-454337042
            for (unwrapProperty in inlineValues.values) {
                val inner = requireNotNull(unwrapProperty.innerValue)
                poetMap.addMapping(INLINE_VALUE_CLAZZ, unwrapProperty.type.toTypeName())
                poetMap.addMapping(INNER_CLAZZ, inner.type.toTypeName())
                addNamedStmt("$INLINE_VALUE_UNWRAPPERS.registerUnwrapper($INLINE_VALUE_CLAZZ::class.java, $INNER_CLAZZ::class.java)")
            }
        }

        registration.addFunction(register.build())
        addType(registration.build())

        resourcesCodegen.addSpringListener("$packageName.$registrationName")
    }

    private fun findView(repo: RepoInfo): EntityViewInfo {
        val qualifiedName = repo.entityType.declaration.qualifiedName?.asString()
        return views.coreEntityViewsByQualifiedName[qualifiedName]
            ?: throw IllegalArgumentException("No view for [$qualifiedName] found")
    }
}