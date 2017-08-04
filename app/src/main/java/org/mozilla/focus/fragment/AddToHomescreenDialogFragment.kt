/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView

import org.mozilla.focus.R
import org.mozilla.focus.shortcut.HomeScreen
import org.mozilla.focus.shortcut.IconGenerator
import org.mozilla.focus.telemetry.TelemetryWrapper

/**
 * Fragment displaying a dialog where a user can change the title for a homescreen shortcut
 */
class AddToHomescreenDialogFragment : DialogFragment() {

    override fun onCreateDialog(bundle: Bundle?): AlertDialog {
        val url = arguments.getString(URL)
        val title = arguments.getString(TITLE)

        val builder = AlertDialog.Builder(activity, R.style.DialogStyle)
        builder.setCancelable(true)
        builder.setTitle(activity.getString(R.string.menu_add_to_home_screen))

        val inflater = activity.layoutInflater
        val dialogView = inflater.inflate(R.layout.add_to_homescreen, null)
        builder.setView(dialogView)

        val icon = IconGenerator.generateLauncherIcon(activity, url)
        val d = BitmapDrawable(resources, icon)
        val iconView = dialogView.findViewById(R.id.homescreen_icon) as ImageView
        iconView.setImageDrawable(d)

        val editableTitle = dialogView.findViewById(R.id.edit_title) as EditText

        if (!TextUtils.isEmpty(title)) {
            editableTitle.setText(title)
            editableTitle.setSelection(title!!.length)
        }

        builder.setPositiveButton(context.getString(R.string.dialog_addtohomescreen_action_add)) { dialog, id ->
            HomeScreen.installShortCut(activity, icon, url, editableTitle.text.toString().trim { it <= ' ' })
            TelemetryWrapper.addToHomescreenShortcutEvent()
            dialog.dismiss()
        }

        builder.setNegativeButton(context.getString(R.string.dialog_addtohomescreen_action_cancel)) { dialog, id ->
            TelemetryWrapper.cancelAddToHomescreenShortcutEvent()
            dialog.dismiss()
        }

        return builder.create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val dialog = dialog
        if (dialog != null) {
            val window = dialog.window
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    companion object {
        val FRAGMENT_TAG = "add-to-homescreen-prompt-dialog"
        private val URL = "url"
        private val TITLE = "title"

        fun newInstance(url: String, title: String): AddToHomescreenDialogFragment {
            val frag = AddToHomescreenDialogFragment()
            val args = Bundle()
            args.putString(URL, url)
            args.putString(TITLE, title)
            frag.arguments = args
            return frag
        }
    }
}
