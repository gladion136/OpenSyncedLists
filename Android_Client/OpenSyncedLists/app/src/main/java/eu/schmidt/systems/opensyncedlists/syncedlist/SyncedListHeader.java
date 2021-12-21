package eu.schmidt.systems.opensyncedlists.syncedlist;

import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import eu.schmidt.systems.opensyncedlists.utils.Cryptography;

/**
 * Header for a SyncedList, contains all information about a list
 */
public class SyncedListHeader {
    private String id;
    private String name;
    private boolean checkOption;
    private boolean checkedList;
    private boolean autoSync;
    private boolean invertElement;
    private String hostname;
    private byte[] secret;
    private SecretKey localSecret;
    private String listSize;

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
        invertElement = jsonObject.getBoolean("invertElement");
        hostname = jsonObject.getString("hostname");
        listSize = jsonObject.getString("listSize");
        secret = Cryptography.stringToByteArray(jsonObject.getString("secret"));
        byte[] encodedKey = Cryptography
                .stringToByteArray(jsonObject.getString("localSecret"));
        localSecret =
                new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
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
                            SecretKey localSecret) {
        this.id = id;
        this.name = name;
        this.hostname = hostname;
        this.secret = secret;
        this.localSecret = localSecret;
        this.checkOption = true;
        this.checkedList = true;
        this.invertElement = false;
        this.autoSync = true;
        this.listSize = "0 / 0";
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

    public SecretKey getLocalSecret() {
        return localSecret;
    }

    public void setLocalSecret(SecretKey localSecret) {
        this.localSecret = localSecret;
    }

    public boolean isInvertElement() {
        return invertElement;
    }

    public void setInvertElement(boolean invertElement) {
        this.invertElement = invertElement;
    }

    public String getListSize() {
        return listSize;
    }

    public void setListSize(String listSize) {
        this.listSize = listSize;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("name", name);
            jsonObject.put("hostname", hostname);
            jsonObject.put("checkOption", checkOption);
            jsonObject.put("checkedList", checkedList);
            jsonObject.put("autoSync", autoSync);
            jsonObject.put("invertElement", invertElement);
            jsonObject.put("listSize", listSize);
            jsonObject.put("secret", Cryptography.byteArrayToString(secret));
            jsonObject.put("localSecret", Cryptography
                    .byteArrayToString(localSecret.getEncoded()));
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return jsonObject;
    }
}
