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
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
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
 * UI integration tests for the four bulk-action menu items in ListActivity:
 *   • Check all unchecked
 *   • Uncheck all done
 *   • Delete all done
 *   • Toggle all
 *
 * Run on a connected device or emulator:
 *   ./gradlew connectedAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=\
 *     eu.schmidt.systems.opensyncedlists.activities.ListActivityBulkActionsTest
 */
@RunWith(AndroidJUnit4.class)
public class ListActivityBulkActionsTest {

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
        onView(withId(R.id.eTNewElement))
                .perform(replaceText(name), closeSoftKeyboard());
        onView(withId(R.id.iVNewElementBottom)).perform(click());
    }

    private void checkElementAt(int position) {
        onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition(position,
                        TestHelper.clickChildWithId(R.id.checkBox)));
    }

    /** Custom matcher: checks the RecyclerView item at {@code position}. */
    private static Matcher<View> atPosition(int position, Matcher<View> itemMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("has item at position " + position + ": ");
                itemMatcher.describeTo(description);
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
     * "Check all" marks every unchecked element as done.
     * Setup: 2 unchecked elements → Check all → both appear in the checked section.
     */
    @Test
    public void testCheckAllUnchecked() {
        createAndOpenList("CheckAll-Test");
        addElement("Alpha");
        addElement("Beta");

        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_check_all)).perform(click());

        // With isCheckedList=true: position 0 = isolator, 1 = "Alpha", 2 = "Beta"
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(1,
                        hasDescendant(allOf(withId(R.id.checkBox), isChecked())))));
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(2,
                        hasDescendant(allOf(withId(R.id.checkBox), isChecked())))));
    }

    /**
     * "Uncheck all done" resets every checked element back to unchecked.
     * Setup: 2 elements, both checked → Uncheck all → both unchecked at top.
     */
    @Test
    public void testUncheckAllDone() {
        createAndOpenList("UncheckAll-Test");
        addElement("Alpha");
        addElement("Beta");
        checkElementAt(0); // check "Alpha" (moves below isolator)
        checkElementAt(0); // check "Beta"  (was at 0 after Alpha moved)

        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_uncheck_all)).perform(click());

        // Both elements back in the unchecked section → positions 0 and 1
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(0,
                        hasDescendant(allOf(withId(R.id.checkBox), isNotChecked())))));
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(1,
                        hasDescendant(allOf(withId(R.id.checkBox), isNotChecked())))));
    }

    /**
     * "Delete all done" removes only checked elements and keeps unchecked ones.
     * Setup: "Keep" (unchecked) + "Gone" (checked) → Delete all done → only "Keep" remains.
     */
    @Test
    public void testDeleteAllDone() {
        createAndOpenList("DeleteChecked-Test");
        addElement("Keep");
        addElement("Gone");
        // Check "Gone": after adding, "Keep" is at 0, "Gone" at 1
        onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition(1,
                        TestHelper.clickChildWithId(R.id.checkBox)));

        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_delete_checked)).perform(click());

        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Keep"))));
        onView(withId(R.id.recyclerView))
                .check(matches(not(hasDescendant(withText("Gone")))));
    }

    /**
     * "Toggle all" inverts every element's check state.
     * Setup: "Checked" (checked) + "Unchecked" (unchecked) → Toggle all
     * → "Checked" becomes unchecked, "Unchecked" becomes checked.
     */
    @Test
    public void testToggleAll() {
        createAndOpenList("Toggle-Test");
        addElement("WillBeChecked");
        addElement("WillBeUnchecked");
        // Check "WillBeUnchecked" (position 1 after both added)
        onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition(1,
                        TestHelper.clickChildWithId(R.id.checkBox)));

        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_toggle_all)).perform(click());

        // After toggle:
        //   position 0 = "WillBeUnchecked" (was checked, now unchecked)
        //   position 1 = isolator
        //   position 2 = "WillBeChecked" (was unchecked, now checked)
        //
        // Note: text and checkbox are sibling views inside each item, so they
        // must be matched as SEPARATE descendants at the item level.
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(0,
                        allOf(hasDescendant(withText("WillBeUnchecked")),
                                hasDescendant(allOf(withId(R.id.checkBox),
                                        isNotChecked()))))));
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(2,
                        allOf(hasDescendant(withText("WillBeChecked")),
                                hasDescendant(allOf(withId(R.id.checkBox),
                                        isChecked()))))));
    }

    /**
     * "Check all" on a list that is already fully checked is a no-op —
     * the list must remain consistent (no crash, same item count).
     */
    @Test
    public void testCheckAllWhenAlreadyAllChecked() {
        createAndOpenList("CheckAll-NOP-Test");
        addElement("Item");
        checkElementAt(0);

        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_check_all)).perform(click());

        // Still exactly one element, still checked
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(1,
                        hasDescendant(allOf(withId(R.id.checkBox), isChecked())))));
    }

    /**
     * "Delete all done" on a list with no checked elements is a no-op.
     */
    @Test
    public void testDeleteAllDoneWhenNoneChecked() {
        createAndOpenList("DeleteNone-Test");
        addElement("Safe");

        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_delete_checked)).perform(click());

        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Safe"))));
    }
}
