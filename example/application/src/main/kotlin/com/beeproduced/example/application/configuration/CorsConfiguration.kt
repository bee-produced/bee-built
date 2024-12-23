package com.beeproduced.example.application.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration as CorsConstants
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * @author Kacper Urbaniec
 * @version 2022-05-04
 */
@Configuration
class CorsConfiguration {
  @Bean
  fun corsConfigurer(): WebMvcConfigurer {
    return object : WebMvcConfigurer {
      override fun addCorsMappings(registry: CorsRegistry) {
        registry
          .addMapping("/graphql/**")
          .allowedOrigins(CorsConstants.ALL)
          .allowedHeaders(CorsConstants.ALL)
          .allowedMethods(CorsConstants.ALL)
      }
    }
  }
}
