package com.beeproduced.bee.generative

import com.google.devtools.ksp.gradle.KspExtension
import java.util.Properties
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.kotlin.dsl.closureOf

/**
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
  operator fun invoke(dependencyNotation: String): Triple<Dependency?, Dependency?, Dependency?> {
    val main = dependencies.add("implementation", dependencyNotation)
    val processorMain = dependencies.add("ksp", dependencyNotation)
    val processorCapability =
      dependencies.add(
        "ksp",
        dependencyNotation,
        closureOf<DefaultExternalModuleDependency> {
          val capabilityNotation = "$group:$name-processor${versionString(version)}"
          capabilities { requireCapability(capabilityNotation) }
        },
      )
    return Triple(main, processorMain, processorCapability)
  }

  operator fun invoke(
    dependencyNotation: String,
    capability: String,
  ): Triple<Dependency?, Dependency?, Dependency?> {
    val capabilityMain =
      dependencies.add(
        "implementation",
        dependencyNotation,
        closureOf<DefaultExternalModuleDependency> {
          val capabilityNotation = "$group:$name-$capability${versionString(version)}"
          capabilities { requireCapability(capabilityNotation) }
        },
      )
    val processorMain =
      dependencies.add(
        "ksp",
        dependencyNotation,
        closureOf<DefaultExternalModuleDependency> {
          val capabilityNotation = "$group:$name-$capability${versionString(version)}"
          capabilities { requireCapability(capabilityNotation) }
        },
      )
    val processorCapability =
      dependencies.add(
        "ksp",
        dependencyNotation,
        closureOf<DefaultExternalModuleDependency> {
          val capabilityNotation = "$group:$name-$capability-processor${versionString(version)}"
          capabilities { requireCapability(capabilityNotation) }
        },
      )
    return Triple(capabilityMain, processorMain, processorCapability)
  }

  private fun versionString(version: String?): String {
    return if (version == null) "" else ":$version"
  }
}

class BeeGenerativePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    // Get version
    // https://discuss.gradle.org/t/how-can-a-custom-gradle-plugin-determine-its-own-version/36761/3
    val props = Properties()
    val propStream = javaClass.classLoader.getResourceAsStream("bee.generative.properties")
    propStream?.use { props.load(it) }
    val version = props.getProperty("version")

    // Adds bee generative to ksp context
    project.dependencies.add("ksp", "com.beeproduced:bee.generative:$version")
    // Allows importing bee generative features easily via `beeGenerative´ in dependencies block
    project.dependencies.extensions.add("beeGenerative", BeeDependencies(project.dependencies))
    // Allows configuring of bee generative features via `beeGenerative` block
    val extension =
      project.extensions.create("beeGenerative", BeeGenerativePluginExtension::class.java)
    // Passes bee generative options to ksp which it builds upon
    project.afterEvaluate { configureKsp(project, extension.configMap) }
  }

  private fun configureKsp(project: Project, configMap: Map<String, String>) {
    val kspExtension = project.extensions.getByType(KspExtension::class.java)
    val action: Action<KspExtension> = Action { for ((k, v) in configMap) arg(k, v) }
    action.execute(kspExtension)
  }
}
