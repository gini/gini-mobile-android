package net.gini.android.capture.analysis

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.Job
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression tests for [AnalysisScreenPresenterExtension] cancellation.
 *
 * Root cause: [AnalysisScreenPresenterExtension.doWhenEducationFinished] used to create a fresh
 * CoroutineScope(Dispatchers.IO) for every invocation. These anonymous scopes were never
 * cancelled when the user pressed Back (fragment destroyed), so the pending navigation callback
 * could still execute on a dead NavController, causing an NPE crash.
 */
@RunWith(AndroidJUnit4::class)
class AnalysisScreenPresenterExtensionCancellationTest {

    /**
     * The extension must own a named [Job] so that [AnalysisScreenPresenterExtension.cancel]
     * can reliably cancel ALL coroutines launched by [AnalysisScreenPresenterExtension].
     *
     * **Fails when reverted**: Reverting to `CoroutineScope(Dispatchers.IO).launch` inside
     * `doWhenEducationFinished` removes the `job` field entirely, causing this test to throw
     * [NoSuchFieldException].
     */
    @Test
    fun extension_owns_active_coroutine_job_on_construction() {
        val view = mock<AnalysisScreenContract.View>()
        val extension = AnalysisScreenPresenterExtension(view)

        val job = getJobViaReflection(extension)

        assertThat(job.isActive).isTrue()
    }

    /**
     * After [AnalysisScreenPresenterExtension.cancel] is called the owned [Job] must be
     * cancelled. A cancelled job means any subsequent `scope.launch { ... }` inside
     * `doWhenEducationFinished` is a no-op and will never invoke the navigation callback.
     *
     * **Fails when reverted**: Removing [AnalysisScreenPresenterExtension.cancel] (or reverting
     * the named scope) means the job is never cancelled and [Job.isCancelled] stays false.
     */
    @Test
    fun cancel_cancels_the_owned_coroutine_job() {
        val view = mock<AnalysisScreenContract.View>()
        val extension = AnalysisScreenPresenterExtension(view)

        extension.cancel()

        val job = getJobViaReflection(extension)
        assertThat(job.isCancelled).isTrue()
    }

    // ── helper ────────────────────────────────────────────────────────────────

    /**
     * Reads the private `job` field from [AnalysisScreenPresenterExtension] via reflection.
     * Throws [NoSuchFieldException] if the field does not exist, which is itself evidence
     * that Fix 2 was reverted.
     */
    private fun getJobViaReflection(extension: AnalysisScreenPresenterExtension): Job {
        val field = AnalysisScreenPresenterExtension::class.java.getDeclaredField("job")
        field.isAccessible = true
        return field.get(extension) as Job
    }
}

