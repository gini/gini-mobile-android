package net.gini.android.capture.internal.util

import android.graphics.Point
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BundleHelperTest {

    @Test
    fun `map survives the round trip through a bundle`() {
        val map = mapOf("first" to Point(1, 2), "second" to Point(3, 4))

        val restored = toMap(fromMap(map), Point::class.java)

        assertThat(restored).isEqualTo(map)
    }

    @Test
    fun `map is restored with the given class loader`() {
        val map = mapOf("first" to Point(1, 2))
        val bundle = fromMap(map)

        val restored = toMap(bundle, Point::class.java, Point::class.java.classLoader)

        assertThat(restored).isEqualTo(map)
        assertThat(bundle.classLoader).isEqualTo(Point::class.java.classLoader)
    }

    @Test
    fun `entries of another type are left out`() {
        val bundle = Bundle().apply {
            putParcelable("point", Point(1, 2))
            putString("text", "not a parcelable")
        }

        val restored = toMap(bundle, Point::class.java)

        assertThat(restored).containsExactly("point", Point(1, 2))
    }

    @Test
    fun `list of maps survives the round trip through bundles`() {
        val mapList = listOf(mapOf("first" to Point(1, 2)), mapOf("second" to Point(3, 4)))

        val restored = toMapList(fromMapList(mapList), Point::class.java)

        assertThat(restored).isEqualTo(mapList)
    }

    @Test
    fun `list of maps is restored with the given class loader`() {
        val mapList = listOf(mapOf("first" to Point(1, 2)))

        val restored = toMapList(fromMapList(mapList), Point::class.java, Point::class.java.classLoader)

        assertThat(restored).isEqualTo(mapList)
    }
}
