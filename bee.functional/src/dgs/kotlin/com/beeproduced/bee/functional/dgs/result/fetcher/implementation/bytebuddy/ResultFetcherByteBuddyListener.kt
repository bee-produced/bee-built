package com.beeproduced.bee.functional.dgs.result.fetcher.implementation.bytebuddy

import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplicationRunListener
import org.springframework.context.ConfigurableApplicationContext

/**
 * @author Kacper Urbaniec
 * @version 2023-11-04
 */
class ResultFetcherByteBuddyListener : SpringApplicationRunListener {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun contextPrepared(context: ConfigurableApplicationContext?) {
    runCatching {
        // See https://bytebuddy.net/#/tutorial
        // Chapter Delegating a method call
        // Since Java 9, an agent installation is also possible at runtime without a
        // JDK-installation
        ByteBuddyAgent.install()
        val byteBuddy = ByteBuddy()
        DataFetcherFactoriesRedefinition.makeWrapDataFetcherFunctional(byteBuddy)
        logger.info("Added support for result data fetchers")
      }
      .onFailure { e ->
        logger.warn("Could not redefine data fetcher")
        logger.warn(e.stackTraceToString())
        logger.warn("Fix or try aspect approach instead")
      }
  }
}
