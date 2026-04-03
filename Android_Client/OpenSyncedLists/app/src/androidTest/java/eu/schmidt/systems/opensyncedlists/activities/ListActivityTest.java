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
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.helpers.TestHelper;

/**
 * UI integration tests for ListActivity (single-list editing screen).
 *
 * Covers all element-level operations in two large tests to minimise
 * activity restarts while maximising duration/durability coverage:
 *
 *   1. testElementOperationsAndBulkActions
 *      – add (bottom / top / IME), check toggle, element editor (rename,
 *        delete), check-all, uncheck-all, delete-done, toggle-all
 *
 *   2. testSearchAndMenuActions
 *      – search filter + clear, overview-mode toggle, clear list, text
 *        import with markdown checkbox syntax, list-settings navigation
 *
 * Run on a connected device or emulator:
 *   ./gradlew connectedAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=\
 *     eu.schmidt.systems.opensyncedlists.activities.ListActivityTest
 */
@RunWith(AndroidJUnit4.class)
public class ListActivityTest {

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

    private void createAndOpenList(String name) {
        onView(withId(R.id.floatingActionButton)).perform(click());
        onView(isAssignableFrom(android.widget.EditText.class))
                .inRoot(isDialog())
                .perform(replaceText(name), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
        onView(withId(R.id.lVLists)).perform(actionOnItemAtPosition(0, click()));
    }

    private void addElement(String name) {
        onView(withId(R.id.eTNewElement)).perform(replaceText(name), closeSoftKeyboard());
        onView(withId(R.id.iVNewElementBottom)).perform(click());
    }

    private void checkAt(int position) {
        onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition(position,
                        TestHelper.clickChildWithId(R.id.checkBox)));
    }

    /** Custom matcher: item at {@code position} must satisfy {@code itemMatcher}. */
    private static Matcher<View> atPosition(int position, Matcher<View> itemMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(org.hamcrest.Description d) {
                d.appendText("has item at position " + position + ": ");
                itemMatcher.describeTo(d);
            }
            @Override
            protected boolean matchesSafely(View view) {
                if (!(view instanceof RecyclerView)) return false;
                RecyclerView.ViewHolder vh =
                        ((RecyclerView) view).findViewHolderForAdapterPosition(position);
                return vh != null && itemMatcher.matches(vh.itemView);
            }
        };
    }

    // =========================================================================
    // Tests
    // =========================================================================

    /**
     * Covers in sequence (no app restart between sections):
     *
     * A. Add at bottom / top / IME  – verify text and position
     * B. Check-toggle               – verifies a checked checkbox appears
     * C. Element editor             – open, rename, re-open, delete
     * D. Bulk check-all             – check all → verify, check-all no-op
     * E. Bulk uncheck-all           – verify all unchecked
     * F. Bulk delete-done           – check one → delete done → check no-op
     * G. Bulk toggle-all            – check one → toggle → verify inversion
     */
    @Test
    public void testElementOperationsAndBulkActions() {
        createAndOpenList("AllOps-Test");

        // ---- A: Add elements ----
        addElement("Bottom");
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Bottom"))));

        addElement("Second");
        onView(withId(R.id.eTNewElement))
                .perform(replaceText("Top"), closeSoftKeyboard());
        onView(withId(R.id.iVNewElementTop)).perform(click());
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(0, hasDescendant(withText("Top")))));

        onView(withId(R.id.eTNewElement))
                .perform(replaceText("Ime"), pressImeActionButton());
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Ime"))));

        // ---- B: Check toggle ----
        // List (unchecked): Top(0), Bottom(1), Second(2), Ime(3)
        checkAt(0); // check "Top"
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(allOf(withId(R.id.checkBox), isChecked()))));

        // ---- C: Element editor ----
        // Unchecked section: Bottom(0), Second(1), Ime(2)
        onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition(0, TestHelper.clickItemRoot()));
        onView(withId(R.id.eTName)).check(matches(isDisplayed()));
        onView(withId(R.id.btnDelete)).check(matches(isDisplayed()));

        onView(withId(R.id.eTName))
                .perform(clearText(), replaceText("Renamed"), closeSoftKeyboard());
        onView(withId(R.id.btnApplyChanges)).perform(click());
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Renamed"))));

        onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition(0, TestHelper.clickItemRoot()));
        onView(withId(R.id.btnDelete)).perform(click());
        onView(withId(R.id.recyclerView))
                .check(matches(not(hasDescendant(withText("Renamed")))));

        // ---- D: check-all ----
        // Current list: Second(0), Ime(1), isolator, Top (all unchecked except Top)
        // Add fresh elements for cleaner bulk-action state
        addElement("Alpha");
        addElement("Beta");
        // Unchecked: Second, Ime, Alpha, Beta  |  Checked: Top
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_check_all)).perform(click());
        // All 5 elements now checked; isolator at 0, elements at 1-5
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(1,
                        hasDescendant(allOf(withId(R.id.checkBox), isChecked())))));
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(2,
                        hasDescendant(allOf(withId(R.id.checkBox), isChecked())))));

        // check-all no-op: all already checked → still checked
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_check_all)).perform(click());
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(1,
                        hasDescendant(allOf(withId(R.id.checkBox), isChecked())))));

        // ---- E: uncheck-all ----
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_uncheck_all)).perform(click());
        // All unchecked, no isolator: positions 0 and 1 should be isNotChecked
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(0,
                        hasDescendant(allOf(withId(R.id.checkBox), isNotChecked())))));
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(1,
                        hasDescendant(allOf(withId(R.id.checkBox), isNotChecked())))));

        // ---- F: delete-done ----
        // Check "Beta" by text (order after bulk ops is not guaranteed by position).
        onView(withId(R.id.recyclerView))
                .perform(actionOnItem(hasDescendant(withText("Beta")),
                        TestHelper.clickChildWithId(R.id.checkBox)));
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_delete_checked)).perform(click());
        onView(withId(R.id.recyclerView))
                .check(matches(not(hasDescendant(withText("Beta")))));
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Second"))));

        // delete-done no-op (none checked)
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_delete_checked)).perform(click());
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Second"))));

        // ---- G: toggle-all ----
        addElement("Last");
        // Check "Last" by text (order after prior bulk ops is not guaranteed by position).
        onView(withId(R.id.recyclerView))
                .perform(actionOnItem(hasDescendant(withText("Last")),
                        TestHelper.clickChildWithId(R.id.checkBox)));
        // Before toggle: Second/Ime/Alpha/Top unchecked, Last checked
        // After toggle:  Second/Ime/Alpha/Top checked, Last unchecked
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_toggle_all)).perform(click());
        // Last (was checked) is now unchecked → at position 0
        // Remaining 4 (were unchecked) are now checked → at positions 2-5 (isolator at 1)
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(0,
                        allOf(hasDescendant(withText("Last")),
                                hasDescendant(allOf(withId(R.id.checkBox), isNotChecked()))))));
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(2,
                        hasDescendant(allOf(withId(R.id.checkBox), isChecked())))));
    }

    /**
     * Covers in sequence (no app restart between sections):
     *
     * A. Search filter              – type, verify filtered, clear via X button
     * B. Overview-mode toggle       – check element, toggle, RecyclerView stays
     * C. Clear list                 – all elements removed
     * D. Text import ([x]/[ ])      – checked and unchecked elements at right positions
     * E. List settings navigation   – menu → ListSettingsActivity → back
     */
    @Test
    public void testSearchAndMenuActions() {
        createAndOpenList("Multi-Test");

        // ---- A: Search ----
        addElement("Apfel");
        addElement("Banane");
        addElement("Kirsche");

        onView(withId(R.id.action_search)).perform(click());
        onView(isAssignableFrom(android.widget.AutoCompleteTextView.class))
                .perform(typeText("Ban"), closeSoftKeyboard());
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Banane"))));
        onView(withId(R.id.recyclerView))
                .check(matches(not(hasDescendant(withText("Apfel")))));

        // First click clears the search text; second click collapses the SearchView
        // so the action bar's overflow button is accessible again.
        onView(withId(androidx.appcompat.R.id.search_close_btn)).perform(click());
        onView(withId(androidx.appcompat.R.id.search_close_btn)).perform(click());
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Apfel"))));
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Kirsche"))));

        // ---- B: Overview mode toggle ----
        checkAt(0); // check "Apfel" so overview has something to separate
        onView(withId(R.id.action_overview)).perform(click());
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()));

        // ---- C: Clear list ----
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_list_clear)).perform(click());
        onView(withId(R.id.recyclerView))
                .check(matches(not(hasDescendant(withText("Apfel")))));

        // ---- D: Import with markdown checkbox syntax ----
        // After clear, import "- [x] Done" (checked) and "- [ ] Todo" (unchecked).
        // Expected: Todo (unchecked) at 0, isolator at 1, Done (checked) at 2.
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_import_text)).perform(click());
        onView(isAssignableFrom(android.widget.EditText.class))
                .inRoot(isDialog())
                .perform(replaceText("- [x] Done\n- [ ] Todo"), closeSoftKeyboard());
        onView(withText(R.string.import_text_yes)).inRoot(isDialog()).perform(click());

        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(0,
                        allOf(hasDescendant(withText("Todo")),
                                hasDescendant(allOf(withId(R.id.checkBox), isNotChecked()))))));
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(2,
                        allOf(hasDescendant(withText("Done")),
                                hasDescendant(allOf(withId(R.id.checkBox), isChecked()))))));

        // ---- E: List settings navigation ----
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_list_settings)).perform(click());
        onView(withText(R.string.list_settings_title)).check(matches(isDisplayed()));
        pressBack();
    }
}
