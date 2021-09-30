package net.gini.android.authorization;


import bolts.Task;


public interface SessionManager {
    public Task<Session> getSession();
}
