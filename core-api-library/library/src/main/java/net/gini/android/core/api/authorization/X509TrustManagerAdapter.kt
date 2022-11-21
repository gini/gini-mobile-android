package net.gini.android.core.api.authorization

import java.security.cert.X509Certificate
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Created by Alpár Szotyori on 10.10.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Adapts [TrustManager] instances to [X509TrustManager] by forwarding [X509TrustManager] method calls to the
 * [TrustManager] instance if it is an instance of an [X509TrustManager].
 */
internal class X509TrustManagerAdapter(private val trustManager: TrustManager): X509TrustManager {

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        if (trustManager is X509TrustManager) {
            trustManager.checkClientTrusted(chain, authType)
        }
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        if (trustManager is X509TrustManager) {
            trustManager.checkServerTrusted(chain, authType)
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return if (trustManager is X509TrustManager) {
            trustManager.acceptedIssuers
        } else {
            arrayOf()
        }
    }
}