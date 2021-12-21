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
package eu.schmidt.systems.opensyncedlists.syncedlist;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An Element of a SyncedList.
 */
public class SyncedListElement {
    private String id;
    private Boolean checked;
    private String name;
    private String description;

    /**
     * Load element from JSON
     *
     * @param jsonObject element as JSON
     * @throws JSONException
     */
    public SyncedListElement(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getString("id");
        checked = jsonObject.getBoolean("checked");
        name = jsonObject.getString("name");
        description = jsonObject.getString("description");
    }

    /**
     * Create a list element
     *
     * @param id          id of the element
     * @param name        name of the element
     * @param description description of the element
     */
    public SyncedListElement(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.checked = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get SyncedListElement as JSON
     *
     * @return SyncedListElement as JSON
     */
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("checked", checked);
            jsonObject.put("name", name);
            jsonObject.put("description", description);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    /**
     * Get SyncedListElement as Markdown.
     *
     * @return SyncedListElement as Markdown
     */
    public String getAsMarkdown() {
        String result = "";
        result += checked ? "[x]" : "[ ]";
        result += " " + getName();
        if (!getDescription().equals("")) {
            result += " - " + getDescription();
        }
        return result;
    }

    /**
     * Clone the SyncedlistElement into a new SyncedListElement Object.
     *
     * @return cloned Object
     */
    public SyncedListElement clone() {
        try {
            return new SyncedListElement(toJSON());
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return null;
    }
}
