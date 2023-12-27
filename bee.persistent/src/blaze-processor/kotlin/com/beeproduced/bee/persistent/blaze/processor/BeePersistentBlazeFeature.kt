package com.beeproduced.bee.persistent.blaze.processor

import com.beeproduced.bee.generative.BeeGenerativeConfig
import com.beeproduced.bee.generative.BeeGenerativeFeature
import com.beeproduced.bee.generative.BeeGenerativeInput
import com.beeproduced.bee.generative.Shared
import com.beeproduced.bee.generative.processor.Options
import com.beeproduced.bee.generative.util.resolveTypeAlias
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentAnalyser
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentBlazeConfig
import com.beeproduced.bee.persistent.blaze.processor.codegen.BeePersistentViewCodegen
import com.beeproduced.bee.persistent.blaze.processor.info.*
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATIONS_RELATION
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_EMBEDDED
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_EMBEDDED_ID
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_GENERATED_VALUE
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_ID
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_INHERITANCE
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_LAZY_FIELD
import com.beeproduced.bee.persistent.blaze.processor.info.AnnotationInfo.ANNOTATION_TRANSIENT
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import jakarta.persistence.Entity

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-14
 */
class BeePersistentBlazeFeature : BeeGenerativeFeature {
    override fun order(): Int = Int.MAX_VALUE

    override fun multipleRoundProcessing(): Boolean = false

    override fun setup(options: Options, shared: Shared): BeeGenerativeConfig {

        val name = options["datasource"] ?: "meh"
        shared["datasource$name"] = "Hey"
        return BeeGenerativeConfig(
            packages = setOf(),
            annotatedBy = setOf(Entity::class.qualifiedName!!),
        )
    }

    override fun process(input: BeeGenerativeInput) {
        val logger = input.logger
        logger.info("Hey")
        logger.info(input.shared.keys.toString())

        val symbols = input.symbols

        val entityDeclarations = symbols.annotatedBy.getValue(Entity::class.qualifiedName!!)
        val entitiesWithoutInheritanceInfo = entityDeclarations.map { entityDeclaration ->
            val entityName = entityDeclaration.simpleName.asString()
            val entityAnnotations = resolveAnnotations(entityDeclaration.annotations)
            // logger.info(entityDeclaration.simpleName.asString())
            // logger.info(entityDeclaration.packageName.asString())

            // Properties
            val propertyDeclarations = entityDeclaration
                .getAllProperties()
                .filter { it.hasBackingField }
                .toList()

            // Placeholder is used for entities that inherit the id from
            // their superclass
            var idP: IdProperty = IdProperty.PLACEHOLDER
            val properties: MutableList<EntityProperty> = mutableListOf()
            val columns: MutableList<ColumnProperty> = mutableListOf()
            val lazyColumns: MutableList<ColumnProperty> = mutableListOf()
            val relations: MutableList<ColumnProperty> = mutableListOf()

            for (p in propertyDeclarations) {
                val propertyName = p.simpleName.asString()
                val annotations = resolveAnnotations(p.annotations)
                val type = p.type.resolve().resolveTypeAlias()

                val entityProperty = EntityProperty(p, type, annotations)
                properties.add(entityProperty)

                if (annotations.hasAnnotation(ANNOTATION_TRANSIENT)) continue

                val innerValue = getInnerValue(type)
                val hasId = annotations.hasAnnotation(ANNOTATION_ID)
                val hasEmbeddedId = annotations.hasAnnotation(ANNOTATION_EMBEDDED_ID)
                if (hasId || hasEmbeddedId) {
                    val isGenerated = annotations.hasAnnotation(ANNOTATION_GENERATED_VALUE)
                    val embedded = if (hasEmbeddedId) getEmbeddedInfo(type) else null
                    idP = IdProperty(p, type, annotations, innerValue, isGenerated, embedded)
                    continue
                }

                val isEmbedded = annotations.hasAnnotation(ANNOTATION_EMBEDDED)
                val embedded = if (isEmbedded) getEmbeddedInfo(type) else null
                val columnProperty = ColumnProperty(p, type, annotations, innerValue, embedded)
                val hasRelation = annotations.hasAnnotation(ANNOTATIONS_RELATION)
                if (hasRelation) {
                    if (!type.isMarkedNullable) {
                        throw IllegalArgumentException("Relation [$propertyName] of [$entityName] must be marked nullable")
                    }
                    relations.add(columnProperty)
                    continue
                }

                if (annotations.hasAnnotation(ANNOTATION_LAZY_FIELD)) lazyColumns.add(columnProperty)
                else columns.add(columnProperty)
            }

            EntityInfo(
                entityDeclaration, entityAnnotations,
                properties, idP, columns, lazyColumns, relations, null, null
            )
        }

        // Add inheritance info
        val inheritedEntities: MutableMap<String, EntityInfo> = entitiesWithoutInheritanceInfo
            .associateByTo(HashMap()) { it.qualifiedName!! }

        for (entityInfo in entitiesWithoutInheritanceInfo) {
            if (!entityInfo.annotations.hasAnnotation(ANNOTATION_INHERITANCE)) continue

            // Store subclasses for superclass
            val subClasses = entitiesWithoutInheritanceInfo.findSubclasses(entityInfo)
            val subClassNames = subClasses.mapTo(HashSet()) { it.qualifiedName!! }
            inheritedEntities[entityInfo.qualifiedName!!] = entityInfo
                .copy(subClasses = subClassNames)

            // Store superclass for subclass
            // Also set inherited id
            val superClassName = entityInfo.qualifiedName!!
            for (subClass in subClasses) {
                inheritedEntities[subClass.qualifiedName!!] = subClass
                    .copy(id = entityInfo.id, superClass = superClassName)
            }
        }

        // Debug
        for (entityInfo in inheritedEntities.values) {
            // logger.info(entityInfo.toString())
            logger.info(entityInfo.simpleName)
            logger.info(entityInfo.id.toString())
            logger.info(entityInfo.columns.toString())
            logger.info(entityInfo.relations.toString())
            logger.info("${entityInfo.superClass} X ${entityInfo.subClasses}")
            logger.info("---")
        }

        // Validate
        for (entityInfo in inheritedEntities.values) {
            if (entityInfo.id === IdProperty.PLACEHOLDER) {
                throw IllegalArgumentException("Entity [${entityInfo.simpleName}] has no ID")
            }
        }

        val config = BeePersistentBlazeConfig(
            "com.beeproduced.persistent.generated",
            2
        )

        // Process
        val entities = inheritedEntities.values.toList()
        val analyser = BeePersistentAnalyser(input.logger, config)
        val views = analyser.processEntities(entities)

        val viewCodeGen = BeePersistentViewCodegen(
            input.codeGenerator, input.dependencies, input.logger,
            inheritedEntities.values.toList(), config
        )
        viewCodeGen.processViews(views)
    }

    private fun resolveAnnotations(annotations: Sequence<KSAnnotation>): List<ResolvedAnnotation> {
        return annotations.map { a ->
            val type = a.annotationType.resolve().resolveTypeAlias()
            val declaration = type.declaration
            ResolvedAnnotation(a, declaration, type)
        }.toList()
    }

    private fun List<ResolvedAnnotation>.hasAnnotation(annotation: String): Boolean {
        return any { it.qualifiedName == annotation }
    }

    private fun List<ResolvedAnnotation>.hasAnnotation(annotations: Set<String>): Boolean {
        return any { annotations.contains(it.qualifiedName) }
    }

    private fun getInnerValue(type: KSType): ResolvedValue? {
        val declaration = type.declaration
        if (
            declaration !is KSClassDeclaration ||
            !declaration.annotations.any { it.shortName.asString() == "JvmInline" }
        ) return null

        val innerType = declaration.primaryConstructor
            ?.parameters?.firstOrNull()
            ?.type?.resolve()?.resolveTypeAlias()
            ?: return null

        val innerDeclaration = innerType.declaration
        return ResolvedValue(innerDeclaration, innerType)
    }

    private fun List<EntityInfo>.findSubclasses(superEntity: EntityInfo): Set<EntityInfo> {
        val subClasses = mutableSetOf<EntityInfo>()
        for (entity in this) {
            if (entity === superEntity) continue
            for (superType in entity.declaration.getAllSuperTypes()) {
                if (superType.declaration.qualifiedName?.asString() == superEntity.qualifiedName) {
                    subClasses.add(entity)
                }
            }
        }
        return subClasses
    }

    private fun getEmbeddedInfo(eType: KSType): EmbeddedInfo {
        val declaration = eType.declaration as KSClassDeclaration
        val propertyDeclarations = declaration
            .getAllProperties()
            .filter { it.hasBackingField }
            .toList()

        val columns: MutableList<ColumnProperty> = mutableListOf()
        val lazyColumns: MutableList<ColumnProperty> = mutableListOf()

        for (p in propertyDeclarations) {
            val annotations = resolveAnnotations(p.annotations)
            val type = p.type.resolve().resolveTypeAlias()
            val columnProperty = ColumnProperty(p, type, annotations, null, null)

            if (annotations.hasAnnotation(ANNOTATION_LAZY_FIELD)) lazyColumns.add(columnProperty)
            else columns.add(columnProperty)
        }

        return EmbeddedInfo(declaration, columns, lazyColumns)
    }
}