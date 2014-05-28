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
package slash.navigation.converter.gui.models;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Contains values for columns of the positions table.
 *
 * @author Christian Pesch
 */

public class PositionColumnValues {
    private final List<Integer> columnIndices;
    private final List<Object> columnValues;
    private List<Object> previousValues;

    public PositionColumnValues(int columnIndex, Object columnValue) {
        this(singletonList(columnIndex), singletonList(columnValue));
    }

    public PositionColumnValues(List<Integer> columnIndices, List<Object> columnValues) {
        this.columnIndices = columnIndices;
        this.columnValues = columnValues;
    }

    public List<Integer> getColumnIndices() {
        return columnIndices;
    }

    public List<Object> getNextValues() {
        return columnValues;
    }

    public List<Object> getPreviousValues() {
        return previousValues;
    }

    public void setPreviousValues(List<Object> previousValues) {
        this.previousValues = previousValues;
    }
}
