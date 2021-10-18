package eu.schmidt.systems.opensyncedlists;

import static eu.schmidt.systems.opensyncedlists.ListActivity.DEBUG;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ListView;

import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.adapter.ListsAdapater;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;

public class ListsActivity extends AppCompatActivity {

    ArrayList<SyncedList> syncedLists;

    ListView lVLists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lists);
        lVLists = findViewById(R.id.lVLists);
        init();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.all_lists_menu, menu);
        return true;
    }


    public void init() {
        syncedLists = new ArrayList<>();

        for (int i=0; i<30; i++) {
            SyncedList testList = new SyncedList("12"+i, "Liste "+(i+1), null,
                    new ArrayList<>());
            testList.setSecret("Test"+i);
            Log.d(DEBUG, Cryptography.bytesToString(testList.getSecret()));
            syncedLists.add(testList);
        }


        lVLists.setAdapter(new ListsAdapater(this, R.layout.lists_element,
                syncedLists));
    }
}