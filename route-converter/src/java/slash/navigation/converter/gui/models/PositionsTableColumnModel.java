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

import slash.navigation.BaseRoute;
import slash.navigation.converter.gui.renderer.PositionsTableCellHeaderRenderer;
import slash.navigation.converter.gui.renderer.PositionsTableCellRenderer;

import javax.swing.table.*;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Acts as a {@link TableColumnModel} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class PositionsTableColumnModel extends DefaultTableColumnModel {
    public static final int DESCRIPTION_COLUMN_INDEX = 0;
    public static final int TIME_COLUMN_INDEX = 1;
    public static final int LONGITUDE_COLUMN_INDEX = 2;
    public static final int LATITUDE_COLUMN_INDEX = 3;
    public static final int ELEVATION_COLUMN_INDEX = 4;
    public static final int SPEED_COLUMN_INDEX = 5;

    private List<PositionTableColumn> predefinedColumns = new ArrayList<PositionTableColumn>();

    public PositionsTableColumnModel() {
        PositionsTableCellRenderer leftAligned = new PositionsTableCellRenderer(SwingConstants.LEFT);
        PositionsTableCellRenderer rightAligned = new PositionsTableCellRenderer(SwingConstants.RIGHT);
        PositionsTableCellHeaderRenderer headerRenderer = new PositionsTableCellHeaderRenderer();
        predefineColumn(DESCRIPTION_COLUMN_INDEX, "description", null, true, leftAligned, headerRenderer);
        predefineColumn(TIME_COLUMN_INDEX, "time", 108, false, rightAligned, headerRenderer);
        predefineColumn(SPEED_COLUMN_INDEX, "speed", 50, false, rightAligned, headerRenderer);
        predefineColumn(LONGITUDE_COLUMN_INDEX, "longitude", 68, true, rightAligned, headerRenderer);
        predefineColumn(LATITUDE_COLUMN_INDEX, "latitude", 68, true, rightAligned, headerRenderer);
        predefineColumn(ELEVATION_COLUMN_INDEX, "elevation", 50, true, rightAligned, headerRenderer);

        for (PositionTableColumn predefinedColumn : predefinedColumns) {
            if (predefinedColumn.isVisible())
                addColumn(predefinedColumn);
        }
    }

    private void predefineColumn(int modelIndex, String name, Integer maxWidth, boolean visible,
                                 TableCellRenderer cellRenderer, TableCellRenderer headerRenderer) {
        PositionTableColumn column = new PositionTableColumn(modelIndex, name, visible, cellRenderer, null);
        column.setHeaderRenderer(headerRenderer);
        if (maxWidth != null) {
            column.setMaxWidth(maxWidth);
            column.setPreferredWidth(maxWidth);
        }
        predefinedColumns.add(column);
    }

    public List<PositionTableColumn> getPreparedColumns() {
        return predefinedColumns;
    }

    public void addColumn(int columnIndex, TableColumn aColumn) {
        if (aColumn == null) {
            throw new IllegalArgumentException("Object is null");
        }

        tableColumns.add(columnIndex, aColumn);
        aColumn.addPropertyChangeListener(this);
        recalcWidthCache();

        // Post columnAdded event notification
        fireColumnAdded(new TableColumnModelEvent(this, columnIndex, columnIndex));
    }

    public int getVisibleColumnCount() {
        int count = 0;
        for (PositionTableColumn predefinedColumn : predefinedColumns) {
            if (predefinedColumn.isVisible())
                count++;
        }
        return count;
    }

    private int indexOf(PositionTableColumn predefinedColumn) {
        int index = predefinedColumns.indexOf(predefinedColumn);
        int result = 0;
        for (int i = 0; i < index; i++) {
            if (predefinedColumns.get(i).isVisible())
                result++;
        }
        return result;
    }

    public void toggleVisibility(PositionTableColumn column) {
        column.toggleVisibility();
        if (column.isVisible())
            addColumn(indexOf(column), column);
        else
            removeColumn(column);
    }

}
