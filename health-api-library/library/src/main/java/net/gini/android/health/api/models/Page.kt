package net.gini.android.health.api.models

import android.net.Uri
import android.util.Size
import net.gini.android.health.api.response.PageResponse
import net.gini.android.health.api.util.toTreeMap

/**
 * Created by Alpár Szotyori on 27.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

internal data class Page(
    val number: Int,
    val images: Map<Size, Uri>
) {

    fun getLargestImageUriSmallerThan(size: Size): Uri? {
        return images.toTreeMap { size1, size2 -> (size1.width * size1.height) - (size2.width * size2.height) }
            .let { treeMap ->
                treeMap.floorEntry(size)?.value ?: treeMap.firstEntry()?.value
            }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Page

        if (number != other.number) return false
        if (images != other.images) return false

        return true
    }

    override fun hashCode(): Int {
        var result = number
        result = 31 * result + images.hashCode()
        return result
    }

}

@Throws(IllegalArgumentException::class)
internal fun List<Page>.getPageByPageNumber(oneBasedPageNumber: Int): Page {
    require(oneBasedPageNumber > 0) { "Illegal page number: $oneBasedPageNumber. Page numbers are one-based and must not be zero or negative." }
    require(oneBasedPageNumber <= this.size) { "Illegal page number: $oneBasedPageNumber. Larger than page count ${this.size}." }
    return this.first { it.number == oneBasedPageNumber }
}

internal fun List<PageResponse>.toPageList(baseUri: Uri): List<Page> = map { pageResponse ->
    Page(
        number = pageResponse.pageNumber,
        images = pageResponse.images.map { (size, path) ->
            (Size.parseSize(size) to baseUri.buildUpon().appendEncodedPath(path.trimStart('/')).build())
        }.toMap()
    )
}