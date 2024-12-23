package com.beeproduced.example.application.configuration

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.hibernate.validator.HibernateValidator
import org.hibernate.validator.HibernateValidatorConfiguration
import org.hibernate.validator.cfg.ConstraintMapping
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory

/**
 * @author Kacper Urbaniec
 * @version 2022-05-09
 */
@Configuration("MonoValidationConfiguration")
class ValidationConfiguration {
  // Allow validators to access beans
  // See: https://stackoverflow.com/a/37975083/12347616

  @Bean
  fun validatorConfig(
    autowireCapableBeanFactory: AutowireCapableBeanFactory
  ): HibernateValidatorConfiguration {
    return Validation.byProvider(HibernateValidator::class.java)
      .configure()
      .constraintValidatorFactory(SpringConstraintValidatorFactory(autowireCapableBeanFactory))
  }

  @Bean
  fun validator(
    validatorConfig: HibernateValidatorConfiguration,
    mappings: List<ConstraintMapping>,
  ): Validator {
    mappings.forEach { m -> validatorConfig.addMapping(m) }
    val validatorFactory = validatorConfig.buildValidatorFactory()
    return validatorFactory.validator
  }
}
