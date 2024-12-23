package com.beeproduced.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Kacper Urbaniec
 * @version 2023-09-26
 */

/** Convenient way to create loggers. See: https://stackoverflow.com/a/48061770/12347616 */
inline fun <reified T : Any> logFor(): Logger = LoggerFactory.getLogger(T::class.java)
