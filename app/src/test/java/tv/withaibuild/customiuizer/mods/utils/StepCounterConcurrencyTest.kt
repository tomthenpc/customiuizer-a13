package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class StepCounterConcurrencyTest {

    @Test
    fun tryStartQueryAndInvalidateRacesAreSafe() {
        val lifecycle = StepCounterController.Lifecycle()
        lifecycle.setHasViews(true)
        lifecycle.onScreenOn()

        val queries = 400
        val invalidates = 400
        val executor = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        val queryDone = CountDownLatch(queries)
        val invalidateDone = CountDownLatch(invalidates)

        val results = mutableListOf<StepCounterController.QueryTicket?>()

        executor.submit {
            start.await()
            repeat(queries) {
                val t = lifecycle.tryStartQuery()
                results.add(t)
                t?.let { lifecycle.finishQuery(it) }
                queryDone.countDown()
            }
        }

        executor.submit {
            start.await()
            repeat(invalidates) {
                lifecycle.invalidate()
                invalidateDone.countDown()
            }
        }

        start.countDown()
        assertTrue(queryDone.await(10, TimeUnit.SECONDS))
        assertTrue(invalidateDone.await(10, TimeUnit.SECONDS))
        executor.shutdown()
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))

        val nonNull = results.filterNotNull()
        for (i in 1 until nonNull.size) {
            assertTrue("queryId must monotonically increase", nonNull[i].queryId > nonNull[i - 1].queryId)
        }

        // The final valid ticket may belong to an old generation if the last operation was an
        // invalidate; no invariant violation implies the races are safe.
        val lastTicket = nonNull.lastOrNull()
        if (lastTicket != null) {
            val currentById = lifecycle.isCurrent(lastTicket)
            if (currentById) {
                assertTrue(lifecycle.canPublish(lastTicket))
            }
        }
    }
}
