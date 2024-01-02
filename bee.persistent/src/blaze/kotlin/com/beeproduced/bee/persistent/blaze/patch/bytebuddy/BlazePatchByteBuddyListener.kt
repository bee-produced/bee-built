package com.beeproduced.bee.persistent.blaze.patch.bytebuddy

import com.beeproduced.bee.persistent.blaze.patch.bytebuddy.proxy.BlazeAssignmentInstantiatorPatch
import com.beeproduced.bee.persistent.blaze.patch.bytebuddy.proxy.BlazeTupleInstantiatorPatch
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplicationRunListener
import org.springframework.context.ConfigurableApplicationContext

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2024-01-02
 */
class BlazePatchByteBuddyListener : SpringApplicationRunListener {
    private val logger = LoggerFactory.getLogger(this::class.java)
    override fun contextPrepared(context: ConfigurableApplicationContext?) {
        runCatching {
            // See https://bytebuddy.net/#/tutorial
            // Chapter Delegating a method call
            // Since Java 9, an agent installation is also possible at runtime without a JDK-installation
            ByteBuddyAgent.install()
            val byteBuddy = ByteBuddy()
            BlazeTupleInstantiatorPatch
                .patchTupleConstructorReflectionInstantiator(byteBuddy)
            BlazeAssignmentInstantiatorPatch
                .patchAssignmentConstructorReflectionInstantiator(byteBuddy)
            logger.info("Added support bee.persistent.blaze")
        }.onFailure { e ->
            logger.warn("Could not add support for bee.persistent.blaze")
            logger.warn(e.stackTraceToString())
        }
    }
}