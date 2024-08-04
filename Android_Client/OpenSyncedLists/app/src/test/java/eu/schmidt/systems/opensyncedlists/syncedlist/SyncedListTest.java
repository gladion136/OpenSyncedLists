/*
 * Copyright (C) 2024  Etienne Schmidt (eschmidt@schmidt-ti.eu)
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

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

import javax.crypto.spec.SecretKeySpec;

import eu.schmidt.systems.opensyncedlists.syncedlist.ACTION;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListStep;

@RunWith(RobolectricTestRunner.class)
public class SyncedListTest
{
    private SyncedList list;
    private SyncedList list2;
    
    public SyncedList create_test_list(String name)
    {
        byte[] aesKey = new byte[32]; // 256 bits are 32 bytes
        for (int i = 0; i < aesKey.length; i++)
        {
            aesKey[i] = (byte) i; // Simple pattern for example purposes
        }
        SecretKeySpec localSecret = new SecretKeySpec(aesKey, "AES");
        
        // Create SyncedListHeader with direct parameters
        SyncedListHeader header =
            new SyncedListHeader(name, "Test List", "localhost", aesKey,
                localSecret);
        
        // Initialize SyncedList with the header
        return new SyncedList(header, new ArrayList<>());
    }
    
    @Before public void setUp() throws Exception
    {
        list = create_test_list("list");
        list2 = create_test_list("list2");
    }
    
    @Test public void testRemoveElement() throws JSONException
    {
        String id = list.generateUniqueElementId();
        SyncedListElement testObj = new SyncedListElement(id, "Test 1", "");
        SyncedListStep addStep = new SyncedListStep(id, ACTION.ADD, testObj);
        list.addElementStep(addStep);
        
        SyncedListStep removeStep =
            new SyncedListStep(testObj.getId(), ACTION.REMOVE);
        list.addElementStep(removeStep);
        
        Assert.assertTrue("List should be empty after removing the element.",
            list.getReformatElements().isEmpty());
    }
    
    @Test public void testSyncListsNoConflicts() throws JSONException
    {
        SyncedListElement elementToAdd =
            new SyncedListElement("item2", "Item 2", "Description 2");
        SyncedListStep addStep2 =
            new SyncedListStep("item2", ACTION.ADD, elementToAdd);
        list2.addElementStep(addStep2);
        list.addElementStep(addStep2);
        
        list.sync(list2);
        
        Assert.assertEquals(
            "List should have one element after sync, because of "
                + "the same ids " + "syncing.", 1, list.getElements().size());
    }
    
    @Test public void testSyncListsNoConflicts2() throws JSONException
    {
        SyncedListElement elementToAdd =
            new SyncedListElement("item2", "Item 2", "Description 2");
        SyncedListStep addStep2 =
            new SyncedListStep("item2", ACTION.ADD, elementToAdd);
        
        SyncedListStep removeStep = new SyncedListStep("item2", ACTION.REMOVE);
        
        SyncedListStep editStep = new SyncedListStep("item2", ACTION.UPDATE,
            new SyncedListElement("item2", "Item 2", "Description 3"));
        
        list2.addElementStep(addStep2);
        list.addElementStep(addStep2);
        list.addElementStep(removeStep);
        list2.addElementStep(editStep);
        
        list.sync(list2);
        
        Assert.assertEquals(
            "List should have one element after sync, because of "
                + "the same ids " + "syncing.", 1, list.getElements().size());
    }
    
    @Test public void testAddAndRemoveElements() throws JSONException
    {
        // Initial state check
        Assert.assertTrue("List should be initially empty.",
            list.getElements().isEmpty());
        
        // Add elements
        SyncedListElement element1 =
            new SyncedListElement("item1", "Item 1", "Description 1");
        SyncedListStep addStep1 =
            new SyncedListStep("item1", ACTION.ADD, element1);
        list.addElementStep(addStep1);
        
        SyncedListElement element2 =
            new SyncedListElement("item2", "Item 2", "Description 2");
        SyncedListStep addStep2 =
            new SyncedListStep("item2", ACTION.ADD, element2);
        list.addElementStep(addStep2);
        
        // Check after adding
        Assert.assertEquals("List should have two elements after adding.", 2,
            list.getElements().size());
        Assert.assertNotNull("Element 'item1' should exist in the list.",
            list.getElements().stream().filter(e -> e.getId().equals("item1"))
                .findFirst().orElse(null));
        Assert.assertNotNull("Element 'item2' should exist in the list.",
            list.getElements().stream().filter(e -> e.getId().equals("item2"))
                .findFirst().orElse(null));
        
        // Remove one element
        SyncedListStep removeStep1 = new SyncedListStep("item1", ACTION.REMOVE);
        list.addElementStep(removeStep1);
        
        // Check after removing
        Assert.assertEquals("List should have one element after removing.", 1,
            list.getElements().size());
        Assert.assertNull("Element 'item1' should not exist in the list.",
            list.getElements().stream().filter(e -> e.getId().equals("item1"))
                .findFirst().orElse(null));
        Assert.assertNotNull("Element 'item2' should still exist in the list.",
            list.getElements().stream().filter(e -> e.getId().equals("item2"))
                .findFirst().orElse(null));
    }
    
    @Test public void testAddingElements() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        Assert.assertEquals(2, list.getElements().size());
    }
    
    @Test public void testRemovingElements() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        Assert.assertEquals(1, list.getElements().size());
    }
    
    @Test public void testUpdatingElements() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item1", ACTION.UPDATE,
            new SyncedListElement("item1", "Item 1 Updated",
                "Description 1 Updated")));
        Assert.assertEquals(1, list.getElements().size());
    }
    
    @Test public void testClearingList() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("", ACTION.CLEAR));
        Assert.assertTrue("List should be empty after clearing",
            list.getElements().isEmpty());
    }
    
    @Test public void testMovingElement() throws JSONException
    {
        // This test will depend on the implementation of the move operation
    }
    
    @Test public void testSwappingElements() throws JSONException
    {
        // This test will depend on the implementation of the swap operation
    }
    
    @Test public void testAddingAndCheckingElements() throws JSONException
    {
        // This test will depend on the implementation of checking elements
    }
    
    @Test public void testUncheckingElements() throws JSONException
    {
        // This test will depend on the implementation of unchecking elements
    }
    
    @Test public void testAddingElementsWithUniqueIDs() throws JSONException
    {
        String uniqueId1 = list.generateUniqueElementId();
        String uniqueId2 = list.generateUniqueElementId();
        list.addElementStep(new SyncedListStep(uniqueId1, ACTION.ADD,
            new SyncedListElement(uniqueId1, "Item Unique 1",
                "Description Unique 1")));
        list.addElementStep(new SyncedListStep(uniqueId2, ACTION.ADD,
            new SyncedListElement(uniqueId2, "Item Unique 2",
                "Description Unique 2")));
        
        Assert.assertEquals(2, list.getElements().size());
    }
    
    @Test public void testOptimization() throws Exception
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item2", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item2", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item2", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item2", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item2", ACTION.REMOVE));
        
        // Assert
        Assert.assertEquals(0, list.getElements().size());
        
        // Assert the number of element steps
        Assert.assertEquals(10, list.getElementSteps().size());
    }
}
