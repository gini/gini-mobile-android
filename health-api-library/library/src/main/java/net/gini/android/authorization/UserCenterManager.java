package net.gini.android.authorization;

import android.net.Uri;

import org.json.JSONObject;

import bolts.Continuation;
import bolts.Task;

import static net.gini.android.Utils.checkNotNull;


/**
 * The UserCenterManager is responsible for managing Gini users.
 */
public class UserCenterManager {
    final private UserCenterAPICommunicator mUserCenterAPICommunicator;

    // An active session for the User Center API.
    private Session mCurrentSession;

    /**
     * @param userCenterAPICommunicator An implementation of the UserCenterAPIManager which handles the
     *                                  communication with the Gini User Center API for this manager
     *                                  instance.
     */
    public UserCenterManager(final UserCenterAPICommunicator userCenterAPICommunicator) {
        mUserCenterAPICommunicator = userCenterAPICommunicator;
    }

    /**
     * Creates a new user which has the given client credentials.
     *
     * @param userCredentials           The user's credentials.
     * @return                          A (Bolts) task which will resolve to the freshly created user.
     */
    public Task<User> createUser(final UserCredentials userCredentials) {
        // The request needs a valid user session. Get the user session.
        return getUserCenterSession()
                // Next step: Do the user creation request.
                .onSuccessTask(new Continuation<Session, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(Task<Session> sessionTask) throws Exception {
                        return mUserCenterAPICommunicator.createUser(userCredentials, sessionTask.getResult());
                    }
                }, Task.BACKGROUND_EXECUTOR)
                // And then create the user object from the API response.
                .onSuccessTask(new Continuation<Uri, Task<User>>() {
                    @Override
                    public Task<User> then(Task<Uri> task) throws Exception {
                        return getUser(task.getResult());
                    }
                }, Task.BACKGROUND_EXECUTOR);
    }

    public Task<User> getUser(final Uri userUri) {
        checkNotNull(userUri);
        return getUserCenterSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws Exception {
                final Session userCenterSession = task.getResult();
                return mUserCenterAPICommunicator.getUserInfo(userUri, userCenterSession);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccess(new Continuation<JSONObject, User>() {
            @Override
            public User then(Task<JSONObject> task) throws Exception {
                return User.fromApiResponse(task.getResult());
            }
        }, Task.BACKGROUND_EXECUTOR);
    }


    /**
     * Log-in the user which is identified with the given credentials.
     *
     * @param userCredentials           The user's credentials.
     * @return                          A (Bolts) task which will resolve to a session that can be used to do
     *                                  requests to the Gini API.
     */
    public Task<Session> loginUser(final UserCredentials userCredentials) {
        Task<JSONObject> loginTask = mUserCenterAPICommunicator.loginUser(userCredentials);
        return loginTask.onSuccess(new Continuation<JSONObject, Session>() {
            @Override
            public Session then(Task<JSONObject> task) throws Exception {
                return Session.fromAPIResponse(task.getResult());
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Update the email of the logged in user.
     *
     * @param newEmail                  A new email address.
     * @param oldEmail                  The previous email address of the user.
     * @param giniAPISession            The session for the Gini API which was returned when the user
     *                                  was logged in.
     * @return                          A (Bolts) task which will resolve to an empty JSONObject.
     */
    public Task<JSONObject> updateEmail(final String newEmail, final String oldEmail,
                                        final Session giniAPISession) {
        return getUserCenterSession().onSuccessTask(new Continuation<Session, Task<String>>() {
            @Override
            public Task<String> then(Task<Session> task) throws Exception {
                return mUserCenterAPICommunicator.getUserId(giniAPISession);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<String, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<String> task) throws Exception {
                final String userId = task.getResult();
                return mUserCenterAPICommunicator.updateEmail(userId,
                        newEmail, oldEmail, mCurrentSession);
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Returns a future that will resolve to a valid session (for the User Center API!).
     */
    protected synchronized Task<Session> getUserCenterSession() {
        // Reuse the current session if possible.
        if (mCurrentSession != null && !mCurrentSession.hasExpired()) {
            return Task.forResult(mCurrentSession);
        }
        // Or do a login.
        return loginClient();
    }

    protected Task<Session> loginClient() {
        final Task<JSONObject> loginClient = mUserCenterAPICommunicator.loginClient();
        final UserCenterManager userCenterManager = this;

        return loginClient.onSuccess(new Continuation<JSONObject, Session>() {
            @Override
            public Session then(Task<JSONObject> task) throws Exception {
                Session session = Session.fromAPIResponse(task.getResult());
                synchronized (userCenterManager) {
                    mCurrentSession = session;
                }
                return session;
            }
        }, Task.BACKGROUND_EXECUTOR);
    }
}
