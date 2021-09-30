package net.gini.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.VolleyError;

import org.junit.Test;
import org.junit.runner.RunWith;

import bolts.Task;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RequestTaskCompletionSourceTest {

    @Test
    public void testNewRequestTaskCompletionSource() {
        assertNotNull(RequestTaskCompletionSource.newCompletionSource());
    }

    @Test
    public void testGetTaskShouldReturnTask() {
        RequestTaskCompletionSource<String> requestTaskCompletionSource = RequestTaskCompletionSource.newCompletionSource();
        assertNotNull(requestTaskCompletionSource.getTask());
    }

    @Test
    public void testCompletesTask() {
        RequestTaskCompletionSource<String> requestTaskCompletionSource = RequestTaskCompletionSource.newCompletionSource();
        Task<String> task = requestTaskCompletionSource.getTask();

        requestTaskCompletionSource.onResponse("foobar");

        assertEquals("foobar", task.getResult());
        assertFalse(task.isFaulted());
        assertNull(task.getError());
    }

    @Test
    public void testResolvesError() {
        RequestTaskCompletionSource<String> requestTaskCompletionSource = RequestTaskCompletionSource.newCompletionSource();
        Task<String> task = requestTaskCompletionSource.getTask();
        VolleyError error = new VolleyError();
        requestTaskCompletionSource.onErrorResponse(error);

        assertEquals(error, task.getError());
        assertTrue(task.isFaulted());
        assertNull(task.getResult());
    }
}
