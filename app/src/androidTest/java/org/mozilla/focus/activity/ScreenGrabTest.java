package org.mozilla.focus.activity;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

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
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
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

        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        /* Wait for app to load, and take the initial screenshot */

        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.menu),
                        withParent(allOf(withId(R.id.activity_main),
                                withParent(withId(R.id.container)))),
                        isDisplayed()));
        appCompatImageButton.check(matches(isDisplayed()));
        /* Take menu screenshot in main view */
        Screengrab.screenshot("main_menu");
        appCompatImageButton.perform(click());
        Screengrab.screenshot("context_menu");

        UiObject appItem = mDevice.findObject(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(1));
        appItem.click();

        mDevice.pressBack();

        /* Take Settings View */

        /*
        ViewInteraction appCompatTextView = onView(
                allOf(withClassName(equalTo("ListView")),
                        isDisplayed());
        appCompatTextView.perform(click());
        */

        //openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        //Screengrab.screenshot("settings_menu");

        /* take Auto-suggestion View */

        /* Take webView menu */

        /* Take Share menu */

        /* Take Open with menu */

        /* Take 'Your Browsing History is Erased notification */

        /* Take About View */

        /* Take Help View */

        /* Take Your Rights View */

        /*
        ViewInteraction appCompatTextView2 = onView(
                allOf(withId(R.id.title), withText("About"), isDisplayed()));
        appCompatTextView2.perform(click());
        Screengrab.screenshot("about_view");
        */
    }

}
