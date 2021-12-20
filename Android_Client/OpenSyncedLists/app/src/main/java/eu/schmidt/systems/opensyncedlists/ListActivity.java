package eu.schmidt.systems.opensyncedlists;

import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_DEFAULT;
import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_NETWORK;
import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_STORAGE;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

import eu.schmidt.systems.opensyncedlists.adapter.SyncedListAdapter;
import eu.schmidt.systems.opensyncedlists.datatypes.ACTION;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListStep;
import eu.schmidt.systems.opensyncedlists.exceptions.ServerException;
import eu.schmidt.systems.opensyncedlists.utils.Constant;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;
import eu.schmidt.systems.opensyncedlists.utils.FileStorage;
import eu.schmidt.systems.opensyncedlists.utils.SecureStorage;
import eu.schmidt.systems.opensyncedlists.utils.ServerConnection;

/**
 * ListActivity displays one list
 */
public class ListActivity extends AppCompatActivity {

    SecureStorage secureStorage;
    SyncedList syncedList;
    RecyclerView recyclerView;
    EditText eTNewElement;
    ImageView iVNewElementTop, iVNewElementBottom;
    protected RecyclerView.LayoutManager mLayoutManager;
    SyncedListAdapter syncedListAdapter;

    Handler handler;
    Runnable runnableCode;

    /**
     * onCreate
     *
     * @param savedInstanceState
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        recyclerView = findViewById(R.id.recyclerView);
        eTNewElement = findViewById(R.id.eTNewElement);
        iVNewElementTop = findViewById(R.id.iVNewElementTop);
        iVNewElementBottom = findViewById(R.id.iVNewElementBottom);
        mLayoutManager = new LinearLayoutManager(this);
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

    @Override protected void onResume() {
        init();
        super.onResume();
    }

    /**
     * onCreateOptionsMenu
     *
     * @param menu Menu
     * @return true
     */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.one_list_menu, menu);
        return true;
    }

    /**
     * onOptionsItemSelected (events for actionbar)
     *
     * @param item selected item
     * @return action handled?
     */
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.export_md:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                                    syncedList.getAsReadableString());
                sendIntent.setType("text/plain");
                Intent shareIntent =
                        Intent.createChooser(sendIntent, syncedList.getName());
                startActivity(shareIntent);
                return true;
            case R.id.export_list_json:
                String absolutPath = FileStorage.exportList(this, syncedList);
                Log.i(LOG_TITLE_STORAGE, "Exported list to: " + absolutPath);
                FileStorage.shareFile(this, absolutPath);
                return true;
            case R.id.list_clear:
                SyncedListStep syncedListStep =
                        new SyncedListStep("", ACTION.CLEAR, "");
                addElementStepAndSave(syncedListStep, true);
                return true;
            case R.id.list_settings:
                Intent listSettingsIntent =
                        new Intent(this, ListSettingsActivity.class);
                try {
                    listSettingsIntent.putExtra("id", syncedList.getId());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                startActivity(listSettingsIntent);
                finish();
                return true;
            case R.id.manual_sync:
                syncWithHost();
                return true;
            case R.id.export_link:
                if (!syncedList.getHeader().getHostname().equals("")) {
                    String hostname = syncedList.getHeader().getHostname();
                    String[] splitHost = hostname.split("://");
                    String protocoll = splitHost[0];
                    hostname = splitHost[1];
                    Uri.Builder uriBuilder = new Uri.Builder().scheme(protocoll)
                            .encodedAuthority(hostname).path("/list/share");
                    uriBuilder.appendQueryParameter("id", syncedList.getId());
                    uriBuilder.appendQueryParameter("secret",
                                                    syncedList.getSecret());
                    uriBuilder.appendQueryParameter("localSecret", Cryptography
                            .byteArraytoString(
                                    syncedList.getHeader().getLocalSecret()
                                            .getEncoded()));
                    Uri uri = uriBuilder.build();
                    Intent sendUriIntent = new Intent();
                    sendUriIntent.setAction(Intent.ACTION_SEND);
                    sendUriIntent.putExtra(Intent.EXTRA_TEXT,
                                           getString(R.string.share_before_name) +
                                                   syncedList.getName() +
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
                Intent settingsIntent =
                        new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Initialize
     */
    public void init() {
        try {
            syncedList = secureStorage
                    .getList(getIntent().getExtras().getString("id"));
        } catch (Exception e) {
            Log.e(Constant.LOG_TITLE_DEFAULT, "Local storage read error: " + e);
            e.printStackTrace();
        }
        setTitle(syncedList.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        syncedListAdapter =
                new SyncedListAdapter(this, recyclerView, syncedList);
        recyclerView.setAdapter(syncedListAdapter);

        if (syncedList.getHeader().isAutoSync() &&
                !syncedList.getHeader().getHostname().equals("")) {
            // Auto synchronize every 10 seconds
            handler = new Handler();
            runnableCode = () -> {
                syncWithHost();
                handler.postDelayed(runnableCode, 10000);
            };
            handler.post(runnableCode);
        }
        checkServerConnection();
    }

    public void checkServerConnection() {
        String hostname = syncedList.getHeader().getHostname();
        if (hostname.equals("")) {
            return;
        }
        ServerConnection.checkConnection(hostname, (jsonResult, exception) -> {
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

    @Override protected void onDestroy() {
        try {
            handler.removeCallbacks(runnableCode);
        } catch (Exception e) {
        } // Exception not important
        super.onDestroy();
    }

    /**
     * Read name from edittext on bottom and create new element inside list
     * Returns: success(true)
     */
    public boolean createNewElement(boolean top) {
        String elementName = eTNewElement.getText().toString();
        if (!elementName.equals("")) {
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
                SyncedListStep syncedListStepMove =
                        new SyncedListStep(id, ACTION.MOVE, 0);
                addElementStepAndSave(syncedListStepMove, true);
            }
            if (!syncedList.getHeader().isCheckedList()) {
                recyclerView.scrollToPosition(
                        top ? 0 : syncedList.getElements().size() - 1);
            }
            eTNewElement.setText("");
            Log.d(Constant.LOG_TITLE_DEFAULT, "New element added to list");
            return true;
        }
        return false;
    }

    @Override public void onBackPressed() {
        finish();
    }

    public void syncWithHost() {
        String hostname = syncedList.getHeader().getHostname();
        if (hostname.equals("")) {
            return;
        }
        Log.d(LOG_TITLE_NETWORK, "Start synchronize list");
        // Start sync
        ServerConnection.getList(hostname, syncedList.getHeader().getId(),
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
                                                 addListToServer();
                                             }
                                         }
                                         return;
                                     }
                                     try {
                                         SyncedList receivedList =
                                                 new SyncedList(new JSONObject(
                                                         Cryptography
                                                                 .decryptRSA(
                                                                         syncedList
                                                                                 .getHeader()
                                                                                 .getLocalSecret(),
                                                                         jsonListFromServer
                                                                                 .getJSONObject(
                                                                                         "msg")
                                                                                 .getString(
                                                                                         "data"))));
                                         syncAndUpdateListOnServer(
                                                 receivedList);
                                     } catch (JSONException e) {
                                         // Shouldn't entered if the server
                                         // worked fine
                                         Log.e(LOG_TITLE_NETWORK, e.toString());
                                         e.printStackTrace();
                                     }
                                 });
    }

    /**
     * Get called from syncWithHost
     */
    private void addListToServer() {
        ServerConnection.addList(syncedList, (jsonResult, exception) -> {
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

    private void syncAndUpdateListOnServer(SyncedList receivedList) {
        SyncedList synchronizedList = SyncedList.sync(syncedList, receivedList);
        ServerConnection.setList(synchronizedList, Cryptography
                                         .getSHAasString(receivedList.getFullListEncrypted()),
                                 (jsonResult, exception) -> {
                                     if (jsonResult == null ||
                                             exception != null) {
                                         Log.e(LOG_TITLE_NETWORK, "Error: " +
                                                 exception.toString());
                                         if (exception instanceof ServerException) {
                                             // Something went wrong for
                                             // example wrong hash => Resync
                                             syncWithHost();
                                             return;
                                         }
                                         return;
                                     }
                                     Log.d(LOG_TITLE_NETWORK, "Finish sync");
                                     syncedList.sync(synchronizedList);
                                     this.recyclerView
                                             .post(() -> syncedListAdapter
                                                     .notifyDataSetChanged());
                                     try {
                                         secureStorage
                                                 .setList(syncedList, false);
                                     } catch (IOException e) {
                                         e.printStackTrace();
                                     } catch (JSONException e) {
                                         e.printStackTrace();
                                     }
                                 });
    }

    /**
     * Add step to List
     *
     * @param syncedListStep new SyncedListStep
     */
    public void addElementStepAndSave(SyncedListStep syncedListStep,
                                      boolean notify) {
        syncedList.addElementStep(syncedListStep);

        if (notify) {
            this.recyclerView
                    .post(() -> syncedListAdapter.notifyDataSetChanged());
        }

        try {
            secureStorage.setList(syncedList, false);
        } catch (IOException | JSONException e) {
            Log.e(Constant.LOG_TITLE_DEFAULT,
                  "Local storage " + "write" + " error: " + e);
            e.printStackTrace();
        }
    }
}