package com.beeproduced.example.application.configuration

import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-09-26
 */

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val env: Environment
) {

    @Throws(Exception::class)
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        // Disable csrf
        // See: https://github.com/graphql-java-kickstart/graphql-spring-boot/issues/184
        http
            .csrf().disable()
            .headers { headers ->
                headers.frameOptions().disable()
            }
            .authorizeHttpRequests { authorize ->
                var auth = authorize
                    .requestMatchers(
                        "/graphql",
                        "/graphiql",
                        "/schema.json",
                        "/subscriptions",
                        "/actuator/**",
                        "/graphiql/**",
                    )
                    .permitAll()
                if (env.activeProfiles.contains("dev")) {
                    auth = auth.requestMatchers(PathRequest.toH2Console()).permitAll()
                }
                auth
                    .anyRequest()
                    .authenticated()
            }
        return http.build()
    }
}