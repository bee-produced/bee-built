package com.beeproduced.datasource.a

import com.beeproduced.bee.persistent.blaze.annotations.EnableBeeRepositories
import com.beeproduced.bee.persistent.blaze.meta.proxy.BlazeInstantiators
import com.blazebit.persistence.Criteria
import com.blazebit.persistence.CriteriaBuilderFactory
import com.blazebit.persistence.integration.view.spring.EnableEntityViews
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
import org.springframework.context.annotation.*
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


// Todo: EnableBeeRepositories
// * Set, basepackages, entityManagerFactoryRef, cbf, evm

// TODO: Generate Config with Blaze Beans & EnableEntityView Annotation
// TODO: Add option where to generate views (basePackage)?

@Configuration
@EnableBeeRepositories(
    basePackages = ["com.beeproduced.datasource.a"],
    entityManagerFactoryRef = "aEM",
    criteriaBuilderFactoryRef = "aCBF",
    entityViewManagerRef = "aEVM"
)
// TODO: Without this the backend crashes...
@EnableJpaRepositories(
    basePackages = ["com.beeproduced.datasource.a"],
    entityManagerFactoryRef = "aEM",
    transactionManagerRef = "aTM"
)
class DbConfigA(val env: Environment) {
    @Bean(name = ["aDataSource"])
    @ConfigurationProperties(prefix = "spring.datasource-a")
    fun aDataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

    @Bean(name = ["aEM"])
    fun aEntityManager(@Qualifier("aDataSource") dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.setDataSource(dataSource)
        em.setPackagesToScan("com.beeproduced.datasource.a")
        val vendorAdapter = HibernateJpaVendorAdapter()
        em.jpaVendorAdapter = vendorAdapter
        val properties: HashMap<String, Any> = HashMap()
        properties["hibernate.hbm2ddl.auto"] = requireNotNull(env.getProperty("spring.jpa.hibernate.ddl-auto"))
        properties["hibernate.dialect"] = requireNotNull(env.getProperty("spring.jpa.properties.hibernate.dialect"))
        em.setJpaPropertyMap(properties)
        return em
    }

    @Bean(name = ["aTM"])
    fun aTransactionManager(@Qualifier("aEM") aEntityManager: AbstractEntityManagerFactoryBean): PlatformTransactionManager {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = aEntityManager.getObject()
        return transactionManager
    }

    @Bean(name = ["aCBF"])
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Lazy(false)
    fun aCriteriaBuilderFactory(
        @Qualifier("aEM") aEntityManager: EntityManagerFactory,
    ): CriteriaBuilderFactory {
        val config: CriteriaBuilderConfiguration = Criteria.getDefault()
        return config.createCriteriaBuilderFactory(aEntityManager)
    }

    @Bean(name = ["aEVM"])
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Lazy(false)
    fun aEntityViewManager(
        @Qualifier("aCBF") cbf: CriteriaBuilderFactory,
        @Qualifier("aTM") transactionManager: PlatformTransactionManager,
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
            .findCandidateComponents("com.beeproduced.datasource.a")
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
        val aEVM = config.createEntityViewManager(cbf)
        return aEVM
    }
}