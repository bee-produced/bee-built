package com.beeproduced.bee.persistent.blaze.processor.utils

import com.beeproduced.bee.persistent.blaze.processor.info.BaseInfo
import com.beeproduced.bee.persistent.blaze.processor.info.EntityProperty
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

/**
 * @author Kacper Urbaniec
 * @version 2024-01-13
 */
data class AccessInfo(val props: List<EntityProperty>, val reflectionProps: List<EntityProperty>)

fun BaseInfo.accessInfo(jpaPropsOnly: Boolean = true): AccessInfo {
  val propsToUse = if (jpaPropsOnly) jpaProperties else properties

  val props = mutableListOf<EntityProperty>()
  val reflectionProps = mutableListOf<EntityProperty>()

  for (prop in propsToUse) {
    val modifiers = prop.declaration.modifiers
    if (
      modifiers.contains(Modifier.PRIVATE) ||
        modifiers.contains(Modifier.PROTECTED) ||
        modifiers.contains(Modifier.INTERNAL)
    ) {
      reflectionProps.add(prop)
    } else props.add(prop)
  }

  return AccessInfo(props, reflectionProps)
}

data class ConstructionInfo(
  val constructorProps: List<EntityProperty>,
  val setterProps: List<EntityProperty>,
  val reflectionProps: List<EntityProperty>,
)

fun BaseInfo.constructionInfo(jpaPropsOnly: Boolean = true): ConstructionInfo {
  val constructor =
    declaration.primaryConstructor
      ?: throw IllegalArgumentException("Class [${qualifiedName}] has no primary constructor.")

  val propsToUse = if (jpaPropsOnly) jpaProperties else properties
  val props = propsToUse.associateByTo(HashMap(propsToUse.count())) { it.simpleName }

  val constructorProperties =
    constructor.parameters.map { parameter ->
      val pName = requireNotNull(parameter.name).asString()
      if (!parameter.isVal && !parameter.isVar) {
        throw IllegalArgumentException(
          "Class [${qualifiedName}] has non managed constructor parameter type [$pName]"
        )
      }
      val jpaProp =
        props.remove(pName)
          ?: throw IllegalArgumentException(
            "Class [${qualifiedName}] has non managed constructor parameter type [$pName]"
          )

      jpaProp
    }

  val setterProperties = mutableListOf<EntityProperty>()
  val reflectionProperties = mutableListOf<EntityProperty>()
  for (jpaProp in props.values) {
    val modifiers = jpaProp.declaration.modifiers
    if (
      modifiers.contains(Modifier.PRIVATE) ||
        modifiers.contains(Modifier.PROTECTED) ||
        modifiers.contains(Modifier.INTERNAL)
    ) {
      reflectionProperties.add(jpaProp)
    } else if (!jpaProp.declaration.isMutable) {
      reflectionProperties.add(jpaProp)
    } else setterProperties.add(jpaProp)
  }

  return ConstructionInfo(constructorProperties, setterProperties, reflectionProperties)
}

fun KSType.reflectionSetterName(): String {
  val rpType = declaration.simpleName.asString()
  return when (rpType) {
    "Double" -> "setDouble"
    "Float" -> "setFloat"
    "Long" -> "setLong"
    "Int" -> "setInt"
    "Short" -> "setShort"
    "Byte" -> "setByte"
    "Boolean" -> "setBoolean"
    "Char" -> "setChar"
    else -> "set"
  }
}

fun KSType.reflectionGetterName(): String {
  val rpType = declaration.simpleName.asString()
  return when (rpType) {
    "Double" -> "getDouble"
    "Float" -> "getFloat"
    "Long" -> "getLong"
    "Int" -> "getInt"
    "Short" -> "getShort"
    "Byte" -> "getByte"
    "Boolean" -> "getBoolean"
    "Char" -> "getChar"
    else -> "get"
  }
}
