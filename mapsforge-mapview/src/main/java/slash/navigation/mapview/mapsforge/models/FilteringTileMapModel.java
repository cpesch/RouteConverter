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
package slash.navigation.mapview.mapsforge.models;

import slash.common.predicates.FilterPredicate;
import slash.navigation.maps.mapsforge.impl.TileMap;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements a {@link TableModel} that filters its {@link TileMap}s.
 *
 * @author Christian Pesch
 */

public class FilteringTileMapModel<E extends TileMap> extends AbstractTableModel {
    private final TableModel delegate;
    private FilterPredicate<E> predicate;
    private Map<Integer, Integer> mapping;

    public FilteringTileMapModel(TableModel delegate, FilterPredicate<E> predicate) {
        this.delegate = delegate;
        this.predicate = predicate;
        initializeMapping();
        delegate.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                initializeMapping();
                fireTableDataChanged();
            }
        });
    }

    private void initializeMapping() {
        mapping = new HashMap<>();
        for (int i = 0, c = delegate.getRowCount(); i < c; i++) {
            @SuppressWarnings("unchecked")
            E element = (E) delegate.getValueAt(i, 0);
            if (predicate.shouldInclude(element)) {
                int mappedRow = mapping.size();
                mapping.put(mappedRow, i);
            }
        }
    }

    private int mapRow(int rowIndex) {
        Integer integer = mapping.get(rowIndex);
        return integer != null ? integer : -1;
    }

    public int getRowCount() {
        return mapping.size();
    }

    public int getColumnCount() {
        throw new IllegalArgumentException("This is determined by the TableColumnModel");
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return delegate.getValueAt(mapRow(rowIndex), columnIndex);
    }
}
