package net.gini.android.capture.internal.qrcode

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for the Wero candidate path on [PaymentQRCodeData]:
 * - [PaymentQRCodeData.fromWeroCandidate] builds an [PaymentQRCodeData.Format.UNKNOWN] instance.
 * - [PaymentQRCodeData.toJson] emits only the `qrcode` field (no `paymentdata`).
 * - [PaymentQRCodeData.Format.UNKNOWN] survives Parcelable round-trip.
 */
@RunWith(AndroidJUnit4::class)
class PaymentQRCodeDataWeroTest {

    private val weroUrl = "https://pay.weropay.eu/token=abc"

    @Test
    fun `fromWeroCandidate produces an UNKNOWN-format instance with the raw payload`() {
        val data = PaymentQRCodeData.fromWeroCandidate(weroUrl)

        assertThat(data.format).isEqualTo(PaymentQRCodeData.Format.UNKNOWN)
        assertThat(data.unparsedContent).isEqualTo(weroUrl)
    }

    @Test
    fun `fromWeroCandidate leaves every structured payment field empty`() {
        val data = PaymentQRCodeData.fromWeroCandidate(weroUrl)

        assertThat(data.amount).isEmpty()
        assertThat(data.bic).isEmpty()
        assertThat(data.iban).isEmpty()
        assertThat(data.paymentRecipient).isEmpty()
        assertThat(data.paymentReference).isEmpty()
    }

    @Test
    fun `toJson of a Wero candidate contains only the qrcode field`() {
        val data = PaymentQRCodeData.fromWeroCandidate(weroUrl)

        val json = data.toJson()

        assertThat(json).isEqualTo("{\"qrcode\":\"$weroUrl\"}")
    }

    @Test
    fun `UNKNOWN format round-trips via Parcelable`() {
        val original = PaymentQRCodeData.fromWeroCandidate(weroUrl)
        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restored = PaymentQRCodeData.CREATOR.createFromParcel(parcel)

            assertThat(restored).isEqualTo(original)
            assertThat(restored.format).isEqualTo(PaymentQRCodeData.Format.UNKNOWN)
            assertThat(restored.unparsedContent).isEqualTo(weroUrl)
        } finally {
            parcel.recycle()
        }
    }
}
