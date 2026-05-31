/*
 * Copyright (C) 2021  Etienne Schmidt (eschmidt@schmidt-ti.eu)
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
package eu.schmidt.systems.opensyncedlists.utils;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Drives navigation between nested {@link PreferenceScreen}s within a single
 * {@link PreferenceFragmentCompat}.
 *
 * AndroidX preference 1.2.x does not navigate into a nested PreferenceScreen on
 * its own unless the host implements the screen-start callback. This helper
 * swaps the displayed screen via {@link PreferenceFragmentCompat#setPreferenceScreen}
 * and keeps a back-stack so Back returns to the parent screen, updating the
 * activity title to match the current screen.
 */
public class PreferenceScreenNavigator
{
    private PreferenceFragmentCompat fragment;
    private PreferenceScreen rootScreen;
    private CharSequence rootTitle;
    private final Deque<PreferenceScreen> backStack = new ArrayDeque<>();

    /**
     * Bind to the fragment after its preferences have been inflated. Remembers
     * the root screen and the activity's current title so they can be restored.
     *
     * @param fragment the host preference fragment
     */
    public void bind(PreferenceFragmentCompat fragment)
    {
        this.fragment = fragment;
        this.rootScreen = fragment.getPreferenceScreen();
        if (fragment.getActivity() != null)
        {
            this.rootTitle = fragment.getActivity().getTitle();
        }
    }

    /**
     * Show the given subscreen and remember the previous one for Back.
     *
     * @param screen subscreen to display
     */
    public void navigateTo(PreferenceScreen screen)
    {
        if (fragment == null)
        {
            return;
        }
        backStack.push(fragment.getPreferenceScreen());
        fragment.setPreferenceScreen(screen);
        applyTitle(screen.getTitle());
    }

    /**
     * Navigate to a subscreen identified by its key. The key is resolved
     * against the root screen, so this works even while another subscreen is
     * currently displayed (the displayed screen would not contain sibling
     * subscreens).
     *
     * @param key key of the target PreferenceScreen
     * @return true if a matching subscreen was found and shown
     */
    public boolean navigateToKey(String key)
    {
        if (rootScreen == null)
        {
            return false;
        }
        Preference target = rootScreen.findPreference(key);
        if (target instanceof PreferenceScreen)
        {
            navigateTo((PreferenceScreen) target);
            return true;
        }
        return false;
    }

    /**
     * Pop back to the previous screen.
     *
     * @return true if a screen was popped (Back consumed); false if already at
     * the root screen.
     */
    public boolean navigateBack()
    {
        if (fragment == null || backStack.isEmpty())
        {
            return false;
        }
        PreferenceScreen previous = backStack.pop();
        fragment.setPreferenceScreen(previous);
        applyTitle(backStack.isEmpty() ? rootTitle : previous.getTitle());
        return true;
    }

    /**
     * Update the activity title to reflect the current screen.
     *
     * @param title title to apply (falls back to the remembered root title)
     */
    private void applyTitle(CharSequence title)
    {
        if (fragment == null || fragment.getActivity() == null)
        {
            return;
        }
        fragment.getActivity().setTitle(title != null ? title : rootTitle);
    }
}
