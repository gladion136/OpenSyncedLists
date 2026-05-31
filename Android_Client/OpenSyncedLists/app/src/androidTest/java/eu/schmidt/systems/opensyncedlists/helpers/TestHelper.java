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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.UiAutomation;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.view.View;

import androidx.preference.PreferenceManager;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.platform.app.InstrumentationRegistry;

import eu.schmidt.systems.opensyncedlists.activities.ListsActivity;

import org.hamcrest.Matcher;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Utility class for resetting app state before each instrumented test and
 * providing shared ViewActions used across multiple test classes.
 */
public class TestHelper
{
    
    /**
     * SharedPreferences file name used by SecureStorage (mirrors
     * R.string.preference_file_key).
     */
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
    /**
     * Presses Back until the view with {@code rootViewId} (e.g. lVLists) is
     * displayed, up to a maximum of 3 times.  Stops immediately when the root
     * screen is already visible so that it never presses Back on the task root
     * activity (which would finish it on some devices).
     */
    public static void navigateToListsActivity(int rootViewId)
    {
        for (int i = 0; i < 3; i++)
        {
            try
            {
                onView(withId(rootViewId)).check(matches(isDisplayed()));
                return; // already on the right screen
            }
            catch (NoMatchingViewException ignored)
            {
                pressBack();
            }
        }
    }

    public static void clearAll(Context context)
    {
        // Clear the list storage (SecureStorage)
        context.getSharedPreferences(SECURE_STORAGE_PREF_KEY,
            Context.MODE_PRIVATE).edit().clear().commit();
        
        // Resolve the current version code so the changelog "What's New" dialog
        // is suppressed during tests (it would otherwise block every test after
        // clearAll() resets the shared preferences).
        int versionCode = -1;
        try
        {
            versionCode = (int) context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0)
                .getLongVersionCode();
        }
        catch (android.content.pm.PackageManager.NameNotFoundException ignored)
        {
        }
        
        // Clear global app preferences, then set defaults needed by tests:
        //   check_option=true              → CheckBoxes visible in list elements
        //   last_seen_version_code=current → suppress changelog dialog
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear()
            .putBoolean("check_option", true)
            .putInt("last_seen_version_code", versionCode).commit();

        // Reset the one-shot auto-open flag so each test class gets a clean
        // slate and the feature can be verified in testOpenLastListAndSettingsNavigation.
        ListsActivity.resetAutoOpenForTesting();
        
        // Disable system animations so Espresso can interact with
        // popups/dialogs
        // immediately after they open (no animation-idle wait needed).
        disableAnimations();
    }
    
    /**
     * Sets the global "font_size" preference scale factor (e.g. "0.8", "1.0").
     * Must be called before the list activity / adapter is created, because the
     * adapter reads the value once at construction time.
     *
     * @param context target app context
     * @param scale   scale factor as stored string
     */
    public static void setFontSizePref(Context context, String scale)
    {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString("font_size", scale).commit();
    }

    /**
     * Returns a {@link ViewAction} that records the scaled text size (px) of
     * the child TextView/EditText with id {@code childId} into {@code out[0]}.
     * Use with RecyclerViewActions on a list item.
     */
    public static ViewAction captureChildTextSizePx(final int childId,
        final float[] out)
    {
        return new ViewAction()
        {
            @Override public Matcher<View> getConstraints()
            {
                return isDisplayed();
            }

            @Override public String getDescription()
            {
                return "capture child text size (px) for id " + childId;
            }

            @Override public void perform(UiController uiController, View view)
            {
                android.widget.TextView tv = view.findViewById(childId);
                out[0] = tv.getTextSize();
            }
        };
    }

    /**
     * Clicks the positive button of a confirmation {@link AlertDialog} (the
     * dialog shown before dangerous actions like check-all, clear list or
     * delete list). Use right after triggering such an action.
     */
    public static void confirmDialog()
    {
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
    }

    /**
     * Clicks the negative button of a confirmation {@link AlertDialog} to
     * cancel a dangerous action without executing it.
     */
    public static void cancelDialog()
    {
        onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click());
    }

    /**
     * Asserts that a confirmation dialog showing {@code message} is currently
     * displayed (used to verify the affected-item count in the message), then
     * confirms it by clicking the positive button.
     *
     * @param message the exact dialog message text expected (incl. the count)
     */
    public static void confirmDialogWithMessage(String message)
    {
        onView(withText(message)).inRoot(isDialog())
            .check(matches(isDisplayed()));
        confirmDialog();
    }

    /**
     * Asserts that NO confirmation dialog is currently shown. Used to verify
     * that a no-op action (0 affected elements) shows a Toast instead of a
     * dialog. The confirm dialog is identified by its title
     * (R.string.confirm_action_title).
     *
     * Does NOT use Espresso's onView/inRoot, because matching against a dialog
     * root that does not exist makes Espresso wait on the root (and a freshly
     * shown Toast window) until it times out / hangs. Instead it inspects the
     * decor views of all running activities directly on the main thread.
     *
     * @param context target app context to resolve the title string
     */
    public static void assertNoConfirmDialog(Context context)
    {
        String title = context.getString(
            eu.schmidt.systems.opensyncedlists.R.string.confirm_action_title);
        boolean[] found = {false};
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
        {
            // The AlertDialog lives in its own window, not in the activity
            // decor tree, so iterate over ALL root views currently attached to
            // the WindowManager (activity decor + dialog + toast windows).
            for (View root : getWindowManagerRootViews())
            {
                if (viewTreeHasText(root, title))
                {
                    found[0] = true;
                }
            }
        });
        if (found[0])
        {
            throw new AssertionError(
                "Expected no confirm dialog, but one was displayed");
        }
    }

    /**
     * Returns all root views currently attached to the global WindowManager.
     * Uses reflection on WindowManagerGlobal (test-only). This sees dialog and
     * toast windows that are not part of any activity's decor view tree.
     */
    @SuppressWarnings("unchecked")
    private static java.util.List<View> getWindowManagerRootViews()
    {
        java.util.List<View> roots = new java.util.ArrayList<>();
        try
        {
            Class<?> wmgClass =
                Class.forName("android.view.WindowManagerGlobal");
            Object wmg = wmgClass.getMethod("getInstance").invoke(null);
            java.lang.reflect.Field viewsField =
                wmgClass.getDeclaredField("mViews");
            viewsField.setAccessible(true);
            Object views = viewsField.get(wmg);
            if (views instanceof java.util.List)
            {
                roots.addAll((java.util.List<View>) views);
            }
            else if (views instanceof View[])
            {
                java.util.Collections.addAll(roots, (View[]) views);
            }
        }
        catch (Exception ignored)
        {
            // Reflection failed (API change) — return empty; assertion then
            // can't detect a dialog and will not false-fail.
        }
        return roots;
    }

    /**
     * Recursively checks whether the view tree under {@code root} contains a
     * TextView whose text equals {@code text}.
     */
    private static boolean viewTreeHasText(View root, String text)
    {
        if (root instanceof android.widget.TextView)
        {
            CharSequence t = ((android.widget.TextView) root).getText();
            if (t != null && text.contentEquals(t))
            {
                return true;
            }
        }
        if (root instanceof android.view.ViewGroup)
        {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++)
            {
                if (viewTreeHasText(group.getChildAt(i), text))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a {@link ViewAction} that clicks the child view with the given id
     * inside a RecyclerView item. Used by multiple test classes.
     */
    public static ViewAction clickChildWithId(final int id)
    {
        return new ViewAction()
        {
            @Override public Matcher<View> getConstraints()
            {
                return null;
            }
            
            @Override public String getDescription()
            {
                return "click child view with id " + id;
            }
            
            @Override public void perform(UiController uiController, View view)
            {
                view.findViewById(id).performClick();
            }
        };
    }
    
    /**
     * Returns a {@link ViewAction} that calls {@code performClick()} directly
     * on the RecyclerView item's root view, bypassing normal touch-event
     * dispatch.
     *
     * Use this instead of plain {@code click()} when the item root has an
     * {@code OnClickListener} but contains a focusable child (e.g. an EditText)
     * that would otherwise consume the touch event and prevent the root
     * listener from firing.
     */
    public static ViewAction clickItemRoot()
    {
        return new ViewAction()
        {
            @Override public Matcher<View> getConstraints()
            {
                return isDisplayed();
            }
            
            @Override public String getDescription()
            {
                return "direct performClick() on RecyclerView item root";
            }
            
            @Override public void perform(UiController uiController, View view)
            {
                view.performClick();
            }
        };
    }
    
    /**
     * Sets all Android animation scale settings to 0 via shell commands so that
     * Espresso does not time out waiting for animation-idle state when clicking
     * popup menus or dialogs.
     *
     * Requires shell-level privileges, which UiAutomation provides
     * automatically.
     */
    private static void disableAnimations()
    {
        try
        {
            UiAutomation auto =
                InstrumentationRegistry.getInstrumentation().getUiAutomation();
            String[] commands = {"settings put global window_animation_scale 0",
                "settings put global transition_animation_scale 0",
                "settings put global animator_duration_scale 0"};
            for (String cmd : commands)
            {
                ParcelFileDescriptor pfd = auto.executeShellCommand(cmd);
                // Drain output so the command is guaranteed to complete
                // before we return
                try (InputStream in = new FileInputStream(
                    pfd.getFileDescriptor()))
                {
                    byte[] buf = new byte[512];
                    //noinspection StatementWithEmptyBody
                    while (in.read(buf) != -1)
                    { /* drain */ }
                }
            }
        }
        catch (Exception ignored)
        {
            // Running in an environment where UiAutomation isn't available —
            // skip.
        }
    }
}
