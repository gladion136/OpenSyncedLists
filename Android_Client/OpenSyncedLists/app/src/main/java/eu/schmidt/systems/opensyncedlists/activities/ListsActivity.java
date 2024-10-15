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

import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_DEFAULT;
import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_NETWORK;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.adapters.ListsAdapter;
import eu.schmidt.systems.opensyncedlists.network.ServerException;
import eu.schmidt.systems.opensyncedlists.network.ServerWrapper;
import eu.schmidt.systems.opensyncedlists.storages.FileStorage;
import eu.schmidt.systems.opensyncedlists.storages.SecureStorage;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;
import eu.schmidt.systems.opensyncedlists.utils.DialogBuilder;

/**
 * Activity to displaying all lists stored on the device
 */
public class ListsActivity extends AppCompatActivity
{
    /** Result launcher for importing a list */
    private ActivityResultLauncher onImportLauncher;
    /** Stores the global settings */
    private SharedPreferences globalSharedPreferences;
    private SecureStorage secureStorage;
    private ArrayList<SyncedListHeader> syncedListsHeaders;
    private FloatingActionButton fab;
    private RecyclerView recyclerView;
    private ListsAdapter listsAdapter;
    
    /**
     * In onCreate the layout is set and the global Variables are initialised.
     *
     * @param savedInstanceState In this case just used for the super call.
     */
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lists);
        recyclerView = findViewById(R.id.lVLists);
        fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(v -> showCreateListDialog());
        secureStorage = new SecureStorage(this);
        onImportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result ->
            {
                // File selected?
                if (result.getResultCode() == RESULT_OK)
                {
                    Uri importFile = result.getData().getData();
                    importFile(importFile);
                }
            });
        init();
    }
    
    /**
     * Handles start events of the activity.
     *
     * @param intent started with this intent
     */
    @Override protected void onNewIntent(Intent intent)
    {
        if (intent.getType() != null && intent.getType()
            .equals("application" + "/json"))
        {
            // Started to open a json file
            Uri receivedFile = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            importFile(receivedFile);
        }
        else if (intent.getData() != null)
        {
            importListFromUrl(intent.getData());
        }
        super.onNewIntent(intent);
    }
    
    /**
     * Import list from url
     *
     * @param url url to import
     */
    private void importListFromUrl(Uri url)
    {
        // Started to open a link (import list via link)
        String id = url.getQueryParameter("id");
        String secret = url.getQueryParameter("secret");
        String localSecret = url.getQueryParameter("localSecret");
        String hostname = url.getScheme() + "://" + url.getAuthority();
        Log.d(LOG_TITLE_DEFAULT,
            "Import list via link from host: " + hostname);
        if (id != null && secret != null && localSecret != null
            && hostname != null)
        {
            byte[] encodedLocalSecret =
                Cryptography.stringToByteArray(localSecret);
            SecretKey secretKey = new SecretKeySpec(encodedLocalSecret, 0,
                encodedLocalSecret.length, "AES");
            importListFromHost(hostname, id, secret, secretKey);
        }
        else
        {
            Log.e(LOG_TITLE_DEFAULT, "Wrong query parameters");
        }
    }
    
    @Override protected void onResume()
    {
        try
        {
            syncedListsHeaders = secureStorage.getListsHeaders();
        }
        catch (Exception e)
        {
            Log.e(LOG_TITLE_DEFAULT, "Local storage read error: " + e);
            e.printStackTrace();
        }
        listsAdapter.updateItems(syncedListsHeaders, true);
        super.onResume();
    }
    
    /**
     * In onCreateOptionsMenu the menu from the ActionBar is inflated.
     *
     * @param menu Menu to inflate
     * @return true
     */
    @Override public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.all_lists_menu, menu);
        return true;
    }
    
    /**
     * onOptionsItemSelected handles the events from the ActionBar.
     *
     * @param item selected item
     * @return action handled?
     */
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.new_list:
                // Create new list
                showCreateListDialog();
                return true;
            case R.id.import_lists:
                // Import list/s from file
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                chooseFile.setType("application/json");
                Intent intent = Intent.createChooser(chooseFile,
                    getString(R.string.choose_file_to_import));
                onImportLauncher.launch(intent);
                return true;
            case R.id.import_lists_url:
                // Import list from url
                importListFromUrlDialog();
                return true;
            case R.id.export_lists:
                // Export all lists to json file
                ArrayList<SyncedList> syncedLists = new ArrayList<>();
                for (SyncedListHeader header : syncedListsHeaders)
                {
                    try
                    {
                        syncedLists.add(secureStorage.getList(header.getId()));
                    }
                    catch (Exception exception)
                    {
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
                // Open global settings
                Intent settingsIntent =
                    new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.about:
                // Show about activity
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void importListFromUrlDialog()
    {
        DialogBuilder
            .editTextDialog(this, getString(R.string.import_list_url_title),
                getString(R.string.import_list_url_msg),
                getString(R.string.import_list_url_yes),
                getString(R.string.import_list_url_cancel), result ->
                {
                    if (result != null)
                    {
                        Uri url = Uri.parse(result);
                        importListFromUrl(url);
                    }
                });
    }
    
    /**
     * Checks connection to the default server and shows Toast on error.
     */
    protected void checkServerConnection()
    {
        String defaultHostname =
            globalSharedPreferences.getString("default_server", "");
        if (defaultHostname.equals(""))
        {
            return;
        }
        ServerWrapper
            .checkConnection(defaultHostname, (jsonResult, exception) ->
            {
                if (jsonResult == null || exception != null)
                {
                    Log.e(LOG_TITLE_DEFAULT,
                        "No connection to server: " + exception);
                    Toast.makeText(this, getString(R.string.no_connection),
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(LOG_TITLE_DEFAULT, "Connection is good!");
            });
    }
    
    /**
     * Shows and handle create new list dialog.
     */
    protected void showCreateListDialog()
    {
        DialogBuilder
            .editTextDialog(this, getString(R.string.create_list_title),
                getString(R.string.create_list_msg),
                getString(R.string.create_list_yes),
                getString(R.string.create_list_cancel), result ->
                {
                    if (result != null)
                    {
                        if (result.equals(""))
                        {
                            Toast.makeText(this,
                                getString(R.string.no_name_entered),
                                Toast.LENGTH_SHORT).show();
                            return;
                        }
                        SyncedListHeader header =
                            new SyncedListHeader(getUniqueListId(), result,
                                globalSharedPreferences
                                    .getString("default_server", ""),
                                Cryptography.stringToByteArray(
                                    Cryptography.generatingRandomString(50)),
                                Cryptography.generateAESKey());
                        header.setCheckOption(globalSharedPreferences
                            .getBoolean("check_option", true));
                        header.setCheckedList(globalSharedPreferences
                            .getBoolean("checked_list", true));
                        header.setJumpButtons(globalSharedPreferences
                            .getBoolean("jump_buttons", false));
                        header.setInvertElement(globalSharedPreferences
                            .getBoolean("invert_element", false));
                        SyncedList newList =
                            new SyncedList(header, new ArrayList<>());
                        addListAndHandleCallback(newList);
                    }
                });
    }
    
    /**
     * Import new list from file.
     *
     * @param uri Filepath
     */
    protected void importFile(Uri uri)
    {
        try
        {
            InputStream in = getContentResolver().openInputStream(uri);
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; )
            {
                total.append(line).append('\n');
            }
            
            String content = total.toString();
            try
            {
                JSONArray jsonArray = new JSONArray(content);
                
                for (int i = 0; i < jsonArray.length(); i++)
                {
                    addListAndHandleCallback(
                        new SyncedList((JSONObject) jsonArray.get(i)));
                }
            }
            catch (JSONException e)
            {
                try
                {
                    JSONObject jsonObject = new JSONObject(content);
                    SyncedList importedList = new SyncedList(jsonObject);
                    addListAndHandleCallback(importedList);
                }
                catch (JSONException exception)
                {
                    Toast.makeText(this, getString(R.string.cant_import_file),
                        Toast.LENGTH_LONG).show();
                    Log.e(LOG_TITLE_DEFAULT,
                        "Cant import file: " + exception);
                }
            }
        }
        catch (IOException ignored)
        {
            ignored.printStackTrace();
        }
    }
    
    /**
     * Adds a list, save it, update views and handle exceptions.
     *
     * @param syncedList List to add
     */
    protected void addListAndHandleCallback(SyncedList syncedList)
    {
        String result;
        try
        {
            result = secureStorage.addList(syncedList);
            if (!result.equals(""))
            {
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
            }
            syncedListsHeaders = secureStorage.getListsHeaders();
            listsAdapter.updateItems(syncedListsHeaders, true);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }
    
    /**
     * Initialize fills the activity with content. (Fill list view, read
     * settings, ..)
     */
    private void init()
    {
        try
        {
            syncedListsHeaders = secureStorage.getListsHeaders();
        }
        catch (Exception e)
        {
            Log.e(LOG_TITLE_DEFAULT, "Local storage read error: " + e);
            e.printStackTrace();
        }
        
        listsAdapter = new ListsAdapter(this,
            (ArrayList<SyncedListHeader>) syncedListsHeaders.clone(),
            recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(listsAdapter);
        
        // Read and use preferences
        globalSharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        if (globalSharedPreferences.getString("design", "")
            .equals(getString(R.string.pref_design_light)))
        {
            AppCompatDelegate
                .setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        else if (globalSharedPreferences.getString("design", "")
            .equals(getString(R.string.pref_design_dark)))
        {
            AppCompatDelegate
                .setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        else
        {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
        checkServerConnection();
    }
    
    /**
     * Import a list from a server
     *
     * @param hostname    Hostname with protocoll and port
     * @param id          Id of the list
     * @param secret      Secret to access to the list
     * @param localSecret localSecret to decrypt the list
     */
    private void importListFromHost(String hostname, String id, String secret,
        SecretKey localSecret)
    {
        ServerWrapper.getList(hostname, id, secret,
            (jsonListFromServer, exceptionListFromServer) ->
            {
                if (jsonListFromServer == null
                    || exceptionListFromServer != null)
                {
                    Log.e(LOG_TITLE_NETWORK,
                        "Error: " + exceptionListFromServer.toString());
                    if (exceptionListFromServer instanceof ServerException)
                    {
                        if (exceptionListFromServer.getMessage()
                            .equals("Not found"))
                        {
                            Toast.makeText(this,
                                getString(R.string.cant_import_list) + " "
                                    + getString(R.string.not_found),
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                    else
                    {
                        Toast.makeText(this,
                            getString(R.string.cant_import_list) + getString(
                                R.string.no_connection), Toast.LENGTH_SHORT)
                            .show();
                    }
                    return;
                }
                try
                {
                    SyncedList receivedList = new SyncedList(new JSONObject(
                        Cryptography.decryptRSA(localSecret,
                            jsonListFromServer.getJSONObject("msg")
                                .getString("data"))));
                    addListAndHandleCallback(receivedList);
                }
                catch (JSONException e)
                {
                    // Shouldn't entered if the server
                    // worked fine
                    Log.e(LOG_TITLE_NETWORK, e.toString());
                    e.printStackTrace();
                }
            });
    }
    
    /**
     * Generate a local unique list id
     *
     * @return unique list name
     */
    private String getUniqueListId()
    {
        String newId = Cryptography.generatingRandomString(50);
        for (int i = 0; i < syncedListsHeaders.size(); i++)
        {
            if (newId.equals(syncedListsHeaders.get(i).getId()))
            {
                i = -1;
                newId = Cryptography.generatingRandomString(50);
            }
        }
        return newId;
    }
}