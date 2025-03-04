package com.beeproduced.bee.persistent.jpa.meta

import com.beeproduced.bee.persistent.jpa.exceptions.EntityNotFound
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.jvm.kotlinProperty
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.persister.entity.EntityPersister
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Kacper Urbaniec
 * @version 2023-01-30
 */
typealias Entities = MutableMap<Class<out Any>, EntityInfo>

object MetaModel {
  private val entities: Entities = ConcurrentHashMap()
  private val entityPersisters: MutableMap<Class<out Any>, EntityPersister> = mutableMapOf()
  private val logger: Logger = LoggerFactory.getLogger(MetaModel::class.java)

  fun addEntity(
    entityType: Class<out Any>,
    idType: Class<out Any>,
    entityPersister: EntityPersister,
  ): EntityInfo {
    if (entities.containsKey(entityType)) return entities.getValue(entityType)

    val instance = createInstance(entityType)
    val ids = Ids(mutableMapOf())
    val fields = Fields(mutableMapOf())
    val relations = Relations(mutableMapOf())
    val generated = Generated(mutableMapOf())

    for (field in entityType.declaredFields) {
      // Omit companion objects & other static properties
      if (Modifier.isStatic(field.modifiers)) {
        logger.debug("Omitting field [{}]", field)
        continue
      }

      if (MembersInfo.isTransient(field)) {
        logger.debug("Omitting transient field [{}]", field)
        continue
      }

      val member = requireNotNull(field.kotlinProperty)
      val name = member.name
      val metaInfo = MemberInfo(member, field)

      if (MembersInfo.isGenerated(field)) {
        generated[name] = GeneratedInfo(instance, metaInfo)
      }

      if (MembersInfo.isFieldId(field)) {
        ids[name] = metaInfo
      } else if (MembersInfo.isFieldRelation(field)) {
        val fieldType = member.returnType
        val isNullable = fieldType.isMarkedNullable
        if (!isNullable) {
          logger.warn(
            "Relation [$member] of [$entityType] is not marked nullable, this can lead to errors when relation is lazy loaded"
          )
        }

        relations[name] = metaInfo
        MembersInfo.alternativeSelectionName(field)?.let { altName ->
          relations[altName] = metaInfo
        }
      } else {
        fields[name] = metaInfo
      }
    }

    val membersInfo = MembersInfo(ids, fields, relations, generated)
    val entityInfo = EntityInfo(entityType, idType, membersInfo)
    entities[entityType] = entityInfo
    entityPersisters[entityType] = entityPersister
    return entityInfo
  }

  fun getEntityInfo(entityClass: Class<out Any>): EntityInfo {
    if (!entities.containsKey(entityClass)) {
      throw EntityNotFound(entityClass)
    }
    return entities.getValue(entityClass)
  }

  fun getMembersInfo(entityClass: Class<out Any>): MembersInfo {
    if (!entities.containsKey(entityClass)) {
      throw EntityNotFound(entityClass)
    }
    return entities.getValue(entityClass).members
  }

  fun getEntityIdentifier(entity: Any, entityClass: Class<out Any>): Any {
    if (!entityPersisters.containsKey(entityClass)) {
      throw EntityNotFound(entityClass)
    }
    // https://stackoverflow.com/a/3335062/12347616
    // Directly without session
    // Reversed from source code
    return entityPersisters
      .getValue(entityClass)
      .getIdentifier(entity, null as SharedSessionContractImplementor?)
  }

  // Inspired by
  // * EntityInstantiatorPojoStandard - instantiate
  // * BasicEntityIdentifierMappingImpl - unsavedStrategy
  // * AbstractEntityPersister - isTransient
  private fun createInstance(type: Class<*>): Any {
    val constructor = type.getConstructor()
    return constructor.newInstance()
  }
}
