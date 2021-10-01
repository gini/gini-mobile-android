package net.gini.pay.app.pager

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.gini.pay.app.databinding.ItemPageBinding

class PagerAdapter : ListAdapter<PagerAdapter.Page, PagerAdapter.PageViewHolder>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        return PageViewHolder(ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.onBind(currentList[position])
    }

    class PageViewHolder(private val binding: ItemPageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(page: Page) {
            binding.image.setImageURI(page.uri)
        }
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<Page>() {
        override fun areItemsTheSame(oldItem: Page, newItem: Page) = oldItem.number == newItem.number

        override fun areContentsTheSame(oldItem: Page, newItem: Page) = oldItem.uri == newItem.uri
    }

    data class Page(val number: Int, val uri: Uri)
}