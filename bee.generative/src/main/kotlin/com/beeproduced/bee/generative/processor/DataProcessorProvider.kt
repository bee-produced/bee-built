package com.beeproduced.bee.generative.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-08-03
 */
class DataProcessorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return DataProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
    }
}