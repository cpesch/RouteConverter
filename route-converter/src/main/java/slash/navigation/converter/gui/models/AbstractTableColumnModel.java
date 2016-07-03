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

import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.renderer.PositionsTableCellEditor;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.prefs.Preferences;

import static java.text.DateFormat.MEDIUM;
import static java.text.DateFormat.SHORT;
import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getDateTimeInstance;
import static java.text.DateFormat.getTimeInstance;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static java.util.Locale.US;
import static slash.navigation.converter.gui.models.PositionTableColumn.VISIBLE_PROPERTY_NAME;

/**
 * Helps to make table columns useable.
 *
 * @author Christian Pesch
 */

public abstract class AbstractTableColumnModel extends DefaultTableColumnModel {
    private static final Preferences preferences = Preferences.userNodeForPackage(AbstractTableColumnModel.class);
    private static final String VISIBLE_INFIX = "-visible-";
    private static final String ORDER_INFIX = "-order-";

    private final String preferencesPrefix;
    private final List<PositionTableColumn> predefinedColumns = new ArrayList<>();

    public AbstractTableColumnModel(String preferencesPrefix) {
        this.preferencesPrefix = preferencesPrefix;
    }

    public List<PositionTableColumn> getPreparedColumns() {
        return predefinedColumns;
    }

    protected void predefineColumn(int modelIndex, String name, Integer maxWidth, boolean visibilityDefault,
                                 TableCellRenderer cellRenderer, TableCellRenderer headerRenderer) {
        predefineColumn(modelIndex, name, maxWidth, visibilityDefault, cellRenderer, null, headerRenderer, null);
    }

    protected void predefineColumn(int modelIndex, String name, Integer maxWidth, boolean visibilityDefault,
                                 PositionsTableCellEditor cellEditor, TableCellRenderer headerRenderer) {
        predefineColumn(modelIndex, name, maxWidth, visibilityDefault, cellEditor, headerRenderer, null);
    }

    protected void predefineColumn(int modelIndex, String name, Integer maxWidth, boolean visibilityDefault,
                                 PositionsTableCellEditor cellEditor, TableCellRenderer headerRenderer,
                                 Comparator<NavigationPosition> comparator) {
        predefineColumn(modelIndex, name, maxWidth, visibilityDefault, cellEditor, cellEditor, headerRenderer, comparator);
    }

    protected void predefineColumn(int modelIndex, String name, Integer maxWidth, boolean visibilityDefault,
                                 TableCellRenderer cellRenderer, TableCellEditor cellEditor, TableCellRenderer headerRenderer,
                                 Comparator<NavigationPosition> comparator) {
        boolean visible = preferences.getBoolean(createVisibleKey(name), visibilityDefault);
        PositionTableColumn column = new PositionTableColumn(modelIndex, name, visible, cellRenderer, cellEditor, comparator);
        column.setHeaderRenderer(headerRenderer);
        if (maxWidth != null) {
            column.setMaxWidth(maxWidth * 2);
            column.setPreferredWidth(maxWidth);
        }
        predefinedColumns.add(column);
    }

    private String createVisibleKey(String columnName) {
        return preferencesPrefix + VISIBLE_INFIX + columnName;
    }

    private String createOrderKey(String columnName) {
        return preferencesPrefix + ORDER_INFIX + columnName;
    }

    protected static Calendar createExampleCalendar() {
        Calendar calendar = Calendar.getInstance(US);
        calendar.set(YEAR, 2030);
        calendar.set(MONTH, 11);
        calendar.set(DAY_OF_MONTH, 22);
        calendar.set(HOUR_OF_DAY, 22);
        calendar.set(MINUTE, 33);
        calendar.set(SECOND, 44);
        return calendar;
    }

    protected static String getExampleDateTimeFromCurrentLocale() {
        Calendar calendar = createExampleCalendar();
        return getDateTimeInstance(SHORT, MEDIUM).format(new Date(calendar.getTimeInMillis()));
    }

    protected static String getExampleDateFromCurrentLocale() {
        Calendar calendar = createExampleCalendar();
        return getDateInstance(SHORT).format(new Date(calendar.getTimeInMillis()));
    }

    protected static String getExampleTimeFromCurrentLocale() {
        Calendar calendar = createExampleCalendar();
        return getTimeInstance(MEDIUM).format(new Date(calendar.getTimeInMillis()));
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
            preferences.putInt(createOrderKey(column.getName()), i);
        }
    }

    protected void initializeColumns() {
        VisibleListener visibleListener = new VisibleListener();
        PositionTableColumn[] columns = new PositionTableColumn[predefinedColumns.size()];
        for (int i = 0; i < predefinedColumns.size(); i++) {
            PositionTableColumn column = predefinedColumns.get(i);
            int index = preferences.getInt(createOrderKey(column.getName()), i);
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

    public int getVisibleColumnCount() {
        int count = 0;
        for (PositionTableColumn predefinedColumn : getPreparedColumns()) {
            if (predefinedColumn.isVisible())
                count++;
        }
        return count;
    }

    private void visibilityChanged(PositionTableColumn column) {
        if (column.isVisible())
            addColumn(indexOf(column), column);
        else
            removeColumn(column);
        preferences.putBoolean(createVisibleKey(column.getName()), column.isVisible());

        fireVisibilityChanged(column);
    }

    private class VisibleListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(VISIBLE_PROPERTY_NAME))
                visibilityChanged((PositionTableColumn) evt.getSource());
        }
    }

    protected void fireVisibilityChanged(PositionTableColumn column) {
        ChangeEvent changeEvent = null;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(column);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }
}
