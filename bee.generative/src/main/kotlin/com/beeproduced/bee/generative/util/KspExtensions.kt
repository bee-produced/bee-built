package com.beeproduced.bee.generative.util

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-14
 */

// Recursively resolve type alias to real type info
fun KSType.resolveTypeAlias(): KSType {
    var currentType = this
    while (currentType.declaration is KSTypeAlias) {
        currentType = (currentType.declaration as KSTypeAlias).type.resolve()
    }
    return currentType
}

inline fun <reified T : Annotation> KSClassDeclaration.getAnnotation(): KSAnnotation? {
    return annotations.firstOrNull { it.shortName.asString() == T::class.simpleName }
}

@JvmName("getArgumentValue")
fun KSAnnotation.argumentValue(argumentName: String): Any? {
    // Returns primitive values or more sophisticated KSP wrapper (KSType, KSAnnotation, ...)
    return arguments.find { it.name?.asString() == argumentName }?.value
}

inline fun <reified T: Any> KSAnnotation.argumentValue(argumentName: String): T {
    return argumentValue(argumentName) as T
}

inline fun <reified T: Any> KSAnnotation.safeArgumentValue(argumentName: String, default: T): T {
    return argumentValue(argumentName) as? T ?: default
}