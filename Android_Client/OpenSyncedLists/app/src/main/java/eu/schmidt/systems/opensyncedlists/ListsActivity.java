package eu.schmidt.systems.opensyncedlists;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.adapter.ListsAdapter;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.utils.Constant;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;
import eu.schmidt.systems.opensyncedlists.utils.DialogBuilder;
import eu.schmidt.systems.opensyncedlists.utils.LocalStorage;
import eu.schmidt.systems.opensyncedlists.utils.ServerConnection;

/**
 * ListsActivity displays all lists on device
 */
public class ListsActivity extends AppCompatActivity {

    LocalStorage localStorage;
    ArrayList<SyncedListHeader> syncedListsHeaders;
    FloatingActionButton fab;

    ListView lVLists;
    ListsAdapter listsAdapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lists);
        lVLists = findViewById(R.id.lVLists);
        fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(v -> showCreateListDialog());
        init();
    }

    public String getUniqueListId() {
        String newId = Cryptography.generatingRandomString(50);
        for (int i = 0; i < syncedListsHeaders.size(); i++) {
            if (newId.equals(syncedListsHeaders.get(i).getId())) {
                i = -1;
                newId = Cryptography.generatingRandomString(50);
            }
        }
        return newId;
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.all_lists_menu, menu);
        return true;
    }

    public void init() {
        localStorage = new LocalStorage(this);
        try {
            syncedListsHeaders = localStorage.getListsHeaders();
        } catch (JSONException e) {
            Log.e(Constant.LOG_TITLE_DEFAULT, "Local storage read error: " + e);
            e.printStackTrace();
        }
        initServerConnection();
        listsAdapter = new ListsAdapter(this, R.layout.lists_element,
                                        (ArrayList<SyncedListHeader>) syncedListsHeaders
                                                .clone());
        lVLists.setAdapter(listsAdapter);

        // Read and use preferences
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getString("design", "").equals(getString(R.string.pref_design_light))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }else if(sharedPreferences.getString("design", "").equals(getString(R.string.pref_design_dark))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public void initServerConnection() {
        ServerConnection.check_connection((jsonObject, exception) -> {
            if (jsonObject == null) {
                Log.e(Constant.LOG_TITLE_DEFAULT,
                      "No connection to server: " + exception);
                return;
            }

            try {
                if (jsonObject.getString("status").equals("OK")) {
                    Log.d(Constant.LOG_TITLE_DEFAULT, "Connection is good!");
                    /**
                     * Next code here !!
                     * --------------------------------------------
                     */
                } else {
                    Log.e(Constant.LOG_TITLE_DEFAULT, "Connection is bad: " +
                            jsonObject.getString("status"));
                }
            } catch (JSONException e) {
                Log.e(Constant.LOG_TITLE_DEFAULT, "Connection is bad.. " + e);
            }
        });
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_list:
                showCreateListDialog();
                return true;
            case R.id.settings:
                Intent settingsIntent = new Intent(this,
                                                   SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Show and handle create list dialog
     */
    public void showCreateListDialog() {
        DialogBuilder.editTextDialog(this, "Create new " + "list",
                                     "Enter list name:", "Create list",
                                     "Cancel", result -> {
                    if (result != null) {
                        if (result.equals("")) {
                            Toast.makeText(this,
                                           "Please enter a name " + "first",
                                           Toast.LENGTH_SHORT).show();
                            return;
                        }
                        SyncedList newList = new SyncedList(
                                new SyncedListHeader(getUniqueListId(), result,
                                                     null, null),
                                new ArrayList<>());
                        newList.setSecret(
                                Cryptography.generatingRandomString(50));
                        newList.setLocalSecret(
                                Cryptography.generatingRandomString(50));
                        syncedListsHeaders.add(newList.getSyncedListHeader());
                        try {
                            localStorage.setList(newList);
                            localStorage.setListsHeaders(syncedListsHeaders);
                            listsAdapter.updateItems(syncedListsHeaders);
                        } catch (Exception e) {
                            Log.e(Constant.LOG_TITLE_DEFAULT,
                                  "Local storage write" + " error: " + e);
                            e.printStackTrace();
                        }
                    }
                });
    }
}