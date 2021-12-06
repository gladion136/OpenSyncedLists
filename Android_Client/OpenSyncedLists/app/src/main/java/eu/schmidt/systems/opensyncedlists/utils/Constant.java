package eu.schmidt.systems.opensyncedlists.utils;

import eu.schmidt.systems.opensyncedlists.BuildConfig;

/**
 * Constants that can used without context.
 */
public class Constant {
    public final static String LOG_TITLE_DEFAULT = "DefaultLog";
    public final static String LOG_TITLE_BUILDING = "BuildingLog";
    public final static String LOG_TITLE_STORAGE = "StorageLog";
    public final static String LOG_TITLE_NETWORK = "NetworkLog";
    public final static String LOG_TITLE_SYNC = "SyncLog";
    public final static String FILE_PROVIDER_AUTHORITY =
            BuildConfig.APPLICATION_ID + ".fileprovider";
}
