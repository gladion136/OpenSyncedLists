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
package eu.schmidt.systems.opensyncedlists.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import android.view.MenuItem;

import androidx.annotation.NonNull;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.fragments.SettingsFragment;
import eu.schmidt.systems.opensyncedlists.helpers.LocaleHelper;

/**
 * Activity to handle global settings
 */
public class SettingsActivity extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    public static final String EXTRA_ROOT_KEY = "root_key";
    private SettingsFragment settingsFragment;

    @Override protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase));
    }

    /**
     * onCreate initialize the view, fragment and PreferenceManager.
     *
     * @param savedInstanceState
     */
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null)
        {
            settingsFragment = new SettingsFragment();
            String rootKey = getIntent().getStringExtra(EXTRA_ROOT_KEY);
            if (rootKey != null)
            {
                Bundle args = new Bundle();
                args.putString(
                    androidx.preference.PreferenceFragmentCompat.ARG_PREFERENCE_ROOT,
                    rootKey);
                settingsFragment.setArguments(args);
            }
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, settingsFragment).commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Route Back / Up first to the preference fragment so it can pop a
     * subscreen before the activity itself finishes.
     */
    @Override public void onBackPressed()
    {
        if (settingsFragment != null && settingsFragment.onBackPressed())
        {
            return;
        }
        super.onBackPressed();
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * onSharedPreferenceChanged use the changed preferences
     *
     * @param sharedPreferences SharedPreferences
     * @param key               Key of changed preference
     */
    @Override public void onSharedPreferenceChanged(
        SharedPreferences sharedPreferences, String key)
    {
        if (key != null && key.equals("design"))
        {
            if (sharedPreferences.getString("design", "")
                .equals(getString(R.string.pref_design_light)))
            {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO);
            }
            else if (sharedPreferences.getString("design", "")
                .equals(getString(R.string.pref_design_dark)))
            {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES);
            }
            else
            {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        }
        else if (key != null && key.equals("language"))
        {
            recreate();
        }
    }
}
