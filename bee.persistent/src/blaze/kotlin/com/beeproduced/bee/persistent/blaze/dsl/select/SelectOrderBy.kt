package com.beeproduced.bee.persistent.blaze.dsl.select

import com.beeproduced.bee.persistent.blaze.dsl.sort.Sort

/**
 * @author Kacper Urbaniec
 * @version 2024-01-11
 */
interface SelectOrderBy<T> : Selection<T> {
  fun orderBy(vararg sorts: Sort): Selection<T>
}
