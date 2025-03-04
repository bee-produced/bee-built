package com.beeproduced.example.application.configuration

import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher
import org.springframework.web.servlet.handler.HandlerMappingIntrospector

/**
 * @author Kacper Urbaniec
 * @version 2023-09-26
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(private val env: Environment) {

  @Throws(Exception::class)
  @Bean
  fun filterChain(
    http: HttpSecurity,
    introspector: HandlerMappingIntrospector,
  ): SecurityFilterChain {
    val mvcMatcherBuilder = MvcRequestMatcher.Builder(introspector)
    // Disable csrf
    // See: https://github.com/graphql-java-kickstart/graphql-spring-boot/issues/184
    http
      .csrf { csrf -> csrf.disable() }
      .headers { headers -> headers.frameOptions { frameOptions -> frameOptions.disable() } }
      .authorizeHttpRequests { authorize ->
        var auth =
          authorize
            .requestMatchers(
              mvcMatcherBuilder.pattern("/graphql"),
              mvcMatcherBuilder.pattern("/graphiql"),
              mvcMatcherBuilder.pattern("/schema.json"),
              mvcMatcherBuilder.pattern("/subscriptions"),
              mvcMatcherBuilder.pattern("/actuator/**"),
              mvcMatcherBuilder.pattern("/graphiql/**"),
              mvcMatcherBuilder.pattern("/h2-console"),
            )
            .permitAll()
        if (env.activeProfiles.contains("dev")) {
          auth = auth.requestMatchers(PathRequest.toH2Console()).permitAll()
        }
        auth.anyRequest().authenticated()
      }
    return http.build()
  }
}
