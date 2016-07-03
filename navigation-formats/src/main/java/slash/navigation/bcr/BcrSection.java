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

package slash.navigation.bcr;

import slash.navigation.base.IniFileSection;

import java.util.HashSet;

/**
 * Represents a section in a Map&Guide Tourenplaner Route (.bcr) file,
 * excluding the positions.
 *
 * @author Christian Pesch
 */

class BcrSection extends IniFileSection {
    static final String STATION_PREFIX = "STATION";

    public BcrSection(String title) {
        super(title);
    }

    int getStationCount() {
        int count = 1;
        while (getStation(count) != null) {
            count++;
        }
        return count;
    }

    String getStation(int index) {
        return get(STATION_PREFIX + index);
    }

    void removeStations() {
        for (String key : new HashSet<>(keySet())) {
            if (key.startsWith(STATION_PREFIX))
                remove(key);
        }
    }

    public void put(String name, String value) {
        super.put(name.toUpperCase(), value);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BcrSection section = (BcrSection) o;

        return nameValues.equals(section.nameValues) && title.equals(section.title);
    }

    public int hashCode() {
        int result;
        result = nameValues.hashCode();
        result = 31 * result + title.hashCode();
        return result;
    }
}
