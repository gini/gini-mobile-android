package net.gini.android.bank.api.test

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
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

        if (other is ExtractionsContainer) {
            return other.specificExtractions.keys.containsAll(specificExtractions.keys) &&
                    specificExtractions.all { fixtureEntry ->
                        return@all other.specificExtractions.any { entry ->
                            return@any fixtureEntry.key == entry.key &&
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