package net.gini.android.capture.internal.cache

import android.content.Context
import android.util.LruCache
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.AsyncCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoryCacheTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `delivers the worker result to the callback`() {
        val cache = RecordingMemoryCache(runningWorkersLimit = 2)
        val callback = RecordingCallback()

        cache.get(context, "a", callback)
        cache.succeed("a", "value")

        assertThat(callback.results).containsExactly("value")
        assertThat(callback.errors).isEmpty()
        assertThat(callback.cancellations).isEqualTo(0)
    }

    @Test
    fun `returns the cached value without executing a worker again`() {
        val cache = RecordingMemoryCache(runningWorkersLimit = 2)
        cache.get(context, "a", RecordingCallback())
        cache.succeed("a", "value")
        val callback = RecordingCallback()

        cache.get(context, "a", callback)

        assertThat(callback.results).containsExactly("value")
        assertThat(cache.executedKeys).containsExactly("a")
    }

    @Test
    fun `delivers the worker error to the callback and caches nothing`() {
        val cache = RecordingMemoryCache(runningWorkersLimit = 2)
        val callback = RecordingCallback()
        val error = IllegalStateException("failed")

        cache.get(context, "a", callback)
        cache.fail("a", error)
        cache.get(context, "a", RecordingCallback())

        assertThat(callback.errors).containsExactly(error)
        assertThat(callback.results).isEmpty()
        assertThat(cache.executedKeys).containsExactly("a", "a").inOrder()
    }

    @Test
    fun `delivers the worker cancellation to the callback and caches nothing`() {
        val cache = RecordingMemoryCache(runningWorkersLimit = 2)
        val callback = RecordingCallback()

        cache.get(context, "a", callback)
        cache.cancel("a")
        cache.get(context, "a", RecordingCallback())

        assertThat(callback.cancellations).isEqualTo(1)
        assertThat(callback.results).isEmpty()
        assertThat(cache.executedKeys).containsExactly("a", "a").inOrder()
    }

    @Test
    fun `queues workers above the running limit and executes them in order`() {
        val cache = RecordingMemoryCache(runningWorkersLimit = 1)
        val firstCallback = RecordingCallback()
        val secondCallback = RecordingCallback()
        val thirdCallback = RecordingCallback()

        cache.get(context, "a", firstCallback)
        cache.get(context, "b", secondCallback)
        cache.get(context, "c", thirdCallback)
        assertThat(cache.executedKeys).containsExactly("a")

        cache.succeed("a", "first")
        assertThat(cache.executedKeys).containsExactly("a", "b").inOrder()

        cache.fail("b", IllegalStateException("failed"))
        assertThat(cache.executedKeys).containsExactly("a", "b", "c").inOrder()

        cache.cancel("c")

        assertThat(firstCallback.results).containsExactly("first")
        assertThat(secondCallback.errors).hasSize(1)
        assertThat(thirdCallback.cancellations).isEqualTo(1)
    }

    @Test
    fun `executes a queued key only once and notifies every callback waiting for it`() {
        val cache = RecordingMemoryCache(runningWorkersLimit = 1)
        val firstQueuedCallback = RecordingCallback()
        val secondQueuedCallback = RecordingCallback()

        cache.get(context, "a", RecordingCallback())
        cache.get(context, "b", firstQueuedCallback)
        cache.get(context, "b", secondQueuedCallback)
        cache.succeed("a", "first")
        cache.succeed("b", "second")

        assertThat(cache.executedKeys).containsExactly("a", "b").inOrder()
        assertThat(firstQueuedCallback.results).containsExactly("second")
        assertThat(secondQueuedCallback.results).containsExactly("second")
    }

    @Test
    fun `executes the worker again after the key was invalidated`() {
        val cache = RecordingMemoryCache(runningWorkersLimit = 2)
        cache.get(context, "a", RecordingCallback())
        cache.succeed("a", "value")

        cache.invalidate("a")
        cache.get(context, "a", RecordingCallback())

        assertThat(cache.executedKeys).containsExactly("a", "a").inOrder()
    }

    @Test
    fun `executes the workers again after the cache was cleared`() {
        val cache = RecordingMemoryCache(runningWorkersLimit = 2)
        cache.get(context, "a", RecordingCallback())
        cache.get(context, "b", RecordingCallback())
        cache.succeed("a", "first")
        cache.succeed("b", "second")

        cache.clear()
        cache.get(context, "a", RecordingCallback())
        cache.get(context, "b", RecordingCallback())

        assertThat(cache.executedKeys).containsExactly("a", "b", "a", "b").inOrder()
    }

    private class RecordingCallback : AsyncCallback<String, Exception> {
        val results = mutableListOf<String>()
        val errors = mutableListOf<Exception>()
        var cancellations = 0

        override fun onSuccess(result: String) {
            results.add(result)
        }

        override fun onError(exception: Exception) {
            errors.add(exception)
        }

        override fun onCancelled() {
            cancellations++
        }
    }

    /**
     * A cache whose workers only record their execution, so that each test decides when and how
     * a worker finishes.
     */
    private class RecordingMemoryCache(runningWorkersLimit: Int) :
        MemoryCache<String, String>(runningWorkersLimit) {

        val executedKeys = mutableListOf<String>()
        private val runningWorkerCallbacks = mutableMapOf<String, AsyncCallback<String, Exception>>()

        fun succeed(key: String, value: String) {
            runningWorkerCallbacks.remove(key)!!.onSuccess(value)
        }

        fun fail(key: String, exception: Exception) {
            runningWorkerCallbacks.remove(key)!!.onError(exception)
        }

        fun cancel(key: String) {
            runningWorkerCallbacks.remove(key)!!.onCancelled()
        }

        override fun createCache(): LruCache<String, String> = LruCache(CACHE_SIZE)

        override fun createWorker(
            runningWorkers: MutableList<Worker<String, String>>,
            subject: String,
            callback: AsyncCallback<String, Exception>
        ): Worker<String, String> = object : Worker<String, String>(runningWorkers, subject, callback) {
            override fun doExecute(
                context: Context,
                subject: String,
                callback: AsyncCallback<String, Exception>
            ) {
                executedKeys.add(subject)
                runningWorkerCallbacks[subject] = callback
            }
        }
    }

    private companion object {
        const val CACHE_SIZE = 10
    }
}
