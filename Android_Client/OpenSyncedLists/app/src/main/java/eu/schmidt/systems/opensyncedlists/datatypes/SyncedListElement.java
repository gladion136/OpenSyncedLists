package eu.schmidt.systems.opensyncedlists.datatypes;

import android.graphics.RadialGradient;

import java.util.Random;

public class SyncedListElement {
    String id;
    String name;
    String description;

    public SyncedListElement() {
    }

    public SyncedListElement(String name, String description) {
        this.name = name;
        this.description = description;
        calcNewId();
    }

    public String getId() {
        return id;
    }

    public String calcNewId() {
        Random random = new Random(new Random().nextInt(1999999999));
        this.id =
                random.nextInt(1999999999) + " - "
                        + random.nextInt(1999999999) + " - "
                        + random.nextInt(1999999999) + " - "
                        + random.nextInt(1999999999) + " - "
                        + random.nextInt(1999999999);
        return this.id;
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
}
