package net.gini.android.capture.internal.camera.photo

import org.junit.Assert

/**
 * Created by Alpár Szotyori on 17.07.23.
 *
 * Copyright (c) 2023 Gini GmbH.
 */

class ExifUserCommentHelper {

    companion object {

        fun getValueForKeyFromUserComment(key: String, userComment: String): String {
            val findValueForKeyRegex = "$key=(.*?)(,|$)".toRegex()
            return findValueForKeyRegex.find(userComment)?.groups?.get(1)?.value ?: run {
                Assert.fail("'$key' not found in jpeg exif user comment")
                ""
            }
        }

    }

}