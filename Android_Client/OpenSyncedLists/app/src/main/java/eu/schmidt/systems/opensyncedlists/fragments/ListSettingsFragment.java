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
import androidx.preference.SwitchPreferenceCompat;

import org.json.JSONException;

import java.io.IOException;

import eu.schmidt.systems.opensyncedlists.ListsActivity;
import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;
import eu.schmidt.systems.opensyncedlists.exceptions.ServerException;
import eu.schmidt.systems.opensyncedlists.utils.Constant;
import eu.schmidt.systems.opensyncedlists.utils.SecureStorage;
import eu.schmidt.systems.opensyncedlists.utils.ServerConnection;

public class ListSettingsFragment extends PreferenceFragmentCompat {

    SyncedList syncedList;
    SecureStorage secureStorage;

    public static void SettingsFragment() {

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.list_preferences, rootKey);
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                       @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {
        // Init Storage and read current list
        secureStorage = new SecureStorage(getContext());
        try {
            syncedList = secureStorage.getList(getArguments().getString("id"));
        } catch (Exception e) {
            Log.e(Constant.LOG_TITLE_DEFAULT, "Local storage read error: " + e);
            e.printStackTrace();
        }

        // Set listeners for preferences
        EditTextPreference editTextPreference = findPreference("list_name");
        editTextPreference.setText(syncedList.getName());
        editTextPreference
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    String newName = (String) newValue;
                    if (!newName.equals("") &&
                            !newName.equals(syncedList.getName())) {
                        syncedList.setName(newName);
                        save();
                    }
                    return true;
                });

        Preference deleteBtn = findPreference("delete_btn");
        deleteBtn.setOnPreferenceClickListener(v -> {
            try {
                secureStorage.deleteList(syncedList.getId());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            Intent intent = new Intent(getContext(), ListsActivity.class);
            startActivity(intent);
            getActivity().finish();
            return true;
        });

        SwitchPreferenceCompat checkOptionPref = findPreference("check_option");
        checkOptionPref.setChecked(syncedList.getHeader().isCheckOption());
        checkOptionPref
                .setOnPreferenceChangeListener(((preference, newValue) -> {
                    boolean newVal = (boolean) newValue;
                    if (newVal != syncedList.getHeader().isCheckOption()) {
                        syncedList.getHeader().setCheckOption(newVal);
                        save();
                    }
                    return true;
                }));

        SwitchPreferenceCompat checkListPref = findPreference("checked_list");
        checkListPref.setChecked(syncedList.getHeader().isCheckedList());
        checkListPref.setOnPreferenceChangeListener(((preference, newValue) -> {
            boolean newVal = (boolean) newValue;
            if (newVal != syncedList.getHeader().isCheckedList()) {
                syncedList.getHeader().setCheckedList(newVal);
                save();
            }
            return true;
        }));

        SwitchPreferenceCompat invertElementPref = findPreference(
                "invert_element");
        invertElementPref.setChecked(syncedList.getHeader().isInvertElement());
        invertElementPref.setOnPreferenceChangeListener(((preference, newValue) -> {
            boolean newVal = (boolean) newValue;
            if (newVal != syncedList.getHeader().isInvertElement()) {
                syncedList.getHeader().setInvertElement(newVal);
                save();
            }
            return true;
        }));

        SwitchPreferenceCompat autoSyncPref = findPreference("auto_sync");
        autoSyncPref.setChecked(syncedList.getHeader().isAutoSync());
        autoSyncPref.setOnPreferenceChangeListener(((preference, newValue) -> {
            boolean newVal = (boolean) newValue;
            if (newVal != syncedList.getHeader().isAutoSync()) {
                syncedList.getHeader().setAutoSync(newVal);
                save();
            }
            return true;
        }));

        EditTextPreference serverNamePref = findPreference("server_name");
        serverNamePref.setText(syncedList.getHeader().getHostname());
        serverNamePref
                .setOnPreferenceChangeListener(((preference, newValue) -> {
                    String newHost = (String) newValue;
                    if (!newHost.equals(syncedList.getHeader().getHostname())) {
                        syncedList.getHeader().setHostname(newHost);
                        save();
                    }
                    return true;
                }));

        Preference deleteOnlineBtn = findPreference("delete_online_btn");
        deleteOnlineBtn.setOnPreferenceClickListener(v -> {
            ServerConnection.removeList(syncedList.getHeader().getHostname(),
                                        syncedList.getId(),
                                        syncedList.getSecret(), (jsonResult,
                                                                 exceptionFromServer) -> {
                        if(jsonResult == null || exceptionFromServer != null) {
                            if(exceptionFromServer instanceof ServerException) {
                                Toast.makeText(getContext(), "Something wrent " +
                                                       "wrong!",
                                               Toast.LENGTH_LONG).show();
                            }else {
                                Toast.makeText(getContext(), "No connection " +
                                                       "to server",
                                               Toast.LENGTH_LONG).show();
                            }
                        }else {
                            try {
                                secureStorage.deleteList(syncedList.getId());
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                            Intent intent = new Intent(getContext(), ListsActivity.class);
                            startActivity(intent);
                            getActivity().finish();
                        }
                    });
            return true;
        });

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public boolean save() {
        try {
            secureStorage.setList(syncedList, true);
        } catch (IOException | JSONException e) {
            Log.e(Constant.LOG_TITLE_DEFAULT,
                  "Local storage " + "write" + " error: " + e);
            e.printStackTrace();
            return false;
        }
        return true;
    }
}