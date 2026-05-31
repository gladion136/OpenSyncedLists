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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import javax.crypto.spec.SecretKeySpec;

import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;

/**
 * Unit tests for {@link DefaultListSettingsApplier#applyToHeader}: verifies
 * that each "default list setting" key maps to the correct header mutation when
 * applied to an existing list.
 */
@RunWith(RobolectricTestRunner.class)
public class DefaultListSettingsApplierTest
{
    private DefaultListSettingsApplier applier;
    private SyncedList list;

    @Before public void setUp()
    {
        // SecureStorage is only used by applyToAllLists/countLists, not by the
        // pure applyToHeader logic under test here.
        applier = new DefaultListSettingsApplier(null);
        list = createTestList();
    }

    @Test public void appliesCheckOptionAndMirrorsCheckedList()
    {
        list.getHeader().setCheckOption(true);
        list.getHeader().setCheckedList(true);

        boolean changed = applier.applyToHeader(list,
            DefaultListSettingsApplier.KEY_CHECK_OPTION, false);

        Assert.assertTrue(changed);
        Assert.assertFalse(list.getHeader().isCheckOption());
        // check_option mirrors into checked_list.
        Assert.assertFalse(list.getHeader().isCheckedList());
    }

    @Test public void appliesCheckedList()
    {
        list.getHeader().setCheckedList(false);
        boolean changed = applier.applyToHeader(list,
            DefaultListSettingsApplier.KEY_CHECKED_LIST, true);
        Assert.assertTrue(changed);
        Assert.assertTrue(list.getHeader().isCheckedList());
    }

    @Test public void appliesJumpButtons()
    {
        list.getHeader().setJumpButtons(false);
        applier.applyToHeader(list,
            DefaultListSettingsApplier.KEY_JUMP_BUTTONS, true);
        Assert.assertTrue(list.getHeader().isJumpButtons());
    }

    @Test public void appliesInvertElement()
    {
        list.getHeader().setInvertElement(false);
        applier.applyToHeader(list,
            DefaultListSettingsApplier.KEY_INVERT_ELEMENT, true);
        Assert.assertTrue(list.getHeader().isInvertElement());
    }

    @Test public void appliesAutoSync()
    {
        list.getHeader().setAutoSync(false);
        applier.applyToHeader(list, DefaultListSettingsApplier.KEY_SYNC, true);
        Assert.assertTrue(list.getHeader().isAutoSync());
    }

    @Test public void appliesDefaultServer()
    {
        applier.applyToHeader(list,
            DefaultListSettingsApplier.KEY_DEFAULT_SERVER,
            "https://example.org");
        Assert.assertEquals("https://example.org",
            list.getHeader().getHostname());
    }

    @Test public void defaultServerNullClearsHostname()
    {
        list.getHeader().setHostname("https://old.example");
        applier.applyToHeader(list,
            DefaultListSettingsApplier.KEY_DEFAULT_SERVER, null);
        Assert.assertEquals("", list.getHeader().getHostname());
    }

    @Test public void unknownKeyDoesNothing()
    {
        boolean changed = applier.applyToHeader(list, "unknown_key", true);
        Assert.assertFalse(changed);
    }

    @Test public void isApplicableKeyMatchesKnownKeys()
    {
        Assert.assertTrue(DefaultListSettingsApplier.isApplicableKey(
            DefaultListSettingsApplier.KEY_JUMP_BUTTONS));
        Assert.assertFalse(
            DefaultListSettingsApplier.isApplicableKey("design"));
    }

    private SyncedList createTestList()
    {
        byte[] aesKey = new byte[32];
        for (int i = 0; i < aesKey.length; i++)
        {
            aesKey[i] = (byte) i;
        }
        SecretKeySpec localSecret = new SecretKeySpec(aesKey, "AES");
        SyncedListHeader header =
            new SyncedListHeader("list", "Test List", "localhost", aesKey,
                localSecret);
        return new SyncedList(header, new ArrayList<>());
    }
}
