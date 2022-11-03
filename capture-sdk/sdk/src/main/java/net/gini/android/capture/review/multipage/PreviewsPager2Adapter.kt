package net.gini.android.capture.review.multipage

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.findFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import androidx.viewpager2.widget.ViewPager2
import net.gini.android.capture.document.GiniCaptureDocumentError
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.review.multipage.previews.PreviewFragment
import net.gini.android.capture.review.multipage.previews.PreviewFragment.ErrorButtonAction
import net.gini.android.capture.review.multipage.previews.PreviewFragmentListener
import net.gini.android.capture.review.multipage.previews.PreviewsAdapterListener

class PreviewsPager2Adapter(
    val fm: FragmentActivity,
    private val multiPageDocument: ImageMultiPageDocument,
    private val listener: PreviewsAdapterListener,
    private val previewFragmentListener: PreviewFragmentListener
) : FragmentStateAdapter(fm) {

    private val mFragments = arrayListOf<PreviewFragment>()

    override fun getItemCount(): Int {
        return multiPageDocument.documents.size
    }

    override fun createFragment(position: Int): Fragment {
        val document: ImageDocument = multiPageDocument.documents[position]
        val documentError: GiniCaptureDocumentError? =
            multiPageDocument.getErrorForDocument(document)
        var errorMessage: String? = null
        var errorButtonAction: ErrorButtonAction? = null
        if (documentError != null) {
            errorMessage = documentError.message
            errorButtonAction = listener.getErrorButtonAction(documentError)
        }
        val instance = PreviewFragment.createInstance(document, errorMessage, errorButtonAction)

        mFragments.add(instance)

        return instance
    }

    override fun onBindViewHolder(
        holder: FragmentViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)

        holder.itemView.setOnClickListener {
            previewFragmentListener.onPageClicked(position)
        }
    }

    fun getCurrentFragment(position: Int): Fragment? {
        if (mFragments.isNotEmpty() && position <= mFragments.size)
            return mFragments[position]

        return null
    }

}