package net.gini.android.health.api;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.Cache;
import com.android.volley.RetryPolicy;

import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.authorization.SessionManager;
import net.gini.android.health.api.GiniHealthAPI;
import net.gini.android.health.api.GiniHealthAPIBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import bolts.Task;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class GiniHealthAPIBuilderTest {

    @Test
    public void testBuilderReturnsGiniInstance() {
        GiniHealthAPIBuilder builder = new GiniHealthAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        assertNotNull(builder.build());
    }

    @Test
    public void testBuilderReturnsCorrectConfiguredGiniInstance() {
        GiniHealthAPIBuilder builder = new GiniHealthAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        GiniHealthAPI giniHealthAPI = builder.build();

        assertNotNull(giniHealthAPI.getDocumentTaskManager());
        assertNotNull(giniHealthAPI.getCredentialsStore());
    }

    @Test
    public void testBuilderWorksWithAlternativeSessionManager() {
        final SessionManager sessionManager = new NullSessionManager();

        final GiniHealthAPIBuilder builder = new GiniHealthAPIBuilder(getApplicationContext(), sessionManager);
        final GiniHealthAPI giniHealthAPI = builder.build();

        assertNotNull(giniHealthAPI);
        assertNotNull(giniHealthAPI.getDocumentTaskManager());
        assertNotNull(giniHealthAPI.getCredentialsStore());
    }

    @Test
    public void testSetWrongConnectionTimeout() {
        GiniHealthAPIBuilder builder = new GiniHealthAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setConnectionTimeoutInMs(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testSetWrongConnectionMaxNumberOfRetries() {
        GiniHealthAPIBuilder builder = new GiniHealthAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setMaxNumberOfRetries(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testSetWrongConnectionBackOffMultiplier() {
        GiniHealthAPIBuilder builder = new GiniHealthAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setConnectionBackOffMultiplier(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testRetryPolicyWiring() {
        GiniHealthAPIBuilder builder = new GiniHealthAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        builder.setConnectionTimeoutInMs(3333);
        builder.setMaxNumberOfRetries(66);
        builder.setConnectionBackOffMultiplier(1.3636f);
        GiniHealthAPI giniHealthAPI = builder.build();

        final DocumentTaskManager documentTaskManager = giniHealthAPI.getDocumentTaskManager();
        final RetryPolicy retryPolicy = documentTaskManager.mApiCommunicator.mRetryPolicyFactory.newRetryPolicy();
        assertEquals(3333, retryPolicy.getCurrentTimeout());
        assertEquals(0, retryPolicy.getCurrentRetryCount());
    }

    @Test
    public void testVolleyCacheConfiguration() {
        GiniHealthAPIBuilder builder = new GiniHealthAPIBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        NullCache nullCache = new NullCache();
        builder.setCache(nullCache);
        GiniHealthAPI giniHealthAPI = builder.build();

        assertSame(giniHealthAPI.getDocumentTaskManager().mApiCommunicator.mRequestQueue.getCache(), nullCache);
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
