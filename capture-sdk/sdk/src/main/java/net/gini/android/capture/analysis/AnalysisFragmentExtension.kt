package net.gini.android.capture.analysis

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.gini.android.capture.R
import net.gini.android.capture.analysis.paymentDueHint.qrcode.PaymentDueHintContent
import net.gini.android.capture.internal.camera.view.education.AnimatedEducationMessageWithIntro
import net.gini.android.capture.ui.theme.GiniTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class AnalysisFragmentExtension {

    private lateinit var educationView: ComposeView
    private lateinit var paymentDueHintView: ComposeView

    fun bindViews(rootView: View) {
        educationView = rootView.findViewById(R.id.gc_education_container)
        paymentDueHintView = rootView.findViewById(R.id.gc_payment_due_hint_container)
    }

    fun showPaymentDueHint(onDismiss: () -> Unit, dueDate: String) {
        paymentDueHintView.visibility = View.VISIBLE
        paymentDueHintView.setContent {
            GiniTheme {
                PaymentDueHintContent(
                    dueDate = formatDateToGermanStyle(dueDate),
                    onDismiss = onDismiss
                )
            }
        }
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
    fun formatDateToGermanStyle(dateString: String): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
            val outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
            LocalDate.parse(dateString, inputFormatter).format(outputFormatter)
        } catch (_: Exception) {
            dateString // fallback if parsing fails
        }
    }
}
