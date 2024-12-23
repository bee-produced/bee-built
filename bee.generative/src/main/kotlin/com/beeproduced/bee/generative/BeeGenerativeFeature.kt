package com.beeproduced.bee.generative

import com.beeproduced.bee.generative.processor.Options
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * @author Kacper Urbaniec
 * @version 2023-10-04
 */
interface BeeGenerativeFeature {
  fun order(): Int

  fun multipleRoundProcessing(): Boolean

  fun setup(options: Options, shared: Shared): BeeGenerativeConfig

  fun process(input: BeeGenerativeInput)
}

typealias Shared = MutableMap<String, Any?>

typealias BeeGenerativeCallback =
  (symbols: BeeGenerativeSymbols, options: Options, shared: Shared) -> BeeGenerativeConfig

data class BeeGenerativeConfig(
  val packages: Set<String> = emptySet(),
  val annotatedBy: Set<String> = emptySet(),
  val classes: Set<String> = emptySet(),
  val callbacks: List<BeeGenerativeCallback> = emptyList(),
) {
  companion object {
    fun merge(configs: Collection<BeeGenerativeConfig>): BeeGenerativeConfig {
      val packages = configs.mergeSets { it.packages }
      val annotatedBy = configs.mergeSets { it.annotatedBy }
      val classes = configs.mergeSets { it.classes }
      val callbacks = configs.mergeLists { it.callbacks }
      return BeeGenerativeConfig(packages, annotatedBy, classes, callbacks)
    }

    private fun <T> Collection<BeeGenerativeConfig>.mergeSets(
      selector: (BeeGenerativeConfig) -> Set<T>?
    ): Set<T> {
      return this.fold(mutableSetOf()) { acc, config ->
        selector(config)?.let { acc.addAll(it) }
        acc
      }
    }

    private fun <T> Collection<BeeGenerativeConfig>.mergeLists(
      selector: (BeeGenerativeConfig) -> List<T>?
    ): List<T> {
      return this.fold(mutableListOf()) { acc, config ->
        selector(config)?.let { acc.addAll(it) }
        acc
      }
    }
  }
}

data class BeeGenerativeSymbols(
  val packages: Map<String, Set<KSClassDeclaration>>,
  val annotatedBy: Map<String, Set<KSClassDeclaration>>,
  val classes: Map<String, KSClassDeclaration>,
)

data class BeeGenerativeInput(
  val codeGenerator: CodeGenerator,
  val dependencies: Dependencies,
  val logger: KSPLogger,
  val symbols: BeeGenerativeSymbols,
  val options: Options,
  val shared: Shared,
)
