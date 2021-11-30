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
    public abstract void setList(SyncedList list, boolean headerChanged)
            throws IOException, ClassNotFoundException, JSONException;

    public abstract SyncedList getList(String id)
            throws IOException, ClassNotFoundException, JSONException,
            Exception;

    public abstract ArrayList<SyncedListHeader> getListsHeaders() throws JSONException;

    public abstract SyncedListHeader getListHeader(String id) throws Exception;

    public abstract void setListsHeaders(ArrayList<SyncedListHeader> headers)
            throws JSONException;

    public abstract void deleteList(String id) throws Exception;
}
