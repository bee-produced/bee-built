package com.beeproduced.lib.data.pagination

import com.beeproduced.data.jpa.entity.DataEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "paginated_foos")
data class PaginatedFoo(
    @Id
    @GeneratedValue
    val id: Long = 0,
    val createdBy: String,
    @Column(columnDefinition = "TIMESTAMP(6)")
    val createdOn: Instant
) : DataEntity<PaginatedFoo> {
    override fun clone(): PaginatedFoo = this.copy()
}