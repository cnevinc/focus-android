/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.os.Bundle
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.view.View

import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.settings.SettingsFragment

class SettingsActivity : LocaleAwareAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener { finish() }

        fragmentManager.beginTransaction()
                .replace(R.id.container, SettingsFragment())
                .commit()

        // Ensure all locale specific Strings are initialised on first run, we don't set the title
        // anywhere before now (the title can only be set via AndroidManifest, and ensuring
        // that that loads the correct locale string is tricky).
        applyLocale()
    }

    override fun applyLocale() {
        setTitle(R.string.menu_settings)
    }

    companion object {
        val ACTIVITY_RESULT_LOCALE_CHANGED = 1
    }
}
