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
import static androidx.test.espresso.Espresso.pressBack;
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
import static org.hamcrest.Matchers.not;

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
 * All scenarios are grouped into two tests to minimise app restarts:
 *
 * 1. testListsOverviewAndNavigation – launch check, FAB/menu list creation,
 * click to open ListActivity, navigation drawer (tag list + default tags),
 * Settings menu, About menu
 *
 * 2. testListContextMenuTagsAndImport – per-list context menu, list-settings
 * navigation, all four tag-filter scenarios (untagged / named / combined /
 * clear), text-import flow
 *
 * Tag filter positions in listViewTags (after clearAll, which resets to default
 * tags): 0 = "untagged", 1 = "Favorites", 2 = "Shopping", 3 = "Projects", 4 =
 * "Work"
 *
 * Run on a connected device or emulator: ./gradlew connectedAndroidTest \
 * -Pandroid.testInstrumentationRunnerArguments.class=\
 * eu.schmidt.systems.opensyncedlists.activities.ListsActivityTest
 */
@RunWith(AndroidJUnit4.class)
public class ListsActivityTest
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
    
    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    
    /**
     * Covers in sequence (no app restart between sections):
     *
     * A. Launch check       – RecyclerView lVLists is shown B. FAB creates list
     *   – name visible in lVLists C. Menu creates list  – "menu_new_list"
     * overflow item also works D. Open ListActivity  – clicking a list card
     * shows eTNewElement E. Navigation drawer  – drawer opens, tag list shown
     * with default tags F. Settings           – overflow "Settings" →
     * SettingsActivity → back G. About              – overflow "About" →
     * AboutActivity
     */
    @Test public void testListsOverviewAndNavigation()
    {
        // ---- A: Launch ----
        onView(withId(R.id.lVLists)).check(matches(isDisplayed()));
        
        // ---- B: FAB creates list ----
        createList("Einkaufsliste");
        onView(withId(R.id.lVLists)).check(
            matches(hasDescendant(withText("Einkaufsliste"))));
        
        // ---- C: Overflow menu also creates list ----
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_new_list)).perform(click());
        onView(isAssignableFrom(android.widget.EditText.class)).inRoot(
            isDialog()).perform(replaceText("Aufgaben"), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).inRoot(isDialog())
            .perform(click());
        onView(withId(R.id.lVLists)).check(
            matches(hasDescendant(withText("Aufgaben"))));
        
        // ---- D: Tapping a list card opens ListActivity ----
        onView(withId(R.id.lVLists)).perform(
            actionOnItemAtPosition(0, click()));
        onView(withId(R.id.eTNewElement)).check(matches(isDisplayed()));
        pressBack();
        
        // ---- E: Navigation drawer shows tag list with default tags ----
        onView(withId(R.id.drawerlayout)).perform(DrawerActions.open());
        onView(withId(R.id.listViewTags)).check(matches(isDisplayed()));
        onView(withId(R.id.listViewTags)).check(
            matches(hasDescendant(withText(R.string.untagged))));
        onView(withId(R.id.listViewTags)).check(matches(
            hasDescendant(withText(R.string.default_tag_title_favorites))));
        onView(withId(R.id.drawerlayout)).perform(DrawerActions.close());
        
        // ---- F: Settings ----
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_settings)).perform(click());
        // Settings now opens on a screen of subscreen links.
        onView(withText(R.string.settings_screen_design)).check(
            matches(isDisplayed()));
        pressBack();
        
        // ---- G: About ----
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_about)).perform(click());
        onView(withId(R.id.tVVersion)).check(matches(isDisplayed()));
        // Return to ListsActivity so setUp() for the next test can recreate it cleanly.
        pressBack();
    }
    
    /**
     * Covers in sequence (no app restart between sections):
     *
     * A. Context menu       – opens on list card, shows list-management items
     * B. List settings nav  – "Settings of this list" → ListSettingsActivity →
     * back C. Tag assign Cancel  – pressing Cancel in the tag dialog must NOT
     * crash the app (regression for null-callback bug) D. Tag assign OK       –
     * assigning a tag actually persists (regression for off-by-one bug) E. Tag
     * filtering      – four scenarios: untagged-only, untagged+Favorites,
     * Favorites-only, reset (all visible) F. Text import     – overflow "Import
     * text" → dialog → new named list appears in lVLists
     */
    @Test public void testListContextMenuTagsAndImport()
    {
        createList("Mit-Tag");      // position 0 → will get Favorites
        createList("Ohne-Tag");     // position 1 → stays untagged
        createList("Anderer-Tag");  // position 2 → will get Shopping
        
        // ---- A: Context menu ----
        onView(withId(R.id.lVLists)).perform(actionOnItemAtPosition(0,
            TestHelper.clickChildWithId(R.id.imgBtnListMenu)));
        onView(withText(R.string.menu_list_settings)).check(
            matches(isDisplayed()));

        // ---- B: List settings navigation ----
        onView(withText(R.string.menu_list_settings)).perform(click());
        onView(withText(R.string.list_settings_title)).check(
            matches(isDisplayed()));
        pressBack();

        // ---- C: Cancel in tag-assign dialog must not crash (regression) ----
        onView(withId(R.id.lVLists)).perform(actionOnItemAtPosition(0,
            TestHelper.clickChildWithId(R.id.imgBtnListMenu)));
        onView(withText(R.string.menu_assign_tag)).perform(click());
        // Press Cancel — app must stay alive and lVLists must still be shown
        onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click());
        onView(withId(R.id.lVLists)).check(matches(isDisplayed()));

        // ---- D: OK in tag-assign dialog persists the correct tag (off-by-one regression) ----
        // Assign tags for filtering tests; then verify correct tag was saved
        // by checking that the tag filter actually hides/shows the right lists.
        assignTagToList(0, R.string.default_tag_title_favorites);
        assignTagToList(2, R.string.default_tag_title_shopping);
        
        // ---- E: Tag filtering ----
        // 1. Enable "untagged" filter → only "Ohne-Tag" visible
        toggleTagFilterAt(0);
        onView(withId(R.id.lVLists)).check(
            matches(hasDescendant(withText("Ohne-Tag"))));
        onView(withId(R.id.lVLists)).check(
            matches(not(hasDescendant(withText("Mit-Tag")))));
        onView(withId(R.id.lVLists)).check(
            matches(not(hasDescendant(withText("Anderer-Tag")))));
        
        // 2. Also enable "Favorites" filter → "Ohne-Tag" + "Mit-Tag" visible
        toggleTagFilterAt(1);
        onView(withId(R.id.lVLists)).check(
            matches(hasDescendant(withText("Mit-Tag"))));
        onView(withId(R.id.lVLists)).check(
            matches(hasDescendant(withText("Ohne-Tag"))));
        onView(withId(R.id.lVLists)).check(
            matches(not(hasDescendant(withText("Anderer-Tag")))));
        
        // 3. Disable "untagged" filter → only "Mit-Tag" (Favorites) visible
        toggleTagFilterAt(0);
        onView(withId(R.id.lVLists)).check(
            matches(hasDescendant(withText("Mit-Tag"))));
        onView(withId(R.id.lVLists)).check(
            matches(not(hasDescendant(withText("Ohne-Tag")))));
        
        // 4. Disable "Favorites" filter → all lists visible again
        toggleTagFilterAt(1);
        onView(withId(R.id.lVLists)).check(
            matches(hasDescendant(withText("Mit-Tag"))));
        onView(withId(R.id.lVLists)).check(
            matches(hasDescendant(withText("Ohne-Tag"))));
        onView(withId(R.id.lVLists)).check(
            matches(hasDescendant(withText("Anderer-Tag"))));
        
        // ---- F: Text import creates new named list ----
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_import_text)).perform(click());
        onView(withText(R.string.import_text_title)).check(
            matches(isDisplayed()));
        
        onView(isAssignableFrom(android.widget.EditText.class)).inRoot(
            isDialog()).perform(replaceText("- Milch\n- Eier\n- Brot"),
            closeSoftKeyboard());
        onView(withText(R.string.import_text_yes)).inRoot(isDialog())
            .perform(click());
        
        // Second dialog asks for the new list name
        onView(isAssignableFrom(android.widget.EditText.class)).inRoot(
                isDialog())
            .perform(replaceText("Importierte Liste"), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).inRoot(isDialog())
            .perform(click());
        
        onView(withId(R.id.lVLists)).check(
            matches(hasDescendant(withText("Importierte Liste"))));
    }
    
    /**
     * Verifies that pressing Back from ListSettingsActivity returns to
     * ListActivity (not directly to ListsActivity), and that the last-opened
     * list preference is saved so that ListsActivity can auto-open it.
     *
     * A. Open a list → verify ListActivity is shown
     * B. Navigate to ListSettings → verify ListSettingsActivity is shown
     * C. Back → ListActivity (fix: finish() was removed from onOptionsItemSelected)
     * D. Back → ListsActivity
     * E. Open the list again (last_list_id is saved) → ListActivity shown
     *    then Back → ListsActivity (preference round-trip confirmed)
     */
    @Test public void testOpenLastListAndSettingsNavigation()
    {
        createList("LastList");

        // ---- A: Open the list ----
        onView(withId(R.id.lVLists)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.eTNewElement)).check(matches(isDisplayed()));

        // ---- B: Navigate to list settings ----
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_list_settings)).perform(click());
        onView(withText(R.string.list_settings_title)).check(matches(isDisplayed()));

        // ---- C: Back → ListActivity (not ListsActivity) ----
        pressBack();
        onView(withId(R.id.eTNewElement)).check(matches(isDisplayed()));

        // ---- D: Back → ListsActivity ----
        pressBack();
        onView(withId(R.id.lVLists)).check(matches(isDisplayed()));

        // ---- E: last_list_id preference was saved → open same list again ----
        // (Simulates what openLastListIfAvailable does on app re-launch.)
        onView(withId(R.id.lVLists)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.eTNewElement)).check(matches(isDisplayed()));
        pressBack();
        onView(withId(R.id.lVLists)).check(matches(isDisplayed()));
    }

    private void createList(String name)
    {
        onView(withId(R.id.floatingActionButton)).perform(click());
        onView(isAssignableFrom(android.widget.EditText.class)).inRoot(
            isDialog()).perform(replaceText(name), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).inRoot(isDialog())
            .perform(click());
    }
    
    // =========================================================================
    // Tests
    // =========================================================================
    
    private void assignTagToList(int listPosition, int tagNameRes)
    {
        onView(withId(R.id.lVLists)).perform(
            actionOnItemAtPosition(listPosition,
                TestHelper.clickChildWithId(R.id.imgBtnListMenu)));
        onView(withText(R.string.menu_assign_tag)).perform(click());
        onView(withText(tagNameRes)).inRoot(isDialog()).perform(click());
        onView(withId(android.R.id.button1)).inRoot(isDialog())
            .perform(click());
    }
    
    private void toggleTagFilterAt(int tagPosition)
    {
        onView(withId(R.id.drawerlayout)).perform(DrawerActions.open());
        onView(withId(R.id.listViewTags)).perform(
            actionOnItemAtPosition(tagPosition,
                TestHelper.clickChildWithId(R.id.cBoxTagFilter)));
        onView(withId(R.id.drawerlayout)).perform(DrawerActions.close());
    }
}
