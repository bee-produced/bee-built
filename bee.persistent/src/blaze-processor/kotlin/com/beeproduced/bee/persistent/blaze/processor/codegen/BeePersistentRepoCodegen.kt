package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.persistent.blaze.processor.info.EntityInfo
import com.beeproduced.bee.persistent.blaze.processor.info.RepoInfo
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
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

    fun processRepo(repo: RepoInfo) {
        logger.info("processRepo($repo)")
        repoInterface = repo.repoInterface
        view = findView(repo)
        entity = view.entity
        className = "Generated${entity.simpleName}Repository"
        FileSpec
            .builder(packageName, className)
            .buildRepo()
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun FileSpec.Builder.buildRepo(): FileSpec.Builder = apply {
        addType(
            TypeSpec.classBuilder(className)
                .addAnnotation(ClassName("org.springframework.stereotype", "Component"))
                .addSuperinterface(repoInterface.toClassName())
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
}