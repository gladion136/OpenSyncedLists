package eu.schmidt.systems.opensyncedlists;

import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_DEFAULT;
import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_STORAGE;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.adapter.ListsAdapter;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;
import eu.schmidt.systems.opensyncedlists.utils.DialogBuilder;
import eu.schmidt.systems.opensyncedlists.utils.FileStorage;
import eu.schmidt.systems.opensyncedlists.utils.SecureStorage;
import eu.schmidt.systems.opensyncedlists.utils.ServerConnection;

/**
 * ListsActivity displays all lists on device
 */
public class ListsActivity extends AppCompatActivity {

    ActivityResultLauncher onImportLauncher;
    SecureStorage secureStorage;
    SharedPreferences globalSharedPreferences;
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

        onImportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Uri importFile = result.getData().getData();
                        importFile(importFile);
                    }
                });
    }

    @Override protected void onResume() {
        init();
        super.onResume();
    }

    @Override protected void onNewIntent(Intent intent) {
        if (intent.getType() != null && intent.getType().equals("application" +
                                                                     "/json")) {
            Uri receivedFile =
                    (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            importFile(receivedFile);
        }
        super.onNewIntent(intent);
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
        secureStorage = new SecureStorage(this);
        try {
            syncedListsHeaders = secureStorage.getListsHeaders();
        } catch (JSONException e) {
            Log.e(LOG_TITLE_DEFAULT, "Local storage read error: " + e);
            e.printStackTrace();
        }
        initServerConnection();
        listsAdapter = new ListsAdapter(this, R.layout.lists_element,
                                        (ArrayList<SyncedListHeader>) syncedListsHeaders
                                                .clone());
        lVLists.setAdapter(listsAdapter);

        // Read and use preferences
        globalSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        if (globalSharedPreferences.getString("design", "")
                .equals(getString(R.string.pref_design_light))) {
            AppCompatDelegate
                    .setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (globalSharedPreferences.getString("design", "")
                .equals(getString(R.string.pref_design_dark))) {
            AppCompatDelegate
                    .setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public void initServerConnection() {
        ServerConnection.check_connection((jsonObject, exception) -> {
            if (jsonObject == null) {
                Log.e(LOG_TITLE_DEFAULT,
                      "No connection to server: " + exception);
                return;
            }

            try {
                if (jsonObject.getString("status").equals("OK")) {
                    Log.d(LOG_TITLE_DEFAULT, "Connection is good!");
                    /**
                     * Next code here !!
                     * --------------------------------------------
                     */
                } else {
                    Log.e(LOG_TITLE_DEFAULT, "Connection is bad: " +
                            jsonObject.getString("status"));
                }
            } catch (JSONException e) {
                Log.e(LOG_TITLE_DEFAULT, "Connection is bad.. " + e);
            }
        });
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_list:
                showCreateListDialog();
                return true;
            case R.id.import_lists:
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                chooseFile.setType("application/json");
                Intent intent = Intent.createChooser(chooseFile,
                                                     "Choose a file to " +
                                                             "import");
                onImportLauncher.launch(intent);
                return true;
            case R.id.export_lists:
                ArrayList<SyncedList> syncedLists = new ArrayList<>();
                for (SyncedListHeader header : syncedListsHeaders) {
                    try {
                        syncedLists.add(secureStorage.getList(header.getId()));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
                String absolutePath =
                        FileStorage.exportLists(this, syncedLists);
                Log.i(LOG_TITLE_DEFAULT,
                      "Export all files to: " + absolutePath);
                FileStorage.shareFile(this, absolutePath);
                return true;
            case R.id.settings:
                Intent settingsIntent =
                        new Intent(this, SettingsActivity.class);
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
                                                     globalSharedPreferences
                                                             .getString(
                                                                     "default_server",
                                                                     ""), null,
                                                     null), new ArrayList<>());
                        newList.setSecret(
                                Cryptography.generatingRandomString(50));
                        newList.setLocalSecret(
                                Cryptography.generatingRandomString(50));
                        addListAndHandleCallback(newList);
                    }
                });
    }

    public void addListAndHandleCallback(SyncedList syncedList) {
        String result = null;
        try {
            result = secureStorage.addList(syncedList);
            if (!result.equals("")) {
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
            }
            syncedListsHeaders = secureStorage.getListsHeaders();
            listsAdapter.updateItems(syncedListsHeaders);
        } catch (JSONException exception) {
            exception.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * MOVE FUNCTION TO SECURESTORAGE ??
     *
     * @param uri
     */
    public void importFile(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append(line).append('\n');
            }

            String content = total.toString();
            try {
                JSONArray jsonArray = new JSONArray(content);

                for (int i = 0; i < jsonArray.length(); i++) {
                    addListAndHandleCallback(
                            new SyncedList((JSONObject) jsonArray.get(i)));
                }
            } catch (JSONException e) {
                try {
                    JSONObject jsonObject = new JSONObject(content);
                    SyncedList importedList = new SyncedList(jsonObject);
                    addListAndHandleCallback(importedList);
                } catch (JSONException exception) {
                    Toast.makeText(this, "Cant import File!", Toast.LENGTH_LONG)
                            .show();
                    Log.e(LOG_TITLE_DEFAULT,
                          "Cant import file: " + exception.toString());
                }
            }
        } catch (IOException e) {

        }
    }
}