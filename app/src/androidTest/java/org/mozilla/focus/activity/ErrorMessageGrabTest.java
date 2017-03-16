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
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static org.hamcrest.Matchers.allOf;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;


@RunWith(AndroidJUnit4.class)
public class ErrorMessageGrabTest {

    private enum ErrorTypes {
        ERROR_UNKNOWN (-1),
        ERROR_HOST_LOOKUP (-2),
        ERROR_CONNECT (-6),
        ERROR_TIMEOUT (-8),
        ERROR_REDIRECT_LOOP (-9),
        ERROR_UNSUPPORTED_SCHEME (-10),
        ERROR_FAILED_SSL_HANDSHAKE (-11),
        ERROR_BAD_URL (-12),
        ERROR_TOO_MANY_REQUESTS (-15);
        private int value;

        private ErrorTypes(int value) {
            this.value = value;
        }
    }

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

    @Test
    public void ErrorMessageGrab() throws InterruptedException, UiObjectNotFoundException {

        UiDevice mDevice;
        final long waitingTime = DateUtils.SECOND_IN_MILLIS * 5;

        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.firstrun_exitbutton), isDisplayed()));
        Screengrab.screenshot("First_View");

        appCompatButton.perform(click());
        BySelector urlbar = By.clazz("android.widget.TextView")
                .res("org.mozilla.focus.debug","url")
                .clickable(true);
        mDevice.wait(Until.hasObject(urlbar), waitingTime);

        ViewInteraction LocationBar = onView(
                allOf(withId(R.id.url), isDisplayed()));
        LocationBar.perform(click());
        ViewInteraction inlineAutocompleteEditText = onView(
                allOf(withId(R.id.url_edit), isDisplayed()));

        for (ErrorTypes error: ErrorTypes.values()) {
            inlineAutocompleteEditText.perform(replaceText("error:"+ error.value));

            ViewInteraction submitURL = onView(
                    allOf(withId(R.id.url_edit), isDisplayed()));
            submitURL.perform(pressKey(KEYCODE_ENTER));
            UiObject tryAgainBtn = mDevice.findObject(new UiSelector()
                    .resourceId("errorTryAgain")
                    .clickable(true));
            tryAgainBtn.waitForExists(waitingTime);

            Screengrab.screenshot(error.name());
            LocationBar.perform(click());
        }
    }
}
