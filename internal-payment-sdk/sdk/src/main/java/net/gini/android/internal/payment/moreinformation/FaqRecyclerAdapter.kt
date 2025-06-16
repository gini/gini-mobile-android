package net.gini.android.internal.payment.moreinformation

import android.text.SpannedString
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import java.util.Locale

sealed class FaqItem {
    data class Entry(
        val title: String,
        val answer: CharSequence,
        var isExpanded: Boolean = false
    ) : FaqItem()
}

class FaqRecyclerAdapter(
    private val locale: Locale?,
    private val originalData: List<Pair<String, CharSequence>>
) : RecyclerView.Adapter<FaqRecyclerAdapter.FaqViewHolder>() {

    private var items: MutableList<FaqItem.Entry> = mutableListOf()

    init {
        resetItems()
    }

    fun resetItems() {
        items.clear()
        originalData.forEach { (question, answer) ->
            items.add(FaqItem.Entry(question, answer))
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val inflater = parent.getLayoutInflaterWithGiniPaymentThemeAndLocale(locale)
        val view = inflater.inflate(R.layout.gps_item_faq_label, parent, false)
        return FaqViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    inner class FaqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // In constructor
        private val questionLabel: TextView = itemView.findViewById(R.id.gps_faq_label)
        private val expandIcon: ImageView = itemView.findViewById(R.id.gps_expand_icon)
        private val answerLabel: TextView = itemView.findViewById(R.id.gps_faq_answer_label)
        private val answerClickableArea: View = itemView.findViewById(R.id.faq_answer_clickable_area)
        private val questionClickableArea: View = itemView.findViewById(R.id.faq_question_clickable_area)
        private val divider: View = itemView.findViewById(R.id.divider2)

        fun bind(item: FaqItem.Entry, position: Int) {
            questionLabel.text = item.title
            answerLabel.text = item.answer

            // Set expansion visuals
            expandIcon.setImageResource(
                if (item.isExpanded) R.drawable.gps_faq_expanded
                else R.drawable.gps_faq_closed
            )

            answerClickableArea.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            divider.visibility = if (item.isExpanded) View.VISIBLE else View.GONE

            if (item.answer is SpannedString) {
                answerLabel.movementMethod = LinkMovementMethod.getInstance()
            } else {
                answerLabel.movementMethod = null
            }
            divider.visibility = if (position == items.size - 1) View.GONE else View.VISIBLE

            questionClickableArea.setOnClickListener {
                item.isExpanded = !item.isExpanded
                notifyItemChanged(position)
            }

            questionClickableArea.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus && v.isPressed) v.performClick()
            }

            answerClickableArea.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus && v.isPressed) v.performClick()
            }

            itemView.setOnClickListener(null)
        }

    }
}
