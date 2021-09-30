package net.gini.android.authorization;


import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import android.net.Uri;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Date;

import bolts.Task;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class UserCenterManagerTest {

    private UserCenterManager mUserCenterManager;
    private UserCenterAPICommunicator mMockUserCenterAPICommunicator;

    @Before
    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getApplicationContext().getCacheDir().getPath());

        mMockUserCenterAPICommunicator = Mockito.mock(UserCenterAPICommunicator.class);
        mUserCenterManager = new UserCenterManager(mMockUserCenterAPICommunicator);
    }

    /**
     * Many tests require a valid response from the Gini API. This method returns a task which will resolve to a
     * JSONObject which is a valid Gini API response.
     */
    public Task<JSONObject> createTestTokenResponse(final String accessToken) throws JSONException {
        final JSONObject responseData = new JSONObject() {{
            put("token_type", "bearer");
            put("access_token", accessToken);
            put("expires_in", 3599);
        }};
        return Task.forResult(responseData);
    }

    /**
     * Creates and returns a task which will resolve to a JSONObject which is a valid Gini User Center API response for
     * requesting a user's information.
     */
    public Task<JSONObject> createLoginUserResponse(final String email) throws JSONException {
        final JSONObject responseData = new JSONObject() {{
            put("id", "88a28076-18e8-4275-b39c-eaacc240d406");
            put("email", email);
        }};
        return Task.forResult(responseData);
    }

    @Test
    public void testLoginClientReturnsFuture() {
        when(mMockUserCenterAPICommunicator.loginClient()).thenReturn(Task.forResult(new JSONObject()));

        assertNotNull(mUserCenterManager.loginClient());
    }

    @Test
    public void testLoginClientResolvesToCorrectSession() throws JSONException, InterruptedException {
        when(mMockUserCenterAPICommunicator.loginClient())
                .thenReturn(createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6"));

        final Task<Session> sessionTask = mUserCenterManager.loginClient();
        sessionTask.waitForCompletion();
        final Session session = sessionTask.getResult();

        assertEquals("74c1e7fe-e464-451f-a6eb-8f0998c46ff6", session.getAccessToken());
    }

    @Test
    public void testGetSessionReusesSession() throws JSONException, InterruptedException {
        // First try which sets the first session.
        when(mMockUserCenterAPICommunicator.loginClient())
                .thenReturn(createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6"));
        final Task<Session> firstSessionTask = mUserCenterManager.getUserCenterSession();
        firstSessionTask.waitForCompletion();
        final Session firstSession = firstSessionTask.getResult();

        // Second try which should reuse the old session.
        final JSONObject secondResponseData = new JSONObject() {{
            put("token_type", "bearer");
            put("access_token", "12345678-e464-451f-a6eb-8f0998c46ff6");
            put("expires_in", 3599);
        }};
        when(mMockUserCenterAPICommunicator.loginClient()).thenReturn(Task.forResult(secondResponseData));
        final Task<Session> secondSessionTask = mUserCenterManager.getUserCenterSession();
        secondSessionTask.waitForCompletion();
        final Session secondSession = secondSessionTask.getResult();

        assertEquals(firstSession, secondSession);
    }

    @Test
    public void testLoginUserShouldReturnTask() throws JSONException {
        UserCredentials userCredentials = new UserCredentials("foobar", "1234");
        when(mMockUserCenterAPICommunicator.loginUser(userCredentials))
                .thenReturn(createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6"));

        assertNotNull(mUserCenterManager.loginUser(userCredentials));
    }

    @Test
    public void testLoginUserShouldReturnCorrectSession() throws JSONException, InterruptedException {
        UserCredentials userCredentials = new UserCredentials("foobar", "1234");
        when(mMockUserCenterAPICommunicator.loginUser(userCredentials))
                .thenReturn(createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6"));

        final Task<Session> sessionTask = mUserCenterManager.loginUser(userCredentials);
        sessionTask.waitForCompletion();
        final Session session = sessionTask.getResult();
        assertNotNull(session);
        assertEquals("74c1e7fe-e464-451f-a6eb-8f0998c46ff6", session.getAccessToken());
    }

    @Test
    public void testCreateUserShouldReturnTask() throws JSONException {
        when(mMockUserCenterAPICommunicator.loginClient())
                .thenReturn(createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6"));

        assertNotNull(mUserCenterManager.createUser(new UserCredentials("foo", "bar")));
    }

    @Test
    public void testCreateUserShouldResolveToUser() throws JSONException, InterruptedException {
        final Uri createdUserUri = Uri.parse("https://user.gini.net/api/users/88a28076-18e8-4275-b39c-eaacc240d406");
        when(mMockUserCenterAPICommunicator.loginClient())
                .thenReturn(createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6"));
        UserCredentials userCredentials = new UserCredentials("foobar@example.com", "1234");
        when(mMockUserCenterAPICommunicator.createUser(eq(userCredentials), any(Session.class)))
                .thenReturn(Task.forResult(createdUserUri));
        final JSONObject userInfo = new JSONObject();
        userInfo.put("id", "88a28076-18e8-4275-b39c-eaacc240d406");
        userInfo.put("email", "foobar@example.com");
        when(mMockUserCenterAPICommunicator.getUserInfo(eq(createdUserUri), any(Session.class)))
                .thenReturn(Task.forResult(userInfo));

        Task<User> creationTask = mUserCenterManager.createUser(userCredentials);
        creationTask.waitForCompletion();
        User user = creationTask.getResult();
        assertNotNull(user);
        assertEquals("foobar@example.com", user.getUsername());
        assertEquals("88a28076-18e8-4275-b39c-eaacc240d406", user.getUserId());
    }

    @Test
    public void testUpdateEmailShouldReturnTask() throws JSONException {
        when(mMockUserCenterAPICommunicator.loginClient())
                .thenReturn(createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6"));
        when(mMockUserCenterAPICommunicator.getUserId(any(Session.class)))
                .thenReturn(Task.forResult("exampleUserId"));
        when(mMockUserCenterAPICommunicator.updateEmail(anyString(), anyString(), anyString(), any(Session.class)))
                .thenReturn(Task.forResult(new JSONObject()));

        assertNotNull(mUserCenterManager.updateEmail("1234@beispiel.com", "5678@example.com", new Session("example_token", new Date())));
    }
}
