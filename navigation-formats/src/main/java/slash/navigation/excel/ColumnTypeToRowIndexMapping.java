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
package slash.navigation.excel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static slash.navigation.excel.ColumnType.Description;
import static slash.navigation.excel.ColumnType.Elevation;
import static slash.navigation.excel.ColumnType.Latitude;
import static slash.navigation.excel.ColumnType.Longitude;
import static slash.navigation.excel.ColumnType.Speed;
import static slash.navigation.excel.ColumnType.Time;

/**
 * Maps column types to indexes.
 *
 * @author Christian Pesch
 */

class ColumnTypeToRowIndexMapping {
    static final ColumnTypeToRowIndexMapping DEFAULT = new ColumnTypeToRowIndexMapping();
    static {
        DEFAULT.add(0, Longitude);
        DEFAULT.add(1, Latitude);
        DEFAULT.add(2, Elevation);
        DEFAULT.add(3, Speed);
        DEFAULT.add(4, Time);
        DEFAULT.add(5, Description);
    }

    private Map<Integer, ColumnType> mapping = new LinkedHashMap<>();

    public void add(int index, ColumnType columnType) {
        mapping.put(index, columnType);
    }

    public List<Integer> getIndices() {
        return new ArrayList<>(mapping.keySet());
    }

    public Integer getIndex(ColumnType type) {
        for(Integer index : mapping.keySet()) {
           ColumnType found = mapping.get(index);
           if(type.equals(found))
               return index;
        }
        return null;
    }

    public ColumnType getColumnType(Integer index) {
        return mapping.get(index);
    }
}
