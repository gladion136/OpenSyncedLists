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

/**
 * Represents a parsed element from text import with name and optional description.
 * Used by TextListParser to return parsed results.
 */
public class ParsedElement
{
    private final String name;
    private final String description;

    /**
     * Create a parsed element with name and description.
     *
     * @param name        the element name (required)
     * @param description the element description (can be null or empty)
     */
    public ParsedElement(String name, String description)
    {
        this.name = name;
        this.description = description != null ? description : "";
    }

    /**
     * Get the element name.
     *
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the element description.
     *
     * @return the description (never null, may be empty)
     */
    public String getDescription()
    {
        return description;
    }

    @Override
    public String toString()
    {
        return "ParsedElement{name='" + name + "', description='" + description + "'}";
    }
}
