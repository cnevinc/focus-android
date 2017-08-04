/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.preference.Preference
import android.provider.Browser
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.widget.Switch

import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.SupportUtils

@TargetApi(Build.VERSION_CODES.N)
class DefaultBrowserPreference : Preference {
    private var switchView: Switch? = null

    // Instantiated from XML
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        widgetLayoutResource = R.layout.preference_default_browser
    }

    // Instantiated from XML
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        widgetLayoutResource = R.layout.preference_default_browser
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        switchView = view.findViewById(R.id.switch_widget) as Switch
        update()
    }

    fun update() {
        if (switchView != null) {
            val browsers = Browsers(context, Browsers.TRADITIONAL_BROWSER_URL)
            switchView!!.isChecked = browsers.isDefaultBrowser(context)
        }
    }

    override fun onClick() {
        val context = context

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            openDefaultAppsSettings(context)
        } else {
            openSumoPage(context)
        }
    }

    private fun openDefaultAppsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // In some cases, a matching Activity may not exist (according to the Android docs).
            openSumoPage(context)
        }

    }

    private fun openSumoPage(context: Context) {
        val intent = InfoActivity.getIntentFor(context, SupportUtils.DEFAULT_BROWSER_URL, title.toString())
        context.startActivity(intent)
    }
}
