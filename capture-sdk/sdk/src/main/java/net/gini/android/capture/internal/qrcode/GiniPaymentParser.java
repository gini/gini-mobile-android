package net.gini.android.capture.internal.qrcode;

import android.net.Uri;

import androidx.annotation.NonNull;

/**
 * Internal use only.
 * QR Code parser for the eps payment.gini QR Code url.
 *
 * @suppress
 */
class GiniPaymentParser implements QRCodeParser<PaymentQRCodeData> {

    private static final String GINIPAYMENT_HOTS = "payment.gini.net";

    @Override
    public PaymentQRCodeData parse(@NonNull final String qrCodeContent)
            throws IllegalArgumentException {
        final Uri uri = Uri.parse(qrCodeContent);
        if (!GINIPAYMENT_HOTS.equals(uri.getHost())) {
            throw new IllegalArgumentException(
                    "QRCode content does not conform to the gini payment QRCodeUrl format.");
        }
        return new PaymentQRCodeData(PaymentQRCodeData.Format.GINI_PAYMENT, qrCodeContent, null,
                null, null, null, null);
    }
}
