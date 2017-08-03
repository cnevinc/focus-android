/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager

import org.mozilla.focus.R
import org.mozilla.focus.fragment.BrowserFragment
import org.mozilla.focus.fragment.FirstrunFragment
import org.mozilla.focus.fragment.UrlInputFragment
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.notification.BrowsingNotificationService
import org.mozilla.focus.shortcut.HomeScreen
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.UrlUtils
import org.mozilla.focus.web.BrowsingSession
import org.mozilla.focus.web.IWebView
import org.mozilla.focus.web.WebViewProvider

class MainActivity : LocaleAwareAppCompatActivity() {

    private var pendingUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Settings.getInstance(this).shouldUseSecureMode()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_main)

        var intent = SafeIntent(intent)

        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0 && !BrowsingSession.getInstance().isActive) {
            // This Intent was launched from history (recent apps). Android will redeliver the
            // original Intent (which might be a VIEW intent). However if there's no active browsing
            // session then we do not want to re-process the Intent and potentially re-open a website
            // from a session that the user already "erased".
            intent = SafeIntent(Intent(Intent.ACTION_MAIN))
            setIntent(intent.unsafe)
        }

        if (savedInstanceState == null) {
            WebViewProvider.performCleanup(this)

            if (Intent.ACTION_VIEW == intent.action) {
                val url = intent.dataString

                BrowsingSession.getInstance().loadCustomTabConfig(this, intent)

                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    pendingUrl = url
                    showFirstrun()
                } else {
                    showBrowserScreen(url)
                }
            } else if (Intent.ACTION_SEND == intent.action) {
                setPendingUrlFromShareIntent(intent)
            } else {
                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    showFirstrun()
                } else {
                    showHomeScreen()
                }
            }
        }

        WebViewProvider.preload(this)
    }

    override fun applyLocale() {
        // We don't care here: all our fragments update themselves as appropriate
    }

    override fun onStart() {
        super.onStart()

        BrowsingNotificationService.foreground(this)
    }

    override fun onResume() {
        super.onResume()

        TelemetryWrapper.startSession()

        if (Settings.getInstance(this).shouldUseSecureMode()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onPause() {
        if (isFinishing) {
            WebViewProvider.performCleanup(this)
        }

        super.onPause()

        TelemetryWrapper.stopSession()
    }

    override fun onStop() {
        super.onStop()

        BrowsingNotificationService.background(this)

        TelemetryWrapper.stopMainActivity()
    }

    override fun onNewIntent(unsafeIntent: Intent) {
        val intent = SafeIntent(unsafeIntent)
        if (Intent.ACTION_VIEW == intent.action) {
            // We can't update our fragment right now because we need to wait until the activity is
            // resumed. So just remember this URL and load it in onResumeFragments().
            pendingUrl = intent.dataString
        }

        if (Intent.ACTION_SEND == intent.action) {
            setPendingUrlFromShareIntent(intent)
        }

        if (ACTION_OPEN == intent.action) {
            TelemetryWrapper.openNotificationActionEvent()
        }

        // We do not care about the previous intent anymore. But let's remember this one.
        setIntent(unsafeIntent)
        BrowsingSession.getInstance().loadCustomTabConfig(this, intent)
    }

    override fun onResumeFragments() {
        super.onResumeFragments()

        val intent = SafeIntent(intent)

        if (ACTION_ERASE == intent.action) {
            processEraseAction(intent)

            // We do not want to erase again the next time we resume the app.
            setIntent(Intent(Intent.ACTION_MAIN))
        }

        if (pendingUrl != null && !Settings.getInstance(this).shouldShowFirstrun()) {
            // We have received an URL in onNewIntent(). Let's load it now.
            // Unless we're trying to show the firstrun screen, in which case we leave it pending until
            // firstrun is dismissed.
            showBrowserScreen(pendingUrl)
            pendingUrl = null
        }
    }

    private fun processEraseAction(intent: SafeIntent) {
        val finishActivity = intent.getBooleanExtra(EXTRA_FINISH, false)
        val fromShortcut = intent.getBooleanExtra(EXTRA_SHORTCUT, false)
        val fromNotification = intent.getBooleanExtra(EXTRA_NOTIFICATION, false)

        val browserFragment = supportFragmentManager
                .findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?

        if (browserFragment != null) {
            // We are currently displaying a browser fragment. Let the fragment handle the erase and
            // play its animation.
            browserFragment.eraseAndShowHomeScreen(!fromNotification)
        } else {
            // There's no fragment available currently. Let's delete manually and notify the service
            // that the session should have ended (normally the fragment would do both).
            WebViewProvider.performCleanup(this)
            BrowsingNotificationService.stop(this)
        }

        // The service will track the foreground/background state of our activity. If we are erasing
        // while the activity is in the background then we want to finish it immediately again.
        if (finishActivity) {
            finishAndRemoveTask()
            overridePendingTransition(0, 0) // This activity should be visible - avoid any animations.
        }

        if (fromShortcut) {
            TelemetryWrapper.eraseShortcutEvent()
        } else if (fromNotification) {
            TelemetryWrapper.eraseAndOpenNotificationActionEvent()
        }
    }

    private fun showHomeScreen() {
        // We add the url input fragment to the layout if it doesn't exist yet. I tried adding the
        // fragment to the layout directly but then I wasn't able to remove it later. It was still
        // visible but without an activity attached. So let's do it manually.
        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG) == null) {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.container, UrlInputFragment.createWithBackground(), UrlInputFragment.FRAGMENT_TAG)
                    .commit()
        }
    }

    private fun showFirstrun() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentByTag(FirstrunFragment.FRAGMENT_TAG) == null) {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.container, FirstrunFragment.create(), FirstrunFragment.FRAGMENT_TAG)
                    .commit()
        }
    }

    private fun showBrowserScreen(url: String?) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.container,
                        BrowserFragment.create(url), BrowserFragment.FRAGMENT_TAG)
                .commit()

        val intent = SafeIntent(intent)

        if (intent.getBooleanExtra(EXTRA_TEXT_SELECTION, false)) {
            TelemetryWrapper.textSelectionIntentEvent()
        } else if (intent.hasExtra(HomeScreen.ADD_TO_HOMESCREEN_TAG)) {
            TelemetryWrapper.openHomescreenShortcutEvent()
        } else if (BrowsingSession.getInstance().isCustomTab) {
            TelemetryWrapper.customTabsIntentEvent(BrowsingSession.getInstance().customTabConfig.optionsList)
        } else {
            TelemetryWrapper.browseIntentEvent()
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        if (name == IWebView::class.java.name) {
            val v = WebViewProvider.create(this, attrs)
            return v
        }

        return super.onCreateView(name, context, attrs)
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager

        val urlInputFragment = fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG) as? UrlInputFragment?
        if (urlInputFragment != null &&
                urlInputFragment.isVisible &&
                urlInputFragment.onBackPressed()) {
            // The URL input fragment has handled the back press. It does its own animations so
            // we do not try to remove it from outside.
            return
        }

        val browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as? BrowserFragment?
        if (browserFragment != null &&
                browserFragment.isVisible &&
                browserFragment.onBackPressed()) {
            // The Browser fragment handles back presses on its own because it might just go back
            // in the browsing history.
            return
        }

        super.onBackPressed()
    }

    fun firstrunFinished() {
        if (pendingUrl != null) {
            // We have received an URL in onNewIntent(). Let's load it now.
            showBrowserScreen(pendingUrl)
            pendingUrl = null
        } else {
            showHomeScreen()
        }
    }

    fun setPendingUrlFromShareIntent(shareIntent: SafeIntent) {
        val dataString = shareIntent.getStringExtra(Intent.EXTRA_TEXT)
        if (!TextUtils.isEmpty(dataString)) {
            val isUrl = UrlUtils.isUrl(dataString)
            TelemetryWrapper.shareIntentEvent(isUrl)
            // We can't update our fragment right now because we need to wait until the activity is
            // resumed. So just remember this URL and load it in onResumeFragments().
            pendingUrl = if (isUrl) dataString else UrlUtils.createSearchUrl(this, dataString)
        }
    }

    companion object {
        val ACTION_ERASE = "erase"
        val ACTION_OPEN = "open"

        val EXTRA_FINISH = "finish"
        val EXTRA_TEXT_SELECTION = "text_selection"
        val EXTRA_NOTIFICATION = "notification"

        private val EXTRA_SHORTCUT = "shortcut"
    }
}
