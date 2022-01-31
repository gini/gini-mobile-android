package net.gini.android.core.api.internal;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.android.volley.Cache;
import com.android.volley.RetryPolicy;
import com.squareup.moshi.Moshi;

import net.gini.android.core.api.ApiCommunicator;
import net.gini.android.core.api.DocumentManager;
import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.GiniApiType;
import net.gini.android.core.api.authorization.CredentialsStore;
import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.authorization.SessionManager;
import net.gini.android.core.api.models.CompoundExtraction;
import net.gini.android.core.api.models.ExtractionsContainer;
import net.gini.android.core.api.models.SpecificExtraction;
import net.gini.android.core.api.test.TestGiniApiType;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import bolts.Task;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class GiniCoreAPIBuilderTest {
    @Test
    public void testBuilderReturnsGiniInstance() {
        CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        assertNotNull(builder.build());
    }

    @Test
    public void testBuilderReturnsCorrectConfiguredGiniInstance() {
        CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        GiniCoreAPI giniCoreAPI = builder.build();

        assertNotNull(giniCoreAPI.getDocumentTaskManager());
        assertNotNull(giniCoreAPI.getCredentialsStore());
    }

    @Test
    public void testBuilderWorksWithAlternativeSessionManager() {
        final SessionManager sessionManager = new NullSessionManager();

        final CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), sessionManager);
        final GiniCoreAPI giniCoreAPI = builder.build();

        assertNotNull(giniCoreAPI);
        assertNotNull(giniCoreAPI.getDocumentTaskManager());
        assertNotNull(giniCoreAPI.getCredentialsStore());
    }

    @Test
    public void testSetWrongConnectionTimeout() {
        CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setConnectionTimeoutInMs(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testSetWrongConnectionMaxNumberOfRetries() {
        CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setMaxNumberOfRetries(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testSetWrongConnectionBackOffMultiplier() {
        CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setConnectionBackOffMultiplier(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testRetryPolicyWiring() {
        CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        builder.setConnectionTimeoutInMs(3333);
        builder.setMaxNumberOfRetries(66);
        builder.setConnectionBackOffMultiplier(1.3636f);
        GiniCoreAPI giniCoreAPI = builder.build();

        final DocumentTaskManager documentTaskManager = giniCoreAPI.getDocumentTaskManager();
        final RetryPolicy retryPolicy = documentTaskManager.mApiCommunicator.mRetryPolicyFactory.newRetryPolicy();
        assertEquals(3333, retryPolicy.getCurrentTimeout());
        assertEquals(0, retryPolicy.getCurrentRetryCount());
    }

    @Test
    public void testVolleyCacheConfiguration() {
        CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        NullCache nullCache = new NullCache();
        builder.setCache(nullCache);
        GiniCoreAPI giniHealthAPI = builder.build();

        assertSame(giniHealthAPI.getDocumentTaskManager().mApiCommunicator.mRequestQueue.getCache(), nullCache);
    }

    @Test
    public void allowsSettingCustomTrustManager() {
        CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");

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

        GiniCoreAPI sdkInstance = builder
                .setTrustManager(trustManager)
                .build();

        assertNotNull(sdkInstance);
    }

    private static class TestDocumentTaskManager extends DocumentTaskManager<ApiCommunicator, ExtractionsContainer> {

        public TestDocumentTaskManager(ApiCommunicator apiCommunicator, SessionManager sessionManager, GiniApiType giniApiType, Moshi moshi) {
            super(apiCommunicator, sessionManager, giniApiType, moshi);
        }

        @NonNull
        @Override
        protected ExtractionsContainer createExtractionsContainer(@NonNull Map<String, SpecificExtraction> specificExtractions, @NonNull Map<String, CompoundExtraction> compoundExtractions, @NonNull JSONObject responseJSON) throws Exception {
            return new ExtractionsContainer(specificExtractions, compoundExtractions);
        }
    }

    private static class TestDocumentManager extends DocumentManager<ApiCommunicator, TestDocumentTaskManager, ExtractionsContainer> {

        public TestDocumentManager(@NonNull TestDocumentTaskManager documentTaskManager) {
            super(documentTaskManager);
        }
    }

    private static class TestGiniCoreAPI extends GiniCoreAPI<TestDocumentTaskManager, TestDocumentManager, ApiCommunicator, ExtractionsContainer> {

        protected TestGiniCoreAPI(TestDocumentTaskManager documentTaskManager, CredentialsStore credentialsStore) {
            super(documentTaskManager, credentialsStore);
        }

        @Override
        public TestDocumentManager getDocumentManager() {
            return Mockito.mock(TestDocumentManager.class);
        }
    }

    private static class CoreAPIBuilder extends GiniCoreAPIBuilder<TestDocumentTaskManager, TestDocumentManager, TestGiniCoreAPI, ApiCommunicator, ExtractionsContainer> {

        protected CoreAPIBuilder(@NonNull Context context, @NonNull String clientId, @NonNull String clientSecret, @NonNull String emailDomain) {
            super(context, clientId, clientSecret, emailDomain);
        }

        protected CoreAPIBuilder(@NonNull Context context, @NonNull SessionManager sessionManager) {
            super(context, sessionManager);
        }

        @NonNull
        @Override
        public GiniApiType getGiniApiType() {
            return new TestGiniApiType();
        }

        @Override
        public TestGiniCoreAPI build() {
            return new TestGiniCoreAPI(getDocumentTaskManager(), getCredentialsStore());
        }

        @NonNull
        @Override
        protected ApiCommunicator createApiCommunicator() {
            return new ApiCommunicator(getApiBaseUrl(), getGiniApiType(), getRequestQueue(), getRetryPolicyFactory());
        }

        @NonNull
        @Override
        protected TestDocumentTaskManager createDocumentTaskManager() {
            return new TestDocumentTaskManager(getApiCommunicator(), getSessionManager(), getGiniApiType(), getMoshi());
        }

    }

    private static final class NullCache implements Cache {

        @Override
        public Entry get(final String key) {
            return null;
        }

        @Override
        public void put(final String key, final Entry entry) {
        }

        @Override
        public void initialize() {
        }

        @Override
        public void invalidate(final String key, final boolean fullExpire) {
        }

        @Override
        public void remove(final String key) {
        }

        @Override
        public void clear() {
        }
    }

    private class NullSessionManager implements SessionManager {

        @Override
        public Task<Session> getSession() {
            return Task.forError(new UnsupportedOperationException("NullSessionManager can't create sessions"));
        }
    }
}
