package net.gini.android.core.api.authorization;


import static net.gini.android.core.api.Utils.CHARSET_UTF8;
import static net.gini.android.core.api.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import bolts.Continuation;
import bolts.Task;
import bolts.TaskCompletionSource;


/**
 * The AnonymousSessionManager is a SessionManager implementation that uses anonymous Gini users.
 */
public class AnonymousSessionManager implements SessionManager {

    /**
     * The UserCenterManager instance which is used to create and log in the anonymous users.
     */
    private final UserCenterManager mUserCenterManager;
    /**
     * The credentials store which is used to store the user credentials.
     */
    private final CredentialsStore mCredentialsStore;

    /**
     * The domain which is used as the e-mail domain for created users.
     */
    private final String mEmailDomain;

    /**
     * The user's current session.
     */
    private Session mCurrentSession;

    /**
     * The current task to get a new session.
     */
    private Task<Session> mCurrentSessionTask;

    public AnonymousSessionManager(final String emailDomain, final UserCenterManager userCenterManager,
                                   final CredentialsStore credentialsStore) {
        mEmailDomain = checkNotNull(emailDomain);
        mUserCenterManager = checkNotNull(userCenterManager);
        mCredentialsStore = checkNotNull(credentialsStore);
    }

    private synchronized void setSession(Session session) {
        mCurrentSession = session;
    }

    private synchronized void setCurrentSessionTask(@Nullable final Task<Session> sessionTask) {
        mCurrentSessionTask = sessionTask;
    }

    @Override
    public Task<Session> getSession() {
        final TaskCompletionSource<Session> completionSource = new TaskCompletionSource<>();

        synchronized (this) {
            if (mCurrentSession != null && !mCurrentSession.hasExpired()) {
                return Task.forResult(mCurrentSession);
            }
            if (mCurrentSessionTask != null) {
                return mCurrentSessionTask;
            }
            mCurrentSessionTask = completionSource.getTask();
        }

        Task<Session> sessionTask;

        final UserCredentials userCredentials = mCredentialsStore.getUserCredentials();
        if (userCredentials == null) {
            sessionTask = createUser().onSuccessTask(task -> loginUser());
        } else {
            sessionTask = loginUser().continueWithTask(task -> {
                if (task.isFaulted()) {
                    if (isInvalidUserError(task)) {
                        mCredentialsStore.deleteUserCredentials();
                        return createUser().onSuccessTask(task1 -> loginUser());
                    }
                }
                return task;
            });
        }

        sessionTask.continueWith(task -> {
            if (task.isFaulted()) {
                setCurrentSessionTask(null);
                completionSource.setError(task.getError());
            } else if (task.isCancelled()) {
                setCurrentSessionTask(null);
                completionSource.setCancelled();
            } else {
                Session session = task.getResult();
                setSession(session);
                setCurrentSessionTask(null);
                completionSource.setResult(session);
            }
            return null;
        });

        return completionSource.getTask();
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private boolean isInvalidUserError(Task<Session> task) {
        if (task.getError() instanceof VolleyError) {
            VolleyError error = (VolleyError) task.getError();
            if (error.networkResponse != null) {
                switch (error.networkResponse.statusCode) {
                    case 400:
                        if (error.networkResponse.data != null) {
                            try {
                                JSONObject responseJson = new JSONObject(new String(error.networkResponse.data, CHARSET_UTF8));
                                return responseJson.get("error").equals("invalid_grant");
                            } catch (JSONException ignore) {
                            }
                        }
                        break;
                    case 401:
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Log in the user whose credentials are currently stored in the credentials store. If there are no stored user
     * credentials, this method will create a new user via the UserCenterManager and then log in the newly created
     * user.
     *
     * If the email domain is different in the existing user credentials from the one that was provided
     * for this instance, a new email with the new domain will be generated and the old email will be replaced.
     *
     * @return A task which will resolve to valid Session instance.
     */
    public Task<Session> loginUser() {
        // Wrap getting the user credentials in a task, because it is much easier to handle the creation of a new
        // user then.
        Task<UserCredentials> credentialsTask;
        final UserCredentials userCredentials = mCredentialsStore.getUserCredentials();
        if (userCredentials != null) {
            if (hasUserCredentialsEmailDomain(mEmailDomain, userCredentials)) {
                credentialsTask = Task.forResult(userCredentials);
            } else {
                credentialsTask = updateEmailDomain(userCredentials);
            }

            // And log in the user when the user credentials are available.
            return credentialsTask.onSuccessTask(task -> mUserCenterManager.loginUser(task.getResult()));
        } else {
            return Task.forError(new IllegalStateException("Missing user credentials."));
        }
    }

    private Task<UserCredentials> updateEmailDomain(@NonNull final UserCredentials userCredentials) {
        final String oldEmail = userCredentials.getUsername();
        final String newEmail = generateUsername();
        return mUserCenterManager.loginUser(userCredentials)
                .onSuccessTask(task -> mUserCenterManager.updateEmail(newEmail, oldEmail, task.getResult()))
                .onSuccess(task -> {
                    mCredentialsStore.deleteUserCredentials();
                    UserCredentials newCredentials = new UserCredentials(newEmail, userCredentials.getPassword());
                    mCredentialsStore.storeUserCredentials(newCredentials);
                    return newCredentials;
                });
    }

    // Visible for testing
    boolean hasUserCredentialsEmailDomain(final String emailDomain, final UserCredentials userCredentials) {
        return userCredentials.getUsername().endsWith("@" + emailDomain);
    }

    /**
     * Creates a new user via the UserCenterManager. The user credentials of the freshly created user are then stored in
     * the credentials store.
     * <p>
     * Warning: This method overwrites the credentials of an existing user.
     *
     * @return A task which will resolve to a UserCredentials instance which store the credentials of the freshly
     * created user.
     */
    protected Task<UserCredentials> createUser() {
        final String username = generateUsername();
        final String password = generatePassword();
        final UserCredentials userCredentials = new UserCredentials(username, password);
        return mUserCenterManager.createUser(userCredentials).onSuccess(new Continuation<User, UserCredentials>() {
            @Override
            public UserCredentials then(Task<User> task) throws Exception {
                mCredentialsStore.storeUserCredentials(userCredentials);
                return userCredentials;
            }
        });
    }

    private String generateUsername() {
        return UUID.randomUUID().toString() + "@" + mEmailDomain;
    }


    private String generatePassword() {
        return UUID.randomUUID().toString();
    }
}
