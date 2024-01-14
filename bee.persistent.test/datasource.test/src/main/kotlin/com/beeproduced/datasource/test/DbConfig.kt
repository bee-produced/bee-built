package com.beeproduced.datasource.test

import com.beeproduced.bee.persistent.blaze.annotations.EnableBeeRepositories
import com.blazebit.persistence.Criteria
import com.blazebit.persistence.CriteriaBuilderFactory
import com.blazebit.persistence.integration.view.spring.impl.EntityViewComponentProvider
import com.blazebit.persistence.integration.view.spring.impl.SpringTransactionSupport
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration
import com.blazebit.persistence.view.EntityView
import com.blazebit.persistence.view.EntityViewManager
import com.blazebit.persistence.view.EntityViews
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.util.ClassUtils
import javax.sql.DataSource

@Configuration
@EnableBeeRepositories(
    basePackages = ["com.beeproduced.datasource.test"],
    entityManagerFactoryRef = "testEM",
    criteriaBuilderFactoryRef = "testCBF",
    entityViewManagerRef = "testEVM"
)
@EnableJpaRepositories(
    basePackages = ["com.beeproduced.datasource.test"],
    entityManagerFactoryRef = "testEM",
    transactionManagerRef = "testTM"
)
class DbConfigB(val env: Environment) {
    @Bean(name = ["testDataSource"])
    @ConfigurationProperties(prefix = "spring.datasource-test")
    fun aDataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

    @Bean(name = ["testEM"])
    fun aEntityManager(@Qualifier("testDataSource") dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.setDataSource(dataSource)
        em.setPackagesToScan("com.beeproduced.datasource.test")
        val vendorAdapter = HibernateJpaVendorAdapter()
        em.jpaVendorAdapter = vendorAdapter
        val properties: HashMap<String, Any> = HashMap()
        properties["hibernate.hbm2ddl.auto"] = requireNotNull(env.getProperty("spring.jpa.hibernate.ddl-auto"))
        properties["hibernate.dialect"] = requireNotNull(env.getProperty("spring.jpa.properties.hibernate.dialect"))
        em.setJpaPropertyMap(properties)
        return em
    }

    @Bean(name = ["testTM"])
    fun aTransactionManager(@Qualifier("testEM") bEntityManager: AbstractEntityManagerFactoryBean): PlatformTransactionManager {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = bEntityManager.getObject()
        return transactionManager
    }

    @Bean(name = ["testCBF"])
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Lazy(false)
    fun aCriteriaBuilderFactory(
        @Qualifier("testEM") bEntityManager: EntityManagerFactory
    ): CriteriaBuilderFactory {
        val config: CriteriaBuilderConfiguration = Criteria.getDefault()
        return config.createCriteriaBuilderFactory(bEntityManager)
    }

    @Bean(name = ["testEVM"])
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Lazy(false)
    fun aEntityViewManager(
        @Qualifier("testCBF") cbf: CriteriaBuilderFactory,
        @Qualifier("testTM") transactionManager: PlatformTransactionManager,
        resourceLoader: ResourceLoader,
        environment: Environment,
    ): EntityViewManager {

        // From: AbstractEntityViewConfigurationSource
        // com.blazebit.persistence.integration.view.spring.impl
        // TODO: Add filters like AnnotationEntityViewConfigurationSource
        val scanner = EntityViewComponentProvider(emptyList())
        scanner.setResourceLoader(resourceLoader)
        scanner.setEnvironment(environment)
        val entityViewClasses: MutableSet<Class<*>> = HashSet()
        val entityViewListenerClasses: MutableSet<Class<*>> = HashSet()
        scanner
            .findCandidateComponents("com.beeproduced.datasource.test")
            .forEach { candidate ->
                try {
                    val clazz = ClassUtils.forName(
                        candidate.beanClassName!!,
                        resourceLoader.classLoader
                    )
                    if (clazz.isAnnotationPresent(EntityView::class.java)) {
                        entityViewClasses.add(clazz)
                    } else {
                        entityViewListenerClasses.add(clazz)
                    }
                } catch (e: ClassNotFoundException) {
                    throw RuntimeException(e)
                }
            }

        val config = EntityViews.createDefaultConfiguration()
        entityViewClasses.forEach { config.addEntityView(it) }
        entityViewListenerClasses.forEach { config.addEntityViewListener(it) }
        config.setTransactionSupport(SpringTransactionSupport(transactionManager))
        val bEVM = config.createEntityViewManager(cbf)
        return bEVM
    }
}
