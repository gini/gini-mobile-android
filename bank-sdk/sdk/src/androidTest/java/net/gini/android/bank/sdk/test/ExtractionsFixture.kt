package net.gini.android.bank.sdk.test

import android.os.Bundle
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.core.api.models.ExtractionsContainer

/**
 * Created by Alpár Szotyori on 16.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

@JsonClass(generateAdapter = true)
class ExtractionsFixture(
    @Json(name = "extractions")
    val specificExtractions: Map<String, SpecificExtractionFixture>
) {

    override fun equals(other: Any?): Boolean {
        if (other is ExtractionsFixture) {
            return super.equals(other)
        }

        if (other is Map<*, *>) {
            return other.keys.containsAll(specificExtractions.keys) &&
                    specificExtractions.all { fixtureEntry ->
                        other.keys.any { key ->
                            val extraction: GiniCaptureSpecificExtraction? = other[key] as? GiniCaptureSpecificExtraction
                            fixtureEntry.key == key &&
                                    fixtureEntry.value.entity == extraction?.entity &&
                                    fixtureEntry.value.value == extraction.value
                        }
                    }
        }

        if (other is ExtractionsContainer) {
            return other.specificExtractions.keys.containsAll(specificExtractions.keys) &&
                    specificExtractions.all { fixtureEntry ->
                        other.specificExtractions.any { entry ->
                            fixtureEntry.key == entry.key &&
                                    fixtureEntry.value.entity == entry.value.entity &&
                                    fixtureEntry.value.value == entry.value.value &&
                                    fixtureEntry.value.box?.top == entry.value.box?.top &&
                                    fixtureEntry.value.box?.left == entry.value.box?.left &&
                                    fixtureEntry.value.box?.width == entry.value.box?.width &&
                                    fixtureEntry.value.box?.height == entry.value.box?.height &&
                                    fixtureEntry.value.box?.page == entry.value.box?.pageNumber
                        }
                    }
        }

        return false
    }

    override fun hashCode(): Int {
        return specificExtractions.hashCode()
    }
}

@JsonClass(generateAdapter = true)
class SpecificExtractionFixture(
    val entity: String,
    val value: String,
    val box: BoxFixture?
)

@JsonClass(generateAdapter = true)
class BoxFixture(
    val top: Double,
    val left: Double,
    val width: Double,
    val height: Double,
    val page: Int
)