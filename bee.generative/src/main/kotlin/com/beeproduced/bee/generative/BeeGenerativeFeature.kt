package com.beeproduced.bee.generative

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import groovyjarjarantlr.CodeGenerator

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-04
 */
interface BeeGenerativeFeature {
    fun setup(config: Map<String, String>): BeeGenerativeConfig

    fun visitor(input: BeeGenerativeInput) : KSVisitorVoid
}

data class BeeGenerativeConfig(
    val packages: Set<String>? = null
)

data class BeeGenerativeInput(
    val codeGenerator: CodeGenerator,
    val dependencies: Dependencies,
    val logger: KSPLogger,
    val packages: Map<String, Set<KSClassDeclaration>>,
    val config: Map<String, String>
)