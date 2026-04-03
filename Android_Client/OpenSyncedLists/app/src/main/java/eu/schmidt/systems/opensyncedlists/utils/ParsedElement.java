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
 * Represents a parsed element from text import with name and optional
 * description. Used by TextListParser to return parsed results.
 */
public class ParsedElement
{
    private final String name;
    private final String description;
    private final boolean checked;
    
    public ParsedElement(String name, String description, boolean checked)
    {
        this.name = name;
        this.description = description != null ? description : "";
        this.checked = checked;
    }
    
    public ParsedElement(String name, String description)
    {
        this(name, description, false);
    }
    
    @Override public String toString()
    {
        return "ParsedElement{name='" + name + "', description='" + description
            + "', checked=" + checked + "}";
    }
    
    public String getName()
    {
        return name;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public boolean isChecked()
    {
        return checked;
    }
}
