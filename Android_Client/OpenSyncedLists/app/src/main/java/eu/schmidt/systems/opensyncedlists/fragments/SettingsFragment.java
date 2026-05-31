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
package eu.schmidt.systems.opensyncedlists.fragments;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.utils.PreferenceScreenNavigator;

/**
 * Fragment to handle global settings.
 *
 * The preferences are organised as nested {@link PreferenceScreen}s. Navigation
 * between the root screen and its subscreens is handled by
 * {@link PreferenceScreenNavigator} so everything stays inside this single
 * fragment.
 */
public class SettingsFragment extends PreferenceFragmentCompat
{
    private final PreferenceScreenNavigator navigator =
        new PreferenceScreenNavigator();

    /**
     * Pass the preference list to handle.
     *
     * @param savedInstanceState not used
     * @param rootKey            RootKey
     */
    @Override public void onCreatePreferences(Bundle savedInstanceState,
        String rootKey)
    {
        setPreferencesFromResource(R.xml.preferences_root, rootKey);
        navigator.bind(this);
    }

    @Override public boolean onPreferenceTreeClick(Preference preference)
    {
        if (preference instanceof PreferenceScreen)
        {
            navigator.navigateTo((PreferenceScreen) preference);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    /**
     * Handle a Back press: pop to the previous (parent) screen if we are
     * currently inside a subscreen.
     *
     * @return true if a subscreen was popped and the Back press was consumed.
     */
    public boolean onBackPressed()
    {
        return navigator.navigateBack();
    }
}
