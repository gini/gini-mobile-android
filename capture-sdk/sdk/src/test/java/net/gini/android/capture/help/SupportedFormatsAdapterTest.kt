package net.gini.android.capture.help

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class SupportedFormatsAdapterTest {

    private val qrLabelsWithoutWero = listOf(
        R.string.gc_supported_format_section_header,
        R.string.gc_supported_format_qr_type_bezahl,
        R.string.gc_supported_format_qr_type_eps,
        R.string.gc_supported_format_qr_type_stuzza,
        R.string.gc_supported_format_qr_type_girocode,
        R.string.gc_supported_format_qr_type_gini_payment,
        R.string.gc_supported_format_qr_type_spc,
        R.string.gc_supported_format_qr_type_spd,
        R.string.gc_supported_format_qr_type_pay_by_square,
        R.string.gc_supported_format_qr_type_upnqr,
        R.string.gc_supported_format_qr_type_hub3,
    )

    @Test
    fun `QR list ends with Wero when the flag is on`() {
        val adapter = SupportedFormatsAdapter(true, false, true)

        assertThat(adapter.itemLabels)
            .containsExactlyElementsIn(qrLabelsWithoutWero + R.string.gc_supported_format_qr_type_wero)
            .inOrder()
    }

    @Test
    fun `QR list is unchanged when the flag is off`() {
        val adapter = SupportedFormatsAdapter(true, false, false)

        assertThat(adapter.itemLabels).containsExactlyElementsIn(qrLabelsWithoutWero).inOrder()
    }

    @Test
    fun `non-QR list never contains Wero when the flag is on`() {
        val adapter = SupportedFormatsAdapter(false, false, true)

        assertThat(adapter.itemLabels).doesNotContain(R.string.gc_supported_format_qr_type_wero)
    }

    @Test
    fun `Wero row is bound with the supported icon and hidden divider while HUB3 keeps its divider`() {
        val adapter = SupportedFormatsAdapter(true, false, true)
        val themedContext = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.GiniCaptureTheme
        )
        val parent = FrameLayout(themedContext)
        val lastPosition = adapter.itemCount - 1

        val holder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(lastPosition))
        adapter.onBindViewHolder(holder, lastPosition)

        val label = holder.itemView.findViewById<TextView>(R.id.gc_format_info_item_label)
        val icon = holder.itemView.findViewById<ImageView>(R.id.gc_format_info_item_icon)
        val divider = holder.itemView.findViewById<View>(R.id.gc_item_format_divider)
        assertThat(label.text.toString()).isEqualTo("Wero QR code")
        assertThat(shadowOf(icon.drawable).createdFromResId)
            .isEqualTo(R.drawable.gc_format_info_supported_icon)
        assertThat(divider.visibility).isEqualTo(View.INVISIBLE)

        val hub3Holder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(lastPosition - 1))
        adapter.onBindViewHolder(hub3Holder, lastPosition - 1)
        assertThat(hub3Holder.itemView.findViewById<View>(R.id.gc_item_format_divider).visibility)
            .isEqualTo(View.VISIBLE)
    }
}
