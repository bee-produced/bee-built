package com.beeproduced.bee.persistent.blaze.patch.bytebuddy.objectbuilder

import com.blazebit.persistence.FullQueryBuilder
import com.blazebit.persistence.ParameterHolder
import com.blazebit.persistence.SelectBuilder
import com.blazebit.persistence.view.impl.objectbuilder.SecondaryMapper
import com.blazebit.persistence.view.impl.objectbuilder.ViewTypeObjectBuilder
import com.blazebit.persistence.view.impl.objectbuilder.mapper.AliasExpressionTupleElementMapper
import com.blazebit.persistence.view.impl.objectbuilder.mapper.TupleElementMapper
import com.blazebit.persistence.view.spi.EmbeddingViewJpqlMacro
import com.blazebit.persistence.view.spi.ViewJpqlMacro
import java.util.*
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.Argument
import net.bytebuddy.implementation.bind.annotation.FieldValue
import net.bytebuddy.matcher.ElementMatchers

/**
 * Based on
 * https://github.com/Blazebit/blaze-persistence/blob/a05387e68de2518344fc8ddfde1d7d1b46c246b0/entity-view/impl/src/main/java/com/blazebit/persistence/view/impl/objectbuilder/ViewTypeObjectBuilder.java
 * Should fix: https://github.com/Blazebit/blaze-persistence/issues/1522 Also incorporates:
 * https://github.com/Blazebit/blaze-persistence/issues/1832
 *
 * @author Kacper Urbaniec
 * @version 2024-01-02
 */
open class BlazeViewTypeObjectBuilderPatch {

  companion object {
    fun patchViewTypeObjectBuilder(byteBuddy: ByteBuddy) {
      byteBuddy
        .redefine(ViewTypeObjectBuilder::class.java)
        .method(ElementMatchers.named("applySelects"))
        .intercept(MethodDelegation.to(BlazeViewTypeObjectBuilderPatch::class.java))
        .make()
        .load(
          ViewTypeObjectBuilder::class.java.getClassLoader(),
          ClassReloadingStrategy.fromInstalledAgent(),
        )
    }

    @JvmStatic
    @Suppress("unused")
    fun <X : SelectBuilder<X?>?> applySelects(
      @Argument(0) queryBuilder: X,
      @FieldValue("fetches") fetches: NavigableSet<String>?,
      @FieldValue("secondaryMappers") secondaryMappers: Array<SecondaryMapper>,
      @FieldValue("mappers") mappers: Array<TupleElementMapper>,
      @FieldValue("parameterHolder") parameterHolder: ParameterHolder<*>,
      @FieldValue("optionalParameters") optionalParameters: Map<String, Any>,
      @FieldValue("viewJpqlMacro") viewJpqlMacro: ViewJpqlMacro,
      @FieldValue("embeddingViewJpqlMacro") embeddingViewJpqlMacro: EmbeddingViewJpqlMacro,
    ) {
      if (fetches.isNullOrEmpty()) {
        if (secondaryMappers.isNotEmpty()) {
          val fullQueryBuilder = queryBuilder as FullQueryBuilder<*, *>
          for (viewRoot in secondaryMappers) {
            viewRoot.apply(
              fullQueryBuilder,
              parameterHolder,
              optionalParameters,
              viewJpqlMacro,
              embeddingViewJpqlMacro,
            )
          }
        }
        for (i in mappers.indices) {
          mappers[i].applyMapping(
            queryBuilder,
            parameterHolder,
            optionalParameters,
            viewJpqlMacro,
            embeddingViewJpqlMacro,
            false,
          )
        }
      } else {
        if (secondaryMappers.isNotEmpty()) {
          val fullQueryBuilder = queryBuilder as FullQueryBuilder<*, *>
          for (viewRoot in secondaryMappers) {
            if (hasSubFetches(viewRoot.attributePath, fetches)) {
              viewRoot.apply(
                fullQueryBuilder,
                parameterHolder,
                optionalParameters,
                viewJpqlMacro,
                embeddingViewJpqlMacro,
              )
            }
          }
        }
        for (i in mappers.indices) {
          val mapper = mappers[i]
          val attributePath = mapper.attributePath
          if (attributePath != null && hasSubFetches(attributePath, fetches)) {
            mapper.applyMapping(
              queryBuilder,
              parameterHolder,
              optionalParameters,
              viewJpqlMacro,
              embeddingViewJpqlMacro,
              false,
            )
          } else {
            // Should fix problem with inheritance
            // Should fetch discriminator column instead of selecting “NULL”
            if (mapper is AliasExpressionTupleElementMapper) {
              val alias = mapper.alias
              if (alias.endsWith("_class")) {
                mapper.applyMapping(
                  queryBuilder,
                  parameterHolder,
                  optionalParameters,
                  viewJpqlMacro,
                  embeddingViewJpqlMacro,
                  false,
                )
                continue
              }
            }
            queryBuilder!!.select("NULL")
          }
        }
      }
    }

    private fun hasSubFetches(attributePath: String, fetches: NavigableSet<String>): Boolean {
      val fetchedPath = fetches.ceiling(attributePath)
      return fetchedPath != null &&
        fetchedPath.startsWith(attributePath) &&
        (fetchedPath.length == attributePath.length || fetchedPath[attributePath.length] == '.')
    }
  }
}
