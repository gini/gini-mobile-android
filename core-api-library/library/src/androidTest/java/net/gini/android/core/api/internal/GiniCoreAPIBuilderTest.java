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

import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.GiniApiType;
import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.authorization.SessionManager;

import org.junit.Test;
import org.junit.runner.RunWith;

import bolts.Task;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class GiniCoreAPIBuilderTest {
    @Test
    public void testBuilderReturnsGiniInstance() {
        CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com")
        builder.setGiniApiType(GiniApiType.DEFAULT);
        assertNotNull(builder.build());
    }

    @Test
    public void testBuilderReturnsCorrectConfiguredGiniInstance() {
        CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        builder.setGiniApiType(GiniApiType.DEFAULT);
        GiniCoreAPI giniHealthAPI = builder.build();

        assertNotNull(giniHealthAPI.getDocumentTaskManager());
        assertNotNull(giniHealthAPI.getCredentialsStore());
    }

    @Test
    public void testBuilderWorksWithAlternativeSessionManager() {
        final SessionManager sessionManager = new NullSessionManager();

        final CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), sessionManager);
        builder.setGiniApiType(GiniApiType.DEFAULT);
        final GiniCoreAPI giniHealthAPI = builder.build();

        assertNotNull(giniHealthAPI);
        assertNotNull(giniHealthAPI.getDocumentTaskManager());
        assertNotNull(giniHealthAPI.getCredentialsStore());
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
        builder.setGiniApiType(GiniApiType.DEFAULT);
        builder.setConnectionTimeoutInMs(3333);
        builder.setMaxNumberOfRetries(66);
        builder.setConnectionBackOffMultiplier(1.3636f);
        GiniCoreAPI giniHealthAPI = builder.build();

        final DocumentTaskManager documentTaskManager = giniHealthAPI.getDocumentTaskManager();
        final RetryPolicy retryPolicy = documentTaskManager.mApiCommunicator.mRetryPolicyFactory.newRetryPolicy();
        assertEquals(3333, retryPolicy.getCurrentTimeout());
        assertEquals(0, retryPolicy.getCurrentRetryCount());
    }

    @Test
    public void testVolleyCacheConfiguration() {
        CoreAPIBuilder builder = new CoreAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        builder.setGiniApiType(GiniApiType.DEFAULT);
        NullCache nullCache = new NullCache();
        builder.setCache(nullCache);
        GiniCoreAPI giniHealthAPI = builder.build();

        assertSame(giniHealthAPI.getDocumentTaskManager().mApiCommunicator.mRequestQueue.getCache(), nullCache);
    }

    private static class CoreAPIBuilder extends GiniCoreAPIBuilder<GiniCoreAPI> {

        protected CoreAPIBuilder(@NonNull Context context, @NonNull String clientId, @NonNull String clientSecret, @NonNull String emailDomain) {
            super(context, clientId, clientSecret, emailDomain);
        }

        protected CoreAPIBuilder(@NonNull Context context, @NonNull SessionManager sessionManager) {
            super(context, sessionManager);
        }

        @Override
        public GiniCoreAPI build() {
            return new GiniCoreAPI(getDocumentTaskManager(), getCredentialsStore());
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
