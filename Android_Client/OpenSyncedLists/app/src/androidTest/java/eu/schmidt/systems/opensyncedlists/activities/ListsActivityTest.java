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
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;

import androidx.test.espresso.contrib.DrawerActions;
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
 * UI integration tests for ListsActivity (the main lists overview screen).
 *
 * Run on a connected device or emulator:
 *   ./gradlew connectedAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=\
 *     eu.schmidt.systems.opensyncedlists.activities.ListsActivityTest
 */
@RunWith(AndroidJUnit4.class)
public class ListsActivityTest {

    @Rule
    public ActivityScenarioRule<ListsActivity> activityRule =
            new ActivityScenarioRule<>(ListsActivity.class);

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        TestHelper.clearAll(ctx);
        // Recreate so the cleared storage takes effect in this activity instance
        activityRule.getScenario().recreate();
    }

    // -------------------------------------------------------------------------
    // Helper: open FAB dialog, type name, confirm creation
    // -------------------------------------------------------------------------
    private void createList(String name) {
        onView(withId(R.id.floatingActionButton)).perform(click());
        onView(isAssignableFrom(android.widget.EditText.class))
                .inRoot(isDialog())
                .perform(replaceText(name), closeSoftKeyboard());
        onView(withId(android.R.id.button1))
                .inRoot(isDialog())
                .perform(click());
    }

    // =========================================================================
    // Tests
    // =========================================================================

    /** The RecyclerView for lists is visible on launch. */
    @Test
    public void testAppLaunchShowsRecyclerView() {
        onView(withId(R.id.lVLists)).check(matches(isDisplayed()));
    }

    /** Tapping the FAB, entering a name and confirming adds a list card. */
    @Test
    public void testFABCreatesNewList() {
        createList("Einkaufsliste");
        onView(withId(R.id.lVLists))
                .check(matches(hasDescendant(withText("Einkaufsliste"))));
    }

    /** Using the overflow menu item "Create new list" also creates a list. */
    @Test
    public void testMenuNewList() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_new_list)).perform(click());
        onView(isAssignableFrom(android.widget.EditText.class))
                .inRoot(isDialog())
                .perform(replaceText("Aufgaben"), closeSoftKeyboard());
        onView(withId(android.R.id.button1))
                .inRoot(isDialog())
                .perform(click());
        onView(withId(R.id.lVLists))
                .check(matches(hasDescendant(withText("Aufgaben"))));
    }

    /** Tapping a list card opens ListActivity (identified by the element input). */
    @Test
    public void testClickListOpensListActivity() {
        createList("Testliste");
        onView(withId(R.id.lVLists))
                .perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.eTNewElement)).check(matches(isDisplayed()));
    }

    /** Swiping the DrawerLayout open reveals the NavigationView. */
    @Test
    public void testNavigationDrawerOpens() {
        onView(withId(R.id.drawerlayout)).perform(DrawerActions.open());
        onView(withId(R.id.navigation_view)).check(matches(isDisplayed()));
    }

    /** "Settings" menu item navigates to the global settings screen. */
    @Test
    public void testOpenSettingsFromMenu() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_settings)).perform(click());
        // SettingsActivity shows the "Design" preference category
        onView(withText(R.string.design_mode_title)).check(matches(isDisplayed()));
    }

    /** "About" menu item navigates to AboutActivity showing the version text. */
    @Test
    public void testOpenAboutFromMenu() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_about)).perform(click());
        // AboutActivity sets its title to the "About" string and shows tVVersion
        onView(withId(R.id.tVVersion)).check(matches(isDisplayed()));
    }

    /** The per-list context menu opens when tapping the three-dot button. */
    @Test
    public void testListContextMenuOpens() {
        createList("Kontextmenü-Liste");
        onView(withId(R.id.lVLists))
                .perform(actionOnItemAtPosition(0,
                        TestHelper.clickChildWithId(R.id.imgBtnListMenu)));
        // The popup shows "Settings of this list"
        onView(withText(R.string.menu_list_settings)).check(matches(isDisplayed()));
    }

    /** "Settings of this list" in the context menu opens ListSettingsActivity. */
    @Test
    public void testListContextMenuOpenListSettings() {
        createList("Einstellungsliste");
        onView(withId(R.id.lVLists))
                .perform(actionOnItemAtPosition(0,
                        TestHelper.clickChildWithId(R.id.imgBtnListMenu)));
        onView(withText(R.string.menu_list_settings)).perform(click());
        onView(withText(R.string.list_settings_title)).check(matches(isDisplayed()));
    }

    /** "Import text list" menu item opens the import dialog. */
    @Test
    public void testImportTextDialogOpens() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_import_text)).perform(click());
        onView(withText(R.string.import_text_title)).check(matches(isDisplayed()));
    }

    /**
     * Importing a plain-text list creates a new named list in the overview.
     *
     * Flow: Import Text dialog → enter items → click Import →
     *       name dialog → enter name → Create → list card appears.
     */
    @Test
    public void testImportTextCreatesNewList() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_import_text)).perform(click());

        // Enter bullet-list text
        onView(isAssignableFrom(android.widget.EditText.class))
                .inRoot(isDialog())
                .perform(replaceText("- Milch\n- Eier\n- Brot"),
                        closeSoftKeyboard());
        onView(withText(R.string.import_text_yes))
                .inRoot(isDialog())
                .perform(click());

        // A second dialog asks for the list name
        onView(isAssignableFrom(android.widget.EditText.class))
                .inRoot(isDialog())
                .perform(replaceText("Importierte Liste"), closeSoftKeyboard());
        onView(withId(android.R.id.button1))
                .inRoot(isDialog())
                .perform(click());

        onView(withId(R.id.lVLists))
                .check(matches(hasDescendant(withText("Importierte Liste"))));
    }
}
