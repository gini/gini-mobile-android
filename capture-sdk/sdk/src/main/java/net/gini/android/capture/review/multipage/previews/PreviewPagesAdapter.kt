package net.gini.android.capture.review.multipage.previews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.internal.camera.photo.Photo
import net.gini.android.capture.internal.ui.setIntervalClickListener
import net.gini.android.capture.review.RotatableImageViewContainer
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTrackerBuilder.getAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen

class PreviewPagesAdapter(
    private val multiPageDocument: ImageMultiPageDocument,
    private val previewFragmentListener: PreviewFragmentListener
) : RecyclerView.Adapter<PreviewPagesAdapter.PagesViewHolder>() {


    inner class PagesViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        val mImageViewContainer: RotatableImageViewContainer? =
            view.findViewById(R.id.gc_image_container)
        private val mDeletePage: ImageButton? = view.findViewById(R.id.gc_button_delete)

        init {

            mDeletePage?.setIntervalClickListener {
                getAnalyticsEventTracker().trackEvent(
                    UserAnalyticsEvent.DELETE_PAGES_TAPPED,
                    UserAnalyticsScreen.REVIEW
                )
                previewFragmentListener.onDeleteDocument(multiPageDocument.documents[absoluteAdapterPosition])
            }

            mImageViewContainer?.setIntervalClickListener {
                previewFragmentListener.onPageClicked(multiPageDocument.documents[absoluteAdapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gc_item_multi_page_preview, parent, false)
        return PagesViewHolder(view)
    }

    override fun onBindViewHolder(holder: PagesViewHolder, position: Int) {

        val mDocument = multiPageDocument.documents[position]

        if (shouldShowPreviewImage(mDocument, holder.mImageViewContainer?.imageView)) {
            if (GiniCapture.hasInstance()) {
                GiniCapture.getInstance()
                    .internal().photoMemoryCache[holder.view.context, mDocument, object :
                    AsyncCallback<Photo?, Exception?> {

                    override fun onCancelled() {
                        // Not used
                    }

                    override fun onSuccess(result: Photo?) {
                        holder.mImageViewContainer?.imageView?.setImageBitmap(result?.bitmapPreview)
                        holder.mImageViewContainer?.rotateImageView(
                            result?.rotationForDisplay ?: 0,
                            false
                        )
                    }

                    override fun onError(exception: Exception?) {
                    }
                }]
            }
        }
    }

    private fun shouldShowPreviewImage(
        mDocument: ImageDocument?,
        mImageViewContainer: ImageView?
    ): Boolean {
        return (mDocument != null
                && mImageViewContainer?.drawable == null)
    }


    override fun getItemCount(): Int {
        return multiPageDocument.documents.size
    }

    companion object {
        @JvmStatic
        fun getNewPositionAfterDeletion(deletedPosition: Int, newSize: Int): Int {
            val newPosition = if (deletedPosition == newSize) {
                // Last item was removed, highlight the new last item
                Math.max(0, deletedPosition - 1)
            } else {
                // Non-last item deletion moves the right neighbour to the same position
                deletedPosition
            }
            return newPosition
        }
    }
}