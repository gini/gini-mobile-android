package net.gini.android.capture.help

import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.databinding.GcFragmentFileImportHelpBinding
import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.internal.util.autoCleared
import net.gini.android.capture.internal.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.NavButtonType
import net.gini.android.capture.view.NavigationBarTopAdapter

/**
 * Internal use only.
 */
class FileImportHelpFragment : Fragment() {
    private var binding: GcFragmentFileImportHelpBinding by autoCleared()
    private var snackbar: Snackbar? = null

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniCaptureTheme(inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GcFragmentFileImportHelpBinding.inflate(inflater)
        setupTopBarNavigation()
        setupBottomBarNavigation()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        waitForHalfSecondAndShowSnackBar()
    }

    override fun onDestroy() {
        super.onDestroy()
        snackbar?.dismiss()
    }

    private fun setupTopBarNavigation() {
        val topBarInjectedViewContainer = binding.gcInjectedNavigationBarContainerTop
        if (GiniCapture.hasInstance()) {
            topBarInjectedViewContainer.injectedViewAdapterHolder = InjectedViewAdapterHolder<NavigationBarTopAdapter>(
                GiniCapture.getInstance().internal().navigationBarTopAdapterInstance
            ) { injectedViewAdapter: NavigationBarTopAdapter ->
                injectedViewAdapter.setNavButtonType(
                    if (GiniCapture.getInstance()
                            .isBottomNavigationBarEnabled
                    ) NavButtonType.NONE else NavButtonType.BACK
                )
                injectedViewAdapter.setTitle(getString(R.string.gc_title_file_import))
                injectedViewAdapter.setOnNavButtonClickListener(IntervalClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                })
            }
        }
    }

    private fun setupBottomBarNavigation() {
        val injectedViewContainer = binding.gcInjectedNavigationBarContainerBottom
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {
            injectedViewContainer.injectedViewAdapterHolder = InjectedViewAdapterHolder<HelpNavigationBarBottomAdapter>(
                GiniCapture.getInstance().internal().helpNavigationBarBottomAdapterInstance
            ) { injectedViewAdapter: HelpNavigationBarBottomAdapter ->
                injectedViewAdapter.setOnBackClickListener(IntervalClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                })
            }
        }
    }

    private fun waitForHalfSecondAndShowSnackBar() {
        Handler(Looper.getMainLooper()).postDelayed({ this.showCustomSnackBar() }, 500)
    }

    private fun showCustomSnackBar() {
        val constraintLayout: ConstraintLayout = binding.gcFileImportConstraintLayout

        snackbar = Snackbar.make(constraintLayout, getString(R.string.gc_snackbar_illustrations), Snackbar.LENGTH_INDEFINITE).apply {
            setTextMaxLines(5)

            val typedValue = TypedValue()
            val theme: Resources.Theme = requireContext().theme
            theme.resolveAttribute(androidx.appcompat.R.attr.colorAccent, typedValue, true)
            @ColorInt val color = typedValue.data

            setAction(getString(R.string.gc_snackbar_dismiss)) {
                dismiss()
            }
            setActionTextColor(color) // snackbar action text color

            val bottomPadding =
                if (GiniCapture.getInstance().isBottomNavigationBarEnabled) resources.getDimension(R.dimen.gc_large_96)
                    .toInt() else resources.getDimension(R.dimen.gc_large).toInt()
            val params = view.layoutParams as FrameLayout.LayoutParams
            params.setMargins(resources.getDimension(R.dimen.gc_large).toInt(), 0, resources.getDimension(R.dimen.gc_large).toInt(), bottomPadding)
            view.layoutParams = params
            view.minimumHeight = resources.getDimension(R.dimen.gc_snackbar_text_height).toInt()

            show()
        }

    }

    companion object {
        @JvmStatic
        fun newInstance() = FileImportHelpFragment()
    }
}