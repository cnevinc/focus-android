/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

object IOUtils {
    @JvmStatic
    @Throws(IOException::class, JSONException::class)
    fun readAsset(context: Context, fileName: String): JSONObject {
        context.assets.open(fileName).buffered().reader().use {
            return JSONObject(it.readText())
        }
    }
}
