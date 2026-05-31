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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import org.json.JSONException;

import java.io.IOException;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.activities.ListsActivity;
import eu.schmidt.systems.opensyncedlists.network.ServerException;
import eu.schmidt.systems.opensyncedlists.network.ServerWrapper;
import eu.schmidt.systems.opensyncedlists.storages.SecureStorage;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.utils.Constant;
import eu.schmidt.systems.opensyncedlists.utils.DialogBuilder;
import eu.schmidt.systems.opensyncedlists.utils.PreferenceScreenNavigator;

/**
 * Fragment to handle the view of one list settings
 */
public class ListSettingsFragment extends PreferenceFragmentCompat
{
    SyncedList syncedList;
    SecureStorage secureStorage;
    private final PreferenceScreenNavigator navigator =
        new PreferenceScreenNavigator();

    /**
     * Pass the preference list to show.
     *
     * @param savedInstanceState not used
     * @param rootKey            RootKey
     */
    @Override public void onCreatePreferences(Bundle savedInstanceState,
        String rootKey)
    {
        setPreferencesFromResource(R.xml.preferences_list, rootKey);
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
    
    /**
     * Initialize the fragment, add listeners, fill preferences with current
     * data.
     *
     * @param inflater           Just used for super call.
     * @param container          Just used for super call.
     * @param savedInstanceState Just used for super call.
     * @return the View with content and listeners
     */
    @Override public View onCreateView(@NonNull LayoutInflater inflater,
        @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        // Init Storage and read current list
        secureStorage = new SecureStorage(getContext());
        try
        {
            syncedList = secureStorage.getList(getArguments().getString("id"));
        }
        catch (Exception e)
        {
            Log.e(Constant.LOG_TITLE_DEFAULT, "Local storage read error: " + e);
            e.printStackTrace();
        }
        
        // Set listeners for preferences
        EditTextPreference editTextPreference = findPreference("list_name");
        Preference deleteBtn = findPreference("delete_btn");
        SwitchPreferenceCompat checkOptionPref = findPreference("check_option");
        SwitchPreferenceCompat checkListPref = findPreference("checked_list");
        SwitchPreferenceCompat invertElementPref =
            findPreference("invert_element");
        SwitchPreferenceCompat autoSyncPref = findPreference("auto_sync");
        SwitchPreferenceCompat jumpButtons = findPreference("jump_buttons");
        EditTextPreference serverNamePref = findPreference("server_name");
        Preference deleteOnlineBtn = findPreference("delete_online_btn");
        
        editTextPreference.setText(syncedList.getName());
        editTextPreference.setOnPreferenceChangeListener(
            (preference, newValue) ->
            {
                String newName = (String) newValue;
                if (!newName.equals("") && !newName.equals(
                    syncedList.getName()))
                {
                    syncedList.setName(newName);
                    save();
                }
                return true;
            });
        
        deleteBtn.setOnPreferenceClickListener(v ->
        {
            DialogBuilder.confirmDialog(getContext(),
                getString(R.string.confirm_action_title),
                getString(R.string.confirm_delete_list_msg),
                getString(R.string.confirm_action_yes),
                getString(R.string.confirm_action_no), () ->
                {
                    try
                    {
                        secureStorage.deleteList(syncedList.getId());
                    }
                    catch (Exception exception)
                    {
                        exception.printStackTrace();
                    }
                    Intent intent =
                        new Intent(getContext(), ListsActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                });
            return true;
        });
        
        checkOptionPref.setChecked(syncedList.getHeader().isCheckOption());
        checkOptionPref.setOnPreferenceChangeListener(((preference, newValue) ->
        {
            boolean newVal = (boolean) newValue;
            if (newVal != syncedList.getHeader().isCheckOption())
            {
                syncedList.getHeader().setCheckOption(newVal);
                syncedList.getHeader().setCheckedList(newVal);
                checkListPref.setChecked(
                    syncedList.getHeader().isCheckedList());
                syncedList.recalculateBuffers();
                save();
            }
            return true;
        }));
        
        checkListPref.setChecked(syncedList.getHeader().isCheckedList());
        checkListPref.setOnPreferenceChangeListener(((preference, newValue) ->
        {
            boolean newVal = (boolean) newValue;
            if (newVal != syncedList.getHeader().isCheckedList())
            {
                syncedList.getHeader().setCheckedList(newVal);
                syncedList.recalculateBuffers();
                save();
            }
            return true;
        }));
        
        invertElementPref.setChecked(syncedList.getHeader().isInvertElement());
        invertElementPref.setOnPreferenceChangeListener(
            ((preference, newValue) ->
            {
                boolean newVal = (boolean) newValue;
                if (newVal != syncedList.getHeader().isInvertElement())
                {
                    syncedList.getHeader().setInvertElement(newVal);
                    save();
                }
                return true;
            }));
        
        autoSyncPref.setChecked(syncedList.getHeader().isAutoSync());
        autoSyncPref.setOnPreferenceChangeListener(((preference, newValue) ->
        {
            boolean newVal = (boolean) newValue;
            if (newVal != syncedList.getHeader().isAutoSync())
            {
                syncedList.getHeader().setAutoSync(newVal);
                save();
            }
            return true;
        }));
        
        jumpButtons.setChecked(syncedList.getHeader().isJumpButtons());
        jumpButtons.setOnPreferenceChangeListener(((preference, newValue) ->
        {
            boolean newVal = (boolean) newValue;
            if (newVal != syncedList.getHeader().isJumpButtons())
            {
                syncedList.getHeader().setJumpButtons(newVal);
                save();
            }
            return true;
        }));
        
        serverNamePref.setText(syncedList.getHeader().getHostname());
        serverNamePref.setOnPreferenceChangeListener(((preference, newValue) ->
        {
            String newHost = (String) newValue;
            if (!newHost.equals(syncedList.getHeader().getHostname()))
            {
                syncedList.getHeader().setHostname(newHost);
                save();
            }
            return true;
        }));
        
        deleteOnlineBtn.setOnPreferenceClickListener(v ->
        {
            DialogBuilder.confirmDialog(getContext(),
                getString(R.string.confirm_action_title),
                getString(R.string.confirm_delete_list_online_msg),
                getString(R.string.confirm_action_yes),
                getString(R.string.confirm_action_no), () ->
                    ServerWrapper.removeList(
                        syncedList.getHeader().getHostname(),
                        syncedList.getId(), syncedList.getSecret(),
                        (jsonResult, exceptionFromServer) ->
                        {
                            if (jsonResult == null
                                || exceptionFromServer != null)
                            {
                                if (exceptionFromServer
                                    instanceof ServerException)
                                {
                                    Toast.makeText(getContext(),
                                        getString(R.string.unexpected_error),
                                        Toast.LENGTH_LONG).show();
                                }
                                else
                                {
                                    Toast.makeText(getContext(),
                                        getString(R.string.no_connection),
                                        Toast.LENGTH_LONG).show();
                                }
                            }
                            else
                            {
                                try
                                {
                                    secureStorage.deleteList(
                                        syncedList.getId());
                                }
                                catch (Exception exception)
                                {
                                    exception.printStackTrace();
                                }
                                Intent intent = new Intent(getContext(),
                                    ListsActivity.class);
                                startActivity(intent);
                                getActivity().finish();
                            }
                        }));
            return true;
        });
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    
    /**
     * Save the list to storage and handle exceptions.
     */
    private void save()
    {
        try
        {
            secureStorage.setList(syncedList);
        }
        catch (IOException | JSONException e)
        {
            Log.e(Constant.LOG_TITLE_DEFAULT,
                "Local storage " + "write" + " error: " + e);
            e.printStackTrace();
        }
    }
}