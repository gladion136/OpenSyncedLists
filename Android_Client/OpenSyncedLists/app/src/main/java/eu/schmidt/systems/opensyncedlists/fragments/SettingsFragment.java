package eu.schmidt.systems.opensyncedlists.fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import eu.schmidt.systems.opensyncedlists.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_root, rootKey);
    }
}