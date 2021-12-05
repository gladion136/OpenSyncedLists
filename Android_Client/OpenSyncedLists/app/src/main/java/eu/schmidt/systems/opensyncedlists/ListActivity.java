package eu.schmidt.systems.opensyncedlists;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;

import eu.schmidt.systems.opensyncedlists.adapter.SyncedListAdapter;
import eu.schmidt.systems.opensyncedlists.datatypes.ACTION;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListStep;
import eu.schmidt.systems.opensyncedlists.utils.Constant;
import eu.schmidt.systems.opensyncedlists.utils.LocalStorage;

/**
 * ListActivity displays one list
 */
public class ListActivity extends AppCompatActivity {

    LocalStorage localStorage;
    SyncedList syncedList;
    RecyclerView recyclerView, recyclerViewChecked;
    EditText eTNewElement;
    ImageView iVNewElementTop, iVNewElementBottom;
    protected RecyclerView.LayoutManager mLayoutManager;
    SyncedListAdapter syncedListAdapter, syncedListCheckedAdapter;

    /**
     * onCreate
     *
     * @param savedInstanceState
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerViewChecked = findViewById(R.id.recyclerViewChecked);
        eTNewElement = findViewById(R.id.eTNewElement);
        iVNewElementTop = findViewById(R.id.iVNewElementTop);
        iVNewElementBottom = findViewById(R.id.iVNewElementBottom);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChecked.setLayoutManager(new LinearLayoutManager(this));
        eTNewElement.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                createNewElement(false); // Need to read default from
                return true;
            }
            return false;
        });
        iVNewElementTop.setOnClickListener(v -> createNewElement(true));
        iVNewElementBottom.setOnClickListener(v -> createNewElement(false));
        localStorage = new LocalStorage(this);
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
            case R.id.list_clear:
                SyncedListStep syncedListStep =
                        new SyncedListStep(null, ACTION.CLEAR, null);
                syncedList.addElementStep(syncedListStep);
                if (!save()) {
                    return false;
                }
                syncedListAdapter.updateItems(syncedList.getElements(), true);
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
            syncedList = localStorage
                    .getList(getIntent().getExtras().getString("id"));
        } catch (Exception e) {
            Log.e(Constant.LOG_TITLE_DEFAULT, "Local storage read error: " + e);
            e.printStackTrace();
        }
        setTitle(syncedList.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        syncedListAdapter = new SyncedListAdapter(this, syncedList.getHeader()
                                                                .isCheckedList()
                                                        ? syncedList
                                                                .getUncheckedElements()
                                                        : syncedList
                                                                .getElements(),
                                                  recyclerView,
                                                  syncedList.getHeader()
                                                          .isCheckOption(),
                                                  syncedList.getHeader()
                                                          .isCheckedList()) {
            @Override public void onAddStep(SyncedListStep syncedListStep,
                                            boolean notify) {
                syncedList.addElementStep(syncedListStep);
                if (syncedList.getHeader().isCheckedList()) {
                    syncedListAdapter
                            .updateItems(syncedList.getUncheckedElements(),
                                         notify);
                    syncedListCheckedAdapter
                            .updateItems(syncedList.getCheckedElements(),
                                         notify);
                } else {
                    syncedListAdapter
                            .updateItems(syncedList.getElements(), notify);
                }
                save();
            }
        };
        recyclerView.setAdapter(syncedListAdapter);

        if (syncedList.getHeader().isCheckedList()) {
            syncedListCheckedAdapter =
                    new SyncedListAdapter(this, syncedList.getCheckedElements(),
                                          recyclerView, syncedList.getHeader()
                                                  .isCheckOption(),
                                          syncedList.getHeader()
                                                  .isCheckedList()) {
                        @Override
                        public void onAddStep(SyncedListStep syncedListStep,
                                              boolean notify) {
                            syncedList.addElementStep(syncedListStep);
                            syncedListAdapter.updateItems(
                                    syncedList.getUncheckedElements(), notify);
                            syncedListCheckedAdapter.updateItems(
                                    syncedList.getCheckedElements(), notify);
                            save();
                        }
                    };
            recyclerViewChecked.setAdapter(syncedListCheckedAdapter);
        } else {
            TextView tVCheckedListTitle = findViewById(R.id.tVcheckedListTitle);
            recyclerViewChecked.setVisibility(View.GONE);
            tVCheckedListTitle.setVisibility(View.GONE);
        }
    }

    /**
     * Read name from edittext on bottom and create new element inside list
     * Returns: success(true)
     */
    public boolean createNewElement(boolean top) {
        String id = syncedList.generateUniqueElementId();
        SyncedListStep syncedListStep = new SyncedListStep(id, ACTION.ADD,
                                                           new SyncedListElement(
                                                                   id,
                                                                   eTNewElement
                                                                           .getText()
                                                                           .toString(),
                                                                   ""));
        syncedList.addElementStep(syncedListStep);
        if (top) {
            SyncedListStep syncedListStepMove =
                    new SyncedListStep(id, ACTION.MOVE, 0);
            syncedList.addElementStep(syncedListStepMove);
        }
        if (!save()) {
            return false;
        }
        if (syncedList.getHeader().isCheckedList()) {
            syncedListAdapter
                    .updateItems(syncedList.getUncheckedElements(), true);

            syncedListCheckedAdapter
                    .updateItems(syncedList.getCheckedElements(), true);
        } else {
            syncedListAdapter.updateItems(syncedList.getElements(), true);
            recyclerView.scrollToPosition(
                    top ? 0 : syncedList.getElements().size() - 1);
        }
        eTNewElement.setText("");
        Log.d(Constant.LOG_TITLE_DEFAULT, "New element added to list");
        return true;
    }

    public boolean save() {
        try {
            localStorage.setList(syncedList, false);
        } catch (IOException | JSONException e) {
            Log.e(Constant.LOG_TITLE_DEFAULT,
                  "Local storage " + "write" + " error: " + e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override public void onBackPressed() {
        finish();
    }
}