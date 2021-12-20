package eu.schmidt.systems.opensyncedlists.syncedlist;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An Element of a SyncedList
 */
public class SyncedListElement {
    private String id;
    private Boolean checked;
    private String name;
    private String description;

    public SyncedListElement(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getString("id");
        checked = jsonObject.getBoolean("checked");
        name = jsonObject.getString("name");
        description = jsonObject.getString("description");
    }

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

    public String getAsReadableString() {
        String result = "";
        result += checked ? "[x]" : "[ ]";
        result += " " + getName();
        if (!getDescription().equals("")) {
            result += " - " + getDescription();
        }
        return result;
    }

    public SyncedListElement clone() {
        try {
            return new SyncedListElement(toJSON());
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return null;
    }
}
