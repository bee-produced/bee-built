@file:Suppress("NAME_SHADOWING")

package com.beeproduced.bee.persistent.jpa.proxy

import com.beeproduced.bee.persistent.jpa.meta.MemberInfo
import com.beeproduced.bee.persistent.jpa.meta.MetaModel
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy

/**
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */
object Unproxy {

  private class VisitedKey(val type: Class<*>, val id: Any) {
    override fun equals(other: Any?): Boolean {
      // if (this === other) return true
      // if (javaClass != other?.javaClass) return false

      other as VisitedKey

      if (type != other.type) return false
      if (id != other.id) return false

      return true
    }

    override fun hashCode(): Int {
      var result = type.hashCode()
      result = 31 * result + id.hashCode()
      // result += id.hashCode()
      return result
    }
  }

  private class Visited {
    val visited = mutableSetOf<VisitedKey>()

    fun hasVisited(key: VisitedKey): Boolean {
      return visited.contains(key)
    }

    fun addVisited(key: VisitedKey) {
      visited.add(key)
    }
  }

  inline fun <reified T : Any> unproxyEntity(entity: T) {
    return unproxyEntity(entity, T::class.java)
  }

  fun <T : Any> unproxyEntity(entity: T, type: Class<*>) {
    unproxyEntitiesRecursive(listOf(entity), type, Visited())
  }

  inline fun <reified T : Any> unproxyEntities(entities: Collection<T>) {
    unproxyEntities(entities, T::class.java)
  }

  fun unproxyEntities(entities: Collection<Any>, type: Class<*>) {
    unproxyEntitiesRecursive(entities, type, Visited())
  }

  private fun unproxyEntitiesRecursive(
    entities: Collection<Any>,
    type: Class<*>,
    visited: Visited,
  ) {
    if (entities.isEmpty()) return

    val members = MetaModel.getMembersInfo(type)
    for (relation in members.relations.values) {
      if (relation.isCollection) {
        unproxyCollectionMember(entities, relation, visited)
      } else {
        unproxyMember(entities, relation, visited)
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun unproxyCollectionMember(
    targets: Collection<Any>,
    fieldInfo: MemberInfo,
    visited: Visited,
  ) {
    val flattenedEntities = mutableListOf<Any>()
    val entityClass = requireNotNull(fieldInfo.nonCollectionType)

    for (target in targets) {
      // Skip null values, already valid state
      val entities = fieldInfo.get(target) ?: continue
      entities as Collection<Any>
      // Continue with initialized members, others set to null
      if (isInitialized(entities)) {
        // Check if object was already visited to omit
        // endless recursion aka stack overflow
        for (entity in entities) {
          val key =
            VisitedKey(
              fieldInfo.nonCollectionType,
              MetaModel.getEntityIdentifier(entity, fieldInfo.nonCollectionType),
            )
          if (!visited.hasVisited(key)) {
            flattenedEntities.add(entity)
            visited.addVisited(key)
          }
        }
      } else {
        fieldInfo.set(target, null)
      }
    }

    unproxyEntitiesRecursive(flattenedEntities, entityClass, visited)
  }

  private fun unproxyMember(targets: Collection<Any>, fieldInfo: MemberInfo, visited: Visited) {
    val flattenedEntities = mutableListOf<Any>()
    val entityClass = requireNotNull(fieldInfo.type)

    for (target in targets) {
      // Skip null values, already valid state
      val possibleProxyEntity = fieldInfo.get(target) ?: continue
      // Continue with initialized members, others set to null
      if (isInitialized(possibleProxyEntity)) {
        // Get concrete object if not already
        val entity =
          if (possibleProxyEntity is HibernateProxy) {
            Hibernate.unproxy(possibleProxyEntity).also { entity ->
              // Replace proxy with concrete object in parent
              fieldInfo.set(target, entity)
            }
          } else possibleProxyEntity
        // Check if object was already visited to omit
        // endless recursion aka stack overflow
        val key =
          VisitedKey(
            fieldInfo.nonCollectionType,
            MetaModel.getEntityIdentifier(entity, fieldInfo.nonCollectionType),
          )
        if (!visited.hasVisited(key)) {
          flattenedEntities.add(entity)
          visited.addVisited(key)
        }
      } else {
        fieldInfo.set(target, null)
      }
    }

    unproxyEntitiesRecursive(flattenedEntities, entityClass, visited)
  }

  private fun isInitialized(entity: Any?): Boolean {
    // TODO REFACTORING: Analyse if this has side effects
    // Reasoning: Query described in
    // https://git.beeproduced.com/beeproduced/backend-spring/-/issues/114
    // loads unexpectedly the company of CompanyMember even when it is not selected.
    // It is also not "fully loaded", normally with the loadgraph API no proxy but the object is
    // returned
    // but in this case an unexpected proxy is returned instead of null.
    // Even though it is a proxy but more own lazy loaded properties it is marked internally as
    // initialized.
    // Update: 15.06.2023
    // Query described here https://git.beeproduced.com/beeproduced/backend-spring/-/issues/176
    // returns proxy even if selected in loadgraph, therefore reverting to old behavior but
    // additionally
    // unproxy entities and set them via reflection
    // return entity != null && entity !is HibernateProxy
    return Hibernate.isInitialized(entity)
  }

  private fun isInitialized(entities: Collection<Any>): Boolean {
    return Hibernate.isInitialized(entities)
  }
}
