package com.praxim.service

import kotlinx.coroutines.Dispatchers

object PraximDispatchers {
    // Background thread priority, limited to 2 to constrain CPU footprint
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val BackgroundIO = Dispatchers.Default.limitedParallelism(2)
}
