package com.beeproduced.bee.generative.processor

import com.beeproduced.bee.generative.BeeGenerativeConfig
import com.beeproduced.bee.generative.BeeGenerativeFeature
import com.beeproduced.bee.generative.BeeGenerativeInput
import com.beeproduced.bee.generative.BeeGenerativeSymbols
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.util.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-08-07
 */
typealias Options = Map<String, String>

class DataProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Options,
) : SymbolProcessor {
    private var invoked = false

    private val beeFeatures = ServiceLoader.load(
        BeeGenerativeFeature::class.java,
        DataProcessor::class.java.classLoader
    ).sortedBy { feature -> feature.order() }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (beeFeatures.isEmpty()) {
            logger.warn("No bee.features found")
            logger.warn("Possible misconfiguration detected")
            return emptyList()
        }

        // Not every feature supports ksp multiple round processing
        val features = if (!invoked) {
            invoked = true
            beeFeatures
        }
        else beeFeatures.filter { it.multipleRoundProcessing() }
        if (features.isEmpty()) return emptyList()

        // Acquire config per feature & merge them
        logger.info("Received Options $options")
        val config = features
            .map { it.setup(options) }
            .let(BeeGenerativeConfig.Companion::merge)
        logger.info("Received Config to parse $config")

        // Get symbols mandated by config
        val symbols = getSymbols(resolver, config)

        // Process feature implementations
        val dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray())
        val input = BeeGenerativeInput(
            codeGenerator = codeGenerator,
            dependencies = dependencies,
            logger = logger,
            symbols = symbols,
            options = options
        )
        features.forEach { feature ->
            logger.info("Processing ${feature::class.java.name} ...")
            feature.process(input)
        }

        logger.info( "Process finished")
        return emptyList()
    }

    @OptIn(KspExperimental::class)
    private fun getSymbols(resolver: Resolver, config: BeeGenerativeConfig): BeeGenerativeSymbols {
        val packages = mutableMapOf<String, Set<KSClassDeclaration>>()
        for (p in config.packages) {
            val symbols = resolver
                .getDeclarationsFromPackage(p)
                .mapNotNull { if (it is KSClassDeclaration) it else null }
                .toSet()
            packages[p] = symbols
        }
        val annotatedBy = mutableMapOf<String, Set<KSClassDeclaration>>()
        for (ab in config.annotatedBy) {
            val symbols = resolver
                .getSymbolsWithAnnotation(ab)
                .mapNotNull { if (it is KSClassDeclaration) it else null }
                .toSet()
            annotatedBy[ab] = symbols
        }

        return BeeGenerativeSymbols(packages, annotatedBy)
    }
}