package net.gini.android.capture.analysis

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Job
import net.gini.android.capture.Document
import net.gini.android.capture.document.DocumentFactory
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression tests for [AnalysisViewModel] coroutine cancellation.
 *
 * Root cause (from the former MVP presenter extension): post-analysis navigation callbacks were
 * launched in anonymous, never-cancelled coroutine scopes. When the user pressed Back (fragment
 * destroyed) the pending navigation callback could still execute on a dead NavController, causing
 * an NPE crash. The view model must own a named [Job] which is cancelled in
 * [AnalysisViewModel.stop].
 */
@RunWith(AndroidJUnit4::class)
class AnalysisViewModelCancellationTest {

    private fun createViewModel(): AnalysisViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val document = DocumentFactory.newEmptyImageDocument(
            Document.Source.newCameraSource(), Document.ImportMethod.NONE
        )
        return AnalysisViewModel(app, document, null, false)
    }

    /**
     * The view model must own a named [Job] so that [AnalysisViewModel.stop] can reliably cancel
     * ALL coroutines launched for post-analysis navigation.
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
     * After [AnalysisViewModel.stop] is called the owned [Job] must be cancelled. A cancelled job
     * means any subsequent `scope.launch { ... }` inside `doWhenEducationFinished` is a no-op and
     * will never invoke the navigation callback.
     */
    @Test
    fun stop_cancels_the_owned_coroutine_job() {
        val viewModel = createViewModel()

        viewModel.stop()

        val job = getJobViaReflection(viewModel)
        assertThat(job.isCancelled).isTrue()
    }

    // ── helper ────────────────────────────────────────────────────────────────

    /**
     * Reads the private `job` field from [AnalysisViewModel] via reflection. Throws
     * [NoSuchFieldException] if the field does not exist, which is itself evidence that the
     * cancellation fix was reverted.
     */
    private fun getJobViaReflection(viewModel: AnalysisViewModel): Job {
        val field = AnalysisViewModel::class.java.getDeclaredField("job")
        field.isAccessible = true
        return field.get(viewModel) as Job
    }
}
