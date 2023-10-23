package com.beeproduced.bee.persistent.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration("PersistenceOrder")
@EnableJpaRepositories(
    basePackages = ["com.beeproduced.bee.persistent"],
    entityManagerFactoryRef = "orderEntityManager",
    transactionManagerRef = "orderTransactionManager"
)
class PersistenceConfiguration {
    // See: https://www.baeldung.com/spring-data-jpa-multiple-databases

    @Autowired
    private lateinit var env: Environment

    @Bean(name = ["orderDataSource"])
    @ConfigurationProperties(prefix = "spring.datasource-order")
    fun orderDataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

    @Bean(name = ["orderEntityManager"])
    fun orderEntityManager(
        @Qualifier("orderDataSource") orderDataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.setDataSource(orderDataSource)
        em.setPackagesToScan("com.beeproduced.bee.persistent")
        val vendorAdapter = HibernateJpaVendorAdapter()
        em.setJpaVendorAdapter(vendorAdapter)
        val properties: HashMap<String, Any> = HashMap()
        properties["hibernate.hbm2ddl.auto"] = env.getProperty("spring.jpa.hibernate.ddl-auto")!!
        properties["hibernate.dialect"] = env.getProperty("spring.jpa.database-platform")!!
        em.setJpaPropertyMap(properties)
        return em
    }

    @Bean(name = ["orderTransactionManager"])
    fun orderTransactionManager(@Qualifier("orderEntityManager") orderEntityManager: AbstractEntityManagerFactoryBean): PlatformTransactionManager {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = orderEntityManager.getObject()
        return transactionManager
    }
}
