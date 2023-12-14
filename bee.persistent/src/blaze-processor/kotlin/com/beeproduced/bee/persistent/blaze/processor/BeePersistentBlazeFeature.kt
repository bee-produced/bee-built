package com.beeproduced.bee.persistent.blaze.processor

import com.beeproduced.bee.generative.BeeGenerativeConfig
import com.beeproduced.bee.generative.BeeGenerativeFeature
import com.beeproduced.bee.generative.BeeGenerativeInput
import com.beeproduced.bee.generative.Shared
import com.beeproduced.bee.generative.processor.Options

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-14
 */
class BeePersistentBlazeFeature : BeeGenerativeFeature {
    override fun order(): Int = Int.MAX_VALUE

    override fun multipleRoundProcessing(): Boolean = false

    override fun setup(options: Options, shared: Shared): BeeGenerativeConfig {

        val name = options["datasource"] ?: "meh"
        shared["datasource$name"] = "Hey"
        return BeeGenerativeConfig(
            packages = setOf(),
            annotatedBy = setOf(),
        )
    }

    override fun process(input: BeeGenerativeInput) {
        val logger = input.logger
        logger.info("Hey")
        logger.info(input.shared.keys.toString())
    }
}