package com.beeproduced.bee.persistent.blaze.dsl.sort

import com.blazebit.persistence.OrderByBuilder

/**
 * @author Kacper Urbaniec
 * @version 2024-01-12
 */
interface Sort {

  fun <W : OrderByBuilder<W>> Sort.applyBuilder(builder: W): W

  enum class Order(val sort: Boolean) {
    ASC(true),
    DESC(false),
  }
}
