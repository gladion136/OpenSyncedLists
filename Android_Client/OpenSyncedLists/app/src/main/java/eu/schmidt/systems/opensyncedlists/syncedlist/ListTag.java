/*
 * Copyright (C) 2025  Etienne Schmidt (eschmidt@schmidt-ti.eu)
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
package eu.schmidt.systems.opensyncedlists.syncedlist;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class ListTag
{
    public String name;
    public Boolean filterEnabled;
    public Boolean untagged;
    public String colorHex;
    
    public ListTag(String name)
    {
        this.name = name;
        this.filterEnabled = false;
        this.untagged = false;
        this.colorHex = "#e0e0e0"; // Default color
    }
    
    public ListTag(JSONObject jsonObject) throws JSONException
    {
        name = "UNKNOWN";
        filterEnabled = false;
        untagged = false;
        this.colorHex = "#e0e0e0";
        try
        {
            name = jsonObject.getString("name");
            filterEnabled = jsonObject.getBoolean("filterEnabled");
            untagged = jsonObject.getBoolean("untagged");
            if (jsonObject.has("colorHex"))
            {
                colorHex = jsonObject.getString("colorHex");
            }
        }
        catch (JSONException exception)
        {
            Log.e("ListTag", "Error parsing JSON: " + exception.getMessage());
        }
    }
    
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        try
        {
            jsonObject.put("name", name);
            jsonObject.put("filterEnabled", filterEnabled);
            jsonObject.put("untagged", untagged);
            jsonObject.put("colorHex", colorHex);
        }
        catch (JSONException exception)
        {
            exception.printStackTrace();
        }
        return jsonObject;
    }
}
