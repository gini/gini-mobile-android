package net.gini.android.health.sdk.review

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import net.gini.android.health.sdk.R

/**
 * Created by Alpár Szotyori on 29.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

/**
 * [MaterialButton] subclass which draws the [R.drawable.ghs_edit_icon] icon in the top right corner.
 */
internal class BankButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : MaterialButton(context, attrs, defStyle) {

    private val editIconSize = context.resources.getDimensionPixelSize(R.dimen.ghs_bank_edit_icon_size)
    private val editIconPadding = context.resources.getDimensionPixelSize(R.dimen.ghs_bank_edit_icon_padding)
    private val editIconDrawable =
        ResourcesCompat.getDrawable(context.resources, R.drawable.ghs_edit_icon, context.theme)?.apply {
            setBounds(0, 0, editIconSize, editIconSize)
        }

    var showEditIcon = true

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (showEditIcon) {
            canvas?.apply {
                save()
                translate((width - editIconSize - editIconPadding).toFloat(), insetTop.toFloat() + editIconPadding)
                editIconDrawable?.draw(this)
                restore()
            }
        }
    }
}