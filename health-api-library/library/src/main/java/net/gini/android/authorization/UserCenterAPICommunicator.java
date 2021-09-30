package net.gini.android.authorization;

import android.net.Uri;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import net.gini.android.GiniApiType;
import net.gini.android.RequestTaskCompletionSource;
import net.gini.android.authorization.requests.BearerJsonObjectRequest;
import net.gini.android.authorization.requests.TokenRequest;
import net.gini.android.requests.BearerLocationRequest;
import net.gini.android.requests.RetryPolicyFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import bolts.Continuation;
import bolts.Task;

import static com.android.volley.Request.Method.GET;
import static com.android.volley.Request.Method.POST;
import static com.android.volley.Request.Method.PUT;


/**
 * The UserCenterAPIManager is responsible for communication with the Gini User Center API. This includes handling HTTP
 * related error handling.
 */
public class UserCenterAPICommunicator {

    final private RequestQueue mRequestQueue;
    final private String mBaseUrl;
    final private String mClientId;
    final private String mClientSecret;
    final private RetryPolicyFactory mRetryPolicyFactory;
    private final GiniApiType mGiniApiType;

    public UserCenterAPICommunicator(final RequestQueue requestQueue, final String baseUrl,
                                     final GiniApiType giniApiType,
                                     final String clientId, final String clientSecret,
                                     final RetryPolicyFactory retryPolicyFactory) {
        mRequestQueue = requestQueue;
        mBaseUrl = baseUrl;
        mGiniApiType = giniApiType;
        mClientId = clientId;
        mClientSecret = clientSecret;
        this.mRetryPolicyFactory = retryPolicyFactory;
    }

    /**
     * Logs in this client to the Gini User Center API. Uses the instance's client credentials to identify the client.
     *
     * Please note that this is used to login the client (in other words: the app) and not a Gini user. Please do not
     * mix the different sessions.
     *
     * @return A task which will resolve to a JSONObject representing the API's response, which is a valid access token
     * for the Gini User Center API.
     */
    public Task<JSONObject> loginClient() {
        // Build and execute the request.
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final String url = mBaseUrl + "oauth/token?grant_type=client_credentials";
        TokenRequest loginRequest =
                new TokenRequest(mClientId, mClientSecret, url, null, completionSource, completionSource,
                        mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(loginRequest);

        return completionSource.getTask();
    }

    /**
     * Logs in a Gini user.
     *
     * @param userCredentials The user's credentials.
     * @return A task which will resolve to a JSONObject representing the API's response, which is a valid access token
     * for the Gini API.
     */
    public Task<JSONObject> loginUser(UserCredentials userCredentials) {
        // Build and execute the request.
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final String url = mBaseUrl + "oauth/token?grant_type=password";
        final HashMap<String, String> data = new HashMap<String, String>();
        data.put("username", userCredentials.getUsername());
        data.put("password", userCredentials.getPassword());
        TokenRequest loginRequest =
                new TokenRequest(mClientId, mClientSecret, url, data, completionSource, completionSource,
                        mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(loginRequest);

        return completionSource.getTask();
    }

    /**
     * Creates a new Gini user.
     *
     * @param userCredentials           The user's credentials.
     * @param userCenterApiSession      A valid session to do requests to the Gini User Center API.
     *
     * @return                          A task which will resolve to a JSONObject representing the API's response, which
     *                                  is a user information.
     * @throws JSONException            If the user credentials can't be JSON serialized.
     */
    public Task<Uri> createUser(final UserCredentials userCredentials, Session userCenterApiSession)
            throws JSONException {

        final RequestTaskCompletionSource<Uri> completionSource = RequestTaskCompletionSource.newCompletionSource();
        final String url = mBaseUrl + "api/users";
        final JSONObject data = new JSONObject() {{
            put("email", userCredentials.getUsername());
            put("password", userCredentials.getPassword());
        }};
        BearerLocationRequest request =
                new BearerLocationRequest(POST, url, data, userCenterApiSession, completionSource,
                        completionSource, mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<JSONObject> getUserInfo(Uri userUri, Session userCenterApiSession) {
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final BearerJsonObjectRequest request =
                new BearerJsonObjectRequest(GET, userUri.toString(), null, userCenterApiSession, mGiniApiType, completionSource,
                        completionSource, mRetryPolicyFactory.newRetryPolicy());

        mRequestQueue.add(request);
        return completionSource.getTask();
    }

    /**
     * Returns information about the Gini API session.
     *
     * Although you may use this method with any session (for Gini API or User Center API) the
     * purpose of this method is to retrieve information about Gini API sessions.
     *
     * @param giniApiSession            A valid session for the Gini API.
     * @return                          A task which will resolve to a JSONObject containing information
     *                                  about the session.
     */
    // Visible for testing
    Task<JSONObject> getGiniApiSessionTokenInfo(final Session giniApiSession) {
        final RequestTaskCompletionSource<JSONObject> completionSource = RequestTaskCompletionSource.newCompletionSource();
        try {
            final String url = mBaseUrl + "oauth/check_token?token=" +
                    URLEncoder.encode(giniApiSession.getAccessToken(), "UTF-8");
            final JsonObjectRequest request =
                    new JsonObjectRequest(GET, url, null, completionSource, completionSource);
            mRequestQueue.add(request);
        } catch (UnsupportedEncodingException e) {
            completionSource.onErrorResponse(new VolleyError(e));
        }

        return completionSource.getTask();
    }

    /**
     * Returns the user id of the logged in user.
     *
     * @param giniAPISession            The session for the Gini API, which was returned when the
     *                                  user was logged in.
     * @return                          A task which will resolve to a String containing the user id.
     */
    public Task<String> getUserId(final Session giniAPISession) {
        return getGiniApiSessionTokenInfo(giniAPISession)
                .onSuccessTask(new Continuation<JSONObject, Task<String>>() {
                    @Override
                    public Task<String> then(Task<JSONObject> task) throws Exception {
                        String userId = "";
                        try {
                            userId = task.getResult().getString("user_name");
                        } catch (JSONException e) {
                            return Task.forError(e);
                        }
                        return Task.forResult(userId);
                    }
                });
    }

    /**
     * Update the email of a user.
     *
     * @param userId                    The user's id.
     * @param newEmail                  A new email address.
     * @param oldEmail                  The previous email address of the user.
     * @param userCenterApiSession      The session for the User Center API which was returned when
     *                                  the client was authenticated.
     * @return                          A task which will resolve to an empty JSONObject.
     * @throws JSONException            If the emails can't be JSON serialized.
     */
    public Task<JSONObject> updateEmail(final String userId,
                                        final String newEmail,
                                        final String oldEmail,
                                        final Session userCenterApiSession) throws JSONException {
        final RequestTaskCompletionSource<JSONObject> completionSource = RequestTaskCompletionSource.newCompletionSource();
        final String url = mBaseUrl + "api/users/" + userId;
        final JSONObject data = new JSONObject() {{
            put("oldEmail", oldEmail);
            put("email", newEmail);
        }};
        final BearerJsonObjectRequest request =
                new BearerJsonObjectRequest(PUT, url, data, userCenterApiSession, mGiniApiType,
                        completionSource, completionSource, mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(request);

        return completionSource.getTask();
    }
}
