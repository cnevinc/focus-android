package org.mozilla.focus.activity;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class ScreenGrabTest {

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void screenGrabTest() throws InterruptedException {

        // Wait for app to load, and take the initial screenshot
        //Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        //Thread.sleep(3000);
        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.menu),
                        withParent(allOf(withId(R.id.activity_main),
                                withParent(withId(R.id.container)))),
                        isDisplayed()));
        //Screengrab.screenshot("main_menu");
        appCompatImageButton.perform(click());
        //Screengrab.screenshot("context_menu");

        /*
        ViewInteraction appCompatTextView = onView(
                allOf(withClassName(equalTo("ListView")),
                        isDisplayed());
        appCompatTextView.perform(click());
        */

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        //Screengrab.screenshot("settings_menu");

        ViewInteraction appCompatTextView2 = onView(
                allOf(withId(R.id.title), withText("About"), isDisplayed()));
        appCompatTextView2.perform(click());
        Screengrab.screenshot("about_view");

    }

}
