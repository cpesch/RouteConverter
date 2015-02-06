/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.base;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents INI file sections.
 *
 * @author Christian Pesch
 */

public abstract class IniFileSection {
    protected final Map<String, String> nameValues = new LinkedHashMap<>();
    protected final String title;

    public IniFileSection(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String get(String name) {
        return nameValues.get(name);
    }

    protected Integer getInteger(String name) {
        String value = get(name);
        return value != null ? Integer.parseInt(value) : null;
    }

    public Set<String> keySet() {
        return nameValues.keySet();
    }

    public void put(String name, String value) {
        nameValues.put(name, value);
    }

    public void remove(String name) {
        nameValues.remove(name);
    }
}