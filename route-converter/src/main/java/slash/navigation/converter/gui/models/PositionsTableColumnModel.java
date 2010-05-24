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

import slash.navigation.converter.gui.renderer.PositionsTableCellHeaderRenderer;
import slash.navigation.converter.gui.renderer.PositionsTableCellRenderer;
import slash.navigation.base.*;

import javax.swing.*;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Acts as a {@link TableColumnModel} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class PositionsTableColumnModel extends DefaultTableColumnModel {
    private static final Preferences preferences = Preferences.userNodeForPackage(PositionsTableColumnModel.class);
    private static final String VISIBLE_PREFERENCE = "visible";
    private static final String ORDER_PREFERENCE = "order";

    private final List<PositionTableColumn> predefinedColumns = new ArrayList<PositionTableColumn>();

    public PositionsTableColumnModel() {
        PositionsTableCellRenderer leftAligned = new PositionsTableCellRenderer(SwingConstants.LEFT);
        PositionsTableCellRenderer rightAligned = new PositionsTableCellRenderer(SwingConstants.RIGHT);
        PositionsTableCellHeaderRenderer headerRenderer = new PositionsTableCellHeaderRenderer();
        predefineColumn(PositionColumns.DESCRIPTION_COLUMN_INDEX, "description", null, true, leftAligned, headerRenderer);
        predefineColumn(PositionColumns.TIME_COLUMN_INDEX, "time", 108, false, rightAligned, headerRenderer);
        predefineColumn(PositionColumns.SPEED_COLUMN_INDEX, "speed", 60, false, rightAligned, headerRenderer);
        predefineColumn(PositionColumns.DISTANCE_COLUMN_INDEX, "distance", 54, false, rightAligned, headerRenderer);
        predefineColumn(PositionColumns.ELEVATION_ASCEND_COLUMN_INDEX, "elevation-ascend", 40, false, rightAligned, headerRenderer);
        predefineColumn(PositionColumns.ELEVATION_DESCEND_COLUMN_INDEX, "elevation-descend", 40, false, rightAligned, headerRenderer);
        predefineColumn(PositionColumns.LONGITUDE_COLUMN_INDEX, "longitude", 68, true, rightAligned, headerRenderer);
        predefineColumn(PositionColumns.LATITUDE_COLUMN_INDEX, "latitude", 68, true, rightAligned, headerRenderer);
        predefineColumn(PositionColumns.ELEVATION_COLUMN_INDEX, "elevation", 40, true, rightAligned, headerRenderer);

        PositionTableColumn[] columns = new PositionTableColumn[predefinedColumns.size()];
        for (int i = 0; i < predefinedColumns.size(); i++) {
            PositionTableColumn column = predefinedColumns.get(i);
            int index = preferences.getInt(ORDER_PREFERENCE + column.getName(), i);
            if(columns[index] == null)
                columns[index] = column;
            else if (columns[i] == null)
                columns[i] = column;
            else if(column.isVisible())
                column.toggleVisibility();
        }

        for (PositionTableColumn column : columns) {
            if (column != null && column.isVisible())
                addColumn(column);
        }
    }

    private void predefineColumn(int modelIndex, String name, Integer maxWidth, boolean visiblityDefault,
                                 TableCellRenderer cellRenderer, TableCellRenderer headerRenderer) {
        boolean visible = preferences.getBoolean(VISIBLE_PREFERENCE + name, visiblityDefault);
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

    private void addColumn(int columnIndex, TableColumn aColumn) {
        if (aColumn == null) {
            throw new IllegalArgumentException("Object is null");
        }

        tableColumns.add(columnIndex, aColumn);
        aColumn.addPropertyChangeListener(this);
        recalcWidthCache();

        // post columnAdded event notification
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
        preferences.putBoolean(VISIBLE_PREFERENCE + column.getName(), column.isVisible());
    }

    public void moveColumn(int columnIndex, int newIndex) {
        super.moveColumn(columnIndex, newIndex);
        if (columnIndex == newIndex)
            return;

        for (int i = 0; i < getColumnCount(); i++) {
            PositionTableColumn column = (PositionTableColumn) getColumn(i);
            preferences.putInt(ORDER_PREFERENCE + column.getName(), i);
        }
    }
}
