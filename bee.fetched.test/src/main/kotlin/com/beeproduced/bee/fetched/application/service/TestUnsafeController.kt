package com.beeproduced.bee.fetched.application.service

import com.beeproduced.bee.fetched.graphql.dto.*
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery

/**
 * @author Kacper Urbaniec
 * @version 2023-10-16
 */
@DgsComponent
class TestUnsafeController {
  @DgsQuery
  fun alpha(): Alpha {
    return Alpha("Alpha")
  }

  @DgsQuery
  fun bravo(): Bravo {
    return Bravo(listOf("Bravo"))
  }

  @DgsQuery
  fun charlie(): Charlie {
    return Charlie("Charlie")
  }

  @DgsQuery
  fun delta(): Delta {
    return Delta(listOf("Delta"))
  }

  @DgsQuery
  fun echo(): Echo {
    return Echo("Echo")
  }

  data class MyFoxtrot(val zuluId: String)

  @DgsQuery
  fun foxtrot(): MyFoxtrot {
    return MyFoxtrot("Foxtrot")
  }

  data class MyGolf(val zuluId: String?)

  @DgsQuery
  fun golf(): MyGolf {
    return MyGolf("Golf")
  }

  data class MyHotel(val zuluIds: List<String>)

  @DgsQuery
  fun hotel(): MyHotel {
    return MyHotel(listOf("Hotel"))
  }

  data class MyIndia(val zuluIds: List<String>?)

  @DgsQuery
  fun india(): MyIndia {
    return MyIndia(listOf("India"))
  }

  @DgsQuery
  fun juliet(): Juliet {
    return Juliet()
  }
}
