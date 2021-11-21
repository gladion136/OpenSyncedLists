package eu.schmidt.systems.opensyncedlists.datatypes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One changes Step for a SyncedList
 */
public class SyncedListStep {
    Long timestamp;

    String changeId;
    ACTION changeAction;
    Object changeValue;

    public SyncedListStep(JSONObject jsonObject) throws JSONException {
        this.changeId = jsonObject.getString("changeId");
        this.changeAction = ACTION.values()[jsonObject.getInt("changeAction")];
        if (jsonObject.has("changeValueElement")) {
            this.changeValue = new SyncedListElement(
                    jsonObject.getJSONObject("changeValueElement"));
        } else {
            this.changeValue = jsonObject.get("changeValue");
        }
    }

    public SyncedListStep(String changeId,
                          ACTION changeAction,
                          Object changeValue) {
        this.changeId = changeId;
        this.changeAction = changeAction;
        this.changeValue = changeValue;
        this.timestamp = System.nanoTime();
    }

    public String getChangeId() {
        return changeId;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    public ACTION getChangeAction() {
        return changeAction;
    }

    public void setChangeAction(ACTION changeAction) {
        this.changeAction = changeAction;
    }

    public Object getChangeValue() {
        return changeValue;
    }

    public void setChangeValue(Object changeValue) {
        this.changeValue = changeValue;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("changeId", this.changeId);
        jsonObject.put("changeAction", this.changeAction.ordinal());
        if (this.changeValue instanceof SyncedListElement) {
            jsonObject.put("changeValueElement",
                           ((SyncedListElement) this.changeValue).toJSON());
        } else {
            jsonObject.put("changeValue", this.changeValue);
        }
        return jsonObject;
    }
}
