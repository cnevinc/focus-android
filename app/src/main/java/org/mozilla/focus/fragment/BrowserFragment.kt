/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.Manifest
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.AppBarLayout
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.*

import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.activity.InstallFirefoxActivity
import org.mozilla.focus.broadcastreceiver.DownloadBroadcastReceiver
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.menu.BrowserMenu
import org.mozilla.focus.menu.WebContextMenu
import org.mozilla.focus.notification.BrowsingNotificationService
import org.mozilla.focus.open.OpenWithFragment
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.ColorUtils
import org.mozilla.focus.utils.DrawableUtils
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.UrlUtils
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.web.BrowsingSession
import org.mozilla.focus.web.CustomTabConfig
import org.mozilla.focus.web.Download
import org.mozilla.focus.web.IWebView
import org.mozilla.focus.widget.AnimatedProgressBar

import java.lang.ref.WeakReference

/**
 * Fragment for displaying the browser UI.
 */
class BrowserFragment : WebFragment(), View.OnClickListener, DownloadDialogFragment.DownloadDialogListener {

    private var pendingDownload: Download? = null
    private var backgroundView: View? = null
    private var backgroundTransition: TransitionDrawable? = null
    private var urlView: TextView? = null
    private var progressView: AnimatedProgressBar? = null
    private var blockView: FrameLayout? = null
    private var lockView: ImageView? = null
    private var menuView: ImageButton? = null
    private var menuWeakReference = WeakReference<BrowserMenu>(null)

    /**
     * Container for custom video views shown in fullscreen mode.
     */
    private var videoContainer: ViewGroup? = null

    /**
     * Container containing the browser chrome and web content.
     */
    private var browserContainer: View? = null

    private var forwardButton: Button? = null
    private var backButton: Button? = null

    private var refreshButton: View? = null
    private var stopButton: View? = null

    private var fullscreenCallback: IWebView.FullscreenCallback? = null

    var isLoading = false
        private set

    private var manager: DownloadManager? = null

    private var downloadBroadcastReceiver: DownloadBroadcastReceiver? = null

    // Set an initial WeakReference so we never have to handle loadStateListenerWeakReference being null
    // (i.e. so we can always just .get()).
    private var loadStateListenerWeakReference = WeakReference<LoadStateListener>(null)

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        BrowsingNotificationService.start(context)
    }

    override fun onPause() {
        super.onPause()
        context.unregisterReceiver(downloadBroadcastReceiver)

        val menu = menuWeakReference.get()
        if (menu != null) {
            menu.dismiss()

            menuWeakReference.clear()
        }
    }

    override fun getInitialUrl(): String? {
        return arguments.getString(ARGUMENT_URL)
    }

    private fun updateURL(url: String?) {
        urlView?.text = UrlUtils.stripUserInfo(url)
    }

    override fun inflateLayout(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (savedInstanceState != null && savedInstanceState.containsKey(RESTORE_KEY_DOWNLOAD)) {
            // If this activity was destroyed before we could start a download (e.g. because we were waiting for a permission)
            // then restore the download object.
            pendingDownload = savedInstanceState.getParcelable<Download>(RESTORE_KEY_DOWNLOAD)
        }

        val view = inflater.inflate(R.layout.fragment_browser, container, false)

        videoContainer = view.findViewById(R.id.video_container) as ViewGroup
        browserContainer = view.findViewById(R.id.browser_container)

        urlView = view.findViewById(R.id.display_url) as TextView
        updateURL(initialUrl)

        backgroundView = view.findViewById(R.id.background)
        backgroundTransition = backgroundView?.background as TransitionDrawable

        refreshButton = view.findViewById(R.id.refresh)
        refreshButton?.setOnClickListener(this)


        stopButton = view.findViewById(R.id.stop)
        stopButton?.setOnClickListener(this)

        forwardButton = view.findViewById(R.id.forward) as? Button?
        forwardButton?.setOnClickListener(this)

        backButton = view.findViewById(R.id.back) as? Button?
        backButton?.setOnClickListener(this)

        blockView = view.findViewById(R.id.block) as FrameLayout

        lockView = view.findViewById(R.id.lock) as ImageView

        progressView = view.findViewById(R.id.progress) as AnimatedProgressBar

        menuView = view.findViewById(R.id.menu) as ImageButton
        menuView?.setOnClickListener(this)

        if (BrowsingSession.getInstance().isCustomTab) {
            initialiseCustomTabUi(view)
        } else {
            initialiseNormalBrowserUi(view)
        }

        return view
    }

    private fun initialiseNormalBrowserUi(view: View) {
        val erase = view.findViewById(R.id.erase)
        erase.setOnClickListener(this)

        urlView?.setOnClickListener(this)
    }

    private fun initialiseCustomTabUi(view: View) {
        val customTabConfig = BrowsingSession.getInstance().customTabConfig

        // Unfortunately there's no simpler way to have the FAB only in normal-browser mode.
        // - ViewStub: requires splitting attributes for the FAB between the ViewStub, and actual FAB layout file.
        //             Moreover, the layout behaviour just doesn't work unless you set it programatically.
        // - View.GONE: doesn't work because the layout-behaviour makes the FAB visible again when scrolling.
        // - Adding at runtime: works, but then we need to use a separate layout file (and you need
        //   to set some attributes programatically, same as ViewStub).
        val erase = view.findViewById(R.id.erase)
        val eraseContainer = erase.parent as ViewGroup
        eraseContainer.removeView(erase)

        val textColor: Int

        val toolbar = view.findViewById(R.id.urlbar)
        if (customTabConfig.toolbarColor != null) {
            toolbar.setBackgroundColor(customTabConfig.toolbarColor)

            textColor = ColorUtils.getReadableTextColor(customTabConfig.toolbarColor)
            urlView?.setTextColor(textColor)
        } else {
            textColor = Color.WHITE
        }

        val closeButton = view.findViewById(R.id.customtab_close) as ImageView

        closeButton.visibility = View.VISIBLE
        closeButton.setOnClickListener(this)

        if (customTabConfig.closeButtonIcon != null) {
            closeButton.setImageBitmap(customTabConfig.closeButtonIcon)
        } else {
            // Always set the icon in case it's been overridden by a previous CT invocation
            val closeIcon = DrawableUtils.loadAndTintDrawable(context, R.drawable.ic_close, textColor)

            closeButton.setImageDrawable(closeIcon)
        }

        if (customTabConfig.disableUrlbarHiding) {
            val params = toolbar.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = 0
        }

        if (customTabConfig.actionButtonConfig != null) {
            val actionButton = view.findViewById(R.id.customtab_actionbutton) as ImageButton
            actionButton.visibility = View.VISIBLE

            actionButton.setImageBitmap(customTabConfig.actionButtonConfig.icon)
            actionButton.contentDescription = customTabConfig.actionButtonConfig.description

            val pendingIntent = customTabConfig.actionButtonConfig.pendingIntent

            actionButton.setOnClickListener {
                try {
                    val intent = Intent()
                    intent.data = Uri.parse(url)

                    pendingIntent.send(context, 0, intent)
                } catch (e: PendingIntent.CanceledException) {
                    // There's really nothing we can do here...
                }

                TelemetryWrapper.customTabActionButtonEvent()
            }
        }

        // We need to tint some icons.. We already tinted the close button above. Let's tint our other icons too.
        val lockIcon = DrawableUtils.loadAndTintDrawable(context, R.drawable.ic_lock, textColor)
        lockView?.setImageDrawable(lockIcon)

        val menuIcon = DrawableUtils.loadAndTintDrawable(context, R.drawable.ic_menu, textColor)
        menuView?.setImageDrawable(menuIcon)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        if (pendingDownload != null) {
            // We were not able to start this download yet (waiting for a permission). Save this download
            // so that we can start it once we get restored and receive the permission.
            outState?.putParcelable(RESTORE_KEY_DOWNLOAD, pendingDownload)
        }
    }

    interface LoadStateListener {
        fun isLoadingChanged(isLoading: Boolean)
    }

    /**
     * Set a (singular) LoadStateListener. Only one listener is supported at any given time. Setting
     * a new listener means any previously set listeners will be dropped. This is only intended
     * to be used by NavigationItemViewHolder. If you want to use this method for any other
     * parts of the codebase, please extend it to handle a list of listeners. (We would also need
     * to automatically clean up expired listeners from that list, probably when adding to that list.)

     * @param listener The listener to notify of load state changes. Only a weak reference will be kept,
     * *                 no more calls will be sent once the listener is garbage collected.
     */
    fun setIsLoadingListener(listener: LoadStateListener) {
        loadStateListenerWeakReference = WeakReference(listener)
    }

    private fun updateIsLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        val currentListener = loadStateListenerWeakReference.get()
        currentListener?.isLoadingChanged(isLoading)
    }

    override fun createCallback(): IWebView.Callback {
        return object : IWebView.Callback {
            override fun onPageStarted(url: String) {
                updateIsLoading(true)

                lockView?.visibility = View.GONE

                // Hide badging while loading
                updateBlockingBadging(true)

                progressView?.announceForAccessibility(getString(R.string.accessibility_announcement_loading))

                backgroundTransition?.resetTransition()

                progressView?.progress = 5
                progressView?.visibility = View.VISIBLE

                updateToolbarButtonStates()
            }

            override fun onPageFinished(isSecure: Boolean) {
                updateIsLoading(false)

                backgroundTransition?.startTransition(ANIMATION_DURATION)

                progressView?.announceForAccessibility(getString(R.string.accessibility_announcement_loading_finished))

                progressView?.visibility = View.GONE

                if (isSecure) {
                    lockView?.visibility = View.VISIBLE
                }

                updateBlockingBadging(isBlockingEnabled)

                updateToolbarButtonStates()
            }

            override fun onURLChanged(url: String) {
                updateURL(url)
            }

            override fun onProgress(progress: Int) {
                progressView?.progress = progress
            }

            override fun handleExternalUrl(url: String): Boolean {
                val webView = webView

                return webView != null && IntentUtils.handleExternalUri(context, webView, url)
            }

            override fun onLongPress(hitTarget: IWebView.HitTarget) {
                WebContextMenu.show(activity, this, hitTarget)
            }

            override fun onEnterFullScreen(callback: IWebView.FullscreenCallback, view: View?) {
                fullscreenCallback = callback

                if (view != null) {
                    // Hide browser UI and web content
                    browserContainer?.visibility = View.INVISIBLE

                    // Add view to video container and make it visible
                    val params = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    videoContainer?.addView(view, params)
                    videoContainer?.visibility = View.VISIBLE

                    // Switch to immersive mode: Hide system bars other UI controls
                    switchToImmersiveMode()
                }
            }

            override fun onExitFullScreen() {
                // Remove custom video views and hide container
                videoContainer?.removeAllViews()
                videoContainer?.visibility = View.GONE

                // Show browser UI and web content again
                browserContainer?.visibility = View.VISIBLE

                exitImmersiveMode()

                // Notify renderer that we left fullscreen mode.
                if (fullscreenCallback != null) {
                    fullscreenCallback?.fullScreenExited()
                    fullscreenCallback = null
                }
            }

            override fun onDownloadStart(download: Download) {
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Long press image displays its own dialog and we handle other download cases here
                    if (!isDownloadFromLongPressImage(download)) {
                        showDownloadPromptDialog(download)
                    } else {
                        // Download dialog has already been shown from long press on image. Proceed with download.
                        queueDownload(download)
                    }
                } else {
                    // We do not have the permission to write to the external storage. Request the permission and start the
                    // download from onRequestPermissionsResult().
                    activity ?: return

                    pendingDownload = download

                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_STORAGE_PERMISSION)
                }
            }
        }
    }

    /**
     * Checks a download's destination directory to determine if it is being called from
     * a long press on an image or otherwise.
     */
    private fun isDownloadFromLongPressImage(download: Download): Boolean {
        return download.destinationDirectory == Environment.DIRECTORY_PICTURES
    }

    /**
     * Hide system bars. They can be revealed temporarily with system gestures, such as swiping from
     * the top of the screen. These transient system bars will overlay app’s content, may have some
     * degree of transparency, and will automatically hide after a short timeout.
     */
    private fun switchToImmersiveMode() {
        val activity = activity ?: return

        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    /**
     * Show the system bars again.
     */
    private fun exitImmersiveMode() {
        val activity = activity ?: return

        val window = activity.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != REQUEST_CODE_STORAGE_PERMISSION) {
            return
        }

        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            // We didn't get the storage permission: We are not able to start this download.
            pendingDownload = null
        }

        // The actual download dialog will be shown from onResume(). If this activity/fragment is
        // getting restored then we need to 'resume' first before we can show a dialog (attaching
        // another fragment).
    }

    internal fun showDownloadPromptDialog(download: Download?) {
        val fragmentManager = fragmentManager

        if (fragmentManager.findFragmentByTag(DownloadDialogFragment.FRAGMENT_TAG) != null) {
            // We are already displaying a download dialog fragment (Probably a restored fragment).
            // No need to show another one.
            return
        }

        val downloadDialogFragment = DownloadDialogFragment.newInstance(download)
        downloadDialogFragment.setTargetFragment(this@BrowserFragment, 300)

        try {
            downloadDialogFragment.show(fragmentManager, DownloadDialogFragment.FRAGMENT_TAG)
        } catch (e: IllegalStateException) {
            // It can happen that at this point in time the activity is already in the background
            // and onSaveInstanceState() has already been called. Fragment transactions are not
            // allowed after that anymore. It's probably safe to guess that the user might not
            // be interested in the download at this point. So we could just *not* show the dialog.
            // Unfortunately we can't call commitAllowingStateLoss() because committing the
            // transaction is happening inside the DialogFragment code. Therefore we just swallow
            // the exception here. Gulp!
        }

    }

    internal fun showAddToHomescreenDialog(url: String, title: String) {
        val fragmentManager = fragmentManager

        if (fragmentManager.findFragmentByTag(AddToHomescreenDialogFragment.FRAGMENT_TAG) != null) {
            // We are already displaying a homescreen dialog fragment (Probably a restored fragment).
            // No need to show another one.
            return
        }

        val addToHomescreenDialogFragment = AddToHomescreenDialogFragment.newInstance(url, title)
        addToHomescreenDialogFragment.setTargetFragment(this@BrowserFragment, 300)

        try {
            addToHomescreenDialogFragment.show(fragmentManager, AddToHomescreenDialogFragment.FRAGMENT_TAG)
        } catch (e: IllegalStateException) {
            // It can happen that at this point in time the activity is already in the background
            // and onSaveInstanceState() has already been called. Fragment transactions are not
            // allowed after that anymore. It's probably safe to guess that the user might not
            // be interested in adding to homescreen now.
        }

    }

    override fun onFinishDownloadDialog(download: Download, shouldDownload: Boolean) {
        if (shouldDownload) {
            queueDownload(download)
        }
    }

    override fun onCreateViewCalled() {
        manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadBroadcastReceiver = DownloadBroadcastReceiver(browserContainer, manager)
    }

    override fun onResume() {
        super.onResume()

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        context.registerReceiver(downloadBroadcastReceiver, filter)

        if (pendingDownload != null && PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // There's a pending download (waiting for the storage permission) and now we have the
            // missing permission: Show the dialog to ask whether the user wants to actually proceed
            // with downloading this file.
            showDownloadPromptDialog(pendingDownload)
            pendingDownload = null
        }
    }

    /**
     * Use Android's Download Manager to queue this download.
     */
    private fun queueDownload(download: Download?) {
        if (download == null) {
            return
        }

        context ?: return

        val cookie = CookieManager.getInstance().getCookie(download.url)
        val fileName = URLUtil.guessFileName(
                download.url, download.contentDisposition, download.mimeType)

        val request = DownloadManager.Request(Uri.parse(download.url))
                .addRequestHeader("User-Agent", download.userAgent)
                .addRequestHeader("Cookie", cookie)
                .addRequestHeader("Referer", url)
                .setDestinationInExternalPublicDir(download.destinationDirectory, fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setMimeType(download.mimeType)

        request.allowScanningByMediaScanner()

        val downloadReference = manager?.enqueue(request)
        downloadReference?.let {
            downloadBroadcastReceiver?.addQueuedDownload(it)
        }

    }

    private // No SafeIntent needed here because intent.getAction() is safe (SafeIntent simply calls intent.getAction()
            // without any wrapping):
    val isStartedFromExternalApp: Boolean
        get() {
            val activity = activity ?: return false
            val intent = activity.intent
            return intent != null && Intent.ACTION_VIEW == intent.action
        }

    fun onBackPressed(): Boolean {
        if (canGoBack()) {
            // Go back in web history
            goBack()
        } else {
            if (isStartedFromExternalApp) {
                // We have been started from a VIEW intent. Go back to the previous app immediately
                // and erase the current browsing session.
                erase()

                // We remove the whole task because otherwise the old session might still be
                // partially visible in the app switcher.
                activity.finishAndRemoveTask()

                // We can't show a snackbar outside of the app. So let's show a toast instead.
                Toast.makeText(context, R.string.feedback_erase, Toast.LENGTH_SHORT).show()

                TelemetryWrapper.eraseBackToAppEvent()
            } else {
                // Just go back to the home screen.
                eraseAndShowHomeScreen(true)

                TelemetryWrapper.eraseBackToHomeEvent()
            }
        }

        return true
    }

    fun erase() {
        val webView = webView
        webView?.cleanup()

        BrowsingNotificationService.stop(context)
    }

    fun eraseAndShowHomeScreen(animateErase: Boolean) {
        erase()

        val transaction = activity.supportFragmentManager
                .beginTransaction()

        if (animateErase) {
            transaction.setCustomAnimations(0, R.anim.erase_animation)
        }

        transaction
                .replace(R.id.container, UrlInputFragment.createWithBackground(), UrlInputFragment.FRAGMENT_TAG)
                .commit()

        ViewUtils.showBrandedSnackbar(activity.findViewById(android.R.id.content),
                R.string.feedback_erase,
                resources.getInteger(R.integer.erase_snackbar_delay))
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.menu -> {
                val customTabConfig: CustomTabConfig?
                if (BrowsingSession.getInstance().isCustomTab) {
                    customTabConfig = BrowsingSession.getInstance().customTabConfig
                } else {
                    customTabConfig = null
                }

                val menu = BrowserMenu(activity, this, customTabConfig)
                menu.show(menuView)

                menuWeakReference = WeakReference(menu)
            }

            R.id.display_url -> {
                val urlFragment = UrlInputFragment
                        .createAsOverlay(UrlUtils.getSearchTermsOrUrl(context, url), urlView)

                activity.supportFragmentManager
                        .beginTransaction()
                        .add(R.id.container, urlFragment, UrlInputFragment.FRAGMENT_TAG)
                        .commit()
            }

            R.id.erase -> {
                eraseAndShowHomeScreen(true)

                TelemetryWrapper.eraseEvent()
            }

            R.id.back -> {
                goBack()
            }

            R.id.forward -> {
                val webView = webView
                webView?.goForward()
            }

            R.id.refresh -> {
                reload()
            }

            R.id.stop -> {
                val webView = webView
                webView?.stopLoading()
            }

            R.id.share -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_dialog_title)))

                TelemetryWrapper.shareEvent()
            }

            R.id.settings -> (activity as LocaleAwareAppCompatActivity).openPreferences()

            R.id.open_default -> {
                val browsers = Browsers(context, url)

                val defaultBrowser = browsers.defaultBrowser ?: // We only add this menu item when a third party default exists, in
                        // BrowserMenuAdapter.initializeMenu()
                        throw IllegalStateException("<Open with \$Default> was shown when no default browser is set")

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.`package` = defaultBrowser.packageName
                startActivity(intent)

                TelemetryWrapper.openDefaultAppEvent()
            }

            R.id.open_firefox -> {
                val browsers = Browsers(context, url)

                if (browsers.hasFirefoxBrandedBrowserInstalled()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    intent.`package` = browsers.firefoxBrandedBrowser.packageName
                    startActivity(intent)
                } else {
                    InstallFirefoxActivity.open(context)
                }

                TelemetryWrapper.openFirefoxEvent()
            }

            R.id.open_select_browser -> {
                val browsers = Browsers(context, url)

                val fragment = OpenWithFragment.newInstance(
                        browsers.installedBrowsers, url)
                fragment.show(fragmentManager, OpenWithFragment.FRAGMENT_TAG)

                TelemetryWrapper.openSelectionEvent()
            }

            R.id.customtab_close -> {
                erase()
                activity.finish()

                TelemetryWrapper.closeCustomTabEvent()
            }

            R.id.help -> {
                val helpIntent = InfoActivity.getHelpIntent(activity)
                startActivity(helpIntent)
            }

            R.id.help_trackers -> {
                val trackerHelpIntent = InfoActivity.getTrackerHelpIntent(activity)
                startActivity(trackerHelpIntent)
            }

            R.id.add_to_homescreen -> {
                val webView = webView ?: return

                val url = webView.url
                val title = webView.title
                showAddToHomescreenDialog(url, title)
            }

            else -> throw IllegalArgumentException("Unhandled menu item in BrowserFragment")
        }
    }

    private fun updateToolbarButtonStates() {
        if (forwardButton == null || backButton == null || refreshButton == null || stopButton == null) {
            return
        }

        val webView = webView ?: return

        val canGoForward = webView.canGoForward()
        val canGoBack = webView.canGoBack()

        forwardButton?.isEnabled = canGoForward
        forwardButton?.alpha = if (canGoForward) 1.0f else 0.5f
        backButton?.isEnabled = canGoBack
        backButton?.alpha = if (canGoBack) 1.0f else 0.5f

        refreshButton?.visibility = if (isLoading) View.GONE else View.VISIBLE
        stopButton?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    // getUrl() is used for things like sharing the current URL. We could try to use the webview,
    // but sometimes it's null, and sometimes it returns a null URL. Sometimes it returns a data:
    // URL for error pages. The URL we show in the toolbar is (A) always correct and (B) what the
    // user is probably expecting to share, so lets use that here:
    val url: String
        get() = urlView?.text.toString()

    fun canGoForward(): Boolean {
        val webView = webView
        return webView != null && webView.canGoForward()
    }

    fun canGoBack(): Boolean {
        val webView = webView
        return webView != null && webView.canGoBack()
    }

    fun goBack() {
        val webView = webView
        webView?.goBack()
    }

    fun loadUrl(url: String) {
        val webView = webView
        webView?.loadUrl(url)
    }

    fun reload() {
        val webView = webView
        webView?.reload()
    }

    var isBlockingEnabled: Boolean
        get() {
            val webView = webView
            return webView == null || webView.isBlockingEnabled
        }
        set(enabled) {
            val webView = webView
            if (webView != null) {
                webView.isBlockingEnabled = enabled
            }
            backgroundView?.setBackgroundResource(if (enabled) R.drawable.animated_background else R.drawable.animated_background_disabled)
            backgroundTransition = backgroundView?.background as? TransitionDrawable?
        }

    // In the future, if more badging icons are needed, this should be abstracted
    fun updateBlockingBadging(enabled: Boolean) {
        blockView?.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    companion object {
        val FRAGMENT_TAG = "browser"

        private val REQUEST_CODE_STORAGE_PERMISSION = 101
        private val ANIMATION_DURATION = 300
        private val ARGUMENT_URL = "url"
        private val RESTORE_KEY_DOWNLOAD = "download"

        fun create(url: String?): BrowserFragment {
            val arguments = Bundle()
            arguments.putString(ARGUMENT_URL, url)

            val fragment = BrowserFragment()
            fragment.arguments = arguments

            return fragment
        }
    }


}



