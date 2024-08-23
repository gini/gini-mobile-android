package net.gini.android.merchant.sdk.util

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.gini.android.merchant.sdk.R


internal open class GmsBottomSheetDialogFragment: BottomSheetDialogFragment() {
    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniMerchantTheme(inflater)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val wrappedContext = requireContext().wrappedWithGiniMerchantTheme()
        val dialog = BottomSheetDialog(wrappedContext, theme)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(wrappedContext, R.color.gms_bottom_sheet_scrim))
        colorDrawable.alpha = 102 // 40% alpha
        dialog.window?.setBackgroundDrawable(colorDrawable)

        dialog.behavior.isFitToContents = true
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        return dialog
    }
}