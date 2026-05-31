/*
 * Copyright (C) 2026  Etienne Schmidt (eschmidt@schmidt-ti.eu)
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
package eu.schmidt.systems.opensyncedlists.screenshots;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Locale;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.activities.ListActivity;
import eu.schmidt.systems.opensyncedlists.activities.ListSettingsActivity;
import eu.schmidt.systems.opensyncedlists.activities.ListsActivity;
import eu.schmidt.systems.opensyncedlists.activities.SettingsActivity;
import eu.schmidt.systems.opensyncedlists.helpers.TestHelper;
import eu.schmidt.systems.opensyncedlists.storages.SecureStorage;
import eu.schmidt.systems.opensyncedlists.syncedlist.ACTION;
import eu.schmidt.systems.opensyncedlists.syncedlist.ListTag;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListStep;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;

@RunWith(AndroidJUnit4.class)
public class ScreenshotGenerationTest
{
    private static final String SCREENSHOT_DIR_NAME = "screenshots";
    
    private Context ctx;
    
    @Before public void setUp() throws Exception
    {
        ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        forceEnglishResources(ctx);
        forceEnglishResources(
            InstrumentationRegistry.getInstrumentation().getContext());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags("en-US"));
        TestHelper.clearAll(ctx);
        SharedPreferences.Editor prefs =
            PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        prefs.putString("design", "Light");
        prefs.putBoolean("check_option", true);
        prefs.putBoolean("checked_list", true);
        prefs.putBoolean("jump_buttons", true);
        prefs.putBoolean("list_overview_instead_cards", false);
        prefs.putString("font_size", "1.0");
        prefs.apply();
        clearScreenshotDir();
        seedScreenshotData();
    }
    
    private void forceEnglishResources(Context context)
    {
        Locale locale = Locale.US;
        Locale.setDefault(locale);
        Configuration configuration =
            new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        context.getResources().updateConfiguration(configuration,
            context.getResources().getDisplayMetrics());
    }
    
    @Test public void generateScreenshots() throws Exception
    {
        try (ActivityScenario<ListsActivity> scenario =
                 ActivityScenario.launch(ListsActivity.class))
        {
            onView(withId(R.id.lVLists)).check(matches(isDisplayed()));
            waitForUi();
            capture("1_Screenshot_lists.png");
            
            openShoppingList();
            onView(withId(R.id.recyclerView)).check(matches(isDisplayed()));
            openActionBarOverflowOrOptionsMenu(ctx);
            onView(withText(R.string.menu_manual_sync)).check(
                matches(isDisplayed()));
            waitForUi();
            capture("2_Screenshot_list.png");
        }
        
        try (ActivityScenario<ListActivity> scenario = launchList("shopping"))
        {
            onView(withId(R.id.recyclerView)).check(matches(isDisplayed()));
            waitForUi();
            capture("3_Screenshot_list_second.png");
        }
        
        try (ActivityScenario<ListsActivity> scenario =
                 ActivityScenario.launch(ListsActivity.class))
        {
            onView(withId(R.id.drawerlayout)).perform(DrawerActions.open());
            onView(withId(R.id.listViewTags)).check(matches(isDisplayed()));
            waitForUi();
            capture("4_Screenshot_lists_filter.png");
        }
        
        enableShoppingOverview();
        try (ActivityScenario<ListActivity> scenario = launchList("shopping"))
        {
            onView(withId(R.id.recyclerView)).check(matches(isDisplayed()));
            waitForUi();
            capture("5_Screenshot_overview.png");
        }
        
        try (ActivityScenario<SettingsActivity> scenario =
                 ActivityScenario.launch(SettingsActivity.class))
        {
            onView(withText(R.string.settings_screen_defaults)).perform(
                click());
            onView(withText(R.string.list_pref_jump_buttons_title)).check(
                matches(isDisplayed()));
            waitForUi();
            capture("6_Screenshot_settings.png");
        }
    }
    
    private void openShoppingList()
    {
        onView(withId(R.id.lVLists)).perform(actionOnItemAtPosition(0, click()));
    }
    
    private ActivityScenario<ListActivity> launchList(String id)
    {
        Intent intent = new Intent(ctx, ListActivity.class);
        intent.putExtra("id", id);
        return ActivityScenario.launch(intent);
    }
    
    private void enableShoppingOverview() throws Exception
    {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
            .putString("font_size", "0.8").apply();
        SecureStorage storage = new SecureStorage(ctx);
        SyncedList shopping = storage.getList("shopping");
        shopping.getHeader().setOverviewActive(true);
        storage.setList(shopping);
    }
    
    private void seedScreenshotData() throws Exception
    {
        SecureStorage storage = new SecureStorage(ctx);
        ArrayList<ListTag> allTags = storage.getAllTags();
        storage.saveAllTags(allTags, false);
        
        storage.addList(createList("shopping", "Shoppinglist", "Shopping",
            "#ff8a65", true,
            new Item("Bread", false), new Item("Toast", false),
            new Item("Tomatoes", false), new Item("Potatoes", false),
            new Item("Bananas", false), new Item("Pasta", false),
            new Item("Rice", false), new Item("Apples", true),
            new Item("Broccoli", true), new Item("Salat", true),
            new Item("Cheese", true), new Item("Cookies", true),
            new Item("Eggs", true)));
        storage.addList(createList("vacations", "Vacations", "Favorites",
            "#f06292", false, new Item("Book hotel", false)));
        storage.addList(createList("todo", "Todolist", "Favorites",
            "#f06292", false, new Item("Write release notes", false),
            new Item("Review screenshots", false),
            new Item("Publish update", false)));
        storage.addList(createList("issues", "Issue Tracker", "Projects",
            "#4fc3f7", false, new Item("Crash report", true),
            new Item("Sync retry", true), new Item("Import polish", false),
            new Item("Settings copy", false), new Item("Tablet layout", false)));
        storage.addList(createList("daily", "Daily", "Work", "#dce775",
            false, new Item("Standup", false), new Item("Email", false),
            new Item("Planning", false)));
        storage.addList(createList("notes", "Notes", "Favorites", "#f06292",
            false, new Item("Meeting", false), new Item("Ideas", false),
            new Item("Links", false)));
    }
    
    private SyncedList createList(String id, String name, String tagName,
        String tagColor, boolean jumpButtons, Item... items)
    {
        SyncedListHeader header = new SyncedListHeader(id, name, "",
            Cryptography.stringToByteArray(
                Cryptography.generatingRandomString(50)),
            Cryptography.generateAESKey());
        header.setCheckOption(true);
        header.setCheckedList(true);
        header.setJumpButtons(jumpButtons);
        header.setAutoSync(false);
        header.setTagList(tags(tagName, tagColor));
        
        ArrayList<SyncedListStep> steps = new ArrayList<>();
        for (int i = 0; i < items.length; i++)
        {
            SyncedListElement element =
                new SyncedListElement(id + "-element-" + i, items[i].name, "");
            element.setChecked(items[i].checked);
            steps.add(new SyncedListStep(element.getId(), ACTION.ADD,
                element));
        }
        return new SyncedList(header, steps);
    }
    
    private ArrayList<ListTag> tags(String name, String color)
    {
        ArrayList<ListTag> tags = new ArrayList<>();
        ListTag tag = new ListTag(name);
        tag.colorHex = color;
        tags.add(tag);
        return tags;
    }
    
    private void capture(String name) throws Exception
    {
        UiAutomation automation =
            InstrumentationRegistry.getInstrumentation().getUiAutomation();
        Bitmap bitmap = automation.takeScreenshot();
        if (bitmap == null)
        {
            throw new IllegalStateException("Screenshot capture failed");
        }
        File dir = new File(ctx.getExternalFilesDir(null),
            SCREENSHOT_DIR_NAME);
        if (!dir.exists() && !dir.mkdirs())
        {
            throw new IllegalStateException("Could not create " + dir);
        }
        File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file))
        {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
    }
    
    private void clearScreenshotDir()
    {
        File dir = new File(ctx.getExternalFilesDir(null),
            SCREENSHOT_DIR_NAME);
        if (!dir.exists())
        {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null)
        {
            return;
        }
        for (File file : files)
        {
            file.delete();
        }
    }
    
    private void waitForUi()
    {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(500);
    }
    
    private static class Item
    {
        final String name;
        final boolean checked;
        
        Item(String name, boolean checked)
        {
            this.name = name;
            this.checked = checked;
        }
    }
}
