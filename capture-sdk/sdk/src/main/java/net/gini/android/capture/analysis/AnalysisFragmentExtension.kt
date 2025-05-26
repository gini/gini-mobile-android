package net.gini.android.capture.analysis

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.gini.android.capture.R
import net.gini.android.capture.internal.camera.view.education.AnimatedEducationMessageWithIntro

class AnalysisFragmentExtension {

    private lateinit var educationView: ComposeView

    fun bindViews(rootView: View) {
        educationView = rootView.findViewById(R.id.gc_education_container)
    }

    fun showEducation(onComplete: () -> Unit) {
        educationView.visibility = View.VISIBLE
        educationView.setContent {
            AnimatedEducationMessageWithIntro(
                message = stringResource(R.string.gc_invoice_education_message),
                animationKey = Unit,
                imagePainter = painterResource(R.drawable.gc_invoice_education_upload_picture_image),
                onComplete = onComplete
            )
        }
    }

    fun hideEducation() {
        educationView.visibility = View.GONE
    }
}
