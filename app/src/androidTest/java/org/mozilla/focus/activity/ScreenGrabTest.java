package org.mozilla.focus.activity;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.webkit.WebView;

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
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class ScreenGrabTest {

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    private UiDevice mDevice;

    @Test
    public void screenGrabTest() throws InterruptedException, UiObjectNotFoundException {
        final int timeOut = 1000 * 10;

        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        /* Wait for app to load, and take the First View screenshot */
        // TBD
        //Screengrab.screenshot("First_View");

        /* Home View*/
        BySelector urlbar = By.clazz("android.widget.TextView")
                .res("org.mozilla.focus.debug","url")
                .clickable(true);
        mDevice.wait(Until.hasObject(urlbar),timeOut);
        Screengrab.screenshot("Home_View");

        /* Main View Menu */
        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.menu),
                        withParent(allOf(withId(R.id.activity_main),
                                withParent(withId(R.id.container)))),
                        isDisplayed()));

        appCompatImageButton.perform(click());
        Screengrab.screenshot("MainViewMenu");
        mDevice.pressBack();

        /* Location Bar View */
        ViewInteraction LocationBar = onView(
                allOf(withId(R.id.url), isDisplayed()));
        LocationBar.perform(click());
        Screengrab.screenshot("LocationBarEmptyState");

        /* Autocomplete View */
        ViewInteraction inlineAutocompleteEditText = onView(
                allOf(withId(R.id.url_edit), isDisplayed()));
        inlineAutocompleteEditText.perform(replaceText("www.mozilla.org"));
        BySelector hint = By.clazz("android.widget.TextView")
                .res("org.mozilla.focus.debug","search_hint")
                .clickable(true);
        mDevice.wait(Until.hasObject(hint),timeOut);
        Screengrab.screenshot("SearchFor");

        ViewInteraction submitURL = onView(
                allOf(withId(R.id.url_edit), isDisplayed()));
        submitURL.perform(pressKey(KEYCODE_ENTER));

        // Wait until view loads
        mDevice.wait(Until.findObject(By.clazz(WebView.class)), 20000);

        /* Browser View Menu */
        ViewInteraction BrowserViewMenuButton = onView(
                allOf(withId(R.id.menu), isDisplayed()));
        BrowserViewMenuButton.perform(click());
        Screengrab.screenshot("BrowserViewMenu");
        mDevice.pressBack();

        /* History Erase Notification */
        ViewInteraction floatingEraseButton = onView(
                allOf(withId(R.id.erase),
                        withParent(allOf(withId(R.id.main_content),
                                withParent(withId(R.id.container)))),
                        isDisplayed()));
        floatingEraseButton.perform(click());
        Screengrab.screenshot("YourBrowingHistoryHasBeenErased");

        /* Take Settings View */
        appCompatImageButton.perform(click());
        UiObject appItem = mDevice.findObject(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(0));
        appItem.click();
        BySelector settingsHeading = By.clazz("android.view.View")
                .res("org.mozilla.focus.debug","toolbar")
                .enabled(true);
        mDevice.wait(Until.hasObject(settingsHeading),timeOut);
        Screengrab.screenshot("Settings_View");

        /* Take Settings View Menu */
        UiObject SettingsViewMenuButton = mDevice.findObject(new UiSelector()
                .className("android.support.v7.widget.LinearLayoutCompat")
                .instance(0));
        SettingsViewMenuButton.click();

        Screengrab.screenshot("SettingsViewMenu");
        mDevice.pressBack();

        /* Settings - BlockOtherContentTrackers */
        // TBD
        //Screengrab.screenshot("BlockOtherContentTrackers");

//        /* About Page */
//        SettingsViewMenuButton.click();
//        UiObject AboutItem = mDevice.findObject(new UiSelector()
//                .className("android.widget.LinearLayout")
//                .instance(0)
//                .enabled(true));
//        appItem.click();
//        mDevice.wait(Until.gone(settingsHeading),timeOut);
//        Screengrab.screenshot("About_Page");
//        mDevice.pressBack();
//         /* Help Page */
//        SettingsViewMenuButton.click();
//        UiObject HelpItem = mDevice.findObject(new UiSelector()
//                .className("android.widget.LinearLayout")
//                .instance(1));
//        HelpItem.click();
//        mDevice.wait(Until.gone(settingsHeading),timeOut);
//        Screengrab.screenshot("Help_Page");
//        mDevice.pressBack();
//         /* Your Rights Page */
//        SettingsViewMenuButton.click();
//        UiObject RightsItem = mDevice.findObject(new UiSelector()
//                .className("android.widget.LinearLayout")
//                .instance(2));
//        RightsItem.click();
//        mDevice.wait(Until.gone(settingsHeading),timeOut);
//        Screengrab.screenshot("YourRights_Page");
//        mDevice.pressBack();
    }

}
