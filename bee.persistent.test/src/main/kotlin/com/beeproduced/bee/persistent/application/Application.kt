package com.beeproduced.bee.persistent.application

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * @author Kacper Urbaniec
 * @version 2023-12-14
 */
@SpringBootApplication(scanBasePackages = ["com.beeproduced"]) class Application

fun main(args: Array<String>) {
  val application = SpringApplication(Application::class.java)
  application.run(*args)
}
