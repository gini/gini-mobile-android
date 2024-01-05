package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.di.GiniCaptureNetworkServiceDebugEnabled
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureFragmentListener
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.review.multipage.view.DefaultReviewNavigationBarBottomAdapter
import net.gini.android.capture.view.DefaultLoadingIndicatorAdapter
import javax.inject.Inject

@AndroidEntryPoint
class CaptureFlowSampleFragment : Fragment(R.layout.fragment_capture_flow_sample),
    GiniCaptureFragmentListener {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.button_startCaptureFlow).setOnClickListener {

            configureGiniCapture()

            val captureFlowFragment = GiniCapture.createGiniCaptureFragment()
            captureFlowFragment.setListener(this)

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_host, captureFlowFragment, "fragment_host")
                .addToBackStack(null)
                .commit()

        }
    }



    override fun onFinishedWithResult(result: GiniCaptureSpecificExtraction) {
        TODO("Not yet implemented")
    }

    override fun onFinishedWithCancellation() {
        requireActivity().finish()
    }

    @Inject
    @GiniCaptureNetworkServiceDebugEnabled
    lateinit var giniCaptureDefaultNetworkService: GiniCaptureDefaultNetworkService


    private fun configureGiniCapture() {
        
        val builder = GiniCapture.newInstance(requireContext())
            .setGiniCaptureNetworkService(
                giniCaptureDefaultNetworkService
            )
            .setDocumentImportEnabledFileTypes(DocumentImportEnabledFileTypes.PDF_AND_IMAGES)
            .setFileImportEnabled(true)
            .setQRCodeScanningEnabled(true)
            .setMultiPageEnabled(true)
        builder.setFlashButtonEnabled(true)
        builder.setReviewBottomBarNavigationAdapter(DefaultReviewNavigationBarBottomAdapter())
        builder.setLoadingIndicatorAdapter(DefaultLoadingIndicatorAdapter())

        builder.build()
    }


}


