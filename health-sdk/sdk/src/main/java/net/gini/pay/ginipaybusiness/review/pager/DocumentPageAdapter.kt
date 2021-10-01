package net.gini.pay.ginipaybusiness.review.pager

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
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
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import net.gini.pay.ginipaybusiness.GiniBusiness
import net.gini.pay.ginipaybusiness.databinding.GpbItemPageHorizontalBinding
import net.gini.pay.ginipaybusiness.review.model.ResultWrapper
import net.gini.pay.ginipaybusiness.review.model.wrapToResult

internal class DocumentPageAdapter(private val giniBusiness: GiniBusiness) :
    ListAdapter<DocumentPageAdapter.Page, DocumentPageAdapter.PageViewHolder>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder =
        HorizontalViewHolder(giniBusiness, GpbItemPageHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.onBind(currentList[position])
    }

    abstract class PageViewHolder(private val giniBusiness: GiniBusiness, view: View) : RecyclerView.ViewHolder(view) {
        private val imageLoadingScope = CoroutineScope(Dispatchers.Main)

        protected abstract val loadingView: ProgressBar
        protected abstract val imageView: ImageView
        protected abstract val errorView: FrameLayout
        protected abstract val retry: Button

        fun onBind(page: Page) {
            imageLoadingScope.launch {
                loadingView.isVisible = true
                when (val imageResult = wrapToResult { giniBusiness.giniApi.documentManager.getPageImage(page.documentId, page.number) }) {
                    is ResultWrapper.Error -> {
                        loadingView.isVisible = false
                        errorView.isVisible = true
                        retry.setOnClickListener {
                            errorView.isVisible = false
                            onBind(page)
                        }
                    }
                    is ResultWrapper.Success -> {
                        loadingView.isVisible = false
                        imageView.isVisible = true
                        imageView.setImageBitmap(BitmapFactory.decodeByteArray(imageResult.value, 0, imageResult.value.size))
                    }
                }
            }
        }

        fun cancel() {
            imageLoadingScope.coroutineContext.cancelChildren()
        }
    }

    class HorizontalViewHolder(
        giniBusiness: GiniBusiness,
        private val binding: GpbItemPageHorizontalBinding,
        override val loadingView: ProgressBar = binding.loading,
        override val imageView: ImageView = binding.image,
        override val errorView: FrameLayout = binding.error.root,
        override val retry: Button = binding.error.pageErrorRetry,
    ) : PageViewHolder(giniBusiness, binding.root) {
        init {
            imageView.applyInsetter {
                type(statusBars = true) {
                    padding(top = true)
                }
                type(ime = true) {
                    padding(bottom = true)
                }
            }
            binding.image.setOnScaleChangeListener { _, _, _ ->
                binding.root.parent.requestDisallowInterceptTouchEvent(true)
            }
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
                with(binding.image) {
                    post {
                        scale = scale.coerceIn(minimumScale, maximumScale)
                    }
                }
                insets
            }
        }
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        holder.cancel()
        super.onViewRecycled(holder)
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<Page>() {
        override fun areItemsTheSame(oldItem: Page, newItem: Page) = oldItem.number == newItem.number

        override fun areContentsTheSame(oldItem: Page, newItem: Page) = oldItem.documentId == newItem.documentId
    }

    data class Page(val documentId: String, val number: Int)
}