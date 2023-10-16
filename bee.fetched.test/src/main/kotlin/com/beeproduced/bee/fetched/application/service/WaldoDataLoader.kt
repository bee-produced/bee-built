package com.beeproduced.bee.fetched.application.service

import com.beeproduced.bee.fetched.annotations.*
import com.beeproduced.bee.fetched.graphql.DgsConstants
import com.beeproduced.bee.fetched.graphql.dto.*
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.MappedBatchLoaderWithContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-10-14
 */

@BeeFetched(
    mappings = [
        FetcherMapping(Corge::class, DgsConstants.CORGE.Waldo, DgsConstants.CORGE.CorgeToWaldoId)
    ],
    internalTypes = [
        FetcherInternalType(Grault::class, TestController.MyGrault::class, DgsConstants.GRAULT.Waldo),
        FetcherInternalType(Fred::class, TestController.MyFred::class, DgsConstants.FRED.Waldo),
        FetcherInternalType(Plugh::class, TestController.MyPlugh::class, DgsConstants.PLUGH.Waldos),
        FetcherInternalType(Xyzzy::class, TestController.MyXyzzy::class, DgsConstants.XYZZY.Waldos),
    ],
    ignore = [
        FetcherIgnore(Garply::class, DgsConstants.GARPLY.Waldo),
        FetcherIgnore(Waldo::class)
    ]
)
@DgsDataLoader(name = "Waldo")
class WaldoDataLoader : MappedBatchLoaderWithContext<String, Waldo> {
    override fun load(
        keys: Set<String>,
        environment: BatchLoaderEnvironment,
    ): CompletionStage<Map<String, Waldo>> {
        return CompletableFuture.supplyAsync {
            keys.associateWith { Waldo(it) }
        }
    }
}