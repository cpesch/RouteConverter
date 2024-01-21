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
package slash.navigation.maps.item;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * Joins two {@link ItemTableModel} into one.
 *
 * @author Christian Pesch
 */

public class JoinedItemTableModel<T extends Item> extends AbstractTableModel {
    private final TableModel first;
    private final TableModel second;

    public JoinedItemTableModel(final TableModel first, TableModel second) {
        this.first = first;
        this.second = second;

        first.addTableModelListener(e -> fireTableChanged(new TableModelEvent(JoinedItemTableModel.this,
                e.getFirstRow(), e.getLastRow(), e.getColumn(), e.getType())));

        second.addTableModelListener(e -> fireTableChanged(new TableModelEvent(JoinedItemTableModel.this,
                first.getRowCount() + e.getFirstRow(), first.getRowCount() + e.getLastRow(),
                e.getColumn(), e.getType())));
    }

    public int getRowCount() {
        return first.getRowCount() + second.getRowCount();
    }

    public int getColumnCount() {
        return first.getColumnCount();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return rowIndex < first.getRowCount() ? first.getValueAt(rowIndex, columnIndex) :
                second.getValueAt(rowIndex - first.getRowCount(), columnIndex);
    }

    @SuppressWarnings("unchecked")
    public T getItem(int rowIndex) {
        return (T) getValueAt(rowIndex, 0);
    }

    @SuppressWarnings("unchecked")
    public T getItemByUrl(String url) {
        for (int i=0; i < getRowCount(); i++) {
            T item = (T) getValueAt(i, 0);
            if (item.getUrl().equals(url))
                return item;
        }
        return null;
    }
}
