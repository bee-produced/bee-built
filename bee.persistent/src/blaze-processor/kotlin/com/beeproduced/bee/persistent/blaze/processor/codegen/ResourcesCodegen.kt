package com.beeproduced.bee.persistent.blaze.processor.codegen

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-13
 */
class ResourcesCodegen(
    private val codegen: CodeGenerator
) {
    private val listener = mutableListOf<String>()

    fun addSpringListener(fullyQualifiedName: String) {
        listener.add(fullyQualifiedName)
    }

    fun process() {
        if (listener.isEmpty()) return

        val listenerValue = listener.joinToString(",")
        // Based on https://stackoverflow.com/a/76545160/12347616
        codegen.createNewFileByPath(
            Dependencies(false),
            "META-INF/spring", "factories"
        ).use { out ->
            out.write("org.springframework.context.ApplicationListener=$listenerValue".toByteArray())
        }
    }

}