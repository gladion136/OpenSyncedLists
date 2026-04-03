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
package eu.schmidt.systems.opensyncedlists.helpers;

import android.app.UiAutomation;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.view.View;

import androidx.preference.PreferenceManager;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.platform.app.InstrumentationRegistry;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import org.hamcrest.Matcher;

import java.io.InputStream;
import java.io.FileInputStream;

/**
 * Utility class for resetting app state before each instrumented test
 * and providing shared ViewActions used across multiple test classes.
 */
public class TestHelper {

    /** SharedPreferences file name used by SecureStorage (mirrors R.string.preference_file_key). */
    private static final String SECURE_STORAGE_PREF_KEY =
            "asdkjbIGasdad123CTDchabsacTEc89iuHC87C";

    /**
     * Clears all persistent app data and sets minimal preferences required for
     * tests to work predictably (e.g. checkboxes always visible).
     *
     * Also disables window/transition/animator animations so that Espresso can
     * interact with popup menus and dialogs without animation interference.
     *
     * @param context target app context from InstrumentationRegistry
     */
    public static void clearAll(Context context) {
        // Clear the list storage (SecureStorage)
        context.getSharedPreferences(SECURE_STORAGE_PREF_KEY, Context.MODE_PRIVATE)
                .edit().clear().commit();

        // Resolve the current version code so the changelog "What's New" dialog
        // is suppressed during tests (it would otherwise block every test after
        // clearAll() resets the shared preferences).
        int versionCode = -1;
        try {
            versionCode = (int) context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).getLongVersionCode();
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }

        // Clear global app preferences, then set defaults needed by tests:
        //   check_option=true              → CheckBoxes visible in list elements
        //   last_seen_version_code=current → suppress changelog dialog
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .clear()
                .putBoolean("check_option", true)
                .putInt("last_seen_version_code", versionCode)
                .commit();

        // Disable system animations so Espresso can interact with popups/dialogs
        // immediately after they open (no animation-idle wait needed).
        disableAnimations();
    }

    /**
     * Returns a {@link ViewAction} that clicks the child view with the given id
     * inside a RecyclerView item. Used by multiple test classes.
     */
    public static ViewAction clickChildWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "click child view with id " + id;
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.findViewById(id).performClick();
            }
        };
    }

    /**
     * Returns a {@link ViewAction} that calls {@code performClick()} directly on
     * the RecyclerView item's root view, bypassing normal touch-event dispatch.
     *
     * Use this instead of plain {@code click()} when the item root has an
     * {@code OnClickListener} but contains a focusable child (e.g. an EditText)
     * that would otherwise consume the touch event and prevent the root listener
     * from firing.
     */
    public static ViewAction clickItemRoot() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "direct performClick() on RecyclerView item root";
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.performClick();
            }
        };
    }

    /**
     * Sets all Android animation scale settings to 0 via shell commands so that
     * Espresso does not time out waiting for animation-idle state when clicking
     * popup menus or dialogs.
     *
     * Requires shell-level privileges, which UiAutomation provides automatically.
     */
    private static void disableAnimations() {
        try {
            UiAutomation auto =
                    InstrumentationRegistry.getInstrumentation().getUiAutomation();
            String[] commands = {
                    "settings put global window_animation_scale 0",
                    "settings put global transition_animation_scale 0",
                    "settings put global animator_duration_scale 0"
            };
            for (String cmd : commands) {
                ParcelFileDescriptor pfd = auto.executeShellCommand(cmd);
                // Drain output so the command is guaranteed to complete before we return
                try (InputStream in = new FileInputStream(pfd.getFileDescriptor())) {
                    byte[] buf = new byte[512];
                    //noinspection StatementWithEmptyBody
                    while (in.read(buf) != -1) { /* drain */ }
                }
            }
        } catch (Exception ignored) {
            // Running in an environment where UiAutomation isn't available — skip.
        }
    }
}
