package net.gini.android.bank.sdk.capture

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import net.gini.android.bank.sdk.R
import net.gini.android.capture.CaptureResult
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCaptureFragment
import net.gini.android.capture.GiniCaptureFragmentListener
import net.gini.android.capture.camera.CameraFragmentListener

class CaptureFlowFragment : Fragment(), GiniCaptureFragmentListener {

    internal companion object {
        fun createInstance(intent: Intent? = null): CaptureFlowFragment {
            return CaptureFlowFragment()
        }
    }

    fun setListener(listener: CaptureFlowFragmentListener) {
        this.captureFlowFragmentListener = listener
    }

    private lateinit var navController: NavController
    private lateinit var captureFlowFragmentListener: CaptureFlowFragmentListener

    // Related to navigation logic ported from CameraActivity
    private var addPages = false

    // Remember the original primary navigation fragment so that we can restore it when this fragment is detached
    private var originalPrimaryNavigationFragment: Fragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.gbs_fragment_capture_flow, container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = (childFragmentManager.fragments[0]).findNavController()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory = CaptureFlowFragmentFactory(this)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        originalPrimaryNavigationFragment = parentFragmentManager.primaryNavigationFragment

        // To be the first to handle back button pressed events we need to set this fragment as the primary navigation fragment
        parentFragmentManager.beginTransaction()
            .setPrimaryNavigationFragment(this)
            .commit()
    }

    override fun onPause() {
        super.onPause()

        // We need to restore the primary navigation fragment to not break the client's fragment navigation
        parentFragmentManager.beginTransaction()
            .setPrimaryNavigationFragment(originalPrimaryNavigationFragment)
            .commit()
    }

    override fun onFinishedWithResult(result: CaptureResult) {
        captureFlowFragmentListener.onFinishedWithResult(result)
    }

    override fun onFinishedWithCancellation() {
        captureFlowFragmentListener.onFinishedWithCancellation()
    }

    //it shows the camera in the start

    //it handles navigation between camera, review, analysis screens and .... (this navigation should be inside capture)
    //capture-sdk should have its own fragment ->  GiniCaptureFragment

    // final goal of the flow without return assistant: client -> CaptureFlowFragment -> GiniCaptureFragment -> CaptureFlowFragment -> client
    // final goal of the flow with return assistant: client -> CaptureFlowFragment -> GiniCaptureFragment -> CaptureFlowFragment -> DigitalInvoiceFragment -> client


    //new listener onFinishedWithResult -> returns the result to the client

}

interface CaptureFlowFragmentListener {
    fun onFinishedWithResult(result: CaptureResult)

    fun onFinishedWithCancellation()

    fun onCheckImportedDocument(document: Document, callback: CameraFragmentListener.DocumentCheckResultCallback) {
        callback.documentAccepted()
    }
}

class CaptureFlowFragmentFactory(
    private val giniCaptureFragmentListener: GiniCaptureFragmentListener
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        when (className) {
            GiniCaptureFragment::class.java.name -> return GiniCaptureFragment().apply {
                setListener(
                    giniCaptureFragmentListener
                )
            }

            else -> return super.instantiate(classLoader, className)
        }
    }
}
