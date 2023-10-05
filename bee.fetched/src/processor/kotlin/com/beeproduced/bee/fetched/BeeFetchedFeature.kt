package com.beeproduced.bee.fetched

import com.beeproduced.bee.generative.BeeGenerativeConfig
import com.beeproduced.bee.generative.BeeGenerativeFeature
import com.beeproduced.bee.generative.BeeGenerativeInput
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-04
 */
class BeeFetchedFeature : BeeGenerativeFeature {
    override fun setup(config: Map<String, String>): BeeGenerativeConfig {
        val scanPackage = config.getOrElse("fetchedScanPackage") {
            throw IllegalArgumentException("fetchedScanPackage not provided")
        }
        return BeeGenerativeConfig(packages = setOf(scanPackage))
    }

    override fun visitor(input: BeeGenerativeInput): KSVisitorVoid {
        val scanPackage = input.config.getValue("fetchedScanPackage")
        val dtos = input.packages.getValue(scanPackage).let(::transform)
        val packageName = input.config.getOrElse("fetchedPackageName") {
            throw IllegalArgumentException("fetchedScanPackage not provided")
        }
        return BeeFetchedVisitor(input.codeGenerator, input.dependencies, input.logger, dtos, packageName)
    }

    data class DgsDto(
        val name: String,
        val properties: List<PropertyDetails>
    )

    data class PropertyDetails(
        val name: String,
        val nonCollectionType: String,
        val isCollection: Boolean
    )

    private fun transform(classes: Set<KSClassDeclaration>): Map<String, DgsDto> {
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