/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView

import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.SupportUtils

/**
 * Ideally we'd extend SwitchPreference, and only do the summary modification. Unfortunately
 * that results in us using an older Switch which animates differently to the (seemingly AppCompat)
 * switches used in the remaining preferences. There's no AppCompat SwitchPreference to extend,
 * so instead we just build our own preference.
 */
internal class TelemetrySwitchPreference : Preference {
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    private fun init() {
        widgetLayoutResource = R.layout.preference_telemetry

        // We are keeping track of the preference value ourselves.
        isPersistent = false
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        val switchWidget = view.findViewById(R.id.switch_widget) as Switch

        switchWidget.isChecked = TelemetryWrapper.isTelemetryEnabled(context)

        switchWidget.setOnCheckedChangeListener { buttonView, isChecked -> TelemetryWrapper.setTelemetryEnabled(context, isChecked) }

        // The docs don't actually specify that R.id.summary will exist, but we rely on Android
        // using it in e.g. Fennec's AlignRightLinkPreference, so it should be safe to use it (especially
        // since we support a narrower set of Android versions in Focus).
        val summary = view.findViewById(android.R.id.summary) as TextView

        summary.paintFlags = summary.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        summary.setTextColor(Color.WHITE)

        summary.setOnClickListener {
            // This is a hardcoded link: if we ever end up needing more of these links, we should
            // move the link into an xml parameter, but there's no advantage to making it configurable now.
            val url = SupportUtils.getSumoURLForTopic(context, "usage-data")
            val title = title.toString()

            val intent = InfoActivity.getIntentFor(context, url, title)
            context.startActivity(intent)
        }

        val backgroundDrawableArray = view.context.obtainStyledAttributes(intArrayOf(R.attr.selectableItemBackground))
        val backgroundDrawable = backgroundDrawableArray.getDrawable(0)
        backgroundDrawableArray.recycle()
        summary.background = backgroundDrawable

        // We still want to allow toggling the pref by touching any part of the pref (except for
        // the "learn more" link)
        onPreferenceClickListener = OnPreferenceClickListener {
            switchWidget.toggle()
            true
        }
    }
}
