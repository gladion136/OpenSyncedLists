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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.adapters.SyncedListAdapter;
import eu.schmidt.systems.opensyncedlists.network.ServerException;
import eu.schmidt.systems.opensyncedlists.network.ServerWrapper;
import eu.schmidt.systems.opensyncedlists.storages.FileStorage;
import eu.schmidt.systems.opensyncedlists.storages.SecureStorage;
import eu.schmidt.systems.opensyncedlists.syncedlist.ACTION;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListStep;
import eu.schmidt.systems.opensyncedlists.utils.Constant;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;

/**
 * Activity for displaying one syncedList.
 */
public class ListActivity extends AppCompatActivity {

    private SecureStorage secureStorage;
    private SyncedList syncedList;
    private SyncedListAdapter syncedListAdapter;
    private Handler autoSyncHandler;
    private Runnable autoSyncRunnable;
    private RecyclerView recyclerView;
    private EditText eTNewElement;
    private ImageView iVNewElementTop, iVNewElementBottom;

    /**
     * In onCreate the layout is set and the global Variables are
     * initialised.
     *
     * @param savedInstanceState In this case just used for the super call.
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        recyclerView = findViewById(R.id.recyclerView);
        eTNewElement = findViewById(R.id.eTNewElement);
        iVNewElementTop = findViewById(R.id.iVNewElementTop);
        iVNewElementBottom = findViewById(R.id.iVNewElementBottom);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eTNewElement.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                createNewElement(false);
                return true;
            }
            return false;
        });
        iVNewElementTop.setOnClickListener(v -> createNewElement(true));
        iVNewElementBottom.setOnClickListener(v -> createNewElement(false));
        secureStorage = new SecureStorage(this);
    }

    /**
     * In onResume the init function is called.
     */
    @Override protected void onResume() {
        init();
        super.onResume();
    }

    /**
     * onDestroy clears all handlers.
     */
    @Override protected void onDestroy() {
        try {
            autoSyncHandler.removeCallbacks(autoSyncRunnable);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    /**
     * In onCreateOptionsMenu the menu from the ActionBar is inflated.
     *
     * @param menu Menu to inflate
     * @return true
     */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.one_list_menu, menu);
        return true;
    }

    /**
     * onOptionsItemSelected handles the events from the ActionBar.
     *
     * @param item selected item
     * @return action handled?
     */
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Back to ListsActivity
                onBackPressed();
                return true;
            case R.id.export_md:
                // Export the list as markdown/text and send it to another app.
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                                    syncedList.getAsMarkdown());
                sendIntent.setType("text/plain");
                Intent shareIntent =
                        Intent.createChooser(sendIntent, syncedList.getName());
                startActivity(shareIntent);
                return true;
            case R.id.export_list_json:
                // Export the list as json and share the file.
                String absolutPath = FileStorage.exportList(this, syncedList);
                Log.i(LOG_TITLE_STORAGE, "Exported list to: " + absolutPath);
                FileStorage.shareFile(this, absolutPath);
                return true;
            case R.id.list_clear:
                // Remove the list elements
                SyncedListStep syncedListStep =
                        new SyncedListStep("", ACTION.CLEAR, "");
                addElementStepAndSave(syncedListStep, true);
                return true;
            case R.id.list_settings:
                // Open the list settings
                Intent listSettingsIntent =
                        new Intent(this, ListSettingsActivity.class);
                listSettingsIntent.putExtra("id", syncedList.getId());
                startActivity(listSettingsIntent);
                finish();
                return true;
            case R.id.manual_sync:
                // Synchronize the app
                if (!syncedList.getHeader().getHostname().equals("")) {
                    syncWithHost();
                } else {
                    Toast.makeText(this, getString(R.string.no_server_selected),
                                   Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.export_link:
                // Share the list as link (via server)
                if (!syncedList.getHeader().getHostname().equals("")) {
                    // Build link/uri
                    String hostname = syncedList.getHeader().getHostname();
                    String[] splitHost = hostname.split("://");
                    String protocol = splitHost[0];
                    hostname = splitHost[1];
                    Uri.Builder uriBuilder = new Uri.Builder().scheme(protocol)
                            .encodedAuthority(hostname).path("/list/share");
                    uriBuilder.appendQueryParameter("id", syncedList.getId());
                    uriBuilder.appendQueryParameter("secret",
                                                    syncedList.getSecret());
                    uriBuilder.appendQueryParameter("localSecret", Cryptography
                            .byteArrayToString(
                                    syncedList.getHeader().getLocalSecret()
                                            .getEncoded()));
                    Uri uri = uriBuilder.build();
                    // Share the link to another app
                    Intent sendUriIntent = new Intent();
                    sendUriIntent.setAction(Intent.ACTION_SEND);
                    sendUriIntent.putExtra(Intent.EXTRA_TEXT, getString(
                            R.string.share_before_name) + syncedList.getName() +
                            getString(R.string.share_after_name) +
                            uri.toString() +
                            getString(R.string.share_after_link));
                    sendUriIntent.setType("text/plain");
                    Intent shareUriIntent = Intent.createChooser(sendUriIntent,
                                                                 syncedList
                                                                         .getName());
                    startActivity(shareUriIntent);
                } else {
                    Toast.makeText(this, getString(R.string.no_server_selected),
                                   Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.settings:
                // Open global settings
                Intent settingsIntent =
                        new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                finish();
                return true;
        }
        // Item not handled.. pass to super
        return super.onOptionsItemSelected(item);
    }

    /**
     * Initialize fills the activity with content. (Fill list, start sync, ..)
     */
    private void init() {
        // Read the list with passed id
        try {
            syncedList = secureStorage
                    .getList(getIntent().getExtras().getString("id"));
        } catch (Exception e) {
            Log.e(Constant.LOG_TITLE_DEFAULT, "Local storage read error: " + e);
            e.printStackTrace();
        }
        // Update ActionBar
        setTitle(syncedList.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Update Adapter
        syncedListAdapter =
                new SyncedListAdapter(this, recyclerView, syncedList);
        recyclerView.setAdapter(syncedListAdapter);

        // Should autoSync?
        if (syncedList.getHeader().isAutoSync() &&
                !syncedList.getHeader().getHostname().equals("")) {
            // Auto synchronize every 10 seconds
            autoSyncHandler = new Handler();
            autoSyncRunnable = () -> {
                syncWithHost();
                autoSyncHandler.postDelayed(autoSyncRunnable, 10000);
            };
            autoSyncHandler.post(autoSyncRunnable);
        }
        checkServerConnection();
    }

    /**
     * Checks the connection to the server and shows an Toast on error.
     */
    protected void checkServerConnection() {
        String hostname = syncedList.getHeader().getHostname();
        if (hostname.equals("")) {
            return;
        }
        ServerWrapper.checkConnection(hostname, (jsonResult, exception) -> {
            if (jsonResult == null || exception != null) {
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
     * Read name from editText on bottom and create new element inside list.
     *
     * @param top Should the element added on top?
     */
    protected void createNewElement(boolean top) {
        String elementName = eTNewElement.getText().toString();
        if (!elementName.equals("")) {
            // Create SyncedListStep for new the element
            String id = syncedList.generateUniqueElementId();
            SyncedListStep syncedListStep = new SyncedListStep(id, ACTION.ADD,
                                                               new SyncedListElement(
                                                                       id,
                                                                       eTNewElement
                                                                               .getText()
                                                                               .toString(),
                                                                       ""));
            addElementStepAndSave(syncedListStep, true);
            if (top) {
                // Create SyncedListStep for moving the element
                SyncedListStep syncedListStepMove =
                        new SyncedListStep(id, ACTION.MOVE, 0);
                addElementStepAndSave(syncedListStepMove, true);
            }
            // Scroll to the element
            if (syncedList.getHeader().isCheckedList()) {
                recyclerView.scrollToPosition(
                        top ? 0 : syncedList.getUncheckedElements().size() - 1);
            } else {
                recyclerView.scrollToPosition(
                        top ? 0 : syncedList.getElements().size() - 1);
            }
            eTNewElement.setText("");
            Log.d(Constant.LOG_TITLE_DEFAULT, "New element added to list");
        }
    }

    /**
     * onBackPressed finish the activity.
     */
    @Override public void onBackPressed() {
        finish();
    }

    /**
     * Start synchronizing with the connected server of the list.
     */
    public void syncWithHost() {
        String hostname = syncedList.getHeader().getHostname();
        if (hostname.equals("")) {
            return;
        }
        Log.d(LOG_TITLE_NETWORK, "Start synchronize list");
        // Start sync
        ServerWrapper.getList(hostname, syncedList.getHeader().getId(),
                              syncedList.getSecret(),
                              (jsonListFromServer, exceptionListFromServer) -> {
                                  if (jsonListFromServer == null ||
                                          exceptionListFromServer != null) {
                                      Log.e(LOG_TITLE_NETWORK, "Error: " +
                                              exceptionListFromServer
                                                      .toString());

                                      if (exceptionListFromServer instanceof ServerException) {
                                          if (exceptionListFromServer
                                                  .getMessage()
                                                  .equals("Not found")) {
                                              // List not on server, so
                                              // add it
                                              addListToServer();
                                          }
                                      }
                                      return;
                                  }
                                  try {
                                      // Wrap result Data
                                      String encryptedData = jsonListFromServer
                                              .getJSONObject("msg")
                                              .getString("data");
                                      SyncedList receivedList = new SyncedList(
                                              new JSONObject(Cryptography
                                                                     .decryptRSA(
                                                                             syncedList
                                                                                     .getHeader()
                                                                                     .getLocalSecret(),
                                                                             encryptedData)));
                                      // Start sync
                                      syncAndUpdateListOnServer(receivedList,
                                                                Cryptography
                                                                        .getSHAasString(
                                                                                encryptedData));
                                  } catch (JSONException e) {
                                      // Shouldn't entered if the server
                                      // worked fine
                                      Log.e(LOG_TITLE_NETWORK, e.toString());
                                      e.printStackTrace();
                                  }
                              });
    }

    /**
     * Add a new list to the server. Get called from syncWithHost.
     */
    private void addListToServer() {
        ServerWrapper.addList(syncedList, (jsonResult, exception) -> {
            if (jsonResult == null || exception != null) {
                if (exception instanceof ServerException) {
                    Log.e(LOG_TITLE_NETWORK,
                          "Unexpected Error: " + exception.toString());
                    Toast.makeText(this, getString(R.string.unexpected_error),
                                   Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.e(LOG_TITLE_NETWORK, "Error: " + exception.toString());
                return;
            }
            Log.d(LOG_TITLE_NETWORK, "List added to server!");
        });
    }

    /**
     * Synchronize with the receivedList and push result.
     *
     * @param receivedList     List from server
     * @param receivedListHash hash from encrypted list on server
     */
    private void syncAndUpdateListOnServer(SyncedList receivedList,
                                           String receivedListHash) {
        // Sync if changes happened
        if (!receivedList.toJSON().toString()
                .equals(syncedList.toJSON().toString())) {
            SyncedList synchronizedList =
                    SyncedList.sync(syncedList, receivedList);
            ServerWrapper.setList(synchronizedList, receivedListHash,
                                  (jsonResult, exception) -> {
                                      if (jsonResult == null ||
                                              exception != null) {
                                          Log.e(LOG_TITLE_NETWORK, "Error: " +
                                                  exception.toString());
                                          if (exception instanceof ServerException) {
                                              // Something went wrong for
                                              // example wrong hash => Retry
                                              // Sync
                                              syncWithHost();
                                              return;
                                          }
                                          return;
                                      }
                                      Log.d(LOG_TITLE_NETWORK, "Finish sync!");
                                      // Sync successfull written to server,
                                      // so sync again and apply changes.
                                      syncedList.sync(synchronizedList);
                                      this.recyclerView
                                              .post(() -> syncedListAdapter
                                                      .notifyDataSetChanged());
                                      try {
                                          secureStorage.setList(syncedList);
                                      } catch (IOException | JSONException e) {
                                          e.printStackTrace();
                                      }
                                  });
        }
    }

    /**
     * Add step to list and save it
     *
     * @param syncedListStep new SyncedListStep
     * @param notify         should visible view refreshed?
     */
    public void addElementStepAndSave(SyncedListStep syncedListStep,
                                      boolean notify) {
        syncedList.addElementStep(syncedListStep);

        if (notify) {
            this.recyclerView
                    .post(() -> syncedListAdapter.notifyDataSetChanged());
        }

        try {
            secureStorage.setList(syncedList);
        } catch (IOException | JSONException e) {
            Log.e(Constant.LOG_TITLE_DEFAULT,
                  "Local storage " + "write" + " error: " + e);
            e.printStackTrace();
        }
    }
}