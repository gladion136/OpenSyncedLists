package eu.schmidt.systems.opensyncedlists.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListHeader;

public class LocalStorage extends Storage {
    Context context;
    SharedPreferences sharedPref;

    /**
     * Open private local shared preferences storage
     *
     * @param context Context
     */
    public LocalStorage(Context context) {
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
    @Override public void setList(SyncedList list, boolean headerChanged)
            throws IOException, JSONException {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("LIST_" + list.getId(), list.toJSON().toString());
        editor.apply();
        Log.d(Constant.LOG_TITLE_DEFAULT, "Save List: " + list.getName());
        if (headerChanged) {
            ArrayList<SyncedListHeader> headers = getListsHeaders();
            for (int i = 0; i < headers.size(); i++) {
                if (headers.get(i).getId().equals(list.getId())) {
                    headers.set(i, list.getHeader());
                    Log.d(Constant.LOG_TITLE_DEFAULT,
                          "Header of list " + "changed to: " + list.getName());
                    break;
                }
            }
            setListsHeaders(headers);
        }
    }

    /**
     * Read on list from local storage
     *
     * @param id
     * @return SyncedList
     * @throws IOException
     * @throws JSONException
     */
    @Override public SyncedList getList(String id) throws Exception {
        String data = sharedPref.getString("LIST_" + id, "");
        if (data.equals("")) {
            return null;
        }

        SyncedList result =
                new SyncedList(getListHeader(id), new JSONObject(data));
        Log.d(Constant.LOG_TITLE_DEFAULT,
              "Load Lists Headers: " + result.toString());
        return result;
    }

    /**
     * Get all lists headers
     *
     * @return ArrayList<SyncedListHeader>
     * @throws JSONException
     */
    @Override public ArrayList<SyncedListHeader> getListsHeaders()
            throws JSONException {
        String data = sharedPref.getString("LISTS_HEADERS", "");
        if (data.equals("")) {
            return new ArrayList<>();
        }
        ArrayList<SyncedListHeader> result = new ArrayList<>();
        JSONArray jsonArrayLists = new JSONArray(data);
        for (int i = 0; i < jsonArrayLists.length(); i++) {
            result.add(
                    new SyncedListHeader((JSONObject) jsonArrayLists.get(i)));
        }
        Log.d(Constant.LOG_TITLE_DEFAULT,
              "Load Lists Headers: " + result.toString());
        return result;
    }

    @Override public SyncedListHeader getListHeader(String id)
            throws Exception {
        ArrayList<SyncedListHeader> headers = getListsHeaders();
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).getId().equals(id)) {
                Log.d(Constant.LOG_TITLE_DEFAULT,
                      "Found header: " + headers.get(i).getName());
                return headers.get(i);
            }
        }
        throw new Exception("Header not found");
    }

    /**
     * Override all lists headers
     *
     * @param headers all Headers
     * @throws JSONException
     */
    @Override public void setListsHeaders(ArrayList<SyncedListHeader> headers)
            throws JSONException {
        SharedPreferences.Editor editor = sharedPref.edit();
        JSONArray jsonArray = new JSONArray();
        for (SyncedListHeader header : headers) {
            jsonArray.put(header.toJSON());
        }
        editor.putString("LISTS_HEADERS", jsonArray.toString());
        editor.apply();
        Log.d(Constant.LOG_TITLE_DEFAULT,
              "Save Lists Headers: " + jsonArray.toString());
    }

    @Override public void deleteList(String id) throws Exception {
        ArrayList<SyncedListHeader> headers = getListsHeaders();
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).getId().equals(id)) {
                headers.remove(i);
            }
        }
        setListsHeaders(headers);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove("LIST_" + id);
        editor.apply();
    }
}
