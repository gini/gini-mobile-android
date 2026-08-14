package net.gini.android.bank.api.response

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test

/**
 * Verifies the `/configurations` payload maps onto the SDK's configuration model — the entry
 * point of the payment hint feature flags.
 */
class ConfigurationResponseTest {

    private val adapter = Moshi.Builder().build().adapter(ConfigurationResponse::class.java)

    @Test
    fun `paymentScheduleHintEnabled is parsed and mapped`() {
        val response = adapter.fromJson(
            """{"clientID":"client","paymentScheduleHintEnabled":true}"""
        )!!

        assertThat(response.paymentScheduleHintEnabled).isTrue()
        assertThat(response.toConfiguration().isPaymentScheduleHintEnabled).isTrue()
    }

    @Test
    fun `paymentScheduleHintEnabled defaults to false when absent`() {
        val response = adapter.fromJson("""{"clientID":"client"}""")!!

        assertThat(response.paymentScheduleHintEnabled).isNull()
        assertThat(response.toConfiguration().isPaymentScheduleHintEnabled).isFalse()
    }

    @Test
    fun `paymentScheduleHintEnabled is independent of paymentDueHintEnabled`() {
        val response = adapter.fromJson(
            """{"clientID":"client","paymentDueHintEnabled":false,"paymentScheduleHintEnabled":true}"""
        )!!

        val configuration = response.toConfiguration()

        assertThat(configuration.isPaymentDueHintEnabled).isFalse()
        assertThat(configuration.isPaymentScheduleHintEnabled).isTrue()
    }
}
