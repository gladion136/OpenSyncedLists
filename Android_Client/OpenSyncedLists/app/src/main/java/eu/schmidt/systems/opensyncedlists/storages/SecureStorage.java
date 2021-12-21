package eu.schmidt.systems.opensyncedlists.storages;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.utils.Constant;

public class SecureStorage {
    final Context context;
    final SharedPreferences sharedPref;

    /**
     * Open private local shared preferences storage
     *
     * @param context Context
     */
    public SecureStorage(Context context) {
        this.context = context;
        sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
    }

    /**
     * Save on list in local storage
     *
     * @param list List
     * @throws IOException
     * @throws JSONException
     */
    public void setList(SyncedList list) throws IOException, JSONException {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("LIST_" + list.getId(), list.toJSON().toString());
        editor.putString("HEADER_" + list.getId(),
                         list.getHeader().toJSON().toString());
        Log.d(Constant.LOG_TITLE_STORAGE, "Save List");
        editor.apply();
    }

    /**
     * Read on list from local storage
     *
     * @param id
     * @return SyncedList
     * @throws IOException
     * @throws JSONException
     */
    public SyncedList getList(String id) throws Exception {
        String data = sharedPref.getString("LIST_" + id, "");
        if (data.equals("")) {
            return null;
        }
        return new SyncedList(getListHeader(id), new JSONObject(data));
    }

    /**
     * Get all lists headers
     *
     * @return ArrayList<SyncedListHeader>
     * @throws Exception
     */
    public ArrayList<SyncedListHeader> getListsHeaders() throws Exception {
        ArrayList<String> ids = getListsIds();
        if (ids.size() <= 0) {
            return new ArrayList<>();
        }
        ArrayList<SyncedListHeader> result = new ArrayList<>();
        for (String id : ids) {
            String data = sharedPref.getString("HEADER_" + id, "");
            result.add(new SyncedListHeader(new JSONObject(data)));
        }
        Log.d(Constant.LOG_TITLE_STORAGE, "Load Lists Headers");
        return result;
    }

    public ArrayList<String> getListsIds() throws Exception {
        String data = sharedPref.getString("IDs", "");
        if (data.equals("")) {
            return new ArrayList<>();
        }
        ArrayList<String> result = new ArrayList<>();
        JSONArray jsonArrayLists = new JSONArray(data);
        for (int i = 0; i < jsonArrayLists.length(); i++) {
            result.add(jsonArrayLists.getString(i));
        }
        Log.d(Constant.LOG_TITLE_STORAGE, "Load Lists IDs");
        return result;
    }

    public void setListsIds(ArrayList<String> ids) throws Exception {
        SharedPreferences.Editor editor = sharedPref.edit();
        JSONArray jsonArray = new JSONArray();
        for (String id : ids) {
            jsonArray.put(id);
        }
        editor.putString("IDs", jsonArray.toString());
        editor.apply();
        Log.d(Constant.LOG_TITLE_STORAGE, "Save " + ids.size() + " Lists IDs");
    }

    public SyncedListHeader getListHeader(String id) throws Exception {
        String data = sharedPref.getString("HEADER_" + id, "");
        if (data.equals("")) {
            throw new Exception("Header not found");
        }
        return new SyncedListHeader(new JSONObject(data));
    }

    public void deleteList(String id) throws Exception {
        ArrayList<String> ids = getListsIds();
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i).equals(id)) {
                ids.remove(i);
                break;
            }
        }
        setListsIds(ids);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove("LIST_" + id);
        editor.remove("HEADER_" + id);
        editor.apply();
    }

    public String addList(SyncedList syncedList) throws Exception {
        String result = "";
        boolean newList = true;
        ArrayList<String> ids = getListsIds();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            if (id.equals(syncedList.getId())) {
                newList = false;
                result =
                        context.getString(R.string.list_with_id_got_overridden);
                break;
            }
        }
        if (newList) {
            ids.add(syncedList.getId());
            setListsIds(ids);
        }
        setList(syncedList);
        return result;
    }
}
