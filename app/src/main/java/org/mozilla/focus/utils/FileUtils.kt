/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.content.Context

import java.io.File

object FileUtils {
    private val WEBVIEW_DIRECTORY = "app_webview"

    fun File.truncate() : Boolean = listFiles()?.let {
        it.isEmpty() || it
                .asSequence()
                .map {
                    if (it.isDirectory) {
                        it.truncate() && it.delete()
                    } else {
                        it.delete()
                    }
                }
                .reduce { success, value -> success && value }
    } == true

    @JvmStatic
    fun truncateCacheDirectory(context: Context): Boolean {
        with (context.cacheDir) {
            return exists() && truncate()
        }
    }

    @JvmStatic
    fun deleteWebViewDirectory(context: Context): Boolean {
        with (File(context.applicationInfo.dataDir, WEBVIEW_DIRECTORY)) {
            return exists() && truncate() && delete()
        }
    }
}
