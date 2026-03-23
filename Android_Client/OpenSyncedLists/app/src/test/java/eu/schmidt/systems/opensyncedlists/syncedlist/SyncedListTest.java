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

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import javax.crypto.spec.SecretKeySpec;

/**
 * Tests for basic SyncedList operations (ADD, REMOVE, UPDATE, MOVE, SWAP, CLEAR).
 * For sync tests see SyncedListSyncTest.
 * For optimization tests see SyncedListOptimizationTest.
 */
@RunWith(RobolectricTestRunner.class)
public class SyncedListTest
{
    private SyncedList list;

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

    @Before
    public void setUp()
    {
        list = createTestList("list");
    }

    // ========== ADD Tests ==========

    @Test
    public void testAddingElements() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));

        Assert.assertEquals(2, list.getElements().size());
    }

    @Test
    public void testAddingElementsWithUniqueIDs() throws JSONException
    {
        String uniqueId1 = list.generateUniqueElementId();
        String uniqueId2 = list.generateUniqueElementId();

        list.addElementStep(new SyncedListStep(uniqueId1, ACTION.ADD,
            new SyncedListElement(uniqueId1, "Item Unique 1", "Description 1")));
        list.addElementStep(new SyncedListStep(uniqueId2, ACTION.ADD,
            new SyncedListElement(uniqueId2, "Item Unique 2", "Description 2")));

        Assert.assertEquals(2, list.getElements().size());
        Assert.assertNotEquals(uniqueId1, uniqueId2);
    }

    // ========== REMOVE Tests ==========

    @Test
    public void testRemoveElement() throws JSONException
    {
        String id = list.generateUniqueElementId();
        SyncedListElement testObj = new SyncedListElement(id, "Test 1", "");
        list.addElementStep(new SyncedListStep(id, ACTION.ADD, testObj));
        list.addElementStep(new SyncedListStep(id, ACTION.REMOVE));

        Assert.assertTrue("List should be empty after removing the element.",
            list.getReformatElements().isEmpty());
    }

    @Test
    public void testRemovingElements() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));

        Assert.assertEquals(1, list.getElements().size());
        Assert.assertEquals("item2", list.getElements().get(0).getId());
    }

    @Test
    public void testAddAndRemoveElements() throws JSONException
    {
        Assert.assertTrue("List should be initially empty.",
            list.getElements().isEmpty());

        // Add elements
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));

        Assert.assertEquals("List should have two elements after adding.", 2,
            list.getElements().size());
        Assert.assertNotNull("Element 'item1' should exist.",
            list.getElements().stream().filter(e -> e.getId().equals("item1"))
                .findFirst().orElse(null));

        // Remove one element
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));

        Assert.assertEquals("List should have one element after removing.", 1,
            list.getElements().size());
        Assert.assertNull("Element 'item1' should not exist.",
            list.getElements().stream().filter(e -> e.getId().equals("item1"))
                .findFirst().orElse(null));
        Assert.assertNotNull("Element 'item2' should still exist.",
            list.getElements().stream().filter(e -> e.getId().equals("item2"))
                .findFirst().orElse(null));
    }

    // ========== UPDATE Tests ==========

    @Test
    public void testUpdatingElements() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item1", ACTION.UPDATE,
            new SyncedListElement("item1", "Item 1 Updated", "Description Updated")));

        Assert.assertEquals(1, list.getElements().size());
        Assert.assertEquals("Item 1 Updated", list.getElements().get(0).getName());
        Assert.assertEquals("Description Updated", list.getElements().get(0).getDescription());
    }

    @Test
    public void testCheckingElements() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));

        Assert.assertFalse(list.getElements().get(0).getChecked());

        SyncedListElement checkedElement = list.getElements().get(0).clone();
        checkedElement.setChecked(true);
        list.addElementStep(new SyncedListStep("item1", ACTION.UPDATE, checkedElement));

        Assert.assertTrue(list.getElements().get(0).getChecked());
    }

    @Test
    public void testUncheckingElements() throws JSONException
    {
        SyncedListElement checkedElement = new SyncedListElement("item1", "Item 1", "Description 1");
        checkedElement.setChecked(true);
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD, checkedElement));

        Assert.assertTrue(list.getElements().get(0).getChecked());

        SyncedListElement uncheckedElement = list.getElements().get(0).clone();
        uncheckedElement.setChecked(false);
        list.addElementStep(new SyncedListStep("item1", ACTION.UPDATE, uncheckedElement));

        Assert.assertFalse(list.getElements().get(0).getChecked());
    }

    // ========== MOVE Tests ==========

    @Test
    public void testMovingElement() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item3", ACTION.ADD,
            new SyncedListElement("item3", "Item 3", "Description 3")));

        // Initial order: item1, item2, item3
        Assert.assertEquals("item1", list.getElements().get(0).getId());
        Assert.assertEquals("item2", list.getElements().get(1).getId());
        Assert.assertEquals("item3", list.getElements().get(2).getId());

        // Move item1 to position 2
        list.addElementStep(new SyncedListStep("item1", ACTION.MOVE, 2));

        // New order: item2, item3, item1
        Assert.assertEquals("item2", list.getElements().get(0).getId());
        Assert.assertEquals("item3", list.getElements().get(1).getId());
        Assert.assertEquals("item1", list.getElements().get(2).getId());
    }

    @Test
    public void testMovingElementToStart() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item3", ACTION.ADD,
            new SyncedListElement("item3", "Item 3", "Description 3")));

        // Move item3 to position 0
        list.addElementStep(new SyncedListStep("item3", ACTION.MOVE, 0));

        // New order: item3, item1, item2
        Assert.assertEquals("item3", list.getElements().get(0).getId());
        Assert.assertEquals("item1", list.getElements().get(1).getId());
        Assert.assertEquals("item2", list.getElements().get(2).getId());
    }

    // ========== SWAP Tests ==========

    @Test
    public void testSwappingElements() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item3", ACTION.ADD,
            new SyncedListElement("item3", "Item 3", "Description 3")));

        // Initial order: item1, item2, item3
        Assert.assertEquals("item1", list.getElements().get(0).getId());

        // Swap item1 with item3
        list.addElementStep(new SyncedListStep("item1", ACTION.SWAP, "item3"));

        // New order: item3, item2, item1
        Assert.assertEquals("item3", list.getElements().get(0).getId());
        Assert.assertEquals("item2", list.getElements().get(1).getId());
        Assert.assertEquals("item1", list.getElements().get(2).getId());
    }

    @Test
    public void testSwappingAdjacentElements() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));

        // Swap adjacent elements
        list.addElementStep(new SyncedListStep("item1", ACTION.SWAP, "item2"));

        Assert.assertEquals("item2", list.getElements().get(0).getId());
        Assert.assertEquals("item1", list.getElements().get(1).getId());
    }

    // ========== CLEAR Tests ==========

    @Test
    public void testClearingList() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));

        Assert.assertEquals(2, list.getElements().size());

        list.addElementStep(new SyncedListStep("", ACTION.CLEAR));

        Assert.assertTrue("List should be empty after clearing",
            list.getElements().isEmpty());
    }

    @Test
    public void testClearThenAdd() throws JSONException
    {
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("", ACTION.CLEAR));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));

        Assert.assertEquals(1, list.getElements().size());
        Assert.assertEquals("item2", list.getElements().get(0).getId());
    }

    // ========== Helper Method Tests ==========

    @Test
    public void testGenerateUniqueElementId()
    {
        String id1 = list.generateUniqueElementId();
        String id2 = list.generateUniqueElementId();

        Assert.assertNotNull(id1);
        Assert.assertNotNull(id2);
        Assert.assertNotEquals(id1, id2);
        Assert.assertEquals(50, id1.length());
    }

    @Test
    public void testGettersAndSetters()
    {
        Assert.assertEquals("list", list.getId());
        Assert.assertEquals("Test List", list.getName());

        list.setName("New Name");
        Assert.assertEquals("New Name", list.getName());

        list.setId("newId");
        Assert.assertEquals("newId", list.getId());
    }

    // ========== Edge Case Tests ==========

    @Test
    public void testUpdateNonExistentElementAddsIt() throws JSONException
    {
        // UPDATE on non-existent element should add it (implementation behavior)
        list.addElementStep(new SyncedListStep("item1", ACTION.UPDATE,
            new SyncedListElement("item1", "Item 1", "Description 1")));

        Assert.assertEquals(1, list.getElements().size());
        Assert.assertEquals("Item 1", list.getElements().get(0).getName());
    }

    @Test
    public void testRemoveNonExistentElement() throws JSONException
    {
        // REMOVE on non-existent element should not crash
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("nonexistent", ACTION.REMOVE));

        Assert.assertEquals(1, list.getElements().size());
        Assert.assertEquals("item1", list.getElements().get(0).getId());
    }

    @Test
    public void testMoveToInvalidPosition() throws JSONException
    {
        // MOVE to position >= size should be ignored
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));

        // Move to position 10 (invalid)
        list.addElementStep(new SyncedListStep("item1", ACTION.MOVE, 10));

        // List should remain unchanged
        Assert.assertEquals(2, list.getElements().size());
        Assert.assertEquals("item1", list.getElements().get(0).getId());
    }

    @Test
    public void testSwapWithNonExistentElement() throws JSONException
    {
        // SWAP with non-existent element should not crash
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));

        // Swap with non-existent element
        list.addElementStep(new SyncedListStep("item1", ACTION.SWAP, "nonexistent"));

        // List should remain unchanged
        Assert.assertEquals(2, list.getElements().size());
        Assert.assertEquals("item1", list.getElements().get(0).getId());
        Assert.assertEquals("item2", list.getElements().get(1).getId());
    }

    @Test
    public void testDoubleAddSameId() throws JSONException
    {
        // Adding same ID twice creates duplicates (current behavior)
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1 First", "First")));
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1 Second", "Second")));

        // Both elements exist (duplicates allowed)
        Assert.assertEquals(2, list.getElements().size());
    }

    @Test
    public void testReAddAfterRemove() throws JSONException
    {
        // Re-adding an element after removal should work
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item1", ACTION.REMOVE));
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1 Restored", "Restored")));

        Assert.assertEquals(1, list.getElements().size());
        Assert.assertEquals("Item 1 Restored", list.getElements().get(0).getName());
    }

    @Test
    public void testSwapWithSelf() throws JSONException
    {
        // SWAP with self should not crash and list should remain unchanged
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));

        list.addElementStep(new SyncedListStep("item1", ACTION.SWAP, "item1"));

        Assert.assertEquals(2, list.getElements().size());
        Assert.assertEquals("item1", list.getElements().get(0).getId());
        Assert.assertEquals("item2", list.getElements().get(1).getId());
    }

    @Test
    public void testMoveToSamePosition() throws JSONException
    {
        // MOVE to same position should be no-op
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));
        list.addElementStep(new SyncedListStep("item3", ACTION.ADD,
            new SyncedListElement("item3", "Item 3", "Description 3")));

        // Move item2 to position 1 (where it already is)
        list.addElementStep(new SyncedListStep("item2", ACTION.MOVE, 1));

        Assert.assertEquals("item1", list.getElements().get(0).getId());
        Assert.assertEquals("item2", list.getElements().get(1).getId());
        Assert.assertEquals("item3", list.getElements().get(2).getId());
    }

    @Test
    public void testEmptyNameAndDescription() throws JSONException
    {
        // Empty strings should work
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "", "")));

        Assert.assertEquals(1, list.getElements().size());
        Assert.assertEquals("", list.getElements().get(0).getName());
        Assert.assertEquals("", list.getElements().get(0).getDescription());
    }

    @Test
    public void testSpecialCharactersInElementData() throws JSONException
    {
        // Special characters and unicode should work
        String specialName = "Test äöü ß 日本語 emoji: 🎉";
        String specialDesc = "<script>alert('xss')</script> & \"quotes\"";

        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", specialName, specialDesc)));

        Assert.assertEquals(1, list.getElements().size());
        Assert.assertEquals(specialName, list.getElements().get(0).getName());
        Assert.assertEquals(specialDesc, list.getElements().get(0).getDescription());
    }

    @Test
    public void testMoveNonExistentElement() throws JSONException
    {
        // MOVE non-existent element should not crash
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));

        list.addElementStep(new SyncedListStep("nonexistent", ACTION.MOVE, 0));

        // List should remain unchanged
        Assert.assertEquals(2, list.getElements().size());
        Assert.assertEquals("item1", list.getElements().get(0).getId());
    }

    @Test
    public void testMultipleClearOperations() throws JSONException
    {
        // Multiple CLEARs should work
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("", ACTION.CLEAR));
        list.addElementStep(new SyncedListStep("", ACTION.CLEAR));

        Assert.assertTrue(list.getElements().isEmpty());
        Assert.assertEquals(1, list.getElementSteps().size()); // Only last CLEAR
    }

    @Test
    public void testMoveToNegativePosition() throws JSONException
    {
        // MOVE to negative position - behavior depends on implementation
        list.addElementStep(new SyncedListStep("item1", ACTION.ADD,
            new SyncedListElement("item1", "Item 1", "Description 1")));
        list.addElementStep(new SyncedListStep("item2", ACTION.ADD,
            new SyncedListElement("item2", "Item 2", "Description 2")));

        // This may throw or be ignored - verify it doesn't corrupt the list
        try
        {
            list.addElementStep(new SyncedListStep("item1", ACTION.MOVE, -1));
        }
        catch (Exception e)
        {
            // Expected for invalid index
        }

        // List should have 2 elements regardless
        Assert.assertEquals(2, list.getElements().size());
    }
}
