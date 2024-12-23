package com.beeproduced.example.application

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * @author Kacper Urbaniec
 * @version 2022-09-25
 */
@SpringBootApplication(scanBasePackages = ["com.beeproduced"]) class Application

fun main(args: Array<String>) {
  val application = SpringApplication(Application::class.java)
  application.run(*args)
}
