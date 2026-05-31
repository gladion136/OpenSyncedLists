/*
 * Copyright (C) 2021  Etienne Schmidt (eschmidt@schmidt-ti.eu)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package eu.schmidt.systems.opensyncedlists.utils;

import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.storages.SecureStorage;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;

/**
 * Applies a "default list setting" (a global preference shown under
 * App settings &gt; Default values for new lists) to all existing lists.
 *
 * Changing a default preference only affects newly created lists by default.
 * This helper performs the optional second step: pushing the new value into the
 * header of every stored list.
 */
public class DefaultListSettingsApplier
{
    /** Preference keys that can be mirrored into existing list headers. */
    public static final String KEY_CHECK_OPTION = "check_option";
    public static final String KEY_CHECKED_LIST = "checked_list";
    public static final String KEY_JUMP_BUTTONS = "jump_buttons";
    public static final String KEY_INVERT_ELEMENT = "invert_element";
    public static final String KEY_SYNC = "sync";
    public static final String KEY_DEFAULT_SERVER = "default_server";

    private final SecureStorage secureStorage;

    public DefaultListSettingsApplier(SecureStorage secureStorage)
    {
        this.secureStorage = secureStorage;
    }

    /**
     * @return the number of stored lists (used for the confirmation dialog).
     */
    public int countLists()
    {
        try
        {
            return secureStorage.getListsIds().size();
        }
        catch (Exception e)
        {
            Log.e("DefaultListSettings", "Could not count lists: " + e);
            return 0;
        }
    }

    /**
     * Apply the given preference value to the header of every stored list and
     * persist each one.
     *
     * @param key   one of the KEY_* constants
     * @param value the new value (Boolean for switches, String for the server)
     * @return number of lists that were updated
     */
    public int applyToAllLists(String key, Object value)
    {
        int updated = 0;
        try
        {
            for (String id : secureStorage.getListsIds())
            {
                SyncedList list = secureStorage.getList(id);
                if (applyToHeader(list, key, value))
                {
                    secureStorage.setList(list);
                    updated++;
                }
            }
        }
        catch (Exception e)
        {
            Log.e("DefaultListSettings", "Could not apply to all lists: " + e);
        }
        return updated;
    }

    /**
     * Mutate a single list's header according to the changed preference.
     *
     * @param list  the list to mutate
     * @param key   one of the KEY_* constants
     * @param value the new value
     * @return true if the header was changed and the list needs saving
     */
    @VisibleForTesting boolean applyToHeader(SyncedList list, String key,
        Object value)
    {
        SyncedListHeader header = list.getHeader();
        switch (key)
        {
            case KEY_CHECK_OPTION:
            {
                boolean on = (Boolean) value;
                header.setCheckOption(on);
                // Mirror ListSettingsFragment: the checked-list view follows
                // the checkbox option, then buffers must be recalculated.
                header.setCheckedList(on);
                list.recalculateBuffers();
                return true;
            }
            case KEY_CHECKED_LIST:
                header.setCheckedList((Boolean) value);
                list.recalculateBuffers();
                return true;
            case KEY_JUMP_BUTTONS:
                header.setJumpButtons((Boolean) value);
                return true;
            case KEY_INVERT_ELEMENT:
                header.setInvertElement((Boolean) value);
                return true;
            case KEY_SYNC:
                header.setAutoSync((Boolean) value);
                return true;
            case KEY_DEFAULT_SERVER:
                header.setHostname(value == null ? "" : value.toString());
                return true;
            default:
                return false;
        }
    }

    /**
     * @param key preference key
     * @return true if the key can be mirrored to existing lists.
     */
    public static boolean isApplicableKey(String key)
    {
        ArrayList<String> keys = new ArrayList<>();
        keys.add(KEY_CHECK_OPTION);
        keys.add(KEY_CHECKED_LIST);
        keys.add(KEY_JUMP_BUTTONS);
        keys.add(KEY_INVERT_ELEMENT);
        keys.add(KEY_SYNC);
        keys.add(KEY_DEFAULT_SERVER);
        return keys.contains(key);
    }
}
