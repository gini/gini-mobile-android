package com.ortiz.touch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers when [TouchImageView] reports that it can still scroll horizontally. A parent pager
 * relies on this to decide whether a swipe pans the zoomed image or changes the page.
 */
@RunWith(AndroidJUnit4::class)
class TouchImageViewTest {

    @Test
    fun `cannot scroll an image that is not zoomed`() {
        val view = newLaidOutView()

        assertThat(view.canScrollHorizontally(SCROLL_LEFT)).isFalse()
        assertThat(view.canScrollHorizontally(SCROLL_RIGHT)).isFalse()
    }

    @Test
    fun `can scroll a zoomed image in both directions from its center`() {
        val view = newLaidOutView()

        view.setZoom(ZOOM)

        assertThat(view.canScrollHorizontally(SCROLL_LEFT)).isTrue()
        assertThat(view.canScrollHorizontally(SCROLL_RIGHT)).isTrue()
    }

    @Test
    fun `can only scroll right when the zoomed image is at its left edge`() {
        val view = newLaidOutView()

        view.setZoom(ZOOM, 0f, VERTICAL_CENTER)

        assertThat(view.canScrollHorizontally(SCROLL_LEFT)).isFalse()
        assertThat(view.canScrollHorizontally(SCROLL_RIGHT)).isTrue()
    }

    @Test
    fun `can only scroll left when the zoomed image is at its right edge`() {
        val view = newLaidOutView()

        view.setZoom(ZOOM, 1f, VERTICAL_CENTER)

        assertThat(view.canScrollHorizontally(SCROLL_LEFT)).isTrue()
        assertThat(view.canScrollHorizontally(SCROLL_RIGHT)).isFalse()
    }

    @Test
    fun `cannot scroll an image that is narrower than the view`() {
        val view = newLaidOutView(scaleType = ImageView.ScaleType.CENTER)

        assertThat(view.canScrollHorizontally(SCROLL_LEFT)).isFalse()
        assertThat(view.canScrollHorizontally(SCROLL_RIGHT)).isFalse()
        assertThat(view.canScrollHorizontallyFroyo(SCROLL_RIGHT)).isFalse()
    }

    private fun newLaidOutView(scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER): TouchImageView {
        val view = TouchImageView(ApplicationProvider.getApplicationContext())
        view.scaleType = scaleType
        view.setImageBitmap(Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888))
        val exactly = View.MeasureSpec.makeMeasureSpec(VIEW_SIZE, View.MeasureSpec.EXACTLY)
        view.measure(exactly, exactly)
        view.layout(0, 0, VIEW_SIZE, VIEW_SIZE)
        // Zooming is postponed until the view was drawn once
        view.draw(Canvas(Bitmap.createBitmap(VIEW_SIZE, VIEW_SIZE, Bitmap.Config.ARGB_8888)))
        return view
    }

    private companion object {
        const val VIEW_SIZE = 200
        const val BITMAP_SIZE = 100
        const val ZOOM = 2f
        const val VERTICAL_CENTER = 0.5f
        const val SCROLL_LEFT = -1
        const val SCROLL_RIGHT = 1
    }
}
