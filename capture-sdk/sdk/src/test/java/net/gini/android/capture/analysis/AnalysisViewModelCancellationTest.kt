package net.gini.android.capture.analysis

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.Job
import net.gini.android.capture.Document
import net.gini.android.capture.document.DocumentFactory
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression tests for [AnalysisViewModel] cancellation (ported from the former
 * `AnalysisScreenPresenterExtensionCancellationTest`).
 *
 * Root cause: the education/navigation coroutines used to be launched in a fresh
 * CoroutineScope(Dispatchers.IO) for every invocation. These anonymous scopes were never
 * cancelled when the user pressed Back (fragment destroyed), so the pending navigation callback
 * could still execute on a dead NavController, causing an NPE crash.
 */
@RunWith(AndroidJUnit4::class)
class AnalysisViewModelCancellationTest {

    private fun createViewModel(): AnalysisViewModel {
        val document = DocumentFactory.newEmptyImageDocument(
            Document.Source.newCameraSource(), Document.ImportMethod.NONE
        )
        return AnalysisViewModel(
            ApplicationProvider.getApplicationContext<Application>(),
            document,
            null,
            mock(),
            false
        )
    }

    /**
     * The view model must own a named [Job] so that [AnalysisViewModel.onStop]
     * can reliably cancel ALL coroutines launched by [AnalysisViewModel].
     *
     * **Fails when reverted**: Reverting to `CoroutineScope(Dispatchers.IO).launch` inside
     * `doWhenEducationFinished` removes the `job` field entirely, causing this test to throw
     * [NoSuchFieldException].
     */
    @Test
    fun viewModel_owns_active_coroutine_job_on_construction() {
        val viewModel = createViewModel()

        val job = getJobViaReflection(viewModel)

        assertThat(job.isActive).isTrue()
    }

    /**
     * After [AnalysisViewModel.onStop] is called the owned [Job] must be
     * cancelled. A cancelled job means any subsequent `scope.launch { ... }` inside
     * `doWhenEducationFinished` is a no-op and will never invoke the navigation callback.
     *
     * **Fails when reverted**: Removing the `job.cancel()` call from [AnalysisViewModel.onStop]
     * (or reverting the named scope) means the job is never cancelled and [Job.isCancelled]
     * stays false.
     */
    @Test
    fun onStop_cancels_the_owned_coroutine_job() {
        val viewModel = createViewModel()

        viewModel.onStop()

        val job = getJobViaReflection(viewModel)
        assertThat(job.isCancelled).isTrue()
    }

    // ── helper ────────────────────────────────────────────────────────────────

    /**
     * Reads the private `job` field from [AnalysisViewModel] via reflection.
     * Throws [NoSuchFieldException] if the field does not exist, which is itself evidence
     * that the fix was reverted.
     */
    private fun getJobViaReflection(viewModel: AnalysisViewModel): Job {
        val field = AnalysisViewModel::class.java.getDeclaredField("job")
        field.isAccessible = true
        return field.get(viewModel) as Job
    }
}
