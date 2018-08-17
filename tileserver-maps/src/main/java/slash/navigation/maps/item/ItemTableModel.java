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

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Acts as a {@link TableModel} for {@link Item}s.
 *
 * @author Christian Pesch
 */

public class ItemTableModel<T extends Item> extends AbstractTableModel {
    private final int columnCount;
    private final List<T> items = new ArrayList<>();

    public ItemTableModel(int columnCount) {
        this.columnCount = columnCount;
    }

    public List<T> getItems() {
        return items;
    }

    public int getRowCount() {
        return items.size();
    }

    public int getColumnCount() {
        return columnCount;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getItem(rowIndex);
    }

    public T getItem(int rowIndex) {
        return items.get(rowIndex);
    }

    public T getItemByUrl(String url) {
        for (T item : new ArrayList<>(items)) {
            if (item.getUrl().equals(url))
                return item;
        }
        return null;
    }

    public T getItemByDescription(String description) {
        for (T item : new ArrayList<>(items)) {
            if (item.getDescription().equals(description))
                return item;
        }
        return null;
    }

    public int getIndex(T item) {
        return items.indexOf(item);
    }

    public boolean contains(T item) {
        return items.contains(item);
    }

    private void addItem(T item) {
        if (!items.add(item))
            throw new IllegalArgumentException("Item " + item + " not added to " + items);

        final int index = getIndex(item);
        if (index == -1)
            throw new IllegalArgumentException("Item " + item + " not found in " + items);

        fireTableRowsInserted(index, index);
    }

    private void updateItem(T item) {
        final int index = getIndex(item);
        if (index == -1)
            throw new IllegalArgumentException("Item " + item + " not found in " + items);

        fireTableRowsUpdated(index, index);
    }

    public void addOrUpdateItem(T item) {
        int index = getIndex(item);
        if (index == -1)
            addItem(item);
        else
            updateItem(item);
    }

    public void removeItem(T item) {
        final int index = getIndex(item);
        if (index == -1)
            throw new IllegalArgumentException("Item " + item + " not found in " + items);
        items.remove(index);
        fireTableRowsDeleted(index, index);
    }

    public void add(int index, T item) {
        items.add(index, item);
        fireTableRowsInserted(index, index);
    }

    public void update(int index, T item) {
        items.set(index, item);
        fireTableRowsUpdated(index, index);
    }

    public void remove(int firstIndex, int lastIndex) {
        for (int i = lastIndex; i > firstIndex; i--)
            items.remove(i);
        fireTableRowsDeleted(firstIndex, lastIndex);
    }

    public void clear() {
        items.clear();
        fireTableDataChanged();
    }
}
