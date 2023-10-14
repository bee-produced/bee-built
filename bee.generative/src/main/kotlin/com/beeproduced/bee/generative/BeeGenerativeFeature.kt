package com.beeproduced.bee.generative

import com.beeproduced.bee.generative.processor.Options
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-04
 */
interface BeeGenerativeFeature {
    fun order(): Int
    fun multipleRoundProcessing(): Boolean

    fun setup(options: Options): BeeGenerativeConfig

    fun process(input: BeeGenerativeInput)
}

data class BeeGenerativeConfig(
    val packages: Set<String> = emptySet(),
    val annotatedBy: Set<String> = emptySet(),
) {
    companion object {
        fun merge(configs: Collection<BeeGenerativeConfig>): BeeGenerativeConfig {
            val packages = configs.mergeSets { it.packages }
            val annotatedBy = configs.mergeSets { it.annotatedBy }
            return BeeGenerativeConfig(packages, annotatedBy)
        }

        private fun <T> Collection<BeeGenerativeConfig>.mergeSets(selector: (BeeGenerativeConfig) -> Set<T>?): Set<T> {
            return this.fold(mutableSetOf()) { acc, config ->
                selector(config)?.let { acc.addAll(it) }
                acc
            }
        }
    }
}

data class BeeGenerativeSymbols(
    val packages: Map<String, Set<KSClassDeclaration>>,
    val annotatedBy: Map<String, Set<KSClassDeclaration>>,
)

data class BeeGenerativeInput(
    val codeGenerator: CodeGenerator,
    val dependencies: Dependencies,
    val logger: KSPLogger,
    val symbols: BeeGenerativeSymbols,
    val options: Map<String, String>
)

