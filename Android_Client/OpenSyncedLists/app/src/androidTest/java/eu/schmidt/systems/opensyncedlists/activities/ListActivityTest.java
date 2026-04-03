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
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.helpers.TestHelper;

/**
 * UI integration tests for ListActivity (single-list editing screen).
 *
 * Each test navigates the full user flow: ListsActivity → create list → open list,
 * then exercises the specific feature under test.
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

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        TestHelper.clearAll(ctx);
        activityRule.getScenario().recreate();
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates a list via the FAB dialog and opens it. */
    private void createAndOpenList(String name) {
        // Create list via FAB
        onView(withId(R.id.floatingActionButton)).perform(click());
        onView(isAssignableFrom(android.widget.EditText.class))
                .inRoot(isDialog())
                .perform(replaceText(name), closeSoftKeyboard());
        onView(withId(android.R.id.button1))
                .inRoot(isDialog())
                .perform(click());
        // Open the first (and only) list card
        onView(withId(R.id.lVLists))
                .perform(actionOnItemAtPosition(0, click()));
    }

    /** Types text into the new-element input and adds it at the bottom. */
    private void addElementAtBottom(String elementName) {
        onView(withId(R.id.eTNewElement))
                .perform(replaceText(elementName), closeSoftKeyboard());
        onView(withId(R.id.iVNewElementBottom)).perform(click());
    }

    /** Types text into the new-element input and adds it at the top. */
    private void addElementAtTop(String elementName) {
        onView(withId(R.id.eTNewElement))
                .perform(replaceText(elementName), closeSoftKeyboard());
        onView(withId(R.id.iVNewElementTop)).perform(click());
    }

    /** Custom matcher: checks that the RecyclerView item at {@code position}
     *  satisfies {@code itemMatcher}. */
    private static Matcher<View> atPosition(final int position,
            final Matcher<View> itemMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText(
                        "has item at position " + position + ": ");
                itemMatcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(View view) {
                if (!(view instanceof RecyclerView)) return false;
                RecyclerView rv = (RecyclerView) view;
                RecyclerView.ViewHolder vh =
                        rv.findViewHolderForAdapterPosition(position);
                if (vh == null) return false;
                return itemMatcher.matches(vh.itemView);
            }
        };
    }

    // =========================================================================
    // Tests
    // =========================================================================

    /** Adding an element via the "bottom" button makes it appear in the list. */
    @Test
    public void testAddElementAtBottom() {
        createAndOpenList("Einkaufsliste");
        addElementAtBottom("Milch");
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Milch"))));
    }

    /** Adding at top places the element before an existing element. */
    @Test
    public void testAddElementAtTop() {
        createAndOpenList("Reihenfolge-Test");
        addElementAtBottom("Erster");
        addElementAtTop("Zweiter");
        // "Zweiter" should now be at position 0
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(0,
                        hasDescendant(withText("Zweiter")))));
    }

    /** Pressing the IME "Done" action adds the element just like the button. */
    @Test
    public void testAddElementWithImeAction() {
        createAndOpenList("Aufgaben");
        onView(withId(R.id.eTNewElement))
                .perform(replaceText("Milch kaufen"), pressImeActionButton());
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Milch kaufen"))));
    }

    /** Tapping the CheckBox on an element toggles its checked state. */
    @Test
    public void testCheckElementToggle() {
        createAndOpenList("Checkbox-Test");
        addElementAtBottom("Abhaken");
        onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition(0,
                        TestHelper.clickChildWithId(R.id.checkBox)));
        // isCheckedList defaults to true, so after checking the only element:
        //   position 0 = isolator row (separates unchecked / checked sections)
        //   position 1 = "Abhaken" (now checked, in the checked section)
        onView(withId(R.id.recyclerView))
                .check(matches(atPosition(1,
                        hasDescendant(allOf(withId(R.id.checkBox), isChecked())))));
    }

    /** Clicking an element row opens the ElementEditorFragment bottom sheet. */
    @Test
    public void testClickElementOpensEditor() {
        createAndOpenList("Editor-Test");
        addElementAtBottom("Bearbeiten");
        // Use clickItemRoot() instead of click(): the item contains a focusable
        // EditText (eTTitle) that would consume a real touch event and prevent
        // the root view's OnClickListener (which opens the bottom sheet) from firing.
        onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition(0, TestHelper.clickItemRoot()));
        onView(withId(R.id.eTName)).check(matches(isDisplayed()));
        onView(withId(R.id.btnDelete)).check(matches(isDisplayed()));
    }

    /** Renaming an element via the editor updates the list item title. */
    @Test
    public void testEditElementViaEditor() {
        createAndOpenList("Umbenennen-Test");
        addElementAtBottom("Alter Name");
        onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition(0, TestHelper.clickItemRoot()));
        onView(withId(R.id.eTName))
                .perform(clearText(), replaceText("Neuer Name"),
                        closeSoftKeyboard());
        onView(withId(R.id.btnApplyChanges)).perform(click());
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Neuer Name"))));
    }

    /** Deleting an element via the editor removes it from the list. */
    @Test
    public void testDeleteElementViaEditor() {
        createAndOpenList("Löschen-Test");
        addElementAtBottom("Wird gelöscht");
        onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition(0, TestHelper.clickItemRoot()));
        onView(withId(R.id.btnDelete)).perform(click());
        onView(withId(R.id.recyclerView))
                .check(matches(not(hasDescendant(withText("Wird gelöscht")))));
    }

    /** Typing in the search bar filters the element list. */
    @Test
    public void testSearchFiltersElements() {
        createAndOpenList("Suche-Test");
        addElementAtBottom("Apfel");
        addElementAtBottom("Banane");
        addElementAtBottom("Kirsche");

        // Open search. Use AutoCompleteTextView to uniquely identify the SearchView's
        // internal field: it extends AutoCompleteTextView, whereas eTNewElement and every
        // eTTitle in the list are plain EditTexts — so isAssignableFrom(EditText.class)
        // would be ambiguous across all visible fields.
        onView(withId(R.id.action_search)).perform(click());
        onView(isAssignableFrom(android.widget.AutoCompleteTextView.class))
                .perform(typeText("Ban"), closeSoftKeyboard());

        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Banane"))));
        onView(withId(R.id.recyclerView))
                .check(matches(not(hasDescendant(withText("Apfel")))));
        onView(withId(R.id.recyclerView))
                .check(matches(not(hasDescendant(withText("Kirsche")))));
    }

    /** Clearing the search shows all elements again. */
    @Test
    public void testSearchClearShowsAllElements() {
        createAndOpenList("Suche-Reset-Test");
        addElementAtBottom("Apfel");
        addElementAtBottom("Banane");

        onView(withId(R.id.action_search)).perform(click());
        onView(isAssignableFrom(android.widget.AutoCompleteTextView.class))
                .perform(typeText("Apf"), closeSoftKeyboard());
        // clear search
        onView(isAssignableFrom(android.widget.AutoCompleteTextView.class))
                .perform(clearText(), closeSoftKeyboard());

        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Apfel"))));
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Banane"))));
    }

    /** "Clear list" removes all elements from the list immediately. */
    @Test
    public void testClearList() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        createAndOpenList("Leeren-Test");
        addElementAtBottom("Element 1");
        addElementAtBottom("Element 2");

        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_list_clear)).perform(click());

        onView(withId(R.id.recyclerView))
                .check(matches(not(hasDescendant(withText("Element 1")))));
    }

    /** Toggling overview mode reloads the adapter without crashing. */
    @Test
    public void testOverviewModeToggle() {
        createAndOpenList("Übersicht-Test");
        addElementAtBottom("Aufgabe");
        // Check the element so overview mode has something to separate
        onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition(0,
                        TestHelper.clickChildWithId(R.id.checkBox)));

        // Toggle overview (action bar icon – showAsAction="ifRoom")
        onView(withId(R.id.action_overview)).perform(click());

        // The RecyclerView still exists after the toggle
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()));
    }

    /** "Import text list" dialog opens from the overflow menu in ListActivity. */
    @Test
    public void testImportTextDialogOpens() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        createAndOpenList("Import-Test");
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_import_text)).perform(click());
        onView(withText(R.string.import_text_title)).check(matches(isDisplayed()));
    }

    /**
     * Importing a bullet-list text adds the parsed items to the current list.
     * Parsing happens synchronously in ListActivity (unlike ListsActivity).
     */
    @Test
    public void testImportTextAddsElements() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        createAndOpenList("Text-Import-Test");
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_import_text)).perform(click());

        onView(isAssignableFrom(android.widget.EditText.class))
                .inRoot(isDialog())
                .perform(replaceText("- Milch\n- Eier\n- Brot"),
                        closeSoftKeyboard());
        onView(withText(R.string.import_text_yes))
                .inRoot(isDialog())
                .perform(click());

        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Milch"))));
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Eier"))));
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Brot"))));
    }

    /**
     * "Share as Markdown/Text" fires a share chooser intent so the OS share
     * sheet appears. The app wraps the ACTION_SEND intent inside an
     * ACTION_CHOOSER via {@code Intent.createChooser()}, so we assert on the
     * outer ACTION_CHOOSER that is actually started.
     */
    @Test
    public void testExportMarkdownFiresShareIntent() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        createAndOpenList("Export-Test");
        addElementAtBottom("Element");

        // Stub all outgoing intents so the share sheet never actually opens.
        Intents.intending(IntentMatchers.anyIntent())
                .respondWith(new android.app.Instrumentation.ActivityResult(
                        android.app.Activity.RESULT_OK, null));

        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_export_share_md)).perform(click());

        // The app calls startActivity(Intent.createChooser(sendIntent, ...))
        // which fires ACTION_CHOOSER — not ACTION_SEND — as the top-level intent.
        Intents.intended(IntentMatchers.hasAction(Intent.ACTION_CHOOSER));
    }

    /** "Settings of this list" menu item opens ListSettingsActivity. */
    @Test
    public void testNavigateToListSettings() {
        Context ctx = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        createAndOpenList("Einstellungen-Test");
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText(R.string.menu_list_settings)).perform(click());
        onView(withText(R.string.list_settings_title)).check(matches(isDisplayed()));
    }
}
