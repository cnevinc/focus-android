/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi

import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.UrlUtils

/**
 * Activity for receiving and processing an ACTION_PROCESS_TEXT intent.
 */
class TextActionActivity : Activity() {
    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = SafeIntent(intent)

        val searchTextCharSequence = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        val searchText: String
        if (searchTextCharSequence != null) {
            searchText = searchTextCharSequence.toString()
        } else {
            searchText = ""
        }
        val searchUrl = UrlUtils.createSearchUrl(this, searchText)

        val searchIntent = Intent(this, MainActivity::class.java)

        with(searchIntent){
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.EXTRA_TEXT_SELECTION, true)
            data = Uri.parse(searchUrl)
        }
        startActivity(searchIntent)


        finish()
    }
}
