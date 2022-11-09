package net.gini.android.capture.review.multipage.previews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.document.GiniCaptureDocumentError
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.internal.camera.photo.Photo
import net.gini.android.capture.review.RotatableImageViewContainer

class PreviewPagesAdapter(
    private val multiPageDocument: ImageMultiPageDocument,
    private val listener: PreviewsAdapterListener,
    private val previewFragmentListener: PreviewFragmentListener
) : RecyclerView.Adapter<PreviewPagesAdapter.PagesViewHolder>() {


    inner class PagesViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        val mImageViewContainer: RotatableImageViewContainer? = view.findViewById(R.id.gc_image_container)
        private val mImageBlueRect: LinearLayout? = view.findViewById(R.id.gc_image_selected_rect)
        private val mDeletePage: ImageButton? = view.findViewById(R.id.gc_button_delete)
        private val mErrorMessage: String? = null
        val mActivityIndicator: ProgressBar = view.findViewById(R.id.gc_activity_indicator)

        init {
            mDeletePage?.setOnClickListener {
                previewFragmentListener.onDeleteDocument(multiPageDocument.documents[absoluteAdapterPosition])
            }

            mImageViewContainer?.setOnClickListener {

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
            showActivityIndicator(holder.mActivityIndicator)
            if (GiniCapture.hasInstance()) {
                GiniCapture.getInstance()
                    .internal().photoMemoryCache[holder.view.context, mDocument, object :
                    AsyncCallback<Photo?, Exception?> {

                    override fun onCancelled() {
                        // Not used
                    }

                    override fun onSuccess(result: Photo?) {
                        hideActivityIndicator(holder.mActivityIndicator)
                        holder.mImageViewContainer?.imageView?.setImageBitmap(result?.bitmapPreview)
                        holder.mImageViewContainer?.rotateImageView(result?.rotationForDisplay ?: 0, false);
                    }

                    override fun onError(exception: Exception?) {
                        hideActivityIndicator(holder.mActivityIndicator)
                        //showPreviewError(context)
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

    private fun showActivityIndicator(mActivityIndicator: ProgressBar) {
        mActivityIndicator.visibility = View.VISIBLE
    }

    private fun hideActivityIndicator(mActivityIndicator: ProgressBar) {
        mActivityIndicator.visibility = View.GONE
    }

    override fun getItemCount(): Int {
        return multiPageDocument.documents.size
    }
}