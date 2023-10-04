package com.beeproduced.data.selection

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-04-14
 */

interface SkipOver {
    val field: String
    val targetField: String
    val type: Class<*>?
}

class SkipOverCollection(
    skips: Collection<SkipOver>
) {
    private val skipsByName: MutableMap<String, SkipOver> = mutableMapOf()
    private val skipsByType: MutableMap<Class<*>, MutableMap<String, SkipOver>> = mutableMapOf()
    private val removedSkips = mutableListOf<SkipOver>()

    init {
        for (skip in skips) add(skip)
    }

    fun skipOver(field: String, type: Class<*>): String? {
        val skipByType = skipsByType[type]?.get(field)

        if (skipByType != null) {
            if (skipByType is SkipOverOnce) {
                skipsByType.getValue(type).remove(field)
                skipsByName.remove(field)
                removedSkips.add(skipByType)
            }
            return skipByType.targetField
        } else {
            val skipByName = skipsByName[field] ?: return null
            if (skipByName is SkipOverOnce) {
                skipsByName.remove(field)
                removedSkips.add(skipByName)
            }
            return skipByName.targetField
        }
    }

    fun add(skip: SkipOver) {
        val field = skip.field
        val type = skip.type
        if (type != null) {
            if (!skipsByType.containsKey(type)) skipsByType[type] = mutableMapOf()
            skipsByType.getValue(type)[field] = skip
        }
        skipsByName[field] = skip
    }

    fun remainingSkips(): List<SkipOver> = skipsByName.values.toList()
    fun removedSkips(): List<SkipOver> = skipsByName.values.toList()
}

data class SkipOverAll(
    override val field: String,
    override val targetField: String,
    override val type: Class<*>?
) : SkipOver {
    constructor(field: String, targetField: String) : this(field, targetField, null)
}

data class SkipOverOnce(
    override val field: String,
    override val targetField: String,
    override val type: Class<*>?,
) : SkipOver {
    constructor(field: String, targetField: String) : this(field, targetField, null)
}