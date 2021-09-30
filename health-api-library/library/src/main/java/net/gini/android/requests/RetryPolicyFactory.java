package net.gini.android.requests;

import com.android.volley.RetryPolicy;

/**
 * Factory to create new {@link RetryPolicy} instances.
 */
public interface RetryPolicyFactory {

    RetryPolicy newRetryPolicy();
}
