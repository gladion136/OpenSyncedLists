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
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.storages.SecureStorage;
import eu.schmidt.systems.opensyncedlists.utils.DataStorageManager;
import eu.schmidt.systems.opensyncedlists.utils.DefaultListSettingsApplier;
import eu.schmidt.systems.opensyncedlists.utils.DialogBuilder;
import eu.schmidt.systems.opensyncedlists.utils.PreferenceScreenNavigator;

/**
 * Fragment to handle global settings.
 *
 * The preferences are organised as nested {@link PreferenceScreen}s. Navigation
 * between the root screen and its subscreens is handled by
 * {@link PreferenceScreenNavigator} so everything stays inside this single
 * fragment.
 *
 * Changing a value under "Default values for new lists" only affects newly
 * created lists. After such a change the user is offered (via two confirmation
 * dialogs) to also push the new value into all existing lists.
 */
public class SettingsFragment extends PreferenceFragmentCompat
{
    private final PreferenceScreenNavigator navigator =
        new PreferenceScreenNavigator();
    private DefaultListSettingsApplier defaultApplier;
    private DataStorageManager dataManager;

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

        defaultApplier =
            new DefaultListSettingsApplier(new SecureStorage(getContext()));
        dataManager = new DataStorageManager(getContext());
        registerDefaultListListeners();
        registerDataStorageListeners();
        registerSyncHintLinks();
    }

    /**
     * Make the two info hints on the Sync Settings screen navigate to the
     * related subscreens (Default values / Data &amp; Storage).
     */
    private void registerSyncHintLinks()
    {
        Preference hintDefaults = findPreference("sync_hint_defaults");
        if (hintDefaults != null)
        {
            hintDefaults.setOnPreferenceClickListener(p ->
            {
                navigator.navigateToKey("screen_defaults");
                return true;
            });
        }
        Preference hintData = findPreference("sync_hint_data");
        if (hintData != null)
        {
            hintData.setOnPreferenceClickListener(p ->
            {
                navigator.navigateToKey("screen_data");
                return true;
            });
        }
    }

    /**
     * Wire the buttons on the "Data / Storage" screen, each guarded by a
     * confirmation dialog.
     */
    private void registerDataStorageListeners()
    {
        Preference resetLists = findPreference("reset_lists_btn");
        if (resetLists != null)
        {
            resetLists.setOnPreferenceClickListener(p ->
            {
                int count = dataManager.countLists();
                confirm(getString(R.string.data_reset_lists_confirm, count),
                    () ->
                    {
                        dataManager.resetAllLists();
                        toast(getString(R.string.data_reset_lists_done));
                    });
                return true;
            });
        }

        Preference deleteServerLists =
            findPreference("delete_server_lists_btn");
        if (deleteServerLists != null)
        {
            deleteServerLists.setOnPreferenceClickListener(p ->
            {
                confirm(getString(R.string.data_delete_server_lists_confirm),
                    () ->
                    {
                        int n = dataManager.deleteAllListsOnServer();
                        toast(getString(R.string.data_delete_server_lists_done,
                            n));
                    });
                return true;
            });
        }

        Preference resetSettings = findPreference("reset_settings_btn");
        if (resetSettings != null)
        {
            resetSettings.setOnPreferenceClickListener(p ->
            {
                confirm(getString(R.string.data_reset_settings_confirm), () ->
                {
                    dataManager.resetAllSettings();
                    toast(getString(R.string.data_reset_settings_done));
                });
                return true;
            });
        }

        Preference resetEverything = findPreference("reset_everything_btn");
        if (resetEverything != null)
        {
            resetEverything.setOnPreferenceClickListener(p ->
            {
                confirm(getString(R.string.data_reset_everything_confirm),
                    () -> dataManager.resetEverything());
                return true;
            });
        }
    }

    /**
     * Show a single confirmation dialog using the shared title/buttons; runs
     * {@code onConfirm} only when accepted.
     */
    private void confirm(String message, Runnable onConfirm)
    {
        DialogBuilder.confirmDialog(getContext(),
            getString(R.string.confirm_action_title), message,
            getString(R.string.confirm_action_yes),
            getString(R.string.confirm_action_no), onConfirm);
    }

    private void toast(String message)
    {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Attach change listeners to the "default list" preferences so a change can
     * optionally be propagated to all existing lists.
     */
    private void registerDefaultListListeners()
    {
        String[] keys = {
            DefaultListSettingsApplier.KEY_CHECK_OPTION,
            DefaultListSettingsApplier.KEY_CHECKED_LIST,
            DefaultListSettingsApplier.KEY_JUMP_BUTTONS,
            DefaultListSettingsApplier.KEY_INVERT_ELEMENT,
            DefaultListSettingsApplier.KEY_SYNC,
            DefaultListSettingsApplier.KEY_DEFAULT_SERVER
        };
        for (String key : keys)
        {
            Preference pref = findPreference(key);
            if (pref != null)
            {
                pref.setOnPreferenceChangeListener((preference, newValue) ->
                {
                    // Persisting the new value (return true) already updates the
                    // default for new lists. Then offer to apply to all lists.
                    askApplyToAllLists(preference.getKey(), newValue);
                    return true;
                });
            }
        }
    }

    /**
     * First dialog: ask whether the changed default should also be applied to
     * all existing lists or only to new ones. Choosing "all lists" opens a
     * second confirmation dialog. Cancelling (or ignoring) either dialog leaves
     * the change applied to new lists only.
     *
     * @param key      changed preference key
     * @param newValue new preference value
     */
    private void askApplyToAllLists(String key, Object newValue)
    {
        DialogBuilder.confirmDialog(getContext(),
            getString(R.string.default_apply_title),
            getString(R.string.default_apply_msg),
            getString(R.string.default_apply_all_lists),
            getString(R.string.default_apply_new_only),
            () -> confirmApplyToAllLists(key, newValue));
    }

    /**
     * Second dialog: confirm applying to all lists, showing how many lists are
     * affected. Only when this is accepted are the existing lists updated.
     *
     * @param key      changed preference key
     * @param newValue new preference value
     */
    private void confirmApplyToAllLists(String key, Object newValue)
    {
        int count = defaultApplier.countLists();
        if (count == 0)
        {
            return;
        }
        DialogBuilder.confirmDialog(getContext(),
            getString(R.string.confirm_action_title),
            getString(R.string.default_apply_confirm_msg, count),
            getString(R.string.confirm_action_yes),
            getString(R.string.confirm_action_no), () ->
            {
                int updated = defaultApplier.applyToAllLists(key, newValue);
                Toast.makeText(getContext(),
                    getString(R.string.default_apply_done, updated),
                    Toast.LENGTH_LONG).show();
            });
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
