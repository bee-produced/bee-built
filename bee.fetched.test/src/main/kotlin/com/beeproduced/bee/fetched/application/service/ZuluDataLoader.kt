package com.beeproduced.bee.fetched.application.service

import com.beeproduced.bee.fetched.annotations.*
import com.beeproduced.bee.fetched.graphql.DgsConstants
import com.beeproduced.bee.fetched.graphql.dto.*
import com.netflix.graphql.dgs.DgsDataLoader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.MappedBatchLoaderWithContext

/**
 * @author Kacper Urbaniec
 * @version 2023-10-14
 */
@BeeFetched(
  mappings = [FetcherMapping(Echo::class, DgsConstants.ECHO.Zulu, DgsConstants.ECHO.EchoToZuluId)],
  internalTypes =
    [
      FetcherInternalType(
        Foxtrot::class,
        TestUnsafeController.MyFoxtrot::class,
        DgsConstants.FOXTROT.Zulu,
      ),
      FetcherInternalType(Golf::class, TestUnsafeController.MyGolf::class, DgsConstants.GOLF.Zulu),
      FetcherInternalType(
        Hotel::class,
        TestUnsafeController.MyHotel::class,
        DgsConstants.HOTEL.Zulus,
      ),
      FetcherInternalType(
        India::class,
        TestUnsafeController.MyIndia::class,
        DgsConstants.INDIA.Zulus,
      ),
    ],
  ignore = [FetcherIgnore(Juliet::class, DgsConstants.JULIET.Zulu), FetcherIgnore(Zulu::class)],
  safeMode = false,
)
@DgsDataLoader(name = "Zulu")
class ZuluDataLoader : MappedBatchLoaderWithContext<String, Zulu> {
  override fun load(
    keys: Set<String>,
    environment: BatchLoaderEnvironment,
  ): CompletionStage<Map<String, Zulu>> {
    return CompletableFuture.supplyAsync { keys.associateWith { Zulu(it) } }
  }
}
