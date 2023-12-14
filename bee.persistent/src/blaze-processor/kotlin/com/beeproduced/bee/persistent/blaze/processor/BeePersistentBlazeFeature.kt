package com.beeproduced.bee.persistent.blaze.processor

import com.beeproduced.bee.generative.BeeGenerativeConfig
import com.beeproduced.bee.generative.BeeGenerativeFeature
import com.beeproduced.bee.generative.BeeGenerativeInput
import com.beeproduced.bee.generative.Shared
import com.beeproduced.bee.generative.processor.Options
import com.beeproduced.bee.generative.util.resolveTypeAlias
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import jakarta.persistence.Entity
import jakarta.persistence.Transient

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-14
 */
class BeePersistentBlazeFeature : BeeGenerativeFeature {
    override fun order(): Int = Int.MAX_VALUE

    override fun multipleRoundProcessing(): Boolean = false

    override fun setup(options: Options, shared: Shared): BeeGenerativeConfig {

        val name = options["datasource"] ?: "meh"
        shared["datasource$name"] = "Hey"
        return BeeGenerativeConfig(
            packages = setOf(),
            annotatedBy = setOf(Entity::class.qualifiedName!!),
        )
    }

    override fun process(input: BeeGenerativeInput) {
        val logger = input.logger
        logger.info("Hey")
        logger.info(input.shared.keys.toString())

        val symbols = input.symbols

        val types = symbols.annotatedBy.getValue(Entity::class.qualifiedName!!)

        for (t in types) {
            logger.info(t.simpleName.asString())
            logger.info(t.packageName.asString())

            // Properties
            val properties = t.declarations
                .filterIsInstance<KSPropertyDeclaration>()
                .filter { it.annotations.none { isTransientAnnotation(it, logger)} }
                .filter { it.hasBackingField }
            for (p in properties) {
                val simpleName = p.simpleName.asString()
                val typeName = p.type.resolve().resolveTypeAlias().declaration.qualifiedName?.asString()
                val modifier = p.modifiers
                val valueClass = if (isValueClass(p))
                    getUnderlyingTypeOfValueClass(p)?.declaration?.qualifiedName?.asString()
                else "not value class"
                logger.info("  $simpleName, $typeName, $modifier, $valueClass")
            }

            logger.info("---")
        }
    }

    private fun isTransientAnnotation(annotation: KSAnnotation, logger: KSPLogger): Boolean {
        // Resolve the annotation to its KSClassDeclaration and compare it with Transient::class
        val check = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
        val anno = Transient::class.qualifiedName
        // logger.info("check $check")
        // logger.info("anno $anno")
        return check == anno
    }

    fun isValueClass(property: KSPropertyDeclaration): Boolean {
        val type = property.type.resolve().declaration
        return type is KSClassDeclaration && type.annotations.any { it.shortName.asString() == "JvmInline" }
    }

    fun getUnderlyingTypeOfValueClass(property: KSPropertyDeclaration): KSType? {
        val type = property.type.resolve().declaration
        if (type is KSClassDeclaration && type.annotations.any { it.shortName.asString() == "JvmInline" }) {
            // Assuming value classes have only one primary constructor property
            return type.primaryConstructor?.parameters?.firstOrNull()?.type?.resolve()
        }
        return null
    }
}