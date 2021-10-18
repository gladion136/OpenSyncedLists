package eu.schmidt.systems.opensyncedlists;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;

import java.util.ArrayList;
import java.util.Random;

import eu.schmidt.systems.opensyncedlists.adapter.SyncedListAdapter;
import eu.schmidt.systems.opensyncedlists.datatypes.ACTION;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListStep;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;

public class ListActivity extends AppCompatActivity {

    public static final String DEBUG = "DEBUG_MAINACTIVITY";

    SyncedList syncedList;
    RecyclerView recyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;

    /**
     * onCreate
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        recyclerView = findViewById(R.id.recyclerView);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (recyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) recyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }
        recyclerView.scrollToPosition(scrollPosition);

        init();
        createTestData();
        updateListView();
    }

    /**
     * onCreateOptionsMenu
     *
     * @param menu Menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.one_list_menu, menu);
        return true;
    }

    /**
     * Initialize
     */
    public void init() {
        syncedList = new SyncedList("12", "List 1", null,
                new ArrayList<>());
        syncedList.setSecret("Test");
        Log.d(DEBUG, Cryptography.bytesToString(syncedList.getSecret()));
        setTitle(getIntent().getExtras().getString("Name"));
    }

    /**
     * update Listview
     */
    public void updateListView() {
        recyclerView.setAdapter(new SyncedListAdapter(this, syncedList.getElements(),
                recyclerView) {
            @Override public void onAddStep(SyncedListStep syncedListStep) {
                syncedList.addElementStep(syncedListStep);
                updateListView();
            }
        });
    }

    /**
     * Create test data
     */
    public void createTestData() {
        SyncedListStep syncedListStep = new SyncedListStep("1",
                ACTION.ADD,
                new SyncedListElement("Brot", "Vollkorn"));
        syncedList.addElementStep(syncedListStep);

        SyncedListStep syncedListStep2 = new SyncedListStep("2",
                ACTION.ADD,
                new SyncedListElement("Salat", "Eisberg"));
        syncedList.addElementStep(syncedListStep2);

        SyncedListStep syncedListStep3 = new SyncedListStep("1",
                ACTION.REMOVE, null);
        syncedList.addElementStep(syncedListStep3);

        for(int x=0; x < 30; x++) {
            int leftLimit = 48; // numeral '0'
            int rightLimit = 220; // letter 'z'
            int targetStringLength = 10 + 5 * x;
            Random random = new Random();

            String generatedString = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                generatedString = random.ints(leftLimit, rightLimit + 1)
                        .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                        .limit(targetStringLength)
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                        .toString();
            }

            SyncedListStep syncedListStepi = new SyncedListStep("2",
                    ACTION.ADD,
                    new SyncedListElement("Placeholder: " + x,
                            "Test description - " + generatedString));
            syncedList.addElementStep(syncedListStepi);
        }
    }
}