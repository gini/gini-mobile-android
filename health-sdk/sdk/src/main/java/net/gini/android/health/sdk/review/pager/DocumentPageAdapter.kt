package net.gini.android.health.sdk.review.pager

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import dev.chrisbanes.insetter.applyInsetter
import net.gini.android.core.api.Resource
import android.view.View
import net.gini.android.health.sdk.databinding.GhsItemPageHorizontalBinding
import net.gini.android.health.sdk.util.hideKeyboard

internal class DocumentPageAdapter(
    private val onRetryPage: (Int) -> Unit) :
    ListAdapter<DocumentPageAdapter.Page, DocumentPageAdapter.PageViewHolder>(DiffUtilCallback) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder =
        HorizontalViewHolder(
            GhsItemPageHorizontalBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), onRetryPage = onRetryPage
        )

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.onBind(currentList[position])

        holder.itemView.isFocusable = false

    }


    abstract class PageViewHolder(view: View, private val onRetryPage: (Int) -> Unit) :
        RecyclerView.ViewHolder(view) {
        protected abstract val loadingView: ProgressBar
        abstract val imageView: ImageView
        protected abstract val errorView: FrameLayout
        protected abstract val retry: Button

        fun onBind(page: Page) {
            when (val imageResult = page.pageImage) {
                is Resource.Error -> {
                    loadingView.isVisible = false
                    errorView.isVisible = true
                    retry.setOnClickListener {
                        errorView.isVisible = false
                        loadingView.isVisible = true
                        onRetryPage(page.number)
                    }
                }
                is Resource.Success -> {
                    loadingView.isVisible = false
                    imageView.isVisible = true

                    imageView.setImageBitmap(
                        BitmapFactory.decodeByteArray(
                            imageResult.data,
                            0,
                            imageResult.data.size
                        )
                    )
                    imageView.apply {
                        setImageBitmap(
                            BitmapFactory.decodeByteArray(
                                imageResult.data,
                                0,
                                imageResult.data.size
                            )
                        )
                    }
            }
            else -> {
                // Do nothing
            }
        }
    }
}


class HorizontalViewHolder(
    val binding: GhsItemPageHorizontalBinding,
    onRetryPage: (Int) -> Unit,
    override val loadingView: ProgressBar = binding.loading,
    override val imageView: PhotoView = binding.image,
    override val errorView: FrameLayout = binding.error.root,
    override val retry: Button = binding.error.pageErrorRetry,
) : PageViewHolder(binding.root, onRetryPage) {

    private val photoViewMatrix = Matrix()

    init {
        imageView.applyInsetter {
            type(statusBars = true) {
                padding(top = true)
            }
            type(ime = true) {
                padding(bottom = true)
            }
        }

        imageView.setOnViewTapListener { view, _, _ ->
            view.hideKeyboard()
        }

        imageView.setOnScaleChangeListener { _, _, _ ->
            binding.root.parent.requestDisallowInterceptTouchEvent(true)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            with(imageView) {
                post {
                    // Calling setSuppMatrix() translates the image to the edge of the visible area
                    // if the new visible area size results in a gap between the edges of the image and
                    // the visible area. Scale is always preserved.
                    getSuppMatrix(photoViewMatrix)
                    setSuppMatrix(photoViewMatrix)
                }
            }
            insets
        }
    }
}

object DiffUtilCallback : DiffUtil.ItemCallback<Page>() {
    override fun areItemsTheSame(oldItem: Page, newItem: Page) = oldItem.number == newItem.number

    override fun areContentsTheSame(oldItem: Page, newItem: Page) =
        oldItem.pageImage == newItem.pageImage
}

data class Page(val pageImage: Resource<ByteArray>, val number: Int)
}