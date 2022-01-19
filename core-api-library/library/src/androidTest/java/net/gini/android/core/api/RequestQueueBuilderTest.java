package net.gini.android.core.api;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;

import net.gini.android.core.api.RequestQueueBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RequestQueueBuilderTest {

    @Test
    public void testCreateDefaultRequestQueue() throws Exception {
        RequestQueueBuilder requestQueueBuilder = new RequestQueueBuilder(getApplicationContext());
        RequestQueue requestQueue = requestQueueBuilder.build();

        assertNotNull(requestQueue);
    }

    @Test
    public void testCacheConfiguration() {
        RequestQueueBuilder requestQueueBuilder = new RequestQueueBuilder(getApplicationContext());
        NoCache cache = new NoCache();
        RequestQueue requestQueue = requestQueueBuilder
                .setCache(cache)
                .build();

        assertSame(cache, requestQueue.getCache());
    }

    @Test
    public void allowSettingCustomTrustManager() {
        RequestQueueBuilder requestQueueBuilder = new RequestQueueBuilder(getApplicationContext());

        final TrustManager trustManager = new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        RequestQueue requestQueue = requestQueueBuilder
                .setTrustManager(trustManager)
                .build();

        assertNotNull(requestQueue);
    }

}
