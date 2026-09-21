package net.gini.android.capture.analysis

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.gini.android.capture.R
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.ingredientbrand.GetIngredientBrandVisibleUseCase
import net.gini.android.capture.ingredientbrand.GiniLoadingIndicatorAdapter
import net.gini.android.capture.ingredientbrand.IngredientBrandScreen
import net.gini.android.capture.internal.camera.view.education.AnimatedEducationMessageWithIntro
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import net.gini.android.capture.view.InjectedViewAdapterInstance

class AnalysisFragmentExtension {

    private lateinit var educationView: ComposeView

    private val ingredientBrandVisibleUseCase: GetIngredientBrandVisibleUseCase
            by getGiniCaptureKoin().inject()

    /**
     * Held for the lifetime of the fragment rather than rebuilt per view creation.
     * [net.gini.android.capture.view.InjectedViewContainer] tracks adapter ownership through
     * [InjectedViewAdapterInstance.viewContainer]; handing it a fresh instance on every
     * `onCreateView` would break that bookkeeping across configuration changes.
     */
    private var giniLoadingIndicatorInstance:
            InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter>? = null

    fun bindViews(rootView: View) {
        educationView = rootView.findViewById(R.id.gc_education_container)
    }

    /**
     * The Gini ingredient brand loading indicator for the Analysis screen, or `null` when the
     * screen should keep using the integrator's (or the default) loading indicator.
     *
     * Returns `null` when the client configuration does not list the Analysis screen in
     * `ingredientBrandScreens`.
     *
     * Internal use only. It is `public` solely because [AnalysisFragmentImpl] is Java and Kotlin
     * `internal` members are name-mangled in the bytecode, so Java cannot call them cleanly.
     * Integrators must not call this — the ingredient brand is driven by the client configuration
     * and is deliberately not customisable.
     */
    fun giniLoadingIndicatorAdapterInstance():
            InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter>? {
        if (!ingredientBrandVisibleUseCase(IngredientBrandScreen.ANALYSIS)) {
            return null
        }
        return giniLoadingIndicatorInstance
            ?: InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter>(
                GiniLoadingIndicatorAdapter()
            ).also { giniLoadingIndicatorInstance = it }
    }

    fun showEducation(onComplete: () -> Unit) {
        educationView.visibility = View.VISIBLE
        educationView.setContent {
            GiniTheme {
                AnimatedEducationMessageWithIntro(
                    message = stringResource(R.string.gc_invoice_education_message),
                    animationKey = Unit,
                    introImagePainter = painterResource(R.drawable.gc_invoice_education_intro_image),
                    mainImagePainter = painterResource(R.drawable.gc_invoice_education_upload_picture_image),
                    onComplete = onComplete
                )
            }
        }
    }

    fun hideEducation() {
        educationView.visibility = View.GONE
    }
}
