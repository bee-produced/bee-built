package com.beeproduced.bee.persistent.blaze.dsl.predicate

import com.beeproduced.bee.persistent.blaze.dsl.expression.Expression
import com.blazebit.persistence.BaseWhereBuilder

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-10
 */
data class NotEqualValuePredicate<T : Any>(
    val expression: Expression<T>,
    val value: T?
) : PredicateExpression {
    override fun Predicate.expression(): Expression<*>
        = expression

    override fun <W : BaseWhereBuilder<W>> Predicate.applyBuilder(builder: W): W {
        val expressionString = expression.run { toExpressionString() }
        return if (value != null)
            builder.where(expressionString).notEq(value)
        else builder.where(expressionString).isNotNull()
    }
}
