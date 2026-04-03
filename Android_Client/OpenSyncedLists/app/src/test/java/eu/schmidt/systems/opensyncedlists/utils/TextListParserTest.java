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
package eu.schmidt.systems.opensyncedlists.utils;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for TextListParser - serves as documentation of supported formats.
 * <p>
 * This class contains comprehensive tests for all supported text import formats.
 * Each test method name describes the format being tested and can be used as
 * documentation for users.
 */
public class TextListParserTest
{
    // ========== Bullet Point Format Tests ==========

    /**
     * Example: German shopping list with dash bullets
     * <pre>
     * - Äpfel
     * - Birnen
     * - Bananen
     * </pre>
     */
    @Test
    public void testBulletPoints_DashPrefix()
    {
        String input = "- Äpfel\n- Birnen\n- Bananen";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
        assertEquals("Äpfel", result.get(0).getName());
        assertEquals("Birnen", result.get(1).getName());
        assertEquals("Bananen", result.get(2).getName());
        assertEquals("", result.get(0).getDescription());
    }

    /**
     * Example: List with asterisk bullets
     * <pre>
     * * Item 1
     * * Item 2
     * </pre>
     */
    @Test
    public void testBulletPoints_AsteriskPrefix()
    {
        String input = "* Item 1\n* Item 2";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(2, result.size());
        assertEquals("Item 1", result.get(0).getName());
        assertEquals("Item 2", result.get(1).getName());
    }

    /**
     * Example: Mixed bullet prefixes (should all work)
     * <pre>
     * - Item 1
     * * Item 2
     * - Item 3
     * </pre>
     */
    @Test
    public void testBulletPoints_MixedPrefixes()
    {
        String input = "- Item 1\n* Item 2\n- Item 3";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
        assertEquals("Item 1", result.get(0).getName());
        assertEquals("Item 2", result.get(1).getName());
        assertEquals("Item 3", result.get(2).getName());
    }

    /**
     * Example: Bullet with tab separator
     * <pre>
     * -	Item with tab
     * </pre>
     */
    @Test
    public void testBulletPoints_TabAfterBullet()
    {
        String input = "-\tItem with tab\n*\tAnother item";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(2, result.size());
        assertEquals("Item with tab", result.get(0).getName());
        assertEquals("Another item", result.get(1).getName());
    }

    // ========== Comma-Separated Format Tests ==========

    /**
     * Example: Simple comma-separated list
     * <pre>
     * Äpfel, Birne, Tanne
     * </pre>
     */
    @Test
    public void testCommaSeparated_Simple()
    {
        String input = "Äpfel, Birne, Tanne";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
        assertEquals("Äpfel", result.get(0).getName());
        assertEquals("Birne", result.get(1).getName());
        assertEquals("Tanne", result.get(2).getName());
    }

    /**
     * Example: Comma-separated with extra spaces (trimmed)
     * <pre>
     *   Apple  ,  Banana  ,  Cherry
     * </pre>
     */
    @Test
    public void testCommaSeparated_WithSpaces()
    {
        String input = "  Apple  ,  Banana  ,  Cherry  ";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
        assertEquals("Apple", result.get(0).getName());
        assertEquals("Banana", result.get(1).getName());
        assertEquals("Cherry", result.get(2).getName());
    }

    /**
     * Example: Comma-separated with empty items (filtered out)
     * <pre>
     * Apple,,Banana,  ,Cherry
     * </pre>
     */
    @Test
    public void testCommaSeparated_EmptyItemsFiltered()
    {
        String input = "Apple,,Banana,  ,Cherry";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
        assertEquals("Apple", result.get(0).getName());
        assertEquals("Banana", result.get(1).getName());
        assertEquals("Cherry", result.get(2).getName());
    }

    /**
     * Example: Single item with commas (no actual separation)
     * Note: Single line with commas is treated as comma-separated.
     */
    @Test
    public void testCommaSeparated_SingleItem()
    {
        String input = "Only one item";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(1, result.size());
        assertEquals("Only one item", result.get(0).getName());
    }

    // ========== Indented Description Format Tests ==========

    /**
     * Example: Items with 4-space indented descriptions
     * <pre>
     * - Äpfel
     *     Unterbeschreibung
     * - Birne
     *     Test
     * </pre>
     */
    @Test
    public void testBulletWithDescription_FourSpaces()
    {
        String input = "- Äpfel\n    Unterbeschreibung\n- Birne\n    Test";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(2, result.size());
        assertEquals("Äpfel", result.get(0).getName());
        assertEquals("Unterbeschreibung", result.get(0).getDescription());
        assertEquals("Birne", result.get(1).getName());
        assertEquals("Test", result.get(1).getDescription());
    }

    /**
     * Example: Items with tab-indented descriptions
     * <pre>
     * - Item 1
     * 	Description for item 1
     * - Item 2
     * </pre>
     */
    @Test
    public void testBulletWithDescription_Tab()
    {
        String input = "- Item 1\n\tDescription for item 1\n- Item 2";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(2, result.size());
        assertEquals("Item 1", result.get(0).getName());
        assertEquals("Description for item 1", result.get(0).getDescription());
        assertEquals("Item 2", result.get(1).getName());
        assertEquals("", result.get(1).getDescription());
    }

    /**
     * Example: Item with multi-line description
     * <pre>
     * - Item 1
     *     Line 1 of description
     *     Line 2 of description
     * - Item 2
     * </pre>
     */
    @Test
    public void testBulletWithMultiLineDescription()
    {
        String input = "- Item 1\n    Line 1\n    Line 2\n- Item 2";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(2, result.size());
        assertEquals("Item 1", result.get(0).getName());
        assertEquals("Line 1\nLine 2", result.get(0).getDescription());
        assertEquals("Item 2", result.get(1).getName());
    }

    /**
     * Example: Bio products with descriptions
     * <pre>
     * - Äpfel
     *     Bio aus der Region
     * - Birne
     *     Grüne Williams
     * - Banana
     *     In Bio
     * </pre>
     */
    @Test
    public void testBulletWithDescription_GermanExample()
    {
        String input = "- Äpfel\n    Bio aus der Region\n- Birne\n    Grüne Williams\n- Banana\n    In Bio";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
        assertEquals("Äpfel", result.get(0).getName());
        assertEquals("Bio aus der Region", result.get(0).getDescription());
        assertEquals("Birne", result.get(1).getName());
        assertEquals("Grüne Williams", result.get(1).getDescription());
        assertEquals("Banana", result.get(2).getName());
        assertEquals("In Bio", result.get(2).getDescription());
    }

    /**
     * Example: 3-space indent also works
     */
    @Test
    public void testBulletWithDescription_ThreeSpaces()
    {
        String input = "- Item\n   Description with 3 spaces";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(1, result.size());
        assertEquals("Item", result.get(0).getName());
        assertEquals("Description with 3 spaces", result.get(0).getDescription());
    }

    // ========== Plain Lines Format Tests ==========

    /**
     * Example: Plain lines without bullets
     * <pre>
     * Item 1
     * Item 2
     * Item 3
     * </pre>
     */
    @Test
    public void testPlainLines_NoBullets()
    {
        String input = "Item 1\nItem 2\nItem 3";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
        assertEquals("Item 1", result.get(0).getName());
        assertEquals("Item 2", result.get(1).getName());
        assertEquals("Item 3", result.get(2).getName());
    }

    // ========== Edge Cases ==========

    @Test
    public void testEmptyInput()
    {
        assertEquals(0, TextListParser.parse("").size());
    }

    @Test
    public void testNullInput()
    {
        assertEquals(0, TextListParser.parse(null).size());
    }

    @Test
    public void testWhitespaceOnlyInput()
    {
        assertEquals(0, TextListParser.parse("   ").size());
        assertEquals(0, TextListParser.parse("\n\n\n").size());
        assertEquals(0, TextListParser.parse("  \n  \n  ").size());
    }

    @Test
    public void testSingleItem()
    {
        List<ParsedElement> result = TextListParser.parse("- Single item");
        assertEquals(1, result.size());
        assertEquals("Single item", result.get(0).getName());
    }

    /**
     * Empty lines between items are ignored.
     */
    @Test
    public void testEmptyLinesBetweenItems()
    {
        String input = "- Item 1\n\n- Item 2\n\n\n- Item 3";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
    }

    /**
     * Unicode characters (German umlauts, Japanese, emoji) are preserved.
     */
    @Test
    public void testUnicodeCharacters()
    {
        String input = "- 日本語\n- Emoji 🎉\n- Ümläütß";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
        assertEquals("日本語", result.get(0).getName());
        assertEquals("Emoji 🎉", result.get(1).getName());
        assertEquals("Ümläütß", result.get(2).getName());
    }

    /**
     * Indented lines before any bullet should be treated as regular items.
     */
    @Test
    public void testIndentedLineWithoutPreviousBullet()
    {
        String input = "    indented first line\n- Actual item";
        List<ParsedElement> result = TextListParser.parse(input);

        // The indented line is treated as a plain item since no bullet precedes it
        assertEquals(2, result.size());
    }

    /**
     * Empty bullet (just "-") is handled gracefully.
     */
    @Test
    public void testEmptyBullet()
    {
        String input = "-\n- Item with content";
        List<ParsedElement> result = TextListParser.parse(input);

        // Empty bullet creates empty name, filtered or kept
        assertEquals(2, result.size());
        assertEquals("", result.get(0).getName());
        assertEquals("Item with content", result.get(1).getName());
    }

    /**
     * Special characters in content are preserved.
     */
    @Test
    public void testSpecialCharactersInContent()
    {
        String input = "- Item with \"quotes\"\n- Item with <brackets>\n- Item & ampersand";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
        assertEquals("Item with \"quotes\"", result.get(0).getName());
        assertEquals("Item with <brackets>", result.get(1).getName());
        assertEquals("Item & ampersand", result.get(2).getName());
    }

    // ========== Real-World Examples ==========

    /**
     * Example: Shopping list (comma-separated)
     */
    @Test
    public void testRealWorld_ShoppingListCommas()
    {
        String input = "Äpfel, Birnen, Milch, Brot, Käse";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(5, result.size());
        assertEquals("Äpfel", result.get(0).getName());
        assertEquals("Käse", result.get(4).getName());
    }

    /**
     * Example: Shopping list (bullet points)
     */
    @Test
    public void testRealWorld_ShoppingListBullets()
    {
        String input = "- Milch\n- Eier\n- Brot\n- Butter";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(4, result.size());
    }

    /**
     * Example: Todo list with descriptions
     */
    @Test
    public void testRealWorld_TodoListWithDescriptions()
    {
        String input = "- Buy groceries\n    Milk, eggs, bread\n" +
            "- Call mom\n- Fix bike\n    Need new tire";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
        assertEquals("Buy groceries", result.get(0).getName());
        assertEquals("Milk, eggs, bread", result.get(0).getDescription());
        assertEquals("Call mom", result.get(1).getName());
        assertEquals("", result.get(1).getDescription());
        assertEquals("Fix bike", result.get(2).getName());
        assertEquals("Need new tire", result.get(2).getDescription());
    }

    /**
     * Example: Recipe ingredients
     */
    @Test
    public void testRealWorld_RecipeIngredients()
    {
        String input = "- 500g Mehl\n    Typ 405\n- 2 Eier\n- 250ml Milch\n    Vollmilch";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(3, result.size());
        assertEquals("500g Mehl", result.get(0).getName());
        assertEquals("Typ 405", result.get(0).getDescription());
        assertEquals("2 Eier", result.get(1).getName());
        assertEquals("", result.get(1).getDescription());
    }

    /**
     * Example: Packing list
     */
    @Test
    public void testRealWorld_PackingList()
    {
        String input = "Reisepass, Ladekabel, Zahnbürste, Medikamente, Sonnenbrille";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(5, result.size());
        assertEquals("Reisepass", result.get(0).getName());
        assertEquals("Sonnenbrille", result.get(4).getName());
    }

    // ========== Description Edge Cases ==========

    /**
     * Multiple consecutive description lines are joined.
     */
    @Test
    public void testMultipleDescriptionLinesJoined()
    {
        String input = "- Item\n    Line 1\n    Line 2\n    Line 3";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(1, result.size());
        assertEquals("Line 1\nLine 2\nLine 3", result.get(0).getDescription());
    }

    /**
     * Description with only whitespace is trimmed.
     */
    @Test
    public void testDescriptionTrimsWhitespace()
    {
        String input = "- Item\n    Description with trailing space   ";
        List<ParsedElement> result = TextListParser.parse(input);

        assertEquals(1, result.size());
        assertEquals("Description with trailing space", result.get(0).getDescription());
    }
}
