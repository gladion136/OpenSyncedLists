package eu.schmidt.systems.opensyncedlists.datatypes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Header for a SyncedList
 */
public class SyncedListHeader {
    private String id;
    private String name;
    private byte[] secret;
    private byte[] localSecret;

    public SyncedListHeader(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getString("id");
        name = jsonObject.getString("name");
        secret = jsonObject.getString("secret").getBytes();
        localSecret = jsonObject.getString("localSecret").getBytes();
    }

    public SyncedListHeader(String id,
                            String name,
                            byte[] secret,
                            byte[] localSecret) {
        this.id = id;
        this.name = name;
        this.secret = secret;
        this.localSecret = localSecret;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getSecret() {
        return secret;
    }

    public void setSecret(byte[] secret) {
        this.secret = secret;
    }

    public byte[] getLocalSecret() {
        return localSecret;
    }

    public void setLocalSecret(byte[] localSecret) {
        this.localSecret = localSecret;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("name", name);
        jsonObject.put("secret", new String(secret));
        jsonObject.put("localSecret", new String(localSecret));
        return jsonObject;
    }
}
