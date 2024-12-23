package com.beeproduced.bee.persistent.jpa.repository.dsl

import com.linecorp.kotlinjdsl.query.spec.ExpressionOrderSpec
import com.linecorp.kotlinjdsl.query.spec.OrderSpec
import com.linecorp.kotlinjdsl.query.spec.expression.ColumnSpec
import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * @author Kacper Urbaniec
 * @version 2023-12-19
 */
object DummyOrderSpec {

  fun interface ExpressionOrderSpecBreaker {
    fun getColumnSpec(self: ExpressionOrderSpec): ColumnSpec<*>
  }

  val expressionLambda = buildExpressionLambda()

  private fun buildExpressionLambda(): ExpressionOrderSpecBreaker {
    // Class `ExpressionOrderSpec` has no getters and even private ones are not generated
    // One could use reflection, but `LambdaMeta factory` is faster!
    // https://www.optaplanner.org/blog/2018/01/09/JavaReflectionButMuchFaster.html

    // We will make use of generated destructuring methods of data class ^^
    // → val (a, b) = test
    // As the field expression is private the destructuring method is also private
    // → Cannot access 'component1': it is private in 'ExpressionOrderSpec'
    // However it is still there! (unlike getters...)
    val clazz = ExpressionOrderSpec::class.java
    val method = clazz.getDeclaredMethod("component1")
    val lookup =
      MethodHandles.privateLookupIn(ExpressionOrderSpec::class.java, MethodHandles.lookup())
    val methodHandle = lookup.unreflect(method)

    return LambdaMetafactory.metafactory(
        lookup,
        ExpressionOrderSpecBreaker::getColumnSpec.name,
        MethodType.methodType(ExpressionOrderSpecBreaker::class.java),
        MethodType.methodType(ColumnSpec::class.java, ExpressionOrderSpec::class.java),
        methodHandle,
        MethodType.methodType(ColumnSpec::class.java, ExpressionOrderSpec::class.java),
      )
      .target
      .invokeExact() as ExpressionOrderSpecBreaker
  }
}

fun OrderSpec.getColumnSpec(): ColumnSpec<*> {
  return DummyOrderSpec.expressionLambda.getColumnSpec(this as ExpressionOrderSpec)
}
