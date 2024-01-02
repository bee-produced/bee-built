package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.PoetMap.Companion.addNStatementBuilder
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.ACI
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.APPLICATION_READY_EVENT
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.BLAZE_INSTANTIATORS
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.COLLECTION_STAR
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.COMPONENT
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.EVENT_LISTENER
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.FIELD
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.TCI
import com.beeproduced.bee.persistent.blaze.processor.info.*
import com.beeproduced.bee.persistent.blaze.processor.utils.buildUniqueClassName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-01
 */
class BeePersistentInstantiatorCodegen(
    private val codeGenerator: CodeGenerator,
    private val dependencies: Dependencies,
    private val logger: KSPLogger,
    private val config: BeePersistentBlazeConfig
) {
    private val packageName: String = config.viewPackageName
    private val fileName: String = "GeneratedInstantiators"

    private val constructions = mutableMapOf<String, EntityConstruction>()


    private val poetMap: PoetMap = PoetMap()
    private fun FunSpec.Builder.addNamedStmt(format: String)
        = addNStatementBuilder(format, poetMap)
    private fun CodeBlock.Builder.addNamedStmt(format: String)
        = addNStatementBuilder(format, poetMap)
    private fun FileSpec.Builder.addNamedStmt(format: String)
        = addNStatementBuilder(format, poetMap)

    @Suppress("ConstPropertyName")
    object PoetConstants {
        const val FIELD = "%field:T"
        const val BLAZE_INSTANTIATORS = "%blazeinstantiators:T"
        const val TCI = "%tci:T"
        const val ACI = "%aci:T"
        const val COMPONENT = "%component:T"
        const val EVENT_LISTENER = "%eventlistener:T"
        const val APPLICATION_READY_EVENT = "%applicationreadyevent:T"
        const val COLLECTION_STAR = "%collection:T"
    }

    init {
        poetMap.addMapping(FIELD, ClassName("java.lang.reflect", "Field"))
        poetMap.addMapping(BLAZE_INSTANTIATORS, ClassName("com.beeproduced.bee.persistent.blaze.meta.proxy", "BlazeInstantiators"))
        poetMap.addMapping(TCI, ClassName("com.beeproduced.bee.persistent.blaze.meta.proxy", "TupleConstructorInstantiator"))
        poetMap.addMapping(ACI, ClassName("com.beeproduced.bee.persistent.blaze.meta.proxy", "AssignmentConstructorInstantiator"))
        poetMap.addMapping(COMPONENT, ClassName("org.springframework.stereotype", "Component"))
        poetMap.addMapping(EVENT_LISTENER, ClassName("org.springframework.context.event", "EventListener"))
        poetMap.addMapping(APPLICATION_READY_EVENT, ClassName("org.springframework.boot.context.event", "ApplicationReadyEvent"))
        poetMap.addMapping(COLLECTION_STAR, ClassName("kotlin.collections", "Collection").parameterizedBy(STAR))
    }

    fun processViews(views: ViewInfo, entities: List<EntityInfo>) {
        val file = FileSpec.builder(packageName, fileName)

        for (entity in entities) {
            // Do not generate info for super classes
            if (entity.subClasses != null) continue
            file.buildInfo(entity)
        }
        for (embeddedView in views.embeddedViews.values) {
            val embedded = embeddedView.embedded
            file.buildInfo(embedded)
        }

        val assignmentCreators = mutableMapOf<String, String>()
        val tupleCreators = mutableMapOf<String, String>()
        for (entityView in views.entityViews.values) {
            // Do not generate creators for super classes
            if (entityView.entity.subClasses != null) continue
            if (entityView.entity.superClass != null)
                file.buildAssignmentCreator(entityView, assignmentCreators)
            else file.buildTupleCreator(entityView, tupleCreators)
        }
        for (embeddedView in views.embeddedViews.values) {
            file.buildTupleCreator(embeddedView, tupleCreators)
        }

        file.buildRegistration(tupleCreators, assignmentCreators)

        file.build().writeTo(codeGenerator, dependencies)
    }


    private fun FileSpec.Builder.buildAssignmentCreator(
        view: EntityViewInfo, assignmentCreators: MutableMap<String, String>
    ) = apply {
        val construction = constructions.getValue(view.qualifiedName)
        val decClassName = view.entity.declaration.toClassName()

        val entityFields = view.entityFields()
        val viewFields = view.viewFields()
        val keys = viewFields.mapTo(HashSet()) { it.simpleName }
        val missingFields = entityFields.filter { !keys.contains(it.simpleName) }

        val propertyName = "${view.name}__Creator"
        assignmentCreators[view.name] = propertyName
        val property = PropertySpec.builder(
            propertyName,
            poetMap.classMapping(ACI)
        )

        // Use · to omit line breaks
        // See: https://github.com/square/kotlinpoet/issues/598#issuecomment-454337042
        val initializer = CodeBlock.builder().apply {
            addNamedStmt("$ACI(")
            addNamedStmt("  viewProperties = emptyList(),")
            addNamedStmt("  create = { tuple: Array<Any?>, sort: IntArray -> ")
            for ((index, field) in viewFields.withIndex()) {
                addField(index, field, true)
            }
            for (field in missingFields) {
                addStatement("    val ${field.simpleName} = null as %T", field.declaration.type.toTypeName())
            }
            addStatement("    val entity = %T(", decClassName)
            for (field in construction.constructorProps) {
                addStatement("      ${field.simpleName} = ${field.simpleName},")
            }
            addStatement("    )")
            for (field in construction.setterProps) {
                addStatement("    entity.${field.simpleName} = ${field.simpleName}")
            }
            val infoObj = infoName(view.entity.uniqueName)
            for (field in construction.reflectionProps) {
                val infoField = infoField(field)
                val setterName = field.type.reflectionSetterName()
                addStatement("    ${infoObj}.${infoField}.${setterName}(entity, ${field.simpleName})")
            }
            addNamedStmt("    entity")
            addNamedStmt("  }")
            addNamedStmt(")")
        }.build()

        addProperty(property.initializer(initializer).build())
    }

    private fun FileSpec.Builder.buildTupleCreator(
        view: BaseViewInfo,
        tupleCreators: MutableMap<String, String>
    ) = apply {
        val construction = constructions.getValue(view.qualifiedName)
        val decClassName = view.declaration.toClassName()

        val entityFields = view.entityFields()
        val viewFields = view.viewFields()
        val keys = viewFields.mapTo(HashSet()) { it.simpleName }
        val missingFields = entityFields.filter { !keys.contains(it.simpleName) }

        val propertyName = "${view.name}__Creator"
        tupleCreators[view.name] = propertyName
        val property = PropertySpec.builder(
            propertyName,
            poetMap.classMapping(TCI)
        )

        val initializer = CodeBlock.builder().apply {
            addNamedStmt("$TCI(")
            addNamedStmt("  viewProperties = emptyList(),")
            addNamedStmt("  create = { tuple: Array<Any?> -> ")
            for ((index, field) in viewFields.withIndex()) {
                addField(index, field)
            }
            for (field in missingFields) {
                addStatement("    val ${field.simpleName} = null as %T", field.declaration.type.toTypeName())
            }
            addStatement("    val entity = %T(", decClassName)
            for (field in construction.constructorProps) {
                addStatement("      ${field.simpleName} = ${field.simpleName},")
            }
            addStatement("    )")
            for (field in construction.setterProps) {
                addStatement("    entity.${field.simpleName} = ${field.simpleName}")
            }
            val infoObj = infoName(view.uniqueName)
            for (field in construction.reflectionProps) {
                val infoField = infoField(field)
                val setterName = field.type.reflectionSetterName()
                addStatement("    ${infoObj}.${infoField}.${setterName}(entity, ${field.simpleName})")
            }
            addNamedStmt("    entity")
            addNamedStmt("  }")
            addNamedStmt(")")
        }.build()

        addProperty(property.initializer(initializer).build())
    }

    private fun CodeBlock.Builder.addField(index: Int, field: Property, hasSort: Boolean = false) {
        val tupleAccess = if (!hasSort) "$index" else "sort[$index]"

        val isSet = field.type.declaration.qualifiedName?.asString()?.startsWith(
            "kotlin.collections.Set"
        ) ?: false
        if (isSet) {
            // TODO: Change type of generated view to set to omit this problem?
            addStatement("    val ${field.simpleName} = (tuple[$tupleAccess] as %T?)?.toSet() as %T",
                poetMap.mappings[COLLECTION_STAR], field.type.toTypeName()
            )
            return
        }

        if (field.isValueClass) {
            val innerValue = requireNotNull(field.innerValue)
            if (!field.type.isMarkedNullable) {
                addStatement("    val ${field.simpleName} = %T(tuple[$tupleAccess] as %T)",
                    field.type.toTypeName(), innerValue.type.toTypeName()
                )
            } else {
                addStatement("    val ${field.simpleName} = (tuple[$tupleAccess] as %T).let { %T(it) }",
                    innerValue.type.toTypeName(), field.type.toTypeName(),
                )
            }
            return
        }

        addStatement("    val ${field.simpleName} = tuple[$tupleAccess] as %T", field.type.toTypeName())
    }

    private fun FileSpec.Builder.buildInfo(entityInfo: BaseInfo) = apply {
        val construction = entityInfo.construction()
        constructions[entityInfo.qualifiedName] = construction

        val declarationClassName = entityInfo.declaration.toClassName()

        if (construction.reflectionProps.isEmpty()) return@apply

        val infoObj = TypeSpec.objectBuilder(infoName(entityInfo.uniqueName))
        for (field in construction.reflectionProps) {
            val setterProp = PropertySpec
                .builder(infoField(field), poetMap.classMapping(FIELD))
                .initializer(
                    "%T::class.java.getDeclaredField(\"${field.simpleName}\").apply { isAccessible = true }",
                    declarationClassName
                )
                .build()
            infoObj.addProperty(setterProp)
        }
        addType(infoObj.build())
    }

    private fun FileSpec.Builder.buildRegistration(
        tupleCreators: Map<String, String>,
        assignmentCreators: Map<String, String>,
    ) {
        val registration = TypeSpec
            .classBuilder(buildUniqueClassName(packageName, "Registration"))
            .addAnnotation(poetMap.classMapping(COMPONENT))

        val eventListenerAnnotation = AnnotationSpec
            .builder(poetMap.classMapping(EVENT_LISTENER))
            .addMember("%T::class", poetMap.classMapping(APPLICATION_READY_EVENT))

        val register = FunSpec
            .builder("register")
            .addAnnotation(eventListenerAnnotation.build())
        register.apply {
            // Use · to omit line breaks
            // See: https://github.com/square/kotlinpoet/issues/598#issuecomment-454337042
            for ((viewName, creator) in tupleCreators) {
                addNamedStmt("$BLAZE_INSTANTIATORS.tupleInstantiators[\"$viewName\"]·=·$creator")
            }
            for ((viewName, creator) in assignmentCreators) {
                addNamedStmt("$BLAZE_INSTANTIATORS.assignmentInstantiators[\"$viewName\"]·=·$creator")
            }
        }

        registration.addFunction(register.build())
        addType(registration.build())
    }

    data class EntityConstruction(
        val constructorProps: List<EntityProperty>,
        val setterProps: List<EntityProperty>,
        val reflectionProps: List<EntityProperty>
    )

    private fun BaseInfo.construction(): EntityConstruction {
        val constructor = declaration.primaryConstructor
            ?: throw IllegalArgumentException("Class [${qualifiedName}] has no primary constructor.")

        val jpaProperties = jpaProperties
            .associateByTo(HashMap(jpaProperties.count())) {
                it.simpleName
            }

        val constructorProperties = constructor.parameters.map { parameter ->
            val pName = requireNotNull(parameter.name).asString()
            if (!parameter.isVal && !parameter.isVar) {
                throw IllegalArgumentException("Class [${qualifiedName}] has non managed constructor parameter type [$pName]")
            }
            val jpaProp = jpaProperties.remove(pName)
                ?: throw IllegalArgumentException("Class [${qualifiedName}] has non managed constructor parameter type [$pName]")

            jpaProp
        }

        val setterProperties = mutableListOf<EntityProperty>()
        val reflectionProperties = mutableListOf<EntityProperty>()
        for (jpaProp in jpaProperties.values) {
            val modifiers = jpaProp.declaration.modifiers
            if (
                modifiers.contains(Modifier.PRIVATE) ||
                modifiers.contains(Modifier.PROTECTED) ||
                modifiers.contains(Modifier.INTERNAL)
            ) {
                reflectionProperties.add(jpaProp)
            } else if (!jpaProp.declaration.isMutable) {
                reflectionProperties.add(jpaProp)
            } else setterProperties.add(jpaProp)
        }

        return EntityConstruction(constructorProperties, setterProperties, reflectionProperties)
    }



    private fun infoName(uniqueName: String): String
        = "${uniqueName}InstatiatorInfo"
    private fun infoField(prop: EntityProperty): String
        = "${prop.simpleName}Field"


    // TODO: Id column of superclass is present in columns of subclass!
    private fun BaseViewInfo.viewFields(): List<Property> {
        return when (this) {
            is EntityViewInfo -> {
                val id = entity.id
                val keys = relations.keys
                val props = (
                    entity.columns +
                        entity.lazyColumns +
                        entity.relations.filter { keys.contains(it.simpleName) }
                    ).sortedBy { it.simpleName }
                listOf(id) + props
            }
            is EmbeddedViewInfo -> {
                (embedded.columns + embedded.lazyColumns).sortedBy { it.simpleName }
            }
        }
    }

    private fun BaseViewInfo.entityFields(): List<Property> {
        return when (this) {
            is EntityViewInfo -> {
                val id = entity.id
                val props = (
                    entity.columns +
                        entity.lazyColumns +
                        entity.relations
                    ).sortedBy { it.simpleName }
                listOf(id) + props
            }
            is EmbeddedViewInfo -> {
                (embedded.columns + embedded.lazyColumns).sortedBy { it.simpleName }
            }
        }
    }

    private fun KSType.reflectionSetterName(): String {
        val rpType = declaration.simpleName.asString()
        return when (rpType) {
            "Double" -> "setDouble"
            "Float" -> "setFloat"
            "Long" -> "setLong"
            "Int" -> "setInt"
            "Short" -> "setShort"
            "Byte" -> "setByte"
            "Boolean" -> "setBoolean"
            "Char" -> "setChar"
            else -> "set"
        }
    }

}