package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.PoetMap.Companion.addNStatementBuilder
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentAnalyser.Companion.viewName
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.SELECTION_INFO
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants._SELECTION_INFO_VAL
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentRepoCodegen.PoetConstants.__SELECTION_INFO_NAME
import com.beeproduced.bee.persistent.blaze.processor.info.ColumnProperty
import com.beeproduced.bee.persistent.blaze.processor.info.EntityInfo
import com.beeproduced.bee.persistent.blaze.processor.info.EntityProperty
import com.beeproduced.bee.persistent.blaze.processor.info.RepoInfo
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
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

    private val poetMap: PoetMap = PoetMap()
    private fun FunSpec.Builder.addNamedStmt(format: String)
        = addNStatementBuilder(format, poetMap)
    private fun CodeBlock.Builder.addNamedStmt(format: String)
        = addNStatementBuilder(format, poetMap)

    @Suppress("ConstPropertyName")
    object PoetConstants {
        const val _ID_PROPERTY = "%idProperty:L"
        const val SELECTION_INFO = "%selectioninfo:T"
        const val _SELECTION_INFO_VAL = "%selectioninfoval:L"
        const val __SELECTION_INFO_NAME = "%selectioninfoname:S"
    }

    init {
        poetMap.addMapping(SELECTION_INFO, ClassName("com.beeproduced.bee.persistent.blaze.meta", "SelectionInfo"))
    }

    fun processRepo(repo: RepoInfo) {
        logger.info("processRepo($repo)")
        repoInterface = repo.repoInterface
        view = findView(repo)
        entity = view.entity
        className = "${entity.uniqueName}Repository"
        FileSpec
            .builder(packageName, className)
            .buildCreate()
            .buildRepo()
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun FileSpec.Builder.buildRepo(): FileSpec.Builder = apply {
        addType(
            TypeSpec.classBuilder(className)
                .addAnnotation(ClassName("org.springframework.stereotype", "Component"))
                .addSuperinterface(repoInterface.toClassName())
                .buildSelectionInfo()
                .build()
        )
    }

    private fun findView(repo: RepoInfo): EntityViewInfo {
        val qualifiedName = repo.entityType.declaration.qualifiedName?.asString()
        val view = views.entityViews.values.firstOrNull { view ->
            view.name.endsWith("Core") && view.qualifiedName == qualifiedName
        } ?: throw IllegalArgumentException("No view for [$qualifiedName] found")
        return view
    }

    // TODO: Extract to own class?
    // TODO: Should be generated for all classes, not only views
    private fun FileSpec.Builder.buildCreate(): FileSpec.Builder = apply {
        val inputMap = ClassName("kotlin.collections", "Map")
            .parameterizedBy(
                ClassName("kotlin", "String"),
                ClassName("kotlin", "Any").copy(
                    nullable = true
                )
            )

        val declaration = entity.declaration
        val decClassName = declaration.toClassName()

        val constructor = declaration.primaryConstructor
            ?: throw IllegalArgumentException("Class [${entity.qualifiedName}] has no primary constructor.")

        val jpaProperties = entity.jpaProperties
            .associateByTo(HashMap(entity.jpaProperties.count())) {
                it.simpleName
            }

        val constructorProperties = constructor.parameters.map { parameter ->
            val pName = requireNotNull(parameter.name).asString()
            if (!parameter.isVal && !parameter.isVar) {
                throw IllegalArgumentException("Class [${entity.qualifiedName}] has non managed constructor parameter type [$pName]")
            }
            val jpaProp = jpaProperties.remove(pName)
                ?: throw IllegalArgumentException("Class [${entity.qualifiedName}] has non managed constructor parameter type [$pName]")

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

        val fieldClassName = ClassName("java.lang.reflect", "Field")
        val setterObjName = "${entity.uniqueName}Fields"
        if (reflectionProperties.isNotEmpty()) {
            val setterObj = TypeSpec.objectBuilder(setterObjName)
            for (reflectionProperty in reflectionProperties) {
                val setterProp = PropertySpec
                    .builder(reflectionPropertyName(reflectionProperty), fieldClassName)
                    .initializer("%T::class.java.getDeclaredField(\"${reflectionProperty.simpleName}\").apply { isAccessible = true }", decClassName)
                    .build()
                setterObj.addProperty(setterProp)
            }
            addType(setterObj.build())
        }

        val create = FunSpec.builder("create${entity.uniqueName}")
            .returns(decClassName)
            .addParameter("values", inputMap)
            .addStatement("val entity = %T(", decClassName)
            .apply {
                for (cP in constructorProperties) {
                    addStatement("  ${cP.simpleName} = values.getValue(\"${cP.simpleName}\") as %T,", cP.type.toTypeName())
                }
            }
            .addStatement(")")
            .apply {
                for (sP in setterProperties) {
                    sP.type.isMarkedNullable
                    addStatement("entity.${sP.simpleName} = values.getValue(\"${sP.simpleName}\") as %T", sP.type.toTypeName())
                }
            }
            .apply {
                for (rP in reflectionProperties) {
                    val setterName = if (rP.type.isMarkedNullable) "set"
                    else {
                        val rpType = rP.type.declaration.simpleName.asString()
                        when (rpType) {
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
                    val setterPropertyName = reflectionPropertyName(rP)
                    addStatement("${setterObjName}.${setterPropertyName}.${setterName}(entity, values.getValue(\"${rP.simpleName}\") as %T)", rP.type.toTypeName())
                }
            }
            .addStatement("return entity")
            .build()
        addFunction(create)
    }

    private fun reflectionPropertyName(prop: EntityProperty) = "${prop.simpleName}Field"

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

    // TODO: Is ID column required for fetch?
    // If so, add!
    // TODO: Map inheritance fields!
    private fun CodeBlock.Builder.traverseSelectionInfo(
        entityView: EntityViewInfo
    ): CodeBlock.Builder = apply {
        val entity = entityView.entity

        val relationMapInput = entityView.relations.map {
            (simpleName, viewName) -> Pair(simpleName, viewName)
        }
        val relationSI = selectionInfoMappedValues(relationMapInput)


        val (columns, embedded) = entity.columns
            .partition { !it.isEmbedded }
        val columnSI = selectionInfoListValues(columns)
        val embeddedMapInput = embedded.map {
           val viewName = viewName(requireNotNull(it.embedded))
           Pair(it.simpleName, viewName)
        }
        val embeddedSI = selectionInfoMappedValues(embeddedMapInput)

        val (lazyColumns, lazyEmbedded) = entity.lazyColumns
            .partition { !it.isEmbedded }
        val lazyColumnSI = selectionInfoListValues(lazyColumns)
        val lazyEmbeddedMapInput = lazyEmbedded.map {
            val viewName = viewName(requireNotNull(it.embedded))
            Pair(it.simpleName, viewName)
        }
        val lazyEmbeddedSI = selectionInfoMappedValues(lazyEmbeddedMapInput)

        poetMap.addMapping(_SELECTION_INFO_VAL, entityView.name.lowercase())
        poetMap.addMapping(__SELECTION_INFO_NAME, entityView.name)
        addNamedStmt("  val $_SELECTION_INFO_VAL = $SELECTION_INFO(")
        addNamedStmt("    view = $__SELECTION_INFO_NAME,")
        addNamedStmt("    relations = mapOf($relationSI),")
        addNamedStmt("    columns = setOf($columnSI),")
        addNamedStmt("    lazyColumns = setOf($lazyColumnSI),")
        addNamedStmt("    embedded = mapOf($embeddedSI),")
        addNamedStmt("    lazyEmbedded = mapOf($lazyEmbeddedSI)")
        addNamedStmt("  )")
    }

    private fun CodeBlock.Builder.selectionInfoMappedValues(
        simpleNameAndViewName: List<Pair<String, String>>
    ): String {
        for ((_, viewName) in simpleNameAndViewName) {
            val view = views.entityViews.getValue(viewName)
            traverseSelectionInfo(view)
        }
        return simpleNameAndViewName.joinToString(separator = ", ") {
            (p, v) -> "\"$p\" to ${v.lowercase()}"
        }
    }

    private fun selectionInfoListValues(columnProperties: List<ColumnProperty>) : String
        = columnProperties.joinToString(separator = ", ") { "\"${it.simpleName}\"" }
}