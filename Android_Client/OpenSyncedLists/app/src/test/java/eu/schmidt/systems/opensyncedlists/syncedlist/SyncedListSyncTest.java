/*
 * Copyright (C) 2025  Etienne Schmidt (eschmidt@schmidt-ti.eu)
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
package eu.schmidt.systems.opensyncedlists.syncedlist;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import javax.crypto.spec.SecretKeySpec;

/**
 * Tests for SyncedList synchronization functionality.
 */
@RunWith(RobolectricTestRunner.class)
public class SyncedListSyncTest
{
    private SyncedList list1;
    private SyncedList list2;
    
    @Before public void setUp()
    {
        list1 = createTestList("list1");
        list2 = createTestList("list2");
    }
    
    @Test public void testSyncIdenticalLists() throws JSONException
    {
        // Add same element to both lists
        SyncedListStep addStep = new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1"));
        list1.addElementStep(addStep);
        list2.addElementStep(addStep);
        
        // Sync should not create duplicates
        boolean changes = list1.sync(list2);
        
        Assert.assertEquals(1, list1.getElements().size());
        Assert.assertFalse("No changes expected for identical lists", changes);
    }
    
    // ========== Basic Sync Tests ==========
    
    @Test public void testSyncEmptyWithNonEmpty() throws JSONException
    {
        // Add element only to list2
        list2.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        
        // Sync empty list1 with list2
        list1.sync(list2);
        
        // list1 should now have the element from list2
        Assert.assertEquals(1, list1.getElements().size());
        Assert.assertEquals("Item 1", list1.getElements().get(0).getName());
    }
    
    @Test public void testSyncNonEmptyWithEmpty() throws JSONException
    {
        // Add element only to list1
        list1.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        
        // Sync with empty list2 should keep list1 unchanged
        boolean changes = list1.sync(list2);
        
        Assert.assertFalse("No changes expected", changes);
        Assert.assertEquals(1, list1.getElements().size());
    }
    
    @Test public void testSyncBothEmpty() throws JSONException
    {
        boolean changes = list1.sync(list2);
        
        Assert.assertFalse("No changes expected for empty lists", changes);
        Assert.assertEquals(0, list1.getElements().size());
    }
    
    @Test public void testSyncDifferentElements() throws JSONException
    {
        // Add different elements to each list
        list1.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list2.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        
        // Sync should merge both
        list1.sync(list2);
        
        Assert.assertEquals(2, list1.getElements().size());
    }
    
    // ========== Conflict Resolution Tests ==========
    
    @Test public void testSyncRemoveVsUpdateWithDelay() throws JSONException
    {
        // Both lists start with same element
        SyncedListStep addStep = new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1"));
        list1.addElementStep(addStep);
        list2.addElementStep(addStep);
        
        // list1 removes first
        list1.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        
        // Wait to ensure UPDATE has later timestamp
        try
        {
            Thread.sleep(50);
        }
        catch (InterruptedException e)
        {
        }
        
        // list2 updates after (later timestamp should win)
        list2.addElementStep(new SyncedListStep("item1", ACTION.UPDATE,
            new SyncedListElement("item1", "Item 1 Updated", "Updated")));
        
        // Sync - UPDATE has later timestamp, so element should exist
        list1.sync(list2);
        
        Assert.assertEquals(1, list1.getElements().size());
        Assert.assertEquals("Item 1 Updated",
            list1.getElements().get(0).getName());
    }
    
    @Test public void testSyncConcurrentUpdates() throws JSONException
    {
        // Both lists start with same element
        SyncedListStep addStep = new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1"));
        list1.addElementStep(addStep);
        list2.addElementStep(addStep);
        
        // Both update the same element
        list1.addElementStep(new SyncedListStep("item1", ACTION.UPDATE,
            new SyncedListElement("item1", "Updated by List1", "Desc1")));
        
        // Small delay to ensure different timestamp
        try
        {
            Thread.sleep(10);
        }
        catch (InterruptedException e)
        {
        }
        
        list2.addElementStep(new SyncedListStep("item1", ACTION.UPDATE,
            new SyncedListElement("item1", "Updated by List2", "Desc2")));
        
        // Sync - list2's update should win (later timestamp)
        list1.sync(list2);
        
        Assert.assertEquals(1, list1.getElements().size());
        Assert.assertEquals("Updated by List2",
            list1.getElements().get(0).getName());
    }
    
    @Test public void testBidirectionalSync() throws JSONException
    {
        // Add different elements to each list
        list1.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list2.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        
        // Sync both ways
        list1.sync(list2);
        list2.sync(list1);
        
        // Both lists should have both elements
        Assert.assertEquals(2, list1.getElements().size());
        Assert.assertEquals(2, list2.getElements().size());
    }
    
    // ========== Bidirectional Sync Tests ==========
    
    @Test public void testBidirectionalSyncIdempotent() throws JSONException
    {
        // Add elements
        list1.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list2.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        
        // Multiple syncs should be idempotent
        list1.sync(list2);
        list2.sync(list1);
        list1.sync(list2);
        list2.sync(list1);
        
        Assert.assertEquals(2, list1.getElements().size());
        Assert.assertEquals(2, list2.getElements().size());
    }
    
    @Test public void testSyncAfterClear() throws JSONException
    {
        // Both lists have elements
        SyncedListStep addStep1 = new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1"));
        list1.addElementStep(addStep1);
        list2.addElementStep(addStep1);
        
        // list1 clears
        list1.addElementStep(new SyncedListStep("", ACTION.CLEAR));
        
        // list2 adds new element after clear
        try
        {
            Thread.sleep(10);
        }
        catch (InterruptedException e)
        {
        }
        list2.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        
        // Sync
        list1.sync(list2);
        
        // Only item2 should exist (added after clear)
        Assert.assertEquals(1, list1.getElements().size());
        Assert.assertEquals("item2", list1.getElements().get(0).getId());
    }
    
    // ========== CLEAR Action Sync Tests ==========
    
    @Test public void testSyncPreservesAllElements() throws JSONException
    {
        // Add elements to both lists
        SyncedListStep add1 = new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1"));
        SyncedListStep add2 = new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2"));
        SyncedListStep add3 = new SyncedListStep("item3", ACTION.ADD,
            new SyncedListElement("item3", "Item 3", "Description 3"));
        
        list1.addElementStep(add1);
        list1.addElementStep(add2);
        list1.addElementStep(add3);
        list2.addElementStep(add1);
        list2.addElementStep(add2);
        list2.addElementStep(add3);
        
        // Sync
        list1.sync(list2);
        
        // All elements should be preserved
        Assert.assertEquals(3, list1.getElements().size());
    }
    
    // ========== MOVE/SWAP Sync Tests ==========
    
    @Test public void testSyncMergesNewElementsAfterMove() throws JSONException
    {
        // list1 has one element
        list1.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        
        // list2 has same element plus one more
        list2.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        
        try
        {
            Thread.sleep(10);
        }
        catch (InterruptedException e)
        {
        }
        
        list2.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        
        // Sync should add item2 to list1
        list1.sync(list2);
        
        Assert.assertEquals(2, list1.getElements().size());
    }
    
    @Test public void testStaticSyncMethod() throws JSONException
    {
        list1.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list2.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        
        SyncedList result = SyncedList.sync(list1, list2);
        
        Assert.assertEquals(2, result.getElements().size());
        Assert.assertSame(list1, result); // Returns modified list1
    }
    
    // ========== Static Sync Method Tests ==========
    
    @Test public void testSyncPreservesElementData() throws JSONException
    {
        SyncedListElement element =
            new SyncedListElement("item1", "Item 1", "Description 1");
        element.setChecked(true);
        
        list2.addElementStep(new SyncedListStep("item1", ACTION.ADD, element));
        
        list1.sync(list2);
        
        Assert.assertEquals(1, list1.getElements().size());
        Assert.assertTrue(list1.getElements().get(0).getChecked());
        Assert.assertEquals("Description 1",
            list1.getElements().get(0).getDescription());
    }
    
    // ========== Edge Cases ==========
    
    @Test public void testSyncManyElements() throws JSONException
    {
        // Add 100 elements to list2
        for (int i = 0; i < 100; i++)
        {
            list2.addElementStep(new SyncedListStep("item" + i, ACTION.ADD,
                new SyncedListElement("item" + i, "Item " + i, "Desc " + i)));
        }
        
        list1.sync(list2);
        
        Assert.assertEquals(100, list1.getElements().size());
    }
    
    @Test public void testSyncBothListsClear() throws JSONException
    {
        // Both lists have elements then clear
        SyncedListStep addStep = new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1"));
        list1.addElementStep(addStep);
        list2.addElementStep(addStep);
        
        list1.addElementStep(new SyncedListStep("", ACTION.CLEAR));
        
        try
        {
            Thread.sleep(10);
        }
        catch (InterruptedException e)
        {
        }
        
        list2.addElementStep(new SyncedListStep("", ACTION.CLEAR));
        
        list1.sync(list2);
        
        Assert.assertTrue(list1.getElements().isEmpty());
    }
    
    // ========== Sync Edge Cases ==========
    
    @Test public void testSyncConflictingRemoves() throws JSONException
    {
        // Both lists remove same element
        SyncedListStep addStep = new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1"));
        list1.addElementStep(addStep);
        list2.addElementStep(addStep);
        
        list1.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        list2.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        
        list1.sync(list2);
        
        Assert.assertTrue(list1.getElements().isEmpty());
    }
    
    @Test public void testSyncAddSameElementBothLists() throws JSONException
    {
        // Both lists add element with same ID (same step)
        SyncedListStep addStep = new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1"));
        
        list1.addElementStep(addStep);
        list2.addElementStep(addStep);
        
        list1.sync(list2);
        
        // Should have exactly 1 element (no duplicates for same step)
        Assert.assertEquals(1, list1.getElements().size());
    }
    
    @Test public void testSyncRemoveThenReAdd() throws JSONException
    {
        // list1: add, remove
        // list2: add, remove, re-add
        SyncedListStep addStep = new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1"));
        
        list1.addElementStep(addStep);
        list2.addElementStep(addStep);
        
        list1.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        list2.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        
        try
        {
            Thread.sleep(10);
        }
        catch (InterruptedException e)
        {
        }
        
        // list2 re-adds after remove
        list2.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1 ReAdded", "ReAdded")));
        
        list1.sync(list2);
        
        // Element should exist because list2 re-added it
        Assert.assertEquals(1, list1.getElements().size());
        Assert.assertEquals("Item 1 ReAdded",
            list1.getElements().get(0).getName());
    }
    
    @Test public void testSyncWithMixedOperations() throws JSONException
    {
        // Complex scenario with multiple operations
        list1.addElementStep(new SyncedListStep("a", ACTION.ADD,
            new SyncedListElement("a", "A", "")));
        list1.addElementStep(new SyncedListStep("b", ACTION.ADD,
            new SyncedListElement("b", "B", "")));
        
        try
        {
            Thread.sleep(10);
        }
        catch (InterruptedException e)
        {
        }
        
        list2.addElementStep(new SyncedListStep("c", ACTION.ADD,
            new SyncedListElement("c", "C", "")));
        list2.addElementStep(new SyncedListStep("d", ACTION.ADD,
            new SyncedListElement("d", "D", "")));
        
        list1.sync(list2);
        
        // All 4 elements should exist
        Assert.assertEquals(4, list1.getElements().size());
    }
    
    @Test public void testSyncClearWinsOverOlderOperations()
        throws JSONException
    {
        // list1: add item
        // list2: add same item, then clear
        SyncedListStep addStep = new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1"));
        
        list1.addElementStep(addStep);
        list2.addElementStep(addStep);
        
        try
        {
            Thread.sleep(10);
        }
        catch (InterruptedException e)
        {
        }
        
        list2.addElementStep(new SyncedListStep("", ACTION.CLEAR));
        
        list1.sync(list2);
        
        // CLEAR should win, list should be empty
        Assert.assertTrue(list1.getElements().isEmpty());
    }
    
    @Test public void testSyncReturnsChangesCorrectly() throws JSONException
    {
        list2.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        
        // Verify list1 is empty before sync
        Assert.assertEquals(0, list1.getElements().size());
        
        list1.sync(list2);
        
        // Verify sync worked - list1 should have the element now
        Assert.assertEquals(1, list1.getElements().size());
        Assert.assertEquals("Item 1", list1.getElements().get(0).getName());
        
        // Sync again - elements should remain the same
        int countBefore = list1.getElements().size();
        list1.sync(list2);
        int countAfter = list1.getElements().size();
        
        Assert.assertEquals("Element count should not change on second sync",
            countBefore, countAfter);
    }
    
    private SyncedList createTestList(String name)
    {
        byte[] aesKey = new byte[32];
        for (int i = 0; i < aesKey.length; i++)
        {
            aesKey[i] = (byte) i;
        }
        SecretKeySpec localSecret = new SecretKeySpec(aesKey, "AES");
        SyncedListHeader header =
            new SyncedListHeader(name, "Test List", "localhost", aesKey,
                localSecret);
        return new SyncedList(header, new ArrayList<>());
    }
}
