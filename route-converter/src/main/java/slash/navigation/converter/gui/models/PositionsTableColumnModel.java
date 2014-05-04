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

import slash.navigation.base.BaseRoute;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.comparators.DescriptionComparator;
import slash.navigation.converter.gui.comparators.TimeComparator;
import slash.navigation.converter.gui.renderer.*;
import slash.navigation.converter.gui.renderer.PositionsTableHeaderRenderer;
import slash.navigation.converter.gui.renderer.*;

import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.prefs.Preferences;

import static java.text.DateFormat.*;
import static java.text.DateFormat.SHORT;
import static java.util.Calendar.*;
import static java.util.Locale.US;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.gui.helpers.UIHelper.getMaxWidth;

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
        PositionsTableHeaderRenderer headerRenderer = new PositionsTableHeaderRenderer();
        predefineColumn(DESCRIPTION_COLUMN_INDEX, "description", null, true, new DescriptionColumnTableCellEditor(), headerRenderer, new DescriptionComparator());
        predefineColumn(TIME_COLUMN_INDEX, "time", getMaxWidth(getExampleDateFromCurrentLocale(), 10), false, new TimeColumnTableCellEditor(), headerRenderer, new TimeComparator());
        predefineColumn(SPEED_COLUMN_INDEX, "speed", getMaxWidth("999 Km/h", 15), false, new SpeedColumnTableCellEditor(), headerRenderer);
        predefineColumn(DISTANCE_COLUMN_INDEX, "distance", getMaxWidth("12345 Km", 7), false, new DistanceColumnTableCellRenderer(), headerRenderer);
        predefineColumn(LONGITUDE_COLUMN_INDEX, "longitude", 84, true, new LongitudeColumnTableCellEditor(), headerRenderer);
        predefineColumn(LATITUDE_COLUMN_INDEX, "latitude", 84, true, new LatitudeColumnTableCellEditor(), headerRenderer);
        predefineColumn(ELEVATION_COLUMN_INDEX, "elevation", getMaxWidth("9999 m", 5), true, new ElevationColumnTableCellEditor(), headerRenderer);
        predefineColumn(ELEVATION_ASCEND_COLUMN_INDEX, "elevation-ascend", getMaxWidth("9999 m", 5), false, new ElevationDeltaColumnTableCellRenderer(), headerRenderer);
        predefineColumn(ELEVATION_DESCEND_COLUMN_INDEX, "elevation-descend", getMaxWidth("9999 m", 5), false, new ElevationDeltaColumnTableCellRenderer(), headerRenderer);
        predefineColumn(ELEVATION_DIFFERENCE_COLUMN_INDEX, "elevation-difference", getMaxWidth("999 m", 4), false, new ElevationDeltaColumnTableCellRenderer(), headerRenderer);

        VisibleListener visibleListener = new VisibleListener();
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

            column.addPropertyChangeListener(visibleListener);
        }

        for (PositionTableColumn column : columns) {
            if (column != null && column.isVisible())
                addColumn(column);
        }
    }

    private String getExampleDateFromCurrentLocale() {
        Calendar calendar = Calendar.getInstance(US);
        calendar.set(YEAR, 2030);
        calendar.set(MONTH, 11);
        calendar.set(DAY_OF_MONTH, 22);
        calendar.set(HOUR_OF_DAY, 22);
        calendar.set(MINUTE, 33);
        calendar.set(SECOND, 44);
        return getDateTimeInstance(SHORT, MEDIUM).format(new Date(calendar.getTimeInMillis()));
    }

    private void predefineColumn(int modelIndex, String name, Integer maxWidth, boolean visibilityDefault,
                                 TableCellRenderer cellRenderer, TableCellRenderer headerRenderer) {
        predefineColumn(modelIndex, name, maxWidth, visibilityDefault, cellRenderer, null, headerRenderer, null);
    }

    private void predefineColumn(int modelIndex, String name, Integer maxWidth, boolean visibilityDefault,
                                 PositionsTableCellEditor cellEditor, TableCellRenderer headerRenderer) {
        predefineColumn(modelIndex, name, maxWidth, visibilityDefault, cellEditor, headerRenderer, null);
    }

    private void predefineColumn(int modelIndex, String name, Integer maxWidth, boolean visibilityDefault,
                                 PositionsTableCellEditor cellEditor, TableCellRenderer headerRenderer,
                                 Comparator<NavigationPosition> comparator) {
        predefineColumn(modelIndex, name, maxWidth, visibilityDefault, cellEditor, cellEditor, headerRenderer, comparator);
    }

    private void predefineColumn(int modelIndex, String name, Integer maxWidth, boolean visibilityDefault,
                                 TableCellRenderer cellRenderer, TableCellEditor cellEditor, TableCellRenderer headerRenderer,
                                 Comparator<NavigationPosition> comparator) {
        boolean visible = preferences.getBoolean(VISIBLE_PREFERENCE + name, visibilityDefault);
        PositionTableColumn column = new PositionTableColumn(modelIndex, name, visible, cellRenderer, cellEditor, comparator);
        column.setHeaderRenderer(headerRenderer);
        if (maxWidth != null) {
            column.setMaxWidth(maxWidth * 2);
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

    public void moveColumn(int columnIndex, int newIndex) {
        super.moveColumn(columnIndex, newIndex);
        if (columnIndex == newIndex)
            return;

        for (int i = 0; i < getColumnCount(); i++) {
            PositionTableColumn column = (PositionTableColumn) getColumn(i);
            preferences.putInt(ORDER_PREFERENCE + column.getName(), i);
        }
    }

    private void visibilityChanged(PositionTableColumn column) {
        if (column.isVisible())
            addColumn(indexOf(column), column);
        else
            removeColumn(column);
        preferences.putBoolean(VISIBLE_PREFERENCE + column.getName(), column.isVisible());
    }

    private class VisibleListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("visible"))
                visibilityChanged((PositionTableColumn) evt.getSource());
        }
    }
}
