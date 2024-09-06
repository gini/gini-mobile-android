package net.gini.android.internal.payment

import android.content.Context
import android.content.SharedPreferences
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.GiniHealthAPIBuilder
import net.gini.android.internal.payment.paymentComponent.PaymentComponentPreferences
import net.gini.android.internal.payment.util.GiniLocalization

class GiniInternalPaymentModule {

    constructor(
        context: Context,
        clientId: String = "",
        clientSecret: String = "",
        emailDomain: String = "",
        sessionManager: SessionManager? = null,
        apiVersion: Int = DEFAULT_API_VERSION
    ) {
        giniHealthAPI = GiniHealthAPIBuilder(
            context,
            clientId,
            clientSecret,
            emailDomain,
            sessionManager,
            apiVersion = apiVersion
        ).build()
    }

    constructor(
        context: Context,
        clientId: String = "",
        clientSecret: String = "",
        emailDomain: String = "",
        sessionManager: SessionManager? = null,
        merchantApiBaseUrl: String = "",
        userCenterApiBaseUrl: String? = null,
        debuggingEnabled: Boolean = false,
        apiVersion: Int = DEFAULT_API_VERSION
    ) {
        giniHealthAPI = if (sessionManager == null) {
            GiniHealthAPIBuilder(
                context,
                clientId,
                clientSecret,
                emailDomain,
                apiVersion = apiVersion
            )
        } else {
            GiniHealthAPIBuilder(context, sessionManager = sessionManager, apiVersion = apiVersion)
        }.apply {
            setApiBaseUrl(merchantApiBaseUrl)
            if (userCenterApiBaseUrl != null) {
                setUserCenterApiBaseUrl(userCenterApiBaseUrl)
            }
            setDebuggingEnabled(debuggingEnabled)
        }.build()
    }

    var giniHealthAPI: GiniHealthAPI
        private set

    /**
     * Sets the app language to the desired one from the languages the SDK is supporting. If not set then defaults to the system's language locale.
     *
     * @param language enum value for the desired language or null for default system language
     * @param context Context object to save the configuration.
     */
    fun setSDKLanguage(language: GiniLocalization?, context: Context) {
        localizedContext = null
        GiniPaymentPreferences(context).saveSDKLanguage(language)
    }

    internal class GiniPaymentPreferences(context: Context) {
        private val sharedPreferences = context.getSharedPreferences("GiniPaymentPreferences", Context.MODE_PRIVATE)

        fun saveSDKLanguage(value: GiniLocalization?) {
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putString(SDK_LANGUAGE_PREFS_KEY, value?.readableName?.uppercase())
            editor.apply()
        }

        fun getSDKLanguage(): GiniLocalization? {
            val enumValue = sharedPreferences.getString(SDK_LANGUAGE_PREFS_KEY, null)
            return if (enumValue.isNullOrEmpty()) null else GiniLocalization.valueOf(enumValue)
        }
    }

    internal var localizedContext: Context? = null

    companion object {
        private const val SDK_LANGUAGE_PREFS_KEY = "SDK_LANGUAGE_PREFS_KEY"
        private const val DEFAULT_API_VERSION = 1

        fun getSDKLanguage(context: Context): GiniLocalization? {
            return GiniPaymentPreferences(context).getSDKLanguage()
        }
    }
}
