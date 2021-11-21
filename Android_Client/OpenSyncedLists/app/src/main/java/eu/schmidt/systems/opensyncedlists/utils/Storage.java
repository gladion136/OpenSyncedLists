package eu.schmidt.systems.opensyncedlists.utils;

import org.json.JSONException;
import java.io.IOException;
import java.util.ArrayList;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListHeader;

/**
 * Storage parent class offer easy replacement of storage
 */
public abstract class Storage {
    public abstract void setList(SyncedList list)
            throws IOException, ClassNotFoundException, JSONException;

    public abstract SyncedList getList(SyncedListHeader syncedListHeader)
            throws IOException, ClassNotFoundException, JSONException;

    public abstract ArrayList<SyncedListHeader> getListsHeaders() throws JSONException;

    public abstract void setListsHeaders(ArrayList<SyncedListHeader> headers)
            throws JSONException;
}
