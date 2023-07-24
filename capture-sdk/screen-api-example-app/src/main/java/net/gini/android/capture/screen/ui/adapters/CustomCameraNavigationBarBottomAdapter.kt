package net.gini.android.capture.screen.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.camera.view.CameraNavigationBarBottomAdapter
import net.gini.android.capture.screen.databinding.CustomCameraNavigationBarBottomBinding

class CustomCameraNavigationBarBottomAdapter: CameraNavigationBarBottomAdapter {
    var viewBinding: CustomCameraNavigationBarBottomBinding? = null


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
        val binding = CustomCameraNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context), container, false)

        viewBinding = binding

        return viewBinding!!.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

}