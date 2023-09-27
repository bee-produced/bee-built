package com.beeproduced.service.organisation.entities.input

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-27
 */
data class CreateAddressInput(
    val addressLine1: String,
    val addressLine2: String? = null,
    val zipCode: String,
    val city: String
)