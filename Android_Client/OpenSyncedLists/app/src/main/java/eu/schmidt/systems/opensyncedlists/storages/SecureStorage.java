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
package eu.schmidt.systems.opensyncedlists.storages;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.syncedlist.ListTag;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.utils.Constant;

public class SecureStorage
{
    final Context context;
    final SharedPreferences sharedPref;
    
    /**
     * Open private local shared preferences storage.
     *
     * @param context Context
     */
    public SecureStorage(Context context)
    {
        this.context = context;
        sharedPref = context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE);
    }
    
    /**
     * Save one list in local storage
     *
     * @param list List to store
     * @throws IOException
     * @throws JSONException
     */
    public void setList(SyncedList list) throws IOException, JSONException
    {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("LIST_" + list.getId(), list.toJSON().toString());
        editor.putString("HEADER_" + list.getId(),
            list.getHeader().toJSON().toString());
        Log.d(Constant.LOG_TITLE_STORAGE, "Save List");
        editor.apply();
    }
    
    /**
     * Read one list from local storage
     *
     * @param id id of list
     * @return SyncedList
     * @throws IOException
     * @throws JSONException
     */
    public SyncedList getList(String id) throws Exception
    {
        String data = sharedPref.getString("LIST_" + id, "");
        if (data.equals(""))
        {
            return null;
        }
        return new SyncedList(getListHeader(id), new JSONObject(data));
    }
    
    /**
     * Get the headers from all lists.
     *
     * @return all Headers
     * @throws Exception
     */
    public ArrayList<SyncedListHeader> getListsHeaders() throws Exception
    {
        ArrayList<String> ids = getListsIds();
        if (ids.size() <= 0)
        {
            return new ArrayList<>();
        }
        ArrayList<SyncedListHeader> result = new ArrayList<>();
        for (String id : ids)
        {
            String data = sharedPref.getString("HEADER_" + id, "");
            result.add(new SyncedListHeader(new JSONObject(data)));
        }
        Log.d(Constant.LOG_TITLE_STORAGE, "Load Lists Headers");
        return result;
    }
    
    /**
     * Get all list ids.
     *
     * @return all list ids
     * @throws Exception
     */
    public ArrayList<String> getListsIds() throws Exception
    {
        String data = sharedPref.getString("IDs", "");
        if (data.equals(""))
        {
            return new ArrayList<>();
        }
        ArrayList<String> result = new ArrayList<>();
        JSONArray jsonArrayLists = new JSONArray(data);
        for (int i = 0; i < jsonArrayLists.length(); i++)
        {
            result.add(jsonArrayLists.getString(i));
        }
        Log.d(Constant.LOG_TITLE_STORAGE, "Load Lists IDs");
        return result;
    }
    
    /**
     * Set all list ids.
     *
     * @param ids all ids
     * @throws Exception
     */
    public void setListsIds(ArrayList<String> ids) throws Exception
    {
        SharedPreferences.Editor editor = sharedPref.edit();
        JSONArray jsonArray = new JSONArray();
        for (String id : ids)
        {
            jsonArray.put(id);
        }
        editor.putString("IDs", jsonArray.toString());
        editor.apply();
        Log.d(Constant.LOG_TITLE_STORAGE, "Save " + ids.size() + " Lists IDs");
    }
    
    /**
     * Get the header from a list.
     *
     * @param id id of the list
     * @return header of the list
     * @throws Exception
     */
    public SyncedListHeader getListHeader(String id) throws Exception
    {
        String data = sharedPref.getString("HEADER_" + id, "");
        if (data.equals(""))
        {
            throw new Exception("Header not found");
        }
        return new SyncedListHeader(new JSONObject(data));
    }
    
    /**
     * Delete a list.
     *
     * @param id id of the list
     * @throws Exception
     */
    public void deleteList(String id) throws Exception
    {
        ArrayList<String> ids = getListsIds();
        for (int i = 0; i < ids.size(); i++)
        {
            if (ids.get(i).equals(id))
            {
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
    
    /**
     * Add a list.
     *
     * @param syncedList list to add
     * @return info text for example if another list got overridden.
     * @throws Exception
     */
    public String addList(SyncedList syncedList) throws Exception
    {
        String result = "";
        boolean newList = true;
        ArrayList<String> ids = getListsIds();
        for (int i = 0; i < ids.size(); i++)
        {
            String id = ids.get(i);
            if (id.equals(syncedList.getId()))
            {
                newList = false;
                result =
                    context.getString(R.string.list_with_id_got_overridden);
                break;
            }
        }
        if (newList)
        {
            ids.add(syncedList.getId());
            setListsIds(ids);
        }
        setList(syncedList);
        return result;
    }
    
    public ArrayList<ListTag> getAllTags() throws Exception
    {
        ArrayList<ListTag> allTags = new ArrayList<>();
        ArrayList<SyncedListHeader> headers = getListsHeaders();
        ListTag untaggedTag = new ListTag(context.getString(R.string.untagged));
        untaggedTag.untagged = true;
        untaggedTag.filterEnabled = false;
        allTags.add(untaggedTag);
        
        String tagListJson = sharedPref.getString("allTags", "");
        if (tagListJson.equals(""))
        {
            ListTag default_tag_favorites = new ListTag(
                context.getString(R.string.default_tag_title_favorites));
            default_tag_favorites.filterEnabled = false;
            default_tag_favorites.colorHex = "#f06292";
            allTags.add(default_tag_favorites);
            ListTag default_tag_shopping = new ListTag(
                context.getString(R.string.default_tag_title_shopping));
            default_tag_shopping.filterEnabled = false;
            default_tag_shopping.colorHex = "#ff8a65";
            allTags.add(default_tag_shopping);
            ListTag default_tag_ideas = new ListTag(
                context.getString(R.string.default_tag_title_projects));
            default_tag_ideas.filterEnabled = false;
            default_tag_ideas.colorHex = "#4fc3f7";
            allTags.add(default_tag_ideas);
            ListTag default_tag_work =
                new ListTag(context.getString(R.string.default_tag_title_work));
            default_tag_work.filterEnabled = false;
            default_tag_work.colorHex = "#dce775";
            allTags.add(default_tag_work);
        }
        try
        {
            JSONArray tagsJsonArray = new JSONArray(tagListJson);
            for (int i = 0; i < tagsJsonArray.length(); i++)
            {
                JSONObject tagJsonObject = tagsJsonArray.getJSONObject(i);
                ListTag tag = new ListTag(tagJsonObject);
                if (tag.untagged)
                {
                    continue;
                }
                if (allTags.stream()
                    .noneMatch(tag2 -> tag2.name.equals(tag.name)))
                {
                    allTags.add(tag);
                }
                else
                {
                    allTags.replaceAll(existingTag ->
                    {
                        if (existingTag.name.equals(tag.name))
                        {
                            return tag;
                        }
                        return existingTag;
                    });
                }
            }
        }
        catch (JSONException e)
        {
            Log.e("SecureStorage", "Error parsing tags JSON: " + e);
        }
        
        for (int i = 0; i < headers.size(); i++)
        {
            SyncedListHeader header = headers.get(i);
            ArrayList<ListTag> headerTags = header.getTagList();
            
            if (headerTags != null)
            {
                for (int j = 0; j < headerTags.size(); j++)
                {
                    ListTag tag = headerTags.get(j);
                    if (allTags.stream()
                        .noneMatch(t -> t.name.equals(tag.name)))
                    {
                        allTags.add(tag);
                    }
                }
            }
        }
        
        for (ListTag tag : allTags)
        {
            Log.d("SecureStorage",
                "Tag: " + tag.name + ", Color: " + tag.colorHex
                    + ", FilterEnabled: " + tag.filterEnabled);
        }
        return allTags;
    }
    
    public void saveAllTags(ArrayList<ListTag> list, boolean update_all_lists)
        throws Exception
    {
        JSONArray jsonArray = new JSONArray();
        for (ListTag tag : list)
        {
            jsonArray.put(tag.toJSON());
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("allTags", jsonArray.toString());
        editor.apply();
        
        if (!update_all_lists)
        {
            return;
        }
        for (SyncedListHeader listHeader : getListsHeaders())
        {
            for (ListTag tag : list)
            {
                listHeader.getTagList().stream()
                    .filter(t -> t.name.equals(tag.name)).forEach(t ->
                    {
                        t.colorHex = tag.colorHex;
                        Log.d("SecureStorage",
                            "Tag color updated: " + t.name + " from " + t.colorHex
                                + " to " + tag.colorHex);
                        t.filterEnabled = tag.filterEnabled;
                    });
            }
        }
    }
}
