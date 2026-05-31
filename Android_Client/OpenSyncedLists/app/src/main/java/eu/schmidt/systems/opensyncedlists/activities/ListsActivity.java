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
import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_STORAGE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.adapters.ListsAdapter;
import eu.schmidt.systems.opensyncedlists.adapters.TagsAdapter;
import eu.schmidt.systems.opensyncedlists.network.ServerException;
import eu.schmidt.systems.opensyncedlists.network.ServerWrapper;
import eu.schmidt.systems.opensyncedlists.storages.FileStorage;
import eu.schmidt.systems.opensyncedlists.storages.SecureStorage;
import eu.schmidt.systems.opensyncedlists.syncedlist.ACTION;
import eu.schmidt.systems.opensyncedlists.syncedlist.ListTag;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListStep;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;
import eu.schmidt.systems.opensyncedlists.utils.DialogBuilder;
import eu.schmidt.systems.opensyncedlists.utils.ParsedElement;
import eu.schmidt.systems.opensyncedlists.utils.TextListParser;

/**
 * Activity to displaying all lists stored on the device
 */
public class ListsActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener
{
    /**
     * Set to true once the last-list auto-open has been attempted for this
     * process lifetime. Prevents it from firing again on activity recreate
     * (e.g. configuration change or test setUp cycles).
     */
    private static boolean sAutoOpenAttempted = false;
    
    /** Resets the auto-open flag. Intended for use in tests only. */
    public static void resetAutoOpenForTesting()
    {
        sAutoOpenAttempted = false;
    }
    
    /**
     * Reloads all list data and refreshes the UI from the current storage.
     * Called by instrumented tests after {@code TestHelper.clearAll()} so that
     * the adapter reflects the cleared state without requiring a full
     * {@code recreate()} (which fails when the activity is STOPPED because a
     * child activity is on top of it).
     */
    public void resetForTest()
    {
        try
        {
            syncedListsHeaders = secureStorage.getListsHeaders();
        }
        catch (Exception e)
        {
            syncedListsHeaders = new ArrayList<>();
        }
        updateNavigrationDrawer();
        listsAdapter.updateItems(syncedListsHeaders, true);
    }
    
    /** Stores the global settings */
    public SharedPreferences globalSharedPreferences;
    public SecureStorage secureStorage;
    public ListsAdapter listsAdapter;
    /** Result launcher for importing a list */
    private ActivityResultLauncher onImportLauncher;
    private ArrayList<SyncedListHeader> syncedListsHeaders;
    private FloatingActionButton fab;
    private RecyclerView recyclerView;
    private TagsAdapter tagsAdapter;
    private DrawerLayout drawerLayout;
    
    private NavigationView navigationView;
    // Executor for background tasks
    private final Executor backgroundExecutor =
        Executors.newSingleThreadExecutor();
    
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
                    if (importFile == null)
                    {
                        return;
                    }
                    String fileType =
                        this.getContentResolver().getType(importFile);
                    if (fileType != null && fileType.equals("application/json"))
                    {
                        // Import json file
                        importFile(importFile);
                    }
                    else if (fileType != null && fileType.equals("text/plain"))
                    {
                        // Import text file
                        importTextFile(importFile);
                    }
                    else
                    {
                        // Unknown file type
                        Toast.makeText(this,
                            getString(R.string.unknown_file_type),
                            Toast.LENGTH_LONG).show();
                    }
                }
            });
        
        drawerLayout = findViewById(R.id.drawerlayout);
        navigationView = findViewById(R.id.navigation_view);
        
        navigationView.setNavigationItemSelectedListener(this);
        
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        
        ActionBarDrawerToggle drawerToggle =
            new ActionBarDrawerToggle(this, drawerLayout, 0, 0);
        
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        
        init();
        updateNavigrationDrawer();
        showChangelogIfUpdated();
        
        // On first launch after app start, re-open the last viewed list
        if (savedInstanceState == null)
        {
            openLastListIfAvailable();
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
        updateListSettings();
        listsAdapter.updateItems(syncedListsHeaders, true);
        super.onResume();
    }
    
    /**
     * Handles start events of the activity.
     *
     * @param intent started with this intent
     */
    @Override protected void onNewIntent(Intent intent)
    {
        if (intent.getType() != null && intent.getType()
            .equals("application/json"))
        {
            // Started to open a json file
            Uri receivedFile = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            importFile(receivedFile);
        }
        else if (intent.getType() != null && intent.getType()
            .equals("text/plain"))
        {
            
            Uri receivedFile = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            
            importTextFile(receivedFile);
        }
        else if (intent.getData() != null)
        {
            importListFromUrl(intent.getData());
        }
        super.onNewIntent(intent);
    }
    
    @Override public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        onOptionsItemSelected(item);
        return true;
    }
    
    @Override public void onPointerCaptureChanged(boolean hasCapture)
    {
        super.onPointerCaptureChanged(hasCapture);
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
        androidx.core.view.MenuCompat.setGroupDividerEnabled(menu, true);
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
            case android.R.id.home:
                if (drawerLayout.isDrawerOpen(GravityCompat.START))
                {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                else
                {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
            case R.id.new_list:
                // Create new list
                showCreateListDialog();
                return true;
            case R.id.import_text:
                // Import text list
                showImportTextDialog();
                return true;
            case R.id.import_lists:
                // Import list/s from file
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                chooseFile.setType("*/*");
                chooseFile.putExtra(Intent.EXTRA_MIME_TYPES,
                    new String[]{"application/json", "text/plain"});
                chooseFile.setType("application/json");
                chooseFile.setType("plain/text");
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
    
    public static String getFileNameFromUri(Context context, Uri uri)
    {
        String fileName = null;
        try (Cursor cursor = context.getContentResolver()
            .query(uri, null, null, null, null))
        {
            if (cursor != null && cursor.moveToFirst())
            {
                int displayNameIndex =
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (displayNameIndex != -1)
                {
                    String displayName = cursor.getString(displayNameIndex);
                    int dotIndex = displayName.lastIndexOf(".");
                    fileName = (dotIndex == -1) ? displayName
                        : displayName.substring(0, dotIndex);
                }
            }
        }
        return fileName;
    }
    
    public void showListMenu(View view, SyncedListHeader header)
    {
        Log.d("ListsActivity", "Show list menu");
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater()
            .inflate(R.menu.one_list_options_overview, popup.getMenu());
        
        SyncedList syncedList;
        
        secureStorage = new SecureStorage(this);
        try
        {
            syncedList = secureStorage.getList(header.getId());
        }
        catch (Exception e)
        {
            Log.e("ListsActivity", "Error, cant get List: " + e);
            return;
        }
        
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override public boolean onMenuItemClick(MenuItem item)
            {
                
                Log.d("ListsActivity", "Clicked on: " + item.getTitle());
                
                switch (item.getItemId())
                {
                    case R.id.export_md:
                        // Export the list as markdown/text and send it to
                        // another app.
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT,
                            syncedList.getAsMarkdown());
                        sendIntent.setType("text/plain");
                        Intent shareIntent = Intent.createChooser(sendIntent,
                            syncedList.getName());
                        startActivity(shareIntent);
                        return true;
                    case R.id.export_list_json:
                        // Export the list as json and share the file.
                        String absolutPath =
                            FileStorage.exportList(ListsActivity.this,
                                syncedList);
                        Log.i(LOG_TITLE_STORAGE,
                            "Exported list to: " + absolutPath);
                        FileStorage.shareFile(ListsActivity.this, absolutPath);
                        return true;
                    case R.id.list_settings:
                        // Open the list settings
                        Intent listSettingsIntent =
                            new Intent(ListsActivity.this,
                                ListSettingsActivity.class);
                        listSettingsIntent.putExtra("id", syncedList.getId());
                        startActivity(listSettingsIntent);
                        return true;
                    case R.id.export_link:
                        // Share the list as link (via server)
                        if (!syncedList.getHeader().getHostname().equals(""))
                        {
                            // Build link/uri
                            String hostname =
                                syncedList.getHeader().getHostname();
                            String[] splitHost = hostname.split("://");
                            String protocol = splitHost[0];
                            hostname = splitHost[1];
                            Uri.Builder uriBuilder =
                                new Uri.Builder().scheme(protocol)
                                    .encodedAuthority(hostname)
                                    .path("/list/share");
                            uriBuilder.appendQueryParameter("id",
                                syncedList.getId());
                            uriBuilder.appendQueryParameter("secret",
                                syncedList.getSecret());
                            uriBuilder.appendQueryParameter("localSecret",
                                Cryptography.byteArrayToString(
                                    syncedList.getHeader().getLocalSecret()
                                        .getEncoded()));
                            Uri uri = uriBuilder.build();
                            // Share the link to another app
                            Intent sendUriIntent = new Intent();
                            sendUriIntent.setAction(Intent.ACTION_SEND);
                            sendUriIntent.putExtra(Intent.EXTRA_TEXT,
                                getString(R.string.share_before_name)
                                    + syncedList.getName() + getString(
                                    R.string.share_after_name) + uri.toString()
                                    + getString(R.string.share_after_link));
                            sendUriIntent.setType("text/plain");
                            Intent shareUriIntent =
                                Intent.createChooser(sendUriIntent,
                                    syncedList.getName());
                            startActivity(shareUriIntent);
                        }
                        else
                        {
                            Toast.makeText(ListsActivity.this,
                                getString(R.string.no_server_selected),
                                Toast.LENGTH_LONG).show();
                        }
                        return true;
                    case R.id.assign_tag:
                        // Assign a tag to the list
                        try
                        {
                            openAssignTagToListPopUp(header);
                        }
                        catch (Exception e)
                        {
                            Log.e("ListsActivity",
                                "Error opening assign tag to list popup: " + e);
                        }
                        return true;
                }
                
                return false;
            }
        });
        
        popup.show();
    }
    
    public void openAssignTagToListPopUp(SyncedListHeader header)
        throws Exception
    {
        SyncedList syncedList = secureStorage.getList(header.getId());
        DialogBuilder.tagSelectionDialog(this, secureStorage.getAllTags(),
            getString(R.string.assign_tag_to_list_title),
            getString(R.string.assign_tag_to_list_msg), header, tags ->
            {
                
                syncedList.getHeader().setTagList(tags);
                header.setTagList(tags);
                try
                {
                    secureStorage.setList(syncedList);
                    listsAdapter.updateItems(syncedListsHeaders, true);
                    this.recyclerView.post(
                        () -> listsAdapter.notifyDataSetChanged());
                }
                catch (Exception e)
                {
                    Log.e(LOG_TITLE_STORAGE, "Error saving list header: " + e);
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
        ServerWrapper.checkConnection(defaultHostname,
            (jsonResult, exception) ->
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
        DialogBuilder.editTextDialog(this,
            getString(R.string.create_list_title),
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
                            globalSharedPreferences.getString("default_server",
                                ""), Cryptography.stringToByteArray(
                            Cryptography.generatingRandomString(50)),
                            Cryptography.generateAESKey());
                    header.setCheckOption(
                        globalSharedPreferences.getBoolean("check_option",
                            true));
                    header.setCheckedList(
                        globalSharedPreferences.getBoolean("checked_list",
                            true));
                    header.setJumpButtons(
                        globalSharedPreferences.getBoolean("jump_buttons",
                            false));
                    header.setInvertElement(
                        globalSharedPreferences.getBoolean("invert_element",
                            false));
                    SyncedList newList =
                        new SyncedList(header, new ArrayList<>());
                    Log.d("ListsActivity",
                        "Creating new list: " + newList.getName());
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
                    Log.e(LOG_TITLE_DEFAULT, "Cant import file: " + exception);
                }
            }
        }
        catch (Exception ignored)
        {
            Log.e("ListsActivity", "Error importing file: " + ignored);
            ignored.printStackTrace();
        }
    }
    
    protected void importTextFile(Uri uri)
    {
        // Show progress dialog
        final AlertDialog progressDialog =
            new AlertDialog.Builder(this).setTitle("Importing text file...")
                .setMessage("Processing file...").setCancelable(false).create();
        progressDialog.show();
        
        // Process file asynchronously on background thread
        new Thread(() ->
        {
            try
            {
                InputStream in = getContentResolver().openInputStream(uri);
                BufferedReader r =
                    new BufferedReader(new InputStreamReader(in));
                StringBuilder total = new StringBuilder();
                
                SyncedListHeader header =
                    new SyncedListHeader(getUniqueListId(),
                        getFileNameFromUri(this, uri),
                        globalSharedPreferences.getString("default_server", ""),
                        Cryptography.stringToByteArray(
                            Cryptography.generatingRandomString(50)),
                        Cryptography.generateAESKey());
                header.setCheckOption(
                    globalSharedPreferences.getBoolean("check_option", true));
                header.setCheckedList(
                    globalSharedPreferences.getBoolean("checked_list", true));
                header.setJumpButtons(
                    globalSharedPreferences.getBoolean("jump_buttons", false));
                header.setInvertElement(
                    globalSharedPreferences.getBoolean("invert_element",
                        false));
                
                SyncedList newList = new SyncedList(header, new ArrayList<>());
                
                ArrayList<SyncedListStep> steps = new ArrayList<>();
                for (String line; (line = r.readLine()) != null; )
                {
                    String refactored =
                        line.replace(" - ", "").replace(" -", "")
                            .replace("- ", "");
                    if (refactored.isEmpty())
                    {
                        continue;
                    }
                    String id = newList.generateUniqueElementId();
                    SyncedListStep syncedListStep =
                        new SyncedListStep(id, ACTION.ADD,
                            new SyncedListElement(id, refactored, ""));
                    steps.add(syncedListStep);
                }
                newList.setElementSteps(steps);
                
                // Switch back to main thread to add list and dismiss dialog
                new Handler(Looper.getMainLooper()).post(() ->
                {
                    progressDialog.dismiss();
                    addListAndHandleCallback(newList);
                });
            }
            catch (Exception e)
            {
                Log.e("ListsActivity", "Error importing file: " + e);
                new Handler(Looper.getMainLooper()).post(() ->
                {
                    progressDialog.dismiss();
                    Toast.makeText(ListsActivity.this,
                        "Error importing file: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    /**
     * Adds a list, save it, update views and handle exceptions.
     *
     * @param syncedList List to add
     */
    protected void addListAndHandleCallback(SyncedList syncedList)
    {
        Log.d("ListsActivity",
            "Adding list to local storage: " + syncedList.getName());
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
            tagsAdapter.updateItems(secureStorage.getAllTags(), true);
            Log.d("ListsActivity",
                "Added list to local storage: " + syncedList.getName());
        }
        catch (Exception exception)
        {
            Log.e("ListsActivity",
                "Error adding list to local storage: " + exception);
            exception.printStackTrace();
        }
    }
    
    private void updateNavigrationDrawer()
    {
        RecyclerView listView =
            navigationView.getHeaderView(0).findViewById(R.id.listViewTags);
        // TODO: Add on nav menu item clicked!
        
        if (listView == null)
        {
            Log.e("ListsActivity", "ListView is null");
            return;
        }
        
        ArrayList<ListTag> tagList = new ArrayList<>();
        try
        {
            tagList = secureStorage.getAllTags();
        }
        catch (Exception e)
        {
            Log.e("ListsActivity", "Error getting tags: " + e);
        }
        tagsAdapter = new TagsAdapter(this, tagList, listView);
        
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(tagsAdapter);
        
        ImageButton imgBtnAddTag =
            navigationView.getHeaderView(0).findViewById(R.id.imgBtnAddTag);
        imgBtnAddTag.setOnClickListener(v ->
        {
            Log.d("ListsActivity", "Clicked on add tag button");
            DialogBuilder.editTextDialog(this,
                getString(R.string.create_tag_title),
                getString(R.string.create_tag_msg),
                getString(R.string.create_tag_yes),
                getString(R.string.create_tag_cancel), result ->
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
                        ListTag newTag = new ListTag(result);
                        try
                        {
                            tagsAdapter.addTag(newTag);
                            secureStorage.saveAllTags(tagsAdapter.tagList,
                                false);
                        }
                        catch (Exception e)
                        {
                            Toast.makeText(this,
                                getString(R.string.toast_error_creating_tag),
                                Toast.LENGTH_LONG).show();
                        }
                    }
                });
        });
        navigationView.setNavigationItemSelectedListener(this);
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
        Log.d(LOG_TITLE_DEFAULT, "Import list via link from host: " + hostname);
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
    
    private void importListFromUrlDialog()
    {
        DialogBuilder.editTextDialog(this,
            getString(R.string.import_list_url_title),
            getString(R.string.import_list_url_msg),
            getString(R.string.import_list_url_yes),
            getString(R.string.import_list_url_cancel), result ->
            {
                if (result != null && !result.isEmpty())
                {
                    Uri url = Uri.parse(result);
                    importListFromUrl(url);
                }
            });
    }
    
    /**
     * Re-opens the last viewed list on app start if the ID is still valid. Only
     * fires once per process lifetime so that activity recreates (e.g.
     * configuration changes or test setUp cycles) do not trigger a second
     * auto-open.
     */
    private void openLastListIfAvailable()
    {
        if (sAutoOpenAttempted)
        {
            return;
        }
        sAutoOpenAttempted = true;
        
        String lastId = globalSharedPreferences.getString("last_list_id", null);
        if (lastId == null)
        {
            return;
        }
        // Verify the list still exists
        boolean found = false;
        for (SyncedListHeader header : syncedListsHeaders)
        {
            if (header.getId().equals(lastId))
            {
                found = true;
                break;
            }
        }
        if (!found)
        {
            return;
        }
        Intent intent = new Intent(this, ListActivity.class);
        intent.putExtra("id", lastId);
        startActivity(intent);
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
        
        // Read and use preferences
        globalSharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        if (globalSharedPreferences.getString("design", "")
            .equals(getString(R.string.pref_design_light)))
        {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO);
        }
        else if (globalSharedPreferences.getString("design", "")
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
        
        listsAdapter = new ListsAdapter(this, syncedListsHeaders, recyclerView);
        updateListSettings();
        recyclerView.setAdapter(listsAdapter);
        
        // here is your color
        
        checkServerConnection();
    }
    
    private void updateListSettings()
    {
        if (globalSharedPreferences.getBoolean("list_overview_instead_cards",
            false))
        {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            Log.d("ListsActivity", "List view");
        }
        else
        {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            Log.d("ListsActivity", "Card view");
        }
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
    
    /**
     * Shows a "What's New" dialog once per app version, reading the changelog
     * from {@code assets/changelog.txt} (copied from the fastlane metadata by
     * the {@code copyChangelog} Gradle task at build time).
     *
     * The dialog is suppressed on subsequent launches until the version code
     * changes (i.e. after an update).
     */
    private void showChangelogIfUpdated()
    {
        int currentVersion;
        try
        {
            currentVersion =
                (int) getPackageManager().getPackageInfo(getPackageName(), 0)
                    .getLongVersionCode();
        }
        catch (android.content.pm.PackageManager.NameNotFoundException e)
        {
            return;
        }
        if (globalSharedPreferences.getInt("last_seen_version_code", -1)
            == currentVersion)
        {
            return;
        }
        
        String changelog;
        String lang = java.util.Locale.getDefault().getLanguage();
        java.util.Map<String, String> localeAssets = new java.util.HashMap<>();
        localeAssets.put("de", "changelog_de.txt");
        localeAssets.put("fr", "changelog_fr.txt");
        localeAssets.put("pt", "changelog_pt.txt");
        localeAssets.put("es", "changelog_es.txt");
        String assetName =
            localeAssets.containsKey(lang) ? localeAssets.get(lang)
                : "changelog.txt";
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(getAssets().open(assetName))))
        {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line).append('\n');
            }
            changelog = sb.toString().trim();
        }
        catch (Exception e)
        {
            Log.e(LOG_TITLE_DEFAULT, "Failed to load changelog: " + e);
            return;
        }
        
        String playStoreUrl =
            "https://play.google.com/store/apps/details?id=eu.schmidt.systems"
                + ".opensyncedlists";
        String fDroidUrl =
            "https://f-droid.org/packages/eu.schmidt.systems.opensyncedlists/";
        // F-Droid has no rating feature, so we only link to the page there and
        // instead ask for a star on the GitLab repository.
        String gitLabUrl = getString(R.string.sourcecode_webpage);
        
        String html =
            "<b>" + getString(R.string.changelog_whats_new) + "</b><br><br>"
                + android.text.TextUtils.htmlEncode(changelog)
                .replace("\n", "<br>") + "<br><br>"
                + android.text.TextUtils.htmlEncode(
                getString(R.string.changelog_thank_you)).replace("\n", "<br>")
                + "<br><br>" + "<a href=\"" + playStoreUrl + "\">" + getString(
                R.string.changelog_rate_playstore) + "</a>" + "<br>"
                + "<a href=\"" + gitLabUrl + "\">" + getString(
                R.string.changelog_star_gitlab) + "</a>" + "<br>" + "<a href=\""
                + fDroidUrl + "\">" + getString(R.string.changelog_view_fdroid)
                + "</a>";
        
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(
                R.string.changelog_dialog_title).setMessage(
                android.text.Html.fromHtml(html,
                    android.text.Html.FROM_HTML_MODE_COMPACT))
            .setPositiveButton(R.string.changelog_close, null).create();
        dialog.show();
        
        android.widget.TextView messageView =
            dialog.findViewById(android.R.id.message);
        if (messageView != null)
        {
            messageView.setMovementMethod(
                android.text.method.LinkMovementMethod.getInstance());
        }
        
        globalSharedPreferences.edit()
            .putInt("last_seen_version_code", currentVersion).apply();
    }
    
    /**
     * Shows the import text dialog and creates a new list from parsed
     * elements.
     */
    private void showImportTextDialog()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        
        editText.setInputType(
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setMinLines(5);
        editText.setMaxLines(10);
        editText.setGravity(
            android.view.Gravity.TOP | android.view.Gravity.START);
        editText.setHint(getString(R.string.import_text_hint));
        
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin_in_dp = 10;
        int margin_in_px =
            (int) (margin_in_dp * getResources().getDisplayMetrics().density);
        params.setMargins(margin_in_px, margin_in_px, margin_in_px,
            margin_in_px);
        editText.setLayoutParams(params);
        editText.setPadding(margin_in_px, margin_in_px, margin_in_px,
            margin_in_px);
        
        alert.setTitle(getString(R.string.import_text_title));
        alert.setMessage(getString(R.string.import_text_msg));
        alert.setView(editText);
        alert.setPositiveButton(getString(R.string.import_text_yes),
            (dialog, whichButton) ->
            {
                String result = editText.getText().toString();
                if (result != null && !result.isEmpty())
                {
                    // Parse text asynchronously on background thread to
                    // prevent UI freeze
                    parseTextImportAsync(result);
                }
            });
        alert.setNegativeButton(getString(R.string.import_text_cancel),
            (dialog, whichButton) -> dialog.cancel());
        
        alert.create().show();
    }
    
    /**
     * Parse text input asynchronously on a background thread. Prevents UI
     * thread blocking during large text imports.
     *
     * @param input the text input to parse
     */
    private void parseTextImportAsync(String input)
    {
        // Show progress dialog
        final AlertDialog progressDialog =
            new AlertDialog.Builder(this).setTitle(
                    getString(R.string.import_text_title))
                .setMessage("Processing text...").setCancelable(false).create();
        progressDialog.show();
        
        // Parse on background thread
        new Thread(() ->
        {
            try
            {
                TextListParser parser = new TextListParser();
                ArrayList<ParsedElement> elements =
                    new ArrayList<>(TextListParser.parse(input));
                
                // Switch back to main thread for UI updates
                new Handler(Looper.getMainLooper()).post(() ->
                {
                    progressDialog.dismiss();
                    
                    if (elements.isEmpty())
                    {
                        Toast.makeText(ListsActivity.this,
                            getString(R.string.import_text_empty),
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    createListFromParsedElements("Imported List", elements);
                });
            }
            catch (Exception e)
            {
                Log.e("ListsActivity", "Error parsing text: " + e);
                new Handler(Looper.getMainLooper()).post(() ->
                {
                    progressDialog.dismiss();
                    Toast.makeText(ListsActivity.this,
                        "Error parsing text: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    /**
     * Creates a new list from parsed elements.
     *
     * @param name     Name of the new list
     * @param elements List of parsed elements
     */
    private void createListFromParsedElements(String name,
        ArrayList<ParsedElement> elements)
    {
        DialogBuilder.editTextDialog(this,
            getString(R.string.create_list_title),
            getString(R.string.create_list_msg),
            getString(R.string.create_list_yes),
            getString(R.string.create_list_cancel), result ->
            {
                if (result != null && !result.isEmpty())
                {
                    SyncedListHeader header =
                        new SyncedListHeader(getUniqueListId(), result,
                            globalSharedPreferences.getString("default_server",
                                ""), Cryptography.stringToByteArray(
                            Cryptography.generatingRandomString(50)),
                            Cryptography.generateAESKey());
                    header.setCheckOption(
                        globalSharedPreferences.getBoolean("check_option",
                            true));
                    header.setCheckedList(
                        globalSharedPreferences.getBoolean("checked_list",
                            true));
                    header.setJumpButtons(
                        globalSharedPreferences.getBoolean("jump_buttons",
                            false));
                    header.setInvertElement(
                        globalSharedPreferences.getBoolean("invert_element",
                            false));
                    
                    SyncedList newList =
                        new SyncedList(header, new ArrayList<>());
                    
                    ArrayList<SyncedListStep> steps = new ArrayList<>();
                    for (ParsedElement element : elements)
                    {
                        String id = newList.generateUniqueElementId();
                        SyncedListElement listElement =
                            new SyncedListElement(id, element.getName(),
                                element.getDescription());
                        listElement.setChecked(element.isChecked());
                        SyncedListStep step =
                            new SyncedListStep(id, ACTION.ADD, listElement);
                        steps.add(step);
                    }
                    newList.setElementSteps(steps);
                    
                    Log.d("ListsActivity",
                        "Creating new list from parsed elements: "
                            + newList.getName());
                    addListAndHandleCallback(newList);
                    Toast.makeText(this,
                        String.format(getString(R.string.import_text_success),
                            elements.size()), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
