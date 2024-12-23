package com.beeproduced.bee.persistent.test

import jakarta.persistence.EntityManager

/**
 * @author Kacper Urbaniec
 * @version 2024-01-01
 */
fun <T> EntityManager.beePersist(entity: T): T {
  persist(entity)
  flush()
  clear()
  return entity
}
