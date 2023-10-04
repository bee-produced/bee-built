package com.beeproduced.bee.fetched

import com.beeproduced.bee.generative.BeeGenerativeConfig
import com.beeproduced.bee.generative.BeeGenerativeFeature
import com.beeproduced.bee.generative.BeeGenerativeInput
import com.google.devtools.ksp.symbol.KSVisitorVoid

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-04
 */
class BeeFetchedFeature : BeeGenerativeFeature {
    override fun setup(config: Map<String, String>): BeeGenerativeConfig {
        val scanPackage = config.getOrElse("fetchedScanPackage") {
            throw IllegalArgumentException("fetchedScanPackage not provided")
        }
        return BeeGenerativeConfig(packages = setOf(scanPackage))
    }

    override fun visitor(input: BeeGenerativeInput): KSVisitorVoid {
        TODO("Not yet implemented")
    }

}