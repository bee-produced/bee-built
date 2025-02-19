package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.PoetMap.Companion.addNStatementBuilder
import com.beeproduced.bee.generative.util.toPoetClassName
import com.beeproduced.bee.persistent.blaze.processor.FullyQualifiedName
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentBuilderCodegen.PoetConstants.BUILDER
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentBuilderCodegen.PoetConstants.CLAZZ
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentBuilderCodegen.PoetConstants.FIELD
import com.beeproduced.bee.persistent.blaze.processor.info.AbstractProperty
import com.beeproduced.bee.persistent.blaze.processor.info.EntityInfo
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
 * @author Kacper Urbaniec
 * @version 2024-01-13
 */
class BeePersistentBuilderCodegen(
  private val codeGenerator: CodeGenerator,
  private val dependencies: Dependencies,
  private val logger: KSPLogger,
  private val views: ViewInfo,
  private val entities: Map<FullyQualifiedName, EntityInfo>,
  private val config: BeePersistentBlazeConfig,
) {
  private val packageName = config.builderPackageName

  private val poetMap: PoetMap = PoetMap()

  private fun FunSpec.Builder.addNamedStmt(format: String) = addNStatementBuilder(format, poetMap)

  private fun CodeBlock.Builder.addNamedStmt(format: String) = addNStatementBuilder(format, poetMap)

  object PoetConstants {
    const val FIELD = "%field:T"
    const val CLAZZ = "%clazz:T"
    const val BUILDER = "%builder:T"
  }

  init {
    poetMap.addMapping(FIELD, ClassName("java.lang.reflect", "Field"))
  }

  fun processRepoBuilder(repos: List<RepoInfo>) {
    for (repo in repos) processRepoBuilder(repo)
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

    file.build().writeTo(codeGenerator, dependencies)
  }

  private fun FileSpec.Builder.buildBuilderModule(entity: EntityInfo) = apply {
    poetMap.addMapping(CLAZZ, entity.qualifiedName.toPoetClassName())
    buildInfo(entity) // Generate reflection info
    buildBuilder(entity) // Generate builder for cloning
    buildCloneFunction(entity) // Gerate clone extension function
  }

  private fun FileSpec.Builder.buildInfo(entity: EntityInfo) = apply {
    val access = entity.accessInfo(false)
    val hasPrimitiveId = entity.hasPrimitiveId()
    if (access.reflectionProps.isEmpty() && hasPrimitiveId) return@apply

    val infoObj = TypeSpec.objectBuilder(entity.infoName())
    val entityClassName = entity.declaration.toClassName()

    val properties = access.reflectionProps + entity.id

    for (property in properties) {
      val reflectionField =
        PropertySpec.builder(property.infoField(), poetMap.classMapping(FIELD))
          .initializer(
            "%T::class.java.getDeclaredField(\"${property.simpleName}\").apply·{·isAccessible·=·true·}",
            entityClassName,
          )
      infoObj.addProperty(reflectionField.build())
    }
    addType(infoObj.build())
  }

  private fun FileSpec.Builder.buildBuilder(entity: EntityInfo) = apply {
    val builderName = entity.builderName()
    poetMap.addMapping(BUILDER, ClassName(packageName, builderName))
    val entityBuilder = TypeSpec.classBuilder(builderName)

    val instance = "instance"
    val constructor =
      FunSpec.constructorBuilder().addParameter(instance, poetMap.classMapping(CLAZZ))

    entityBuilder.primaryConstructor(constructor.build())

    val accessInfo = entity.accessInfo(false)

    for (prop in accessInfo.props) {
      val propType = prop.type.toTypeName()
      val builderProperty =
        PropertySpec.builder(prop.simpleName, propType)
          .initializer("$instance.${prop.simpleName}")
          .mutable(true)
      entityBuilder.addProperty(builderProperty.build())
    }

    val infoObjName = entity.infoName()
    for (prop in accessInfo.reflectionProps) {
      val propType = prop.type.toTypeName()
      val getterName = prop.type.reflectionGetterName()
      val builderProperty =
        PropertySpec.builder(prop.simpleName, propType)
          .initializer("${infoObjName}.${prop.infoField()}.${getterName}(instance) as %T", propType)
          .mutable(true)
      entityBuilder.addProperty(builderProperty.build())
    }

    addType(entityBuilder.build())
  }

  private fun FileSpec.Builder.buildCloneFunction(entity: EntityInfo) = apply {
    val construction = entity.constructionInfo(false)

    val entityClassName = entity.declaration.toClassName()
    val builderClassName = poetMap.classMapping(BUILDER)
    val cloneFunction =
      FunSpec.builder("beeClone")
        .receiver(entityClassName)
        .returns(entityClassName)
        .addParameter("block", LambdaTypeName.get(receiver = builderClassName, returnType = UNIT))
    cloneFunction.apply {
      addNamedStmt("val builder = $BUILDER(this)")
      addNamedStmt("builder.block()")
      addNamedStmt("val entity = $CLAZZ(")
      for (property in construction.constructorProps) addStatement(
        "  ${property.simpleName} = builder.${property.simpleName},"
      )
      addStatement(")")
      for (property in construction.setterProps) addStatement(
        "entity.${property.simpleName} = builder.${property.simpleName}"
      )
      val infoObjName = entity.infoName()
      for (property in construction.reflectionProps) {
        val infoField = property.infoField()
        val setterName = property.type.reflectionSetterName()
        addStatement(
          "${infoObjName}.${infoField}.${setterName}(entity, builder.${property.simpleName})"
        )
      }
      addStatement("return entity")
    }
    addFunction(cloneFunction.build())
  }

  companion object {
    fun EntityInfo.builderName(): String = "${uniqueName}Builder"

    fun EntityInfo.infoName(): String = "${uniqueName}BuilderInfo"

    fun AbstractProperty.infoField(): String = "${simpleName}Field"

    fun EntityInfo.hasPrimitiveId(): Boolean {
      val type = id.type
      if (type.isMarkedNullable) return false
      val typeName = type.declaration.simpleName.asString()
      return when (typeName) {
        "Double" -> true
        "Float" -> true
        "Long" -> true
        "Int",
        "Short",
        "Byte" -> true
        "Boolean" -> true
        "Char" -> true
        else -> false
      }
    }
  }
}
