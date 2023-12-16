package com.beeproduced.datasource.b

import jakarta.persistence.*
import java.util.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-16
 */

// @Entity
// @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
// open class Composer(
//     @Id
//     val id: UUID,
//     val name: String
// )
//
// @Entity
// data class AiComposer(
//     override val id: UUID,
//     override val name: String,
//     val model: String,
//     @Embedded
//     val params: AiParams,
//     @Column(name = "ai_data_id")
//     val aiDataId: UUID? = null,
//     @OneToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "ai_data_id", insertable = false, updatable = false)
//     val aiData: AiData? = null
// ) : Composer(id, name)
//
// @Entity
// data class AiData(
//     @Id
//     val id: UUID,
//     val data: String
// )
//
// @Embeddable
// data class AiParams(
//     val z1: String,
//     val z2: String
// )
//
// @Entity
// data class HumanComposer(
//     override val id: UUID,
//     override val name: String,
//     val lastname: String,
//     @Column(name = "human_data_id")
//     val humanDataId: UUID? = null,
//     @OneToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "human_data_id", insertable = false, updatable = false)
//     val humanData: HumanData? = null
// ) : Composer(id, name)
//
// @Entity
// data class HumanData(
//     @Id
//     val id: UUID,
//     val data: String
// )
//
// @Entity
// data class ComposerContainer(
//     @Id
//     val id: UUID,
//     @Column(name = "c1_id")
//     val c1Id: UUID,
//     @OneToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "c1_id", insertable = false, updatable = false)
//     val c1: Composer?,
//     @Column(name = "c2_id")
//     val c2Id: UUID,
//     @OneToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "c2_id", insertable = false, updatable = false)
//     val c2: Composer?
// )