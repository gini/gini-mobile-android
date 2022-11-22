package net.gini.android.capture.camera.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcCameraBottomBarBinding
import net.gini.android.capture.view.InjectedViewAdapter

interface CameraNavigationBarBottomAdapter: InjectedViewAdapter {

    fun setOnBackButtonClickListener(click: View.OnClickListener)

    fun setOnHelpButtonClickListener(click: View.OnClickListener)

    fun setBackButtonVisibility(visibility: Int)

}

class DefaultCameraNavigationBarBottomAdapter: CameraNavigationBarBottomAdapter {
    var viewBinding: GcCameraBottomBarBinding? = null


    override fun setOnBackButtonClickListener(click: View.OnClickListener) {
        viewBinding?.gcGoBack?.setOnClickListener(click)
    }

    override fun setOnHelpButtonClickListener(click: View.OnClickListener) {
        viewBinding?.gcHelp?.setOnClickListener(click)
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

