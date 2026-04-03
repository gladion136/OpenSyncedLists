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
 * UI integration tests for tag-based filtering in ListsActivity.
 *
 * Tag filter positions in the drawer's listViewTags RecyclerView
 * (after TestHelper.clearAll, which resets to default tags):
 *   0 = "untagged" (Untagged pseudo-tag, always first)
 *   1 = "Favorites"
 *   2 = "Shopping"
 *   3 = "Projects"
 *   4 = "Work"
 *
 * Run on a connected device or emulator:
 *   ./gradlew connectedAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=\
 *     eu.schmidt.systems.opensyncedlists.activities.TagFilterTest
 */
@RunWith(AndroidJUnit4.class)
public class TagFilterTest {

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

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates a list via the FAB dialog. */
    private void createList(String name) {
        onView(withId(R.id.floatingActionButton)).perform(click());
        onView(isAssignableFrom(android.widget.EditText.class))
                .inRoot(isDialog())
                .perform(replaceText(name), closeSoftKeyboard());
        onView(withId(android.R.id.button1))
                .inRoot(isDialog())
                .perform(click());
    }

    /**
     * Opens the context menu for the list at {@code listPosition} and assigns
     * the tag identified by {@code tagNameRes} to it.
     */
    private void assignTagToList(int listPosition, int tagNameRes) {
        onView(withId(R.id.lVLists))
                .perform(actionOnItemAtPosition(listPosition,
                        TestHelper.clickChildWithId(R.id.imgBtnListMenu)));
        onView(withText(R.string.menu_assign_tag)).perform(click());
        // The tag selection dialog shows a CheckBox per tag; tap the one we want
        onView(withText(tagNameRes)).inRoot(isDialog()).perform(click());
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
    }

    /**
     * Opens the navigation drawer, toggles the filter checkbox at
     * {@code tagPosition} in the tag list, then closes the drawer so the
     * filtered RecyclerView is accessible.
     */
    private void toggleTagFilterAt(int tagPosition) {
        onView(withId(R.id.drawerlayout)).perform(DrawerActions.open());
        onView(withId(R.id.listViewTags))
                .perform(actionOnItemAtPosition(tagPosition,
                        TestHelper.clickChildWithId(R.id.cBoxTagFilter)));
        onView(withId(R.id.drawerlayout)).perform(DrawerActions.close());
    }

    // =========================================================================
    // Tests
    // =========================================================================

    /** The tag list inside the navigation drawer is visible after opening it. */
    @Test
    public void testTagListDisplayedInDrawer() {
        onView(withId(R.id.drawerlayout)).perform(DrawerActions.open());
        onView(withId(R.id.listViewTags)).check(matches(isDisplayed()));
    }

    /** Default tags (Favorites, Shopping, …) appear in the drawer tag list. */
    @Test
    public void testDefaultTagsVisibleInDrawer() {
        onView(withId(R.id.drawerlayout)).perform(DrawerActions.open());
        onView(withId(R.id.listViewTags))
                .check(matches(hasDescendant(withText(R.string.untagged))));
        onView(withId(R.id.listViewTags))
                .check(matches(hasDescendant(
                        withText(R.string.default_tag_title_favorites))));
    }

    /**
     * Enabling the "untagged" filter hides lists that have a tag assigned
     * and shows only lists without any tag.
     */
    @Test
    public void testUntaggedFilterHidesTaggedList() {
        createList("Mit-Tag");     // position 0
        createList("Ohne-Tag");   // position 1
        assignTagToList(0, R.string.default_tag_title_favorites);

        toggleTagFilterAt(0); // 0 = "untagged"

        onView(withId(R.id.lVLists))
                .check(matches(hasDescendant(withText("Ohne-Tag"))));
        onView(withId(R.id.lVLists))
                .check(matches(not(hasDescendant(withText("Mit-Tag")))));
    }

    /**
     * Enabling a named tag filter hides lists that do not carry that tag
     * and shows only the tagged ones.
     */
    @Test
    public void testNamedTagFilterHidesUntaggedList() {
        createList("Mit-Tag");     // position 0
        createList("Ohne-Tag");   // position 1
        assignTagToList(0, R.string.default_tag_title_favorites);

        toggleTagFilterAt(1); // 1 = "Favorites"

        onView(withId(R.id.lVLists))
                .check(matches(hasDescendant(withText("Mit-Tag"))));
        onView(withId(R.id.lVLists))
                .check(matches(not(hasDescendant(withText("Ohne-Tag")))));
    }

    /**
     * Unchecking an active tag filter restores the full list view so that
     * both tagged and untagged lists are visible again.
     */
    @Test
    public void testDisablingFilterRestoresAllLists() {
        createList("Mit-Tag");
        createList("Ohne-Tag");
        assignTagToList(0, R.string.default_tag_title_favorites);

        toggleTagFilterAt(1); // enable "Favorites" filter
        toggleTagFilterAt(1); // disable it again

        onView(withId(R.id.lVLists))
                .check(matches(hasDescendant(withText("Mit-Tag"))));
        onView(withId(R.id.lVLists))
                .check(matches(hasDescendant(withText("Ohne-Tag"))));
    }

    /**
     * When both the "untagged" and a named tag filter are active simultaneously,
     * only lists that satisfy at least one of the active filters are shown.
     */
    @Test
    public void testMultipleFiltersShowMatchingLists() {
        createList("Mit-Tag");     // position 0 – will get Favorites
        createList("Ohne-Tag");   // position 1 – stays untagged
        createList("Anderer-Tag"); // position 2 – will get Shopping
        assignTagToList(0, R.string.default_tag_title_favorites);
        assignTagToList(2, R.string.default_tag_title_shopping);

        toggleTagFilterAt(0); // "untagged"  → shows "Ohne-Tag"
        toggleTagFilterAt(1); // "Favorites" → additionally shows "Mit-Tag"

        onView(withId(R.id.lVLists))
                .check(matches(hasDescendant(withText("Mit-Tag"))));
        onView(withId(R.id.lVLists))
                .check(matches(hasDescendant(withText("Ohne-Tag"))));
        onView(withId(R.id.lVLists))
                .check(matches(not(hasDescendant(withText("Anderer-Tag")))));
    }
}
