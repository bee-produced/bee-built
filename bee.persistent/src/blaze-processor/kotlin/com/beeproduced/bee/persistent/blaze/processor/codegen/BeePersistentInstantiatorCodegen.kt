package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.PoetMap.Companion.addNStatementBuilder
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.BLAZE_INSTANTIATORS
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.FIELD
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.TCI
import com.beeproduced.bee.persistent.blaze.processor.info.EntityInfo
import com.beeproduced.bee.persistent.blaze.processor.info.EntityProperty
import com.beeproduced.bee.persistent.blaze.processor.info.Property
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
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
    }

    init {
        poetMap.addMapping(FIELD, ClassName("java.lang.reflect", "Field"))
        poetMap.addMapping(BLAZE_INSTANTIATORS, ClassName("com.beeproduced.bee.persistent.blaze.meta.proxy", "BlazeInstantiators"))
        poetMap.addMapping(TCI, ClassName("com.beeproduced.bee.persistent.blaze.meta.proxy", "TupleConstructorInstantiator"))
    }

    fun processViews(views: ViewInfo, entities: List<EntityInfo>) {
        val file = FileSpec.builder(packageName, fileName)

        for (entity in entities) {
            // Do not generate info for super classes
            if (entity.subClasses != null) continue
            file.buildInfo(entity)
        }

        for (entityView in views.entityViews.values) {
            // Do not generate creators for super classes
            if (entityView.entity.subClasses != null) continue
            file.buildCreator(entityView)
        }

        file.build().writeTo(codeGenerator, dependencies)
    }


    private fun FileSpec.Builder.buildCreator(view: EntityViewInfo) = apply {
        val construction = constructions.getValue(view.qualifiedName)
        val decClassName = view.entity.declaration.toClassName()

        val entityFields = view.entityFields()
        val viewFields = view.viewFields()
        val keys = viewFields.mapTo(HashSet()) { it.simpleName }
        val missingFields = entityFields.filter { !keys.contains(it.simpleName) }

        val property = PropertySpec.builder(
            "${view.name}__Creator",
            poetMap.classMapping(TCI)
        )

        // Use · to omit line breaks
        // See: https://github.com/square/kotlinpoet/issues/598#issuecomment-454337042
        val initializer = CodeBlock.builder().apply {
            addNamedStmt("$TCI(")
            addNamedStmt("  viewProperties = emptyList(),")
            addNamedStmt("  create = { tuple: Array<Any?>, sort: IntArray -> ")
            for ((index, field) in viewFields.withIndex()) {
                addStatement("    val ${field.simpleName} = tuple[sort[$index]] as %T", field.declaration.type.toTypeName())
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
            addNamedStmt(").also·{·$BLAZE_INSTANTIATORS.tupleInstantiators[\"${view.name}\"]·=·it·}")
        }.build()

        addProperty(property.initializer(initializer).build())
    }

    private fun FileSpec.Builder.buildInfo(entityInfo: EntityInfo) = apply {
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

    data class EntityConstruction(
        val constructorProps: List<EntityProperty>,
        val setterProps: List<EntityProperty>,
        val reflectionProps: List<EntityProperty>
    )

    private fun EntityInfo.construction(): EntityConstruction {
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
    private fun EntityViewInfo.viewFields(): List<Property> {
        val id = entity.id
        val keys = relations.keys
        val props = (
            entity.columns.filter { it.simpleName != id.simpleName } +
            entity.lazyColumns +
            entity.relations.filter { keys.contains(it.simpleName) }
        ).sortedBy { it.simpleName }

        return listOf(id) + props
    }

    private fun EntityViewInfo.entityFields(): List<Property> {
        val id = entity.id
        val props = (
            entity.columns.filter { it.simpleName != id.simpleName } +
            entity.lazyColumns +
            entity.relations
        ).sortedBy { it.simpleName }
        return listOf(id) + props
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