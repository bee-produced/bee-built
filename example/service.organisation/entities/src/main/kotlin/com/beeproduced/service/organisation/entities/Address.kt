package com.beeproduced.service.organisation.entities

import com.beeproduced.bee.persistent.jpa.entity.DataEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
typealias AddressId = UUID

@Entity
@Table(name = "addresses")
data class Address(
  @Id @GeneratedValue val id: AddressId,
  @Column(name = "address_line1") val addressLine1: String,
  @Column(name = "address_line2", nullable = true) val addressLine2: String?,
  @Column(name = "zip_code") val zipCode: String,
  val city: String,
) : DataEntity<Address> {
  override fun clone(): Address = this.copy()
}
