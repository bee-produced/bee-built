package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentInstantiatorCodegen.PoetConstants.FIELD
import com.beeproduced.bee.persistent.blaze.processor.info.EntityInfo
import com.beeproduced.bee.persistent.blaze.processor.info.EntityProperty
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import org.w3c.dom.Entity

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

    @Suppress("ConstPropertyName")
    object PoetConstants {
        const val FIELD = "%field:T"
    }

    init {
        poetMap.addMapping(FIELD, ClassName("java.lang.reflect", "Field"))
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


    private fun FileSpec.Builder.buildCreator(views: EntityViewInfo) = apply {

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
}