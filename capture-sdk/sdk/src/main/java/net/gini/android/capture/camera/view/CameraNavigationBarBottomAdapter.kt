package net.gini.android.capture.camera.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcCameraBottomBarBinding
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.internal.ui.setIntervalClickListener
import net.gini.android.capture.view.InjectedViewAdapter

interface CameraNavigationBarBottomAdapter: InjectedViewAdapter {

    fun setOnBackButtonClickListener(click: IntervalClickListener?)

    fun setOnHelpButtonClickListener(click: IntervalClickListener?)

    fun setBackButtonVisibility(visibility: Int)

}

class DefaultCameraNavigationBarBottomAdapter: CameraNavigationBarBottomAdapter {
    var viewBinding: GcCameraBottomBarBinding? = null


    override fun setOnBackButtonClickListener(click: IntervalClickListener?) {
        viewBinding?.gcGoBack?.setOnClickListener(click)
    }

    override fun setOnHelpButtonClickListener(click: IntervalClickListener?) {
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

