package eu.schmidt.systems.opensyncedlists.datatypes;

import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_DEFAULT;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

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
        secret = Cryptography.stringtoByteArray(jsonObject.getString("secret"));

        byte[] encodedKey = Cryptography
                .stringtoByteArray(jsonObject.getString("localSecret"));
        SecretKey secretKey =
                new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
        localSecret = secretKey;
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
            jsonObject.put("secret", Cryptography.byteArraytoString(secret));
            jsonObject.put("localSecret", Cryptography
                    .byteArraytoString(localSecret.getEncoded()));
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return jsonObject;
    }
}
