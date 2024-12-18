package com.beeproduced.bee.buzz.manager

import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * @author Kacper Urbaniec
 * @version 2022-02-09
 */
interface Scheduler {
  fun schedule(f: () -> Unit, delay: Long, unit: TimeUnit): ScheduledFuture<Unit>
}
