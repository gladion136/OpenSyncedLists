package eu.schmidt.systems.opensyncedlists.datatypes;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SyncedListStep {
    Integer id;
    Long timestamp;

    String changeId;
    ACTION changeAction;
    Object changeValue;

    public SyncedListStep(String changeId,
                          ACTION changeAction, Object changeValue) {
        this.changeId = changeId;
        this.changeAction = changeAction;
        this.changeValue = changeValue;
        this.timestamp = System.nanoTime();
        calcNewId();
    }

    public Integer calcNewId() {
        this.id = new Random().nextInt(1999999999);
        return this.id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
}
