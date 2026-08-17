package net.gini.android.bank.api.mapper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.bank.api.response.ConfigurationResponse
import net.gini.android.bank.api.response.toConfiguration
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BankExtractionsParserTest {

    @Test
    fun `parses return reasons with localized labels`() {
        val returnReasons = BankExtractionsParser.parseReturnReasons(
            JSONObject(EXTRACTIONS_WITH_RETURN_REASONS_JSON).getJSONArray("returnReasons")
        )

        assertThat(returnReasons).hasSize(1)
        assertThat(returnReasons[0].id).isEqualTo("r1")
        assertThat(returnReasons[0].localizedLabels["de"]).isEqualTo("Beschädigt")
    }

    @Test
    fun `parses missing return reasons to an empty list`() {
        val returnReasons = BankExtractionsParser.parseReturnReasons(
            JSONObject("""{"extractions":{},"candidates":{}}""").optJSONArray("returnReasons")
        )

        assertThat(returnReasons).isEmpty()
    }

    @Test
    fun `configuration response maps to configuration with defaults for missing fields`() {
        val configuration = ConfigurationResponse(
            clientID = null,
            userJourneyAnalyticsEnabled = true,
            skontoEnabled = null,
            returnAssistantEnabled = null,
            amplitudeApiKey = null,
            transactionDocsEnabled = null,
            qrCodeEducationEnabled = null,
            instantPaymentEnabled = null,
            eInvoiceEnabled = null,
            alreadyPaidHintEnabled = null,
            paymentDueHintEnabled = null,
            savePhotosLocallyEnabled = null,
            unsupportedQRCodeWarningEnabled = null,
        ).toConfiguration()

        assertThat(configuration.clientID).isEmpty()
        assertThat(configuration.isUserJourneyAnalyticsEnabled).isTrue()
        assertThat(configuration.isSkontoEnabled).isFalse()
        assertThat(configuration.amplitudeApiKey).isNull()
    }

    companion object {
        val EXTRACTIONS_WITH_RETURN_REASONS_JSON = """
            {
                "extractions": {
                    "amountToPay": {
                        "entity": "amount",
                        "value": "335.50:EUR"
                    }
                },
                "candidates": {},
                "compoundExtractions": {
                    "lineItems": [
                        {
                            "description": {
                                "entity": "text",
                                "value": "Shoes"
                            }
                        }
                    ]
                },
                "returnReasons": [
                    {
                        "id": "r1",
                        "de": "Beschädigt"
                    }
                ]
            }
        """.trimIndent()
    }
}
