package net.gini.android.bank.sdk.exampleapp.ui.resources

import android.util.Log
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Retries a failing test up to [retryCount] additional times before finally failing it.
 *
 * The Bank SDK UI tests exercise real end-to-end flows that call the Gini API over the
 * network. On remote/BrowserStack devices these occasionally lose a timing race and fail
 * transiently. Retrying gives such a test another chance, while a genuinely broken test
 * (which fails on every attempt) still fails.
 *
 * Apply it as the OUTERMOST rule (lowest `order`, since in JUnit a lower order wraps
 * further out) so each attempt re-runs the whole test — including the ActivityScenarioRule,
 * which relaunches a fresh activity:
 *
 *     @get:Rule(order = -1)
 *     val retryRule = RetryRule()
 *
 *     @get:Rule
 *     val activityRule = activityScenarioRule<MainActivity>()
 *
 * Skipped tests (AssumptionViolatedException, e.g. the offline test on BrowserStack) are
 * never retried — they stay skipped.
 */
class RetryRule(private val retryCount: Int = 2) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val maxAttempts = retryCount + 1
                var lastError: Throwable? = null
                for (attempt in 1..maxAttempts) {
                    try {
                        base.evaluate()
                        if (attempt > 1) {
                            Log.w(TAG, "${description.displayName} passed on retry (attempt $attempt/$maxAttempts)")
                        }
                        return
                    } catch (skipped: AssumptionViolatedException) {
                        // A skipped test must stay skipped, not be retried into a failure.
                        throw skipped
                    } catch (error: Throwable) {
                        lastError = error
                        Log.w(TAG, "${description.displayName} failed on attempt $attempt/$maxAttempts, retrying", error)
                    }
                }
                throw lastError ?: IllegalStateException("RetryRule produced no result")
            }
        }
    }

    companion object {
        private const val TAG = "RetryRule"
    }
}
