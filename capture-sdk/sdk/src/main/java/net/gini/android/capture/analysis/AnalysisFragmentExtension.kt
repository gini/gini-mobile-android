package net.gini.android.capture.analysis

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.gini.android.capture.R
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.ingredientbrand.GetIngredientBrandVisibleUseCase
import net.gini.android.capture.ingredientbrand.IngredientBrandLoadingIndicatorAdapter
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
     * The Analysis screen's loading indicator.
     *
     * Always an [IngredientBrandLoadingIndicatorAdapter], which chooses between the Gini brand
     * mark and [integratorAdapter] at the moment the indicator is shown rather than when the
     * screen is built. The choice cannot be made at view-creation time on the "open with" path:
     * `GiniCaptureFragment` navigates straight to Analysis for an open-with document, so on a cold
     * first launch `ingredientBrandScreens` has not arrived yet and the screen would keep the
     * integrator's indicator for good.
     *
     * The integrator's adapter is consulted only while ingredient branding is off — it must not be
     * able to replace or suppress the Gini mark when it is on (PP-3512).
     *
     * Internal use only. It is `public` solely because [AnalysisFragmentImpl] is Java and Kotlin
     * `internal` members are name-mangled in the bytecode, so Java cannot call them cleanly.
     */
    fun loadingIndicatorAdapterInstance(
        integratorAdapter: CustomLoadingIndicatorAdapter,
    ): InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter> =
        giniLoadingIndicatorInstance
            ?: InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter>(
                IngredientBrandLoadingIndicatorAdapter(
                    isGiniMarkEnabled = {
                        ingredientBrandVisibleUseCase(IngredientBrandScreen.ANALYSIS)
                    },
                    integratorAdapter = { integratorAdapter },
                )
            ).also { giniLoadingIndicatorInstance = it }

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
