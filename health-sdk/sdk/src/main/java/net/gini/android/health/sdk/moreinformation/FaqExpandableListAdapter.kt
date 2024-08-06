package net.gini.android.health.sdk.moreinformation

import android.text.SpannedString
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import net.gini.android.health.sdk.databinding.GhsItemFaqAnswerBinding
import net.gini.android.health.sdk.databinding.GhsItemFaqLabelBinding
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.util.getLayoutInflaterWithGiniHealthThemeAndLocale
import java.util.Locale

/**
 * Created by dani on 26/02/2024.
 */

internal class FaqExpandableListAdapter(
    val dataSet: List<Pair<String, CharSequence>>,
    private val locale: Locale?
    ) : BaseExpandableListAdapter() {
    override fun getGroupCount(): Int = dataSet.size
    override fun getChildrenCount(listPosition: Int): Int = 1
    override fun getGroup(listPosition: Int): Any = dataSet[listPosition].first

    override fun getChild(listPosition: Int, p1: Int): CharSequence = dataSet[listPosition].second

    override fun getGroupId(position: Int): Long = position.toLong()

    override fun getChildId(p0: Int, childPosition: Int): Long = childPosition.toLong()

    override fun hasStableIds(): Boolean = true

    override fun getGroupView(position: Int, isExpanded: Boolean, p2: View?, parent: ViewGroup): View {
        val groupView = GhsItemFaqLabelBinding.inflate(parent.getLayoutInflaterWithGiniHealthThemeAndLocale(locale), parent, false)
        groupView.ghsFaqLabel.text = dataSet[position].first
        return groupView.root
    }

    override fun getChildView(p0: Int, p1: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        val groupView = GhsItemFaqAnswerBinding.inflate(parent.getLayoutInflaterWithGiniHealthThemeAndLocale(locale), parent, false)
        val text = getChild(p0, p1)
        if (text is SpannedString) {
            groupView.ghsFaqAnswerLabel.movementMethod = LinkMovementMethod.getInstance()
        }
        groupView.ghsFaqAnswerLabel.text = text
        groupView.divider2.visibility = if (p0 != dataSet.size-1) View.VISIBLE else View.GONE
        return groupView.root
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean = false
}