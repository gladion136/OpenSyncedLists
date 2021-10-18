package eu.schmidt.systems.opensyncedlists.utils;

import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;

public abstract class ServerConnection {
    public static boolean check_connection() {
        return false;
    }

    public static SyncedList getList(String name, String secret) {
        return new SyncedList();
    }

    public static boolean setList(SyncedList syncedList) {
        return false;
    }

    public static boolean removeList(String name, String secret) {
        return false;
    }

    public static boolean addList(SyncedList syncedList) {
        return false;
    }
}
