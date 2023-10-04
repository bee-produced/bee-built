package com.beeproduced.bee.generative

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.ServiceLoader

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-04
 */
open class BeeGenerativePluginExtension {
    internal val configMap: MutableMap<String, String> = mutableMapOf()

    fun arg(k: String, v: String) {
        configMap[k] = v
    }
}

class BeeGenerativePlugin : Plugin<Project>{
    override fun apply(project: Project) {
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
        // TODO: Make transitive dependency requirement?
        // project.plugins.apply("com.google.devtools.ksp")
        val kspExtension = project.extensions.getByType(KspExtension::class.java)
        val action: Action<KspExtension> = Action {
            for ((k, v) in configMap)
                it.arg(k, v)
            println("hello! $configMap")
        }
        action.execute(kspExtension)

        // TODO: Remove later
        val serviceLoader = ServiceLoader.load(BeeGenerativeFeature::class.java)
        println("Found features: ${serviceLoader.count()}")
        for (feature in serviceLoader) {
            println("new feature: ${feature::class.java.name}")
            feature.process()
        }
    }
}