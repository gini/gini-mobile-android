package net.gini.android.health.api.models

import android.net.Uri
import android.util.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.health.api.response.PageResponse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Alpár Szotyori on 27.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class PageResponseTest {

    @Test
    fun `finds largest image uri which is smaller than max size`() {
        // Given
        val page = Page(
            number = 1, images = mapOf(
                Size(100, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/100x100"),
                Size(500, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/500x100"),
                Size(300, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/300x100"),
                Size(200, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/200x100"),
                Size(400, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/400x100"),
            )
        )

        // When
        val imageUri = page.getLargestImageUriSmallerThan(Size(350, 100))

        // Then
        assertThat(imageUri).isEqualTo(Uri.parse("https://api.gini.net/documents/1234/pages/1/300x100"))
    }

    @Test
    fun `finds largest image uri which is equal to max size`() {
        // Given
        val page = Page(
            number = 1, images = mapOf(
                Size(100, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/100x100"),
                Size(500, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/500x100"),
                Size(300, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/300x100"),
                Size(200, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/200x100"),
                Size(400, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/400x100"),
            )
        )

        // When
        val imageUri = page.getLargestImageUriSmallerThan(Size(300, 100))

        // Then
        assertThat(imageUri).isEqualTo(Uri.parse("https://api.gini.net/documents/1234/pages/1/300x100"))
    }

    @Test
    fun `returns smallest image uri if all are larger than max size`() {
        // Given
        val page = Page(
            number = 1, images = mapOf(
                Size(100, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/100x100"),
                Size(500, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/500x100"),
                Size(300, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/300x100"),
                Size(200, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/200x100"),
                Size(400, 100) to Uri.parse("https://api.gini.net/documents/1234/pages/1/400x100"),
            )
        )

        // When
        val imageUri = page.getLargestImageUriSmallerThan(Size(50, 50))

        // Then
        assertThat(imageUri).isEqualTo(Uri.parse("https://api.gini.net/documents/1234/pages/1/100x100"))
    }

    @Test
    fun `getting largest image uri returns null if images map is empty`() {
        // Given
        val page = Page(
            number = 1, images = emptyMap()
        )

        // When
        val imageUri = page.getLargestImageUriSmallerThan(Size(100, 100))

        // Then
        assertThat(imageUri).isNull()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getting page by page number requires one-based indexing`() {
        // Given
        val pages = listOf(
            Page(number = 1, images = emptyMap()),
            Page(number = 2, images = emptyMap()),
            Page(number = 3, images = emptyMap()),
        )

        // When
        pages.getPageByPageNumber(0)

        // Then
        // throws IllegalArgumentException exception
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getting page by page number checks that page number is in bounds`() {
        // Given
        val pages = listOf(
            Page(number = 1, images = emptyMap()),
            Page(number = 2, images = emptyMap()),
            Page(number = 3, images = emptyMap()),
        )

        // When
        pages.getPageByPageNumber(4)

        // Then
        // throws IllegalArgumentException exception
    }

    fun `gets page by page number`() {
        // Given
        val pages = listOf(
            Page(number = 1, images = emptyMap()),
            Page(number = 2, images = emptyMap()),
            Page(number = 3, images = emptyMap()),
        )

        // When
        val page = pages.getPageByPageNumber(2)

        // Then
        assertThat(page).isEqualTo(pages[1])
    }

    @Test
    fun `maps PagesResponse to Pages`() {
        // Given
        val pagesResponse = listOf(
            PageResponse(
                pageNumber = 1,
                images = mapOf(
                    "100x100" to "/document/1234/pages/1/100x100",
                    "200x100" to "/document/1234/pages/1/200x100"
                )
            ),
            PageResponse(
                pageNumber = 2,
                images = mapOf(
                    "300x100" to "/document/1234/pages/2/300x100"
                )
            )
        )

        // When
        val pages = pagesResponse.toPageList(baseUri = Uri.parse("https://api.gini.net"))

        // Then
        assertThat(pages).isEqualTo(
            listOf(
                Page(
                    number = 1, images = mapOf(
                        Size(100, 100) to Uri.parse("https://api.gini.net/document/1234/pages/1/100x100"),
                        Size(200, 100) to Uri.parse("https://api.gini.net/document/1234/pages/1/200x100")
                    )
                ),
                Page(
                    number = 2, images = mapOf(
                        Size(300, 100) to Uri.parse("https://api.gini.net/document/1234/pages/2/300x100")
                    )
                )
            )
        )
    }
}