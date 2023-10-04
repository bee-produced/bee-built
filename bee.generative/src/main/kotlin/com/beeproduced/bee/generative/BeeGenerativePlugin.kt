package com.beeproduced.bee.generative

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.kotlin.dsl.closureOf

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-04
 */
open class BeeGenerativePluginExtension {
    // TODO: Allow varargs?
    internal val configMap: MutableMap<String, String> = mutableMapOf()

    fun arg(k: String, v: String) {
        configMap[k] = v
    }
}

open class BeeDependencies(private val dependencies: DependencyHandler) {
    operator fun invoke(dependencyNotation: String): Dependency? {
        return dependencies.add("ksp", dependencyNotation, closureOf<DefaultExternalModuleDependency> {
            val capabilityNotation = if (version != null) {
                "$group:$name-processor:$version"
            } else "$group:$name-processor"
            capabilities {
                requireCapability(capabilityNotation)
            }
        })
    }
}

class BeeGenerativePlugin : Plugin<Project>{
    override fun apply(project: Project) {
        project.dependencies.extensions.add(
            "bee", BeeDependencies(project.dependencies)
        )
        val extension = project
            .extensions
            .create(
                "beeGenerative",
                BeeGenerativePluginExtension::class.java,
            )
        project.afterEvaluate {
            configureKsp(project, extension.configMap)
        }
    }

    private fun configureKsp(project: Project, configMap: Map<String, String>) {
        val kspExtension = project.extensions.getByType(KspExtension::class.java)
        val action: Action<KspExtension> = Action {
            for ((k, v) in configMap)
                arg(k, v)
            println("hello! $configMap")
        }
        action.execute(kspExtension)
    }
}