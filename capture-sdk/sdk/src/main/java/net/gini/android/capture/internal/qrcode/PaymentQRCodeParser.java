package net.gini.android.capture.internal.qrcode;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import net.gini.android.capture.internal.util.FeatureConfiguration;

/**
 * Created by Alpar Szotyori on 08.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * Parser of QRCode content strings for payment data.
 * <p>
 * Currently supports the following QRCode formats:
 * <ul>
 *   <li><a href="http://www.bezahlcode.de/wp-content/uploads/BezahlCode_TechDok.pdf">BezahlCode</a></li>
 *   <li><a href="https://www.europeanpaymentscouncil.eu/document-library/guidance-documents/quick-response-code-guidelines-enable-data-capture-initiation">EPC069-12</a>
 *       (<a href="https://www.stuzza.at/de/zahlungsverkehr/qr-code.html">Stuzza (AT)</a> and
 *       <a href="https://www.girocode.de/rechnungsempfaenger/">GiroCode (DE)</a>)</li>
 *   <li>EPS (Austrian payment standard)</li>
 *   <li><a href="https://www.paymentstandards.ch/dam/downloads/ig-qr-bill-en.pdf">SPC (Swiss QR Bill)</a></li>
 *   <li><a href="https://qr-platba.cz/pro-vyvojare/specifikace-formatu/">SPD (Czech QR Payment)</a></li>
 *   <li><a href="https://upn-qr.si/uploads/files/NavodilaZaUporabnike.pdf">UPNQR (Slovenian payment)</a></li>
 *   <li><a href="https://hub.si/hub3/">HUB3 (Croatian payment)</a></li>
 *   <li><a href="https://bysquare.com/">Pay by Square (Slovak/Czech payment)</a></li>
 *   <li>Gini Payment</li>
 * </ul>
 */
class PaymentQRCodeParser implements QRCodeParser<PaymentQRCodeData> {

    private final List<QRCodeParser<PaymentQRCodeData>> mParsers;

    PaymentQRCodeParser() {
        mParsers = new ArrayList<>();
        if (FeatureConfiguration.isQRCodeScanningEnabled()) {
            mParsers.add(new BezahlCodeParser());
            mParsers.add(new EPC069_12Parser());
            mParsers.add(new EPSPaymentParser());
            mParsers.add(new SPCParser());
            mParsers.add(new SPDParser());
            mParsers.add(new UPNQRParser());
            mParsers.add(new HUB3Parser());
            mParsers.add(new PayBySquareParser());
        }
        mParsers.add(new GiniPaymentParser());
    }

    /**
     * Parses the content of a QRCode to retrieve the payment data.
     *
     * @param qrCodeContent content of a QRCode
     * @return a {@link PaymentQRCodeData} containing the payment information from the QRCode
     * @throws IllegalArgumentException if the QRCode did not conform to any of the supported formats
     */
    @NonNull
    @Override
    public PaymentQRCodeData parse(@NonNull final String qrCodeContent)
            throws IllegalArgumentException {
        for (final QRCodeParser<PaymentQRCodeData> parser : mParsers) {
            try {
                return parser.parse(qrCodeContent);
            } catch (final IllegalArgumentException ignore) { // NOPMD
            }
        }
        throw new IllegalArgumentException("Unknown QRCode content format.");
    }
}
