package net.gini.android;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;

import org.junit.Test;
import org.junit.runner.RunWith;

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

}
