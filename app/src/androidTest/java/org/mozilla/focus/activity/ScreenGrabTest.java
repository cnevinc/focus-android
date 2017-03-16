package org.mozilla.focus.activity;

import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.text.format.DateUtils;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static org.hamcrest.Matchers.allOf;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;

@RunWith(AndroidJUnit4.class)
public class ScreenGrabTest {

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule
            = new ActivityTestRule<MainActivity>(MainActivity.class) {

        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            Context appContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext()
                    .getApplicationContext();
            Resources resources = appContext.getResources();

            PreferenceManager.getDefaultSharedPreferences(appContext)
                    .edit()
                    .putBoolean(FIRSTRUN_PREF, false)
                    .apply();
        }
    };

    public static void swipeDownNotificationBar (UiDevice deviceInstance) {
        int dHeight = deviceInstance.getDisplayHeight();
        int dWidth = deviceInstance.getDisplayWidth();
        int xScrollPosition = dWidth/2;
        int yScrollStop = dHeight/4 * 3;
        deviceInstance.swipe(
                xScrollPosition,
                yScrollStop,
                xScrollPosition,
                0,
                20
        );
    }

    @Test
    public void screenGrabTest() throws InterruptedException, UiObjectNotFoundException {
        UiDevice mDevice;
        final long waitingTime = DateUtils.SECOND_IN_MILLIS * 5;

        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        /* Wait for app to load, and take the First View screenshot */
        UiObject firstViewBtn = mDevice.findObject(new UiSelector()
        .resourceId("org.mozilla.focus.debug:id/firstrun_exitbutton")
        .enabled(true));
        firstViewBtn.waitForExists(waitingTime);
        Screengrab.screenshot("FirstUI_View");

        /* Home View*/
        firstViewBtn.click();
        UiObject urlBar = mDevice.findObject(new UiSelector()
                .resourceId("org.mozilla.focus.debug:id/url")
                .clickable(true));
        urlBar.waitForExists(waitingTime);
        Screengrab.screenshot("Home_View");

        /* Main View Menu */
        ViewInteraction menuButton = onView(
                allOf(withId(R.id.menu),
                        isDisplayed()));
        menuButton.perform(click());

        UiObject RightsItem = mDevice.findObject(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(2));
        RightsItem.waitForExists(waitingTime);
        Screengrab.screenshot("MainViewMenu");

        /* Your Rights Page */
        RightsItem.click();
        UiObject webView = mDevice.findObject(new UiSelector()
        .className("android.webkit.Webview")
        .focused(true)
        .enabled(true));
        webView.waitForExists(waitingTime);
        Screengrab.screenshot("YourRights_Page");

        /* About Page */
        mDevice.pressBack();
        menuButton.perform(click());
        UiObject AboutItem = mDevice.findObject(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(0)
                .enabled(true));
        AboutItem.click();
        webView.waitForExists(waitingTime);
        Screengrab.screenshot("About_Page");

        /* Location Bar View */
        mDevice.pressBack();
        urlBar.click();
        UiObject inlineAutocompleteEditText = mDevice.findObject(new UiSelector()
                .resourceId("org.mozilla.focus.debug:id/url_edit")
                .focused(true)
                .enabled(true));
        inlineAutocompleteEditText.waitForExists(waitingTime);
        Screengrab.screenshot("LocationBarEmptyState");

        /* Autocomplete View */
        inlineAutocompleteEditText.clearTextField();
        inlineAutocompleteEditText.setText("mozilla");
        BySelector hint = By.clazz("android.widget.TextView")
                .res("org.mozilla.focus.debug","search_hint")
                .clickable(true);
        mDevice.wait(Until.hasObject(hint),waitingTime);
        Screengrab.screenshot("SearchFor");

        /* Browser View Menu */
        mDevice.pressKeyCode(KEYCODE_ENTER);
        webView.waitForExists(waitingTime);
        menuButton.perform(click());
        Screengrab.screenshot("BrowserViewMenu");

        /* History Erase Notification */
        mDevice.pressBack();
        ViewInteraction floatingEraseButton = onView(
                allOf(withId(R.id.erase),
                        withParent(allOf(withId(R.id.main_content),
                                withParent(withId(R.id.container)))),
                        isDisplayed()));
        floatingEraseButton.perform(click());
        mDevice.wait(Until.findObject(By.res("org.mozilla.focus.debug","snackbar_text")), waitingTime);
        Screengrab.screenshot("YourBrowingHistoryHasBeenErased");

        /* Take Settings View */
        menuButton.perform(click());
        UiObject appItem = mDevice.findObject(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(3));
        appItem.click();
        BySelector settingsHeading = By.clazz("android.view.View")
                .res("org.mozilla.focus.debug","toolbar")
                .enabled(true);
        mDevice.wait(Until.hasObject(settingsHeading),waitingTime);
        Screengrab.screenshot("Settings_View_Top");

        /* Search Engine List */
        UiScrollable settingsList = new UiScrollable(new UiSelector()
                .resourceId("android:id/list").scrollable(true));
        UiObject SearchEngineSelection = settingsList.getChild(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(0));
        SearchEngineSelection.click();
        mDevice.wait(Until.gone(settingsHeading),waitingTime);
        UiObject SearchEngineList = new UiScrollable(new UiSelector()
                .resourceId("android:id/select_dialog_listview").enabled(true));
        UiObject FirstSelection = SearchEngineList.getChild(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(0));
        Screengrab.screenshot("SearchEngine_Selection");

        /* scroll down */
        FirstSelection.click();
        mDevice.wait(Until.hasObject(settingsHeading),waitingTime);
        swipeDownNotificationBar(mDevice);
        Screengrab.screenshot("Settings_View_Bottom");

//         /* Help Page */
//        SettingsViewMenuButton.click();
//        UiObject HelpItem = mDevice.findObject(new UiSelector()
//                .className("android.widget.LinearLayout")
//                .instance(1));
//        HelpItem.click();
//        mDevice.wait(Until.gone(settingsHeading),timeOut);
//        Screengrab.screenshot("Help_Page");
//        mDevice.pressBack();
    }
}
