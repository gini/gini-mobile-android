package net.gini.android.requests;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RetryPolicy;

/**
 * {@link RetryPolicyFactory} implementation that uses Volley's {@link DefaultRetryPolicy}.
 */
public class DefaultRetryPolicyFactory implements RetryPolicyFactory {

    private final int mConnectionTimeoutInMs;
    private final int mMaxNumRetries;
    private final float mBackoffMultiplier;

    public DefaultRetryPolicyFactory() {
        this(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
             DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    }

    public DefaultRetryPolicyFactory(final int connectionTimeoutInMs, final int maxNumberOfRetries,
                                     final float backoffMultiplier) {
        if (connectionTimeoutInMs < 0) {
            throw new IllegalArgumentException("connectionTimeoutInMs can't be less than 0");
        } else if (maxNumberOfRetries < 0) {
            throw new IllegalArgumentException("maxNumberOfRetries can't be less than 0");
        } else if (backoffMultiplier < 0.0) {
            throw new IllegalArgumentException("backoffMultiplier can't be less than 0");
        }
        mConnectionTimeoutInMs = connectionTimeoutInMs;
        mMaxNumRetries = maxNumberOfRetries;
        mBackoffMultiplier = backoffMultiplier;
    }

    @Override
    public RetryPolicy newRetryPolicy() {
        return new DefaultRetryPolicy(mConnectionTimeoutInMs, mMaxNumRetries, mBackoffMultiplier);
    }
}
