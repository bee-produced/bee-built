package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.beeproduced.bee.generative.util.PoetMap
import com.beeproduced.bee.generative.util.addNStatementBuilder
import com.beeproduced.bee.persistent.blaze.processor.info.EntityInfo
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.writeTo

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-16
 */
class BeePersistentViewCodegen(
    private val codeGenerator: CodeGenerator,
    private val dependencies: Dependencies,
    private val logger: KSPLogger,
    private val entities: List<EntityInfo>,
    private val config: BeePersistentBlazeConfig
) {
    private val packageName: String = "com.beeproduced.bee.persistent.generated"
    private val fileName: String = "GeneratedViews"


    private val poetMap: PoetMap = mutableMapOf()
    private fun FunSpec.Builder.addNStatement(format: String)
        = addNStatementBuilder(format, poetMap)
    @Suppress("ConstPropertyName")
    object PoetConstants {

    }

    private val viewCount: MutableMap<String, Int> = entities
        .map { it.qualifiedName!! }
        .associateWithTo(HashMap()) { 0 }

    fun processEntities() {
        FileSpec
            .builder(packageName, fileName)
            .build()
            .writeTo(codeGenerator, dependencies)
    }

}