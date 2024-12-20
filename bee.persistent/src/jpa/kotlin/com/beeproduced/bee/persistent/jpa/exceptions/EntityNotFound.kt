package com.beeproduced.bee.persistent.jpa.exceptions

/**
 * @author Kacper Urbaniec
 * @version 2023-02-21
 */
class EntityNotFound(type: Class<*>) :
  RuntimeException(
    "Entity [$type] not found\nEntity is not part of the MetaModel, was a repository defined for it?"
  )
