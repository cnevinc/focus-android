package org.mozilla.focus.activity;

import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.Until;
import android.text.format.DateUtils;
import android.webkit.WebView;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
        ERROR_IO (-7),
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

    private IdlingResource mIdlingResource;

    @Test
    public void ErrorMessageGrabTest() throws InterruptedException, UiObjectNotFoundException, IOException {
        UiDevice mDevice;
        final long waitingTime = DateUtils.SECOND_IN_MILLIS * 5;
        IdlingPolicies.setMasterPolicyTimeout(waitingTime * 2, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(waitingTime * 2, TimeUnit.MILLISECONDS);

        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        /* Wait for app to load, and take the First View screenshot */
        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.firstrun_exitbutton), isDisplayed()));
        Screengrab.screenshot("First_View");

        /* Home View*/
        appCompatButton.perform(click());
        BySelector urlbar = By.clazz("android.widget.TextView")
                .res("org.mozilla.focus.debug","url")
                .clickable(true);
        mDevice.wait(Until.hasObject(urlbar), waitingTime);

        /* Location Bar View */
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
            mDevice.wait(Until.findObject(By.clazz(WebView.class).focused(true)
                    .enabled(true)), waitingTime);
            IdlingResource idlingResource = new ElapsedTimeIdlingResource(waitingTime);
            Espresso.registerIdlingResources(idlingResource);
            Espresso.unregisterIdlingResources(idlingResource);

            Screengrab.screenshot(error.name());
            LocationBar.perform(click());
        }
    }
}
