package net.gini.android.core.api.authorization;


import bolts.Task;


public interface SessionManager {
    public Task<Session> getSession();
}
