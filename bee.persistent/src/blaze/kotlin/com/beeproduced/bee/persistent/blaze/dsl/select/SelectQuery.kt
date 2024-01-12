package com.beeproduced.bee.persistent.blaze.dsl.select

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-10
 */
interface SelectQuery<T : Any> :
    SelectWhere<T>,
    SelectOrderBy<T>,
    Selection<T>
{


}

