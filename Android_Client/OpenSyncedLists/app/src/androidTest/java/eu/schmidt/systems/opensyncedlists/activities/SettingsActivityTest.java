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
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.activities.ListsActivity;
import eu.schmidt.systems.opensyncedlists.helpers.TestHelper;

/**
 * UI integration tests for SettingsActivity.
 *
 * All settings checks are consolidated into one test to avoid repeated app
 * restarts.
 *
 * Run on a connected device or emulator: ./gradlew connectedAndroidTest \
 * -Pandroid.testInstrumentationRunnerArguments.class=\
 * eu.schmidt.systems.opensyncedlists.activities.SettingsActivityTest
 */
@RunWith(AndroidJUnit4.class)
public class SettingsActivityTest
{
    
    @Rule
    public ActivityScenarioRule<ListsActivity> activityRule =
        new ActivityScenarioRule<>(ListsActivity.class);
    
    private Context ctx;
    
    @Before public void setUp()
    {
        ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TestHelper.navigateToListsActivity(R.id.lVLists);
        TestHelper.clearAll(ctx);
        activityRule.getScenario().onActivity(ListsActivity::resetForTest);
    }
    
    /**
     * Navigating to Settings shows the title and the top-level subscreen links.
     * Opening the "Design" subscreen reveals the theme preference whose choice
     * dialog offers all three options (Light / Dark / System).
     */
    @Test public void testSettingsScreen()
    {
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_settings)).perform(click());

        onView(withText(R.string.title_activity_settings)).check(
            matches(isDisplayed()));
        // Top-level entry points (subscreen links) are shown.
        onView(withText(R.string.settings_screen_design)).check(
            matches(isDisplayed()));
        onView(withText(R.string.settings_screen_interactions)).check(
            matches(isDisplayed()));
        onView(withText(R.string.settings_screen_sync)).check(
            matches(isDisplayed()));
        onView(withText(R.string.settings_screen_defaults)).check(
            matches(isDisplayed()));

        // Enter the Design subscreen and open the theme choice dialog. After
        // navigation the ActionBar title also reads "Design", so restrict the
        // match to the preference list entry.
        onView(withText(R.string.settings_screen_design)).perform(click());
        onView(allOf(withText(R.string.design_mode_title),
                isDescendantOfA(withId(androidx.preference.R.id.recycler_view))))
                .perform(click());
        onView(withText(R.string.pref_design_light)).check(
            matches(isDisplayed()));
        onView(withText(R.string.pref_design_dark)).check(
            matches(isDisplayed()));
        onView(withText(R.string.pref_design_system)).check(
            matches(isDisplayed()));
        // Dismiss the design dialog
        onView(withText(R.string.pref_design_system)).perform(click());
    }

    /**
     * The font-size preference (inside the Design subscreen) is shown and its
     * choice dialog offers all four size options. Selecting one persists
     * without crashing.
     */
    @Test public void testFontSizePreference()
    {
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_settings)).perform(click());

        // font_size lives in the Design subscreen.
        onView(withText(R.string.settings_screen_design)).perform(click());

        onView(withText(R.string.pref_font_size_title)).check(
            matches(isDisplayed()));

        // Open the font-size choice dialog and verify all options.
        onView(withText(R.string.pref_font_size_title)).perform(click());
        onView(withText(R.string.pref_font_size_small)).check(
            matches(isDisplayed()));
        onView(withText(R.string.pref_font_size_medium)).check(
            matches(isDisplayed()));
        onView(withText(R.string.pref_font_size_large)).check(
            matches(isDisplayed()));
        onView(withText(R.string.pref_font_size_xlarge)).check(
            matches(isDisplayed()));

        // Pick "Small" and confirm the screen is still shown afterwards.
        onView(withText(R.string.pref_font_size_small)).perform(click());
        onView(withText(R.string.pref_font_size_title)).check(
            matches(isDisplayed()));
    }

    /**
     * The two hint links on the Sync Settings screen navigate to the related
     * subscreens (Default values and Data / Storage).
     */
    @Test public void testSyncSettingsHintLinks()
    {
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_settings)).perform(click());
        onView(withText(R.string.settings_screen_sync)).perform(click());

        // Both hints are shown.
        onView(withText(R.string.sync_hint_defaults_title))
            .check(matches(isDisplayed()));
        onView(withText(R.string.sync_hint_data_title))
            .check(matches(isDisplayed()));

        // Hint 1 → Default values screen (its on-screen hint is shown there).
        onView(withText(R.string.sync_hint_defaults_title)).perform(click());
        onView(withText(R.string.defaults_list_hint))
            .check(matches(isDisplayed()));
        androidx.test.espresso.Espresso.pressBack(); // back to sync screen

        // Hint 2 → Data / Storage screen.
        onView(withText(R.string.sync_hint_data_title)).perform(click());
        onView(withText(R.string.data_screen_hint))
            .check(matches(isDisplayed()));
        androidx.test.espresso.Espresso.pressBack(); // back to sync screen
        androidx.test.espresso.Espresso.pressBack(); // back to settings root
    }

    /**
     * Covers the "Data / Storage" screen in one test to keep setup short:
     *
     * A. All reset buttons are reachable on the Data screen.
     * B. "Reset all settings" → confirm → a stored default pref is cleared.
     * C. "Reset all lists" → confirm → the created list is gone from storage.
     * D. "Reset everything" → the confirm dialog appears; we cancel it so the
     *    test process is not killed by clearApplicationUserData().
     */
    @Test public void testDataStorageScreen() throws Exception {
        // Arrange: one stored list + a known default pref present.
        createList("DataTest");
        TestHelper.setFontSizePref(ctx, "0.8");
        org.junit.Assert.assertEquals(1, TestHelper.storedListCount(ctx));
        org.junit.Assert.assertTrue(TestHelper.hasPref(ctx, "font_size"));

        // Open settings → Data / Storage.
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_settings)).perform(click());
        onView(withText(R.string.settings_screen_data)).perform(click());

        // A: buttons present.
        onView(withText(R.string.data_reset_lists_title))
                .check(matches(isDisplayed()));
        onView(withText(R.string.data_delete_server_lists_title))
                .check(matches(isDisplayed()));
        onView(withText(R.string.data_reset_settings_title))
                .check(matches(isDisplayed()));
        onView(withText(R.string.data_reset_everything_title))
                .check(matches(isDisplayed()));

        // B: reset settings.
        onView(withText(R.string.data_reset_settings_title)).perform(click());
        TestHelper.confirmDialog();
        org.junit.Assert.assertFalse("font_size pref should be cleared",
                TestHelper.hasPref(ctx, "font_size"));

        // C: reset lists.
        onView(withText(R.string.data_reset_lists_title)).perform(click());
        TestHelper.confirmDialog();
        org.junit.Assert.assertEquals(0, TestHelper.storedListCount(ctx));

        // D: reset everything → dialog shows, cancel (must not wipe the test).
        onView(withText(R.string.data_reset_everything_title)).perform(click());
        onView(withText(R.string.data_reset_everything_confirm))
                .inRoot(isDialog()).check(matches(isDisplayed()));
        TestHelper.cancelDialog();

        // Leave settings cleanly (data subscreen → settings root → ListsActivity).
        androidx.test.espresso.Espresso.pressBack();
        androidx.test.espresso.Espresso.pressBack();
    }

    /** Creates a list from ListsActivity via the FAB + name dialog. */
    private void createList(String name) {
        onView(withId(R.id.floatingActionButton)).perform(click());
        onView(isAssignableFrom(android.widget.EditText.class))
                .inRoot(isDialog())
                .perform(replaceText(name), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
    }
}
