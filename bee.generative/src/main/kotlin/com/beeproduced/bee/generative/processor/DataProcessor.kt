package com.beeproduced.bee.generative.processor

import com.beeproduced.bee.generative.BeeGenerativeFeature
import com.beeproduced.bee.generative.BeeGenerativeInput
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
class DataProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    private var invoked = false

    private val packageNameProp = "packageName"

    data class DgsDto(
        val name: String,
        val properties: List<PropertyDetails>
    )

    data class PropertyDetails(
        val name: String,
        val nonCollectionType: String,
        val isCollection: Boolean
    )

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()

        val serviceLoader = ServiceLoader.load(
            BeeGenerativeFeature::class.java,
            DataProcessor::class.java.classLoader
        )
        // if (!options.containsKey(packageNameProp))
        //     throw Exception("Please provide package name to ksp via `$packageNameProp` property")
        // val packageName = options.getValue(packageNameProp)
        //
        // val foundDtos = mutableListOf<KSClassDeclaration>()
        //
        // val dl = resolver
        //     .getSymbolsWithAnnotation(AutoFetcher::class.qualifiedName!!)
        //     .mapNotNull { if (it is KSClassDeclaration) it else null }
        //     .toList()
        // val dlNames = dl.map { it.simpleName.asString() }
        // logger.warn("Found DL $dlNames")
        //
        // resolver.getAllFiles().forEach { ksFile ->
        //     ksFile.declarations.forEach { ksDeclaration ->
        //         if (ksDeclaration is KSClassDeclaration) {
        //             val ksPackageName = ksDeclaration.packageName.asString()
        //             if (ksPackageName == packageName)
        //                 foundDtos.add(ksDeclaration)
        //         }
        //
        //     }
        // }
        //
        // val names = foundDtos.map { it.simpleName.asString() }
        // logger.warn("Found DTOs $names")
        // val dtos = transform(foundDtos)
        // logger.warn("$dtos")
        //
        // val dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray())
        // for (dataLoader in dl) {
        //     dataLoader.accept(AutoFetcherVisitor(codeGenerator, dtos, dependencies, logger), Unit)
        // }
        // TODO: Modularize approach, currently hardcoded
        logger.warn("$options")
        val dl = resolver
            .getSymbolsWithAnnotation("com.beeproduced.bee.fetched.annotations.BeeFetched")
            .mapNotNull { if (it is KSClassDeclaration) it else null }
            .toList()
        val dlNames = dl.map { it.simpleName.asString() }
        logger.warn("Found DL $dlNames")

        val packageName = options.getValue("fetchedScanPackage")
        // val foundDtos = mutableListOf<KSClassDeclaration>()
        // resolver.getAllFiles().forEach { ksFile ->
        //     ksFile.declarations.forEach { ksDeclaration ->
        //         if (ksDeclaration is KSClassDeclaration) {
        //             val ksPackageName = ksDeclaration.packageName.asString()
        //             if (ksPackageName == packageName)
        //                 foundDtos.add(ksDeclaration)
        //         }
        //
        //     }
        // }
        val foundDtos = resolver.getDeclarationsFromPackage(packageName)
            .mapNotNull { if (it is KSClassDeclaration) it else null }
            .toList()

        val names = foundDtos.map { it.simpleName.asString() }
        logger.warn("Found DTOs $names")
        val dtos = transform(foundDtos)
        // logger.warn("$dtos")


        for (feature in serviceLoader) {
            logger.warn("New feature in ksp: ${feature::class.java.name}")
            // feature.process()
            logger.warn(feature::class.java.name)
            val config = feature.setup(options)
            logger.warn("Config: $config")
            val dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray())

            val packages = mutableMapOf(packageName to foundDtos.toSet())

            val input = BeeGenerativeInput(codeGenerator, dependencies, logger, packages, options)
            val visitor = feature.visitor(input)

            for (dataLoader in dl) {
                dataLoader.accept(visitor, Unit)
            }

        }
        logger.warn("Service loader: ${serviceLoader.count()}")


        invoked = true
        return emptyList()
    }



    private fun transform(classes: List<KSClassDeclaration>): Map<String, DgsDto> {
        return classes.map { ksClass ->
            val properties = ksClass.getAllProperties().map { property ->
                val type = property.type.resolve()
                if (type.declaration.qualifiedName?.asString() == "kotlin.collections.List" && type.arguments.size == 1) {
                    PropertyDetails(
                        name = property.simpleName.asString(),
                        nonCollectionType = type.arguments.first().type?.resolve()?.declaration?.qualifiedName?.asString() ?: "Unknown",
                        isCollection = true
                    )
                } else {
                    PropertyDetails(
                        name = property.simpleName.asString(),
                        nonCollectionType = type.declaration.qualifiedName?.asString() ?: "Unknown",
                        isCollection = false
                    )
                }
            }.toList()
            DgsDto(
                name = ksClass.qualifiedName?.asString() ?: "Unknown",
                properties = properties
            )
        }.associateBy { it.name }
    }

}