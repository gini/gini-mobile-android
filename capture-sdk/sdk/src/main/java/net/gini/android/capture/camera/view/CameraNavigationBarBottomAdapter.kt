package net.gini.android.capture.camera.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcCameraBottomBarBinding
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Adapter for injecting a custom bottom navigation bar on the camera screen.
 */
interface CameraNavigationBarBottomAdapter: InjectedViewAdapter {

    /**
     * Set the click listener for the back button.
     *
     * @param listener the click listener for the button
     */
    fun setOnBackButtonClickListener(listener: View.OnClickListener?)

    /**
     * Set the click listener for the help button.
     *
     * @param listener the click listener for the button
     */
    fun setOnHelpButtonClickListener(listener: View.OnClickListener?)

    /**
     * Set back button visibility.
     *
     * @param visibility one of the view visibility values: [View.VISIBLE], [View.INVISIBLE], or [View.GONE]
     */
    fun setBackButtonVisibility(visibility: Int)

}

/**
 * Internal use only.
 */
class DefaultCameraNavigationBarBottomAdapter: CameraNavigationBarBottomAdapter {
    var viewBinding: GcCameraBottomBarBinding? = null


    override fun setOnBackButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.gcGoBack?.setOnClickListener(listener)
    }

    override fun setOnHelpButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.gcHelp?.setOnClickListener(listener)
    }

    override fun setBackButtonVisibility(visibility: Int) {
        viewBinding?.gcGoBack?.visibility = visibility
    }

    override fun onCreateView(container: ViewGroup): View {
        val binding = GcCameraBottomBarBinding.inflate(LayoutInflater.from(container.context), container, false)

        viewBinding = binding

        return viewBinding!!.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

}

