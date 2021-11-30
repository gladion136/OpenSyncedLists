package eu.schmidt.systems.opensyncedlists.datatypes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Header for a SyncedList, contains all information about a list
 */
public class SyncedListHeader {
    private String id;
    private String name;
    private boolean checkOption;
    private boolean checkedList;
    private boolean autoSync;
    private String hostname;
    private byte[] secret;
    private byte[] localSecret;

    /**
     * Called when loading a list
     *
     * @param jsonObject
     * @throws JSONException
     */
    public SyncedListHeader(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getString("id");
        name = jsonObject.getString("name");
        checkOption = jsonObject.getBoolean("checkOption");
        checkedList = jsonObject.getBoolean("checkedList");
        autoSync = jsonObject.getBoolean("autoSync");
        hostname = jsonObject.getString("hostname");
        secret = jsonObject.getString("secret").getBytes();
        localSecret = jsonObject.getString("localSecret").getBytes();
    }

    /**
     * Called when creating a list
     *
     * @param id
     * @param name
     * @param secret
     * @param localSecret
     */
    public SyncedListHeader(String id,
                            String name,
                            String hostname,
                            byte[] secret,
                            byte[] localSecret) {
        this.id = id;
        this.name = name;
        this.hostname = hostname;
        this.secret = secret;
        this.localSecret = localSecret;
        this.checkOption = true;
        this.checkedList = true;
        this.autoSync = true;
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

    public boolean isCheckOption() {
        return checkOption;
    }

    public void setCheckOption(boolean checkOption) {
        this.checkOption = checkOption;
    }

    public boolean isCheckedList() {
        return checkedList;
    }

    public void setCheckedList(boolean checkedList) {
        this.checkedList = checkedList;
    }

    public boolean isAutoSync() {
        return autoSync;
    }

    public void setAutoSync(boolean autoSync) {
        this.autoSync = autoSync;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
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
        jsonObject.put("hostname", hostname);
        jsonObject.put("checkOption", checkOption);
        jsonObject.put("checkedList", checkedList);
        jsonObject.put("autoSync", autoSync);
        jsonObject.put("secret", new String(secret));
        jsonObject.put("localSecret", new String(localSecret));
        return jsonObject;
    }
}
