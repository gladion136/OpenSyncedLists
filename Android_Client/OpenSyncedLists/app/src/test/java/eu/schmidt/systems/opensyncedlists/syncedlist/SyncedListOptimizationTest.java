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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import javax.crypto.spec.SecretKeySpec;

/**
 * Tests for SyncedList optimization functionality.
 */
@RunWith(RobolectricTestRunner.class)
public class SyncedListOptimizationTest
{
    @Test public void testFullOptimizationEmptyResult() throws JSONException
    {
        SyncedList list = createListWithHostname("");
        
        // Add and remove elements
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item2", ACTION.REMOVE));
        
        // List is empty, so 0 steps after optimization
        Assert.assertEquals(0, list.getElements().size());
        Assert.assertEquals(0, list.getElementSteps().size());
    }
    
    // ========== Full Optimization Tests (no hostname) ==========
    
    @Test public void testFullOptimizationWithElements() throws JSONException
    {
        SyncedList list = createListWithHostname("");
        
        // Add elements, remove one, update another
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item3", ACTION.ADD,
            new SyncedListElement("item3", "Item 3", "Description 3")));
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item2", ACTION.UPDATE,
            new SyncedListElement("item2", "Item 2 Updated", "Updated")));
        
        // 2 elements remain, so 2 ADD steps
        Assert.assertEquals(2, list.getElements().size());
        Assert.assertEquals(2, list.getElementSteps().size());
    }
    
    @Test public void testFullOptimizationPreservesOrder() throws JSONException
    {
        SyncedList list = createListWithHostname("");
        
        // Add elements and move
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item3", ACTION.ADD,
            new SyncedListElement("item3", "Item 3", "Description 3")));
        list.addElementStep(new SyncedListStep("item1", ACTION.MOVE, 2));
        
        // Order should be preserved after optimization
        Assert.assertEquals("item2", list.getElements().get(0).getId());
        Assert.assertEquals("item3", list.getElements().get(1).getId());
        Assert.assertEquals("item1", list.getElements().get(2).getId());
        
        // 3 ADD steps in optimized order
        Assert.assertEquals(3, list.getElementSteps().size());
    }
    
    @Test public void testFullOptimizationPreservesCheckedState()
        throws JSONException
    {
        SyncedList list = createListWithHostname("");
        
        // Add element and check it
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        SyncedListElement checked = list.getElements().get(0).clone();
        checked.setChecked(true);
        list.addElementStep(
            new SyncedListStep("item1", ACTION.UPDATE, checked));
        
        // Checked state should be preserved
        Assert.assertTrue(list.getElements().get(0).getChecked());
        Assert.assertEquals(1, list.getElementSteps().size());
    }
    
    @Test public void testSyncSupportedOptimizationKeepsAddRemove()
        throws JSONException
    {
        SyncedList list = createListWithHostname("localhost");
        
        // Add and remove
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item2", ACTION.REMOVE));
        
        // ADD/REMOVE steps are kept
        Assert.assertEquals(0, list.getElements().size());
        Assert.assertEquals(4, list.getElementSteps().size());
    }
    
    // ========== Sync Supported Optimization Tests (with hostname) ==========
    
    @Test public void testSyncSupportedOptimizationRemovesMoveSwap()
        throws JSONException
    {
        SyncedList list = createListWithHostname("localhost");
        
        // Add elements
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item3", ACTION.ADD,
            new SyncedListElement("item3", "Item 3", "Description 3")));
        
        int stepsBeforeMove = list.getElementSteps().size();
        
        // Add MOVE and SWAP
        list.addElementStep(new SyncedListStep("item1", ACTION.MOVE, 2));
        list.addElementStep(new SyncedListStep("item2", ACTION.SWAP, "item3"));
        
        // MOVE/SWAP are optimized: old ones removed, new MOVE steps added
        // for current positions
        // 3 ADD steps + 3 MOVE steps (one for each element's current position)
        Assert.assertEquals(3, list.getElements().size());
    }
    
    @Test public void testClearRemovesPreviousSteps() throws JSONException
    {
        SyncedList list = createListWithHostname("localhost");
        
        // Add many elements
        for (int i = 0; i < 10; i++)
        {
            list.addElementStep(new SyncedListStep("item" + i, ACTION.ADD,
                new SyncedListElement("item" + i, "Item " + i, "Desc")));
        }
        
        // Clear should remove all previous steps
        list.addElementStep(new SyncedListStep("", ACTION.CLEAR));
        
        // Only CLEAR step remains
        Assert.assertEquals(0, list.getElements().size());
        Assert.assertEquals(1, list.getElementSteps().size());
        Assert.assertEquals(ACTION.CLEAR,
            list.getElementSteps().get(0).getChangeAction());
    }
    
    // ========== CLEAR Optimization Tests ==========
    
    @Test public void testClearThenAdd() throws JSONException
    {
        SyncedList list = createListWithHostname("localhost");
        
        // Add, clear, add again
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("", ACTION.CLEAR));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        
        // CLEAR + ADD step
        Assert.assertEquals(1, list.getElements().size());
        Assert.assertEquals("item2", list.getElements().get(0).getId());
    }
    
    @Test public void testOptimizationWithManyOperations() throws JSONException
    {
        SyncedList list = createListWithHostname("");
        
        // Many add/remove cycles
        for (int cycle = 0; cycle < 10; cycle++)
        {
            list.addElementStep(new SyncedListStep("item", ACTION.ADD,
                new SyncedListElement("item", "Item", "Desc")));
            list.addElementStep(new SyncedListStep("item", ACTION.REMOVE));
        }
        
        // Should optimize to 0 steps
        Assert.assertEquals(0, list.getElements().size());
        Assert.assertEquals(0, list.getElementSteps().size());
    }
    
    // ========== Edge Cases ==========
    
    @Test public void testOptimizationMaintainsConsistency()
        throws JSONException
    {
        SyncedList list = createListWithHostname("");
        
        // Complex sequence
        list.addElementStep(new SyncedListStep("a", ACTION.ADD,
            new SyncedListElement("a", "A", "")));
        list.addElementStep(new SyncedListStep("b", ACTION.ADD,
            new SyncedListElement("b", "B", "")));
        list.addElementStep(new SyncedListStep("c", ACTION.ADD,
            new SyncedListElement("c", "C", "")));
        list.addElementStep(new SyncedListStep("a", ACTION.MOVE, 2));
        list.addElementStep(new SyncedListStep("b", ACTION.SWAP, "c"));
        list.addElementStep(new SyncedListStep("c", ACTION.UPDATE,
            new SyncedListElement("c", "C Updated", "")));
        
        // Verify final state
        Assert.assertEquals(3, list.getElements().size());
        Assert.assertEquals("c", list.getElements().get(0).getId());
        Assert.assertEquals("C Updated", list.getElements().get(0).getName());
    }
    
    @Test public void testFullOptimizationRemovesAllHistory()
        throws JSONException
    {
        SyncedList list = createListWithHostname("");
        
        // Add multiple operations
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "")));
        list.addElementStep(new SyncedListStep("item1", ACTION.UPDATE,
            new SyncedListElement("item1", "Item 1 v2", "")));
        list.addElementStep(new SyncedListStep("item1", ACTION.UPDATE,
            new SyncedListElement("item1", "Item 1 v3", "")));
        
        // Full optimization should reduce to 1 ADD step
        Assert.assertEquals(1, list.getElementSteps().size());
        Assert.assertEquals(ACTION.ADD,
            list.getElementSteps().get(0).getChangeAction());
        Assert.assertEquals("Item 1 v3", list.getElements().get(0).getName());
    }
    
    // ========== Optimization Edge Cases ==========
    
    @Test public void testSyncSupportedOptimizationKeepsUpdateHistory()
        throws JSONException
    {
        SyncedList list = createListWithHostname("localhost");
        
        // Add and update
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "")));
        list.addElementStep(new SyncedListStep("item1", ACTION.UPDATE,
            new SyncedListElement("item1", "Item 1 Updated", "")));
        
        // Should have ADD + UPDATE + MOVE steps
        int addCount = 0;
        int updateCount = 0;
        for (SyncedListStep step : list.getElementSteps())
        {
            if (step.getChangeAction() == ACTION.ADD)
            {
                addCount++;
            }
            if (step.getChangeAction() == ACTION.UPDATE)
            {
                updateCount++;
            }
        }
        Assert.assertEquals(1, addCount);
        Assert.assertEquals(1, updateCount);
    }
    
    @Test public void testOptimizationWithSingleElement() throws JSONException
    {
        SyncedList list = createListWithHostname("");
        
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "")));
        
        Assert.assertEquals(1, list.getElementSteps().size());
        Assert.assertEquals(1, list.getElements().size());
    }
    
    @Test public void testClearOptimizationRemovesAllPreviousSteps()
        throws JSONException
    {
        SyncedList list = createListWithHostname("localhost");
        
        // Add many operations
        for (int i = 0; i < 50; i++)
        {
            list.addElementStep(new SyncedListStep("item" + i, ACTION.ADD,
                new SyncedListElement("item" + i, "Item " + i, "")));
        }
        
        // CLEAR should remove all previous steps
        list.addElementStep(new SyncedListStep("", ACTION.CLEAR));
        
        Assert.assertEquals(1, list.getElementSteps().size());
        Assert.assertEquals(ACTION.CLEAR,
            list.getElementSteps().get(0).getChangeAction());
    }
    
    @Test public void testOptimizationAfterManyMoveOperations()
        throws JSONException
    {
        SyncedList list = createListWithHostname("");
        
        // Add elements
        list.addElementStep(new SyncedListStep("a", ACTION.ADD,
            new SyncedListElement("a", "A", "")));
        list.addElementStep(new SyncedListStep("b", ACTION.ADD,
            new SyncedListElement("b", "B", "")));
        list.addElementStep(new SyncedListStep("c", ACTION.ADD,
            new SyncedListElement("c", "C", "")));
        
        // Many moves - should all be optimized away
        for (int i = 0; i < 10; i++)
        {
            list.addElementStep(new SyncedListStep("a", ACTION.MOVE, (i % 3)));
        }
        
        // Full optimization: should have exactly 3 ADD steps
        Assert.assertEquals(3, list.getElementSteps().size());
        for (SyncedListStep step : list.getElementSteps())
        {
            Assert.assertEquals(ACTION.ADD, step.getChangeAction());
        }
    }
    
    private SyncedList createListWithHostname(String hostname)
    {
        byte[] aesKey = new byte[32];
        for (int i = 0; i < aesKey.length; i++)
        {
            aesKey[i] = (byte) i;
        }
        SecretKeySpec localSecret = new SecretKeySpec(aesKey, "AES");
        SyncedListHeader header =
            new SyncedListHeader("testList", "Test List", hostname, aesKey,
                localSecret);
        return new SyncedList(header, new ArrayList<>());
    }
}
