package com.beeproduced.data.jpa.meta

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-06-06
 */
class GeneratedInfo(
    instance: Any,
    val memberInfo: MemberInfo,
) {
    val defaultValue: Any?

    init {
        defaultValue = if (memberInfo.type.isPrimitive) memberInfo.field.get(instance)
        else null
    }
}