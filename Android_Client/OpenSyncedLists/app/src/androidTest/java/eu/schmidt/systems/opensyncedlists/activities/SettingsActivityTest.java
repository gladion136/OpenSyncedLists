/*
 * Copyright (C) 2022  Etienne Schmidt (eschmidt@schmidt-ti.eu)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package eu.schmidt.systems.opensyncedlists.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.helpers.TestHelper;

/**
 * UI integration tests for SettingsActivity.
 *
 * Navigates from ListsActivity → overflow menu → Settings and then verifies
 * the contents of the settings screen.
 *
 * Run on a connected device or emulator:
 *   ./gradlew connectedAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=\
 *     eu.schmidt.systems.opensyncedlists.activities.SettingsActivityTest
 */
@RunWith(AndroidJUnit4.class)
public class SettingsActivityTest {

    @Rule
    public ActivityScenarioRule<ListsActivity> activityRule =
            new ActivityScenarioRule<>(ListsActivity.class);

    private Context ctx;

    @Before
    public void setUp() {
        ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TestHelper.clearAll(ctx);
        activityRule.getScenario().recreate();
    }

    /** Opens the Settings screen via the overflow menu. */
    private void openSettings() {
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_settings)).perform(click());
    }

    /** Navigating to Settings from the overflow menu opens SettingsActivity. */
    @Test
    public void testOpenSettings() {
        openSettings();
        onView(withText(R.string.title_activity_settings)).check(matches(isDisplayed()));
    }

    /**
     * The Settings screen contains the "Design" preference category, which
     * lets the user choose Light / Dark / System theme.
     */
    @Test
    public void testThemePreferenceIsVisible() {
        openSettings();
        onView(withText(R.string.design_mode_title)).check(matches(isDisplayed()));
    }

    /**
     * All three theme options (Light, Dark, System) are present in the
     * settings screen.
     */
    @Test
    public void testThemeOptionsArePresent() {
        openSettings();
        onView(withText(R.string.design_mode_title)).perform(click());

        onView(withText(R.string.pref_design_light)).check(matches(isDisplayed()));
        onView(withText(R.string.pref_design_dark)).check(matches(isDisplayed()));
        onView(withText(R.string.pref_design_system)).check(matches(isDisplayed()));
    }
}
