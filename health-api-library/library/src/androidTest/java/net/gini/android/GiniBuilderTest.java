package net.gini.android;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.Cache;
import com.android.volley.RetryPolicy;

import net.gini.android.authorization.Session;
import net.gini.android.authorization.SessionManager;

import org.junit.Test;
import org.junit.runner.RunWith;

import bolts.Task;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class GiniBuilderTest {

    @Test
    public void testBuilderReturnsGiniInstance() {
        GiniBuilder builder = new GiniBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        assertNotNull(builder.build());
    }

    @Test
    public void testBuilderReturnsCorrectConfiguredGiniInstance() {
        GiniBuilder builder = new GiniBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        Gini giniInstance = builder.build();

        assertNotNull(giniInstance.getDocumentTaskManager());
        assertNotNull(giniInstance.getCredentialsStore());
    }

    @Test
    public void testBuilderWorksWithAlternativeSessionManager() {
        final SessionManager sessionManager = new NullSessionManager();

        final GiniBuilder builder = new GiniBuilder(getApplicationContext(), sessionManager);
        final Gini giniInstance = builder.build();

        assertNotNull(giniInstance);
        assertNotNull(giniInstance.getDocumentTaskManager());
        assertNotNull(giniInstance.getCredentialsStore());
    }

    @Test
    public void testSetWrongConnectionTimeout() {
        GiniBuilder builder = new GiniBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setConnectionTimeoutInMs(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testSetWrongConnectionMaxNumberOfRetries() {
        GiniBuilder builder = new GiniBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setMaxNumberOfRetries(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testSetWrongConnectionBackOffMultiplier() {
        GiniBuilder builder = new GiniBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setConnectionBackOffMultiplier(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testRetryPolicyWiring() {
        GiniBuilder builder = new GiniBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        builder.setConnectionTimeoutInMs(3333);
        builder.setMaxNumberOfRetries(66);
        builder.setConnectionBackOffMultiplier(1.3636f);
        Gini gini = builder.build();

        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();
        final RetryPolicy retryPolicy = documentTaskManager.mApiCommunicator.mRetryPolicyFactory.newRetryPolicy();
        assertEquals(3333, retryPolicy.getCurrentTimeout());
        assertEquals(0, retryPolicy.getCurrentRetryCount());
    }

    @Test
    public void testVolleyCacheConfiguration() {
        GiniBuilder builder = new GiniBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        NullCache nullCache = new NullCache();
        builder.setCache(nullCache);
        Gini giniInstance = builder.build();

        assertSame(giniInstance.getDocumentTaskManager().mApiCommunicator.mRequestQueue.getCache(), nullCache);
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
