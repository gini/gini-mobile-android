package net.gini.android.bank.sdk.exampleapp.ui.resources
import androidx.test.espresso.IdlingResource

class SimpleIdlingResource(private val waitTime: Long) : IdlingResource {

    @Volatile
    private var isIdleNow = true
    private var callback: IdlingResource.ResourceCallback? = null

    override fun getName() = SimpleIdlingResource::class.java.name

    override fun isIdleNow() = isIdleNow

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }

    fun setIdleState(isIdleNow: Boolean) {
        this.isIdleNow = isIdleNow
        if (isIdleNow && callback != null) {
            callback!!.onTransitionToIdle()
        }
    }

    fun waitForIdle() {
        isIdleNow = false
        Thread {
            try {
                Thread.sleep(waitTime)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            setIdleState(true)
        }.start()
    }
}
