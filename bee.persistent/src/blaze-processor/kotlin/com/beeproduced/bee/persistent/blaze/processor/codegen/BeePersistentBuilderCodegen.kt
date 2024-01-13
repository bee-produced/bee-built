package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.PoetMap.Companion.addNStatementBuilder
import com.beeproduced.bee.generative.util.toPoetClassName
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentBuilderCodegen.PoetConstants.CLAZZ
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentBuilderCodegen.PoetConstants.FIELD
import com.beeproduced.bee.persistent.blaze.processor.info.EntityInfo
import com.beeproduced.bee.persistent.blaze.processor.info.EntityProperty
import com.beeproduced.bee.persistent.blaze.processor.info.RepoInfo
import com.beeproduced.bee.persistent.blaze.processor.utils.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-13
 */
typealias FullyQualifiedName = String

class BeePersistentBuilderCodegen(
    private val codeGenerator: CodeGenerator,
    private val dependencies: Dependencies,
    private val logger: KSPLogger,
    private val views: ViewInfo,
    private val entities: Map<FullyQualifiedName, EntityInfo>,
    private val config: BeePersistentBlazeConfig
) {
    private val packageName = config.builderPackageName

    private val poetMap: PoetMap = PoetMap()
    private fun FunSpec.Builder.addNamedStmt(format: String)
        = addNStatementBuilder(format, poetMap)
    private fun CodeBlock.Builder.addNamedStmt(format: String)
        = addNStatementBuilder(format, poetMap)

    object PoetConstants {
        const val FIELD = "%field:T"
        const val CLAZZ = "%clazz:T"
    }

    init {
        poetMap.addMapping(FIELD, ClassName("java.lang.reflect", "Field"))
    }

    fun processRepoBuilder(repos: List<RepoInfo>) {
        for (repo in repos)
            processRepoBuilder(repo)
    }

    private fun processRepoBuilder(repo: RepoInfo) {
        logger.info("processRepoBuilder($repo)")
        val view = views.findViewFromRepo(repo)
        val entity = view.entity


        val className = "${entity.uniqueName}Builder"

        val file = FileSpec.builder(packageName, className)
        if (entity.subClasses == null) file.buildBuilderModule(entity)
        else {
            val subEntities = entity.subClasses.map { entities.getValue(it) }
            for (subEntity in subEntities) file.buildBuilderModule(subEntity)
        }

        file
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun FileSpec.Builder.buildBuilderModule(entity: EntityInfo) = apply {
        poetMap.addMapping(CLAZZ, entity.qualifiedName.toPoetClassName())
        buildInfo(entity) // Generate reflection info
        buildBuilder(entity) // Generate builder for cloning
        // Gerate clone extension function
    }

    private fun FileSpec.Builder.buildBuilder(entity: EntityInfo) = apply {
        val entityBuilder = TypeSpec.classBuilder(entity.builderName())

        val instance = "instance"
        val constructor = FunSpec.constructorBuilder()
            .addParameter(instance, poetMap.classMapping(CLAZZ))

        entityBuilder.primaryConstructor(constructor.build())

        val accessInfo = entity.accessInfo(false)

        for (prop in accessInfo.props) {
            val propType = prop.type.toTypeName()
            val builderProperty = PropertySpec.builder(prop.simpleName, propType)
                .initializer("$instance.${prop.simpleName}")
                .mutable(true)
            entityBuilder.addProperty(builderProperty.build())
        }

        val infoObjName = entity.infoName()
        for (prop in accessInfo.reflectionProps) {
            val propType = prop.type.toTypeName()
            val getterName = prop.type.reflectionGettterName()
            val builderProperty = PropertySpec.builder(prop.simpleName, propType)
                .initializer("${infoObjName}.${prop.infoField()}.${getterName}(instance) as %T", propType)
                .mutable(true)
            entityBuilder.addProperty(builderProperty.build())
        }

        addType(entityBuilder.build())
    }

    private fun FileSpec.Builder.buildInfo(entity: EntityInfo) = apply {
        val construction = entity.accessInfo(false)
        if (construction.reflectionProps.isEmpty()) return@apply

        val infoObj = TypeSpec.objectBuilder(entity.infoName())
        val entityClassName = entity.declaration.toClassName()
        for (property in construction.reflectionProps) {
            val reflectionField = PropertySpec
                .builder(property.infoField(), poetMap.classMapping(FIELD))
                .initializer(
                    "%T::class.java.getDeclaredField(\"${property.simpleName}\").apply { isAccessible = true }",
                    entityClassName
                )
            infoObj.addProperty(reflectionField.build())
        }
        addType(infoObj.build())
    }

    private fun EntityInfo.builderName(): String = "${uniqueName}Builder"

    private fun EntityInfo.infoName(): String = "${uniqueName}BuilderInfo"

    private fun EntityProperty.infoField(): String = "${simpleName}Field"

}