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

import androidx.preference.PreferenceFragmentCompat;

import eu.schmidt.systems.opensyncedlists.R;

/**
 * Fragment to handle global settings.
 */
public class SettingsFragment extends PreferenceFragmentCompat
{
    
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
    }
}