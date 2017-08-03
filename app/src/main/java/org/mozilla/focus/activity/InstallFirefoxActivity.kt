/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView

import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.Browsers

/**
 * Helper activity that will open the Google Play store by following a redirect URL.
 */
class InstallFirefoxActivity : Activity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        setContentView(webView)

        webView.loadUrl(REDIRECT_URL)
    }

    override fun onPause() {
        super.onPause()

        webView.onPause()

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }

    companion object {
        private val REDIRECT_URL = "https://app.adjust.com/gs1ao4"

        @JvmStatic fun open(context: Context) {
            if (AppConstants.isKlarBuild()) {
                // Redirect to Google Play directly
                val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + Browsers.KnownBrowser.FIREFOX.packageName))
                context.startActivity(intent)
            } else {
                // Start this activity to load the redirect URL in a WebView.
                val intent = Intent(context, InstallFirefoxActivity::class.java)
                context.startActivity(intent)
            }
        }
    }
}
