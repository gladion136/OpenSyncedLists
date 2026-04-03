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

import java.util.ArrayList;
import java.util.List;

/**
 * Parses text input into a list of ParsedElement objects.
 * <p>
 * Supported formats:
 * <ul>
 *   <li>Bullet points: "- Item" or "* Item"</li>
 *   <li>Comma-separated: "Item1, Item2, Item3"</li>
 *   <li>Plain lines: "Item1\nItem2\nItem3"</li>
 *   <li>With indented descriptions:
 *       <pre>
 *       - Item 1
 *           Description for item 1
 *       - Item 2
 *       </pre>
 *   </li>
 * </ul>
 * <p>
 * Examples:
 * <pre>
 * // Bullet points
 * - Äpfel
 * - Birnen
 * - Bananen
 *
 * // Comma-separated
 * Äpfel, Birne, Tanne
 *
 * // With descriptions (4 spaces or tab indent)
 * - Äpfel
 *     Bio aus der Region
 * - Birne
 *     Grüne Williams
 * </pre>
 */
public class TextListParser
{
    /**
     * Parse text input and return list of elements.
     * Auto-detects format based on content.
     *
     * @param input the text to parse
     * @return list of parsed elements (never null)
     */
    public static List<ParsedElement> parse(String input)
    {
        if (input == null || input.trim().isEmpty())
        {
            return new ArrayList<>();
        }

        String[] lines = input.split("\n");

        // Detect format: if first non-empty line starts with bullet, use bullet mode
        // Otherwise check for commas (comma-separated mode)
        String firstLine = findFirstNonEmptyLine(lines);
        if (firstLine == null)
        {
            return new ArrayList<>();
        }

        if (isBulletLine(firstLine))
        {
            return parseBulletFormat(lines);
        }
        else if (isSingleLineWithCommas(input))
        {
            return parseCommaSeparated(input);
        }
        else
        {
            // Plain lines without bullets
            return parsePlainLines(lines);
        }
    }

    /**
     * Parse bullet-formatted text (lines starting with - or *).
     * Supports indented descriptions after bullet items.
     *
     * @param lines array of text lines
     * @return list of parsed elements
     */
    private static List<ParsedElement> parseBulletFormat(String[] lines)
    {
        List<ParsedElement> result = new ArrayList<>();
        String currentName = null;
        StringBuilder currentDescription = new StringBuilder();

        for (String line : lines)
        {
            if (line.trim().isEmpty())
            {
                continue;
            }

            if (isBulletLine(line))
            {
                // Save previous element if exists
                if (currentName != null)
                {
                    result.add(new ParsedElement(currentName,
                        currentDescription.toString().trim()));
                }
                // Start new element
                currentName = stripBulletPrefix(line).trim();
                currentDescription = new StringBuilder();
            }
            else if (isIndentedLine(line) && currentName != null)
            {
                // This is a description line
                if (currentDescription.length() > 0)
                {
                    currentDescription.append("\n");
                }
                currentDescription.append(line.trim());
            }
            else
            {
                // Non-indented, non-bullet line - treat as new element
                if (currentName != null)
                {
                    result.add(new ParsedElement(currentName,
                        currentDescription.toString().trim()));
                }
                currentName = line.trim();
                currentDescription = new StringBuilder();
            }
        }

        // Don't forget the last element
        if (currentName != null && !currentName.isEmpty())
        {
            result.add(new ParsedElement(currentName,
                currentDescription.toString().trim()));
        }

        return result;
    }

    /**
     * Parse comma-separated text.
     *
     * @param input comma-separated string
     * @return list of parsed elements (without descriptions)
     */
    private static List<ParsedElement> parseCommaSeparated(String input)
    {
        List<ParsedElement> result = new ArrayList<>();
        String[] items = input.split(",");
        for (String item : items)
        {
            String trimmed = item.trim();
            if (!trimmed.isEmpty())
            {
                result.add(new ParsedElement(trimmed, ""));
            }
        }
        return result;
    }

    /**
     * Parse plain lines (no bullets, no commas).
     *
     * @param lines array of text lines
     * @return list of parsed elements (without descriptions)
     */
    private static List<ParsedElement> parsePlainLines(String[] lines)
    {
        List<ParsedElement> result = new ArrayList<>();
        for (String line : lines)
        {
            String trimmed = line.trim();
            if (!trimmed.isEmpty())
            {
                result.add(new ParsedElement(trimmed, ""));
            }
        }
        return result;
    }

    /**
     * Check if line starts with a bullet marker (- or *).
     */
    private static boolean isBulletLine(String line)
    {
        String trimmed = line.trim();
        return trimmed.startsWith("- ") || trimmed.startsWith("* ") ||
            trimmed.startsWith("-\t") || trimmed.startsWith("*\t") ||
            trimmed.equals("-") || trimmed.equals("*");
    }

    /**
     * Check if line is indented (starts with spaces or tab).
     * Used to detect description lines.
     */
    private static boolean isIndentedLine(String line)
    {
        return line.startsWith("    ") || line.startsWith("\t") ||
            line.startsWith("   ");
    }

    /**
     * Remove bullet prefix from line.
     */
    private static String stripBulletPrefix(String line)
    {
        String trimmed = line.trim();
        if (trimmed.startsWith("- ") || trimmed.startsWith("* "))
        {
            return trimmed.substring(2);
        }
        if (trimmed.startsWith("-\t") || trimmed.startsWith("*\t"))
        {
            return trimmed.substring(2);
        }
        if (trimmed.equals("-") || trimmed.equals("*"))
        {
            return "";
        }
        return trimmed;
    }

    /**
     * Find first non-empty line in array.
     */
    private static String findFirstNonEmptyLine(String[] lines)
    {
        for (String line : lines)
        {
            if (!line.trim().isEmpty())
            {
                return line;
            }
        }
        return null;
    }

    /**
     * Check if input is a single line with commas (comma-separated format).
     */
    private static boolean isSingleLineWithCommas(String input)
    {
        return input.contains(",") && !input.contains("\n");
    }
}
