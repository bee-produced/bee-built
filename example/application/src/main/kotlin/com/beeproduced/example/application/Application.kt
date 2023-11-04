package com.beeproduced.example.application

import graphql.schema.DataFetcher
import graphql.schema.DataFetcherFactories
import graphql.schema.DataFetchingEnvironment
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers.named
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.util.concurrent.CompletionStage
import java.util.function.BiFunction

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-09-25
 */
@SpringBootApplication(scanBasePackages = ["com.beeproduced"])
class Application


open class Target {

    companion object {
        @JvmStatic
        fun wrapDataFetcher(
            delegateDataFetcher: DataFetcher<*>,
            mapFunction: BiFunction<DataFetchingEnvironment?, Any?, Any?>,
        ): DataFetcher<*> {

            println("Building ByteBuddy!!")

            return DataFetcher { environment: DataFetchingEnvironment? ->

                println("Running ByteBuddy!!")
                val value = delegateDataFetcher[environment]
                if (value is CompletionStage<*>) {
                    return@DataFetcher (value as CompletionStage<Any?>).thenApply<Any?> { v: Any? ->
                        mapFunction.apply(
                            environment,
                            v
                        )
                    }
                } else {
                    return@DataFetcher mapFunction.apply(environment, value)
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    open class Foo {
        open fun m() = "foo"
    }

    open class Bar {
        open fun m() = "bar"
    }


    /* ByteBuddyAgent.install()
    val foo = Foo()
    println(foo.m())

    ByteBuddy()
        .redefine(Bar::class.java)
        .name(Foo::class.java.name)
        .make()
        .load(Foo::class.java.classLoader, ClassReloadingStrategy.fromInstalledAgent())

    println(foo.m()) */


    // See https://bytebuddy.net/#/tutorial
    // Chapter Delegating a method call
    // Since Java 9, an agent installation is also possible at runtime without a JDK-installation
    /* ByteBuddyAgent.install()
    ByteBuddy()
        .redefine(DataFetcherFactories::class.java)
        .method(named("wrapDataFetcher"))
        .intercept(MethodDelegation.to(Target::class.java))
        .make()
        .load(
            DataFetcherFactories::class.java.classLoader,
            ClassReloadingStrategy.fromInstalledAgent()
        ) */


    val application = SpringApplication(Application::class.java)
    application.run(*args)
}
