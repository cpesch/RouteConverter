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

import slash.navigation.maps.impl.ItemModel;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import java.util.HashMap;
import java.util.Map;

import static javax.swing.event.ListDataEvent.*;
import static javax.swing.event.TableModelEvent.*;

/**
 * Adapts a {@link TableModel} to a {@link ComboBoxModel}.
 *
 * @author Christian Pesch
 */

public class TableModelToComboBoxModelAdapter<E> implements ComboBoxModel<E> {
    private final TableModel modelDelegate;
    private final ItemModel<E> selectedDelegate;
    private final Map<ListDataListener, TableModelListener> listToTableListener = new HashMap<>();

    public TableModelToComboBoxModelAdapter(TableModel modelDelegate, ItemModel<E> selectedDelegate) {
        this.modelDelegate = modelDelegate;
        this.selectedDelegate = selectedDelegate;

        // since DefaultComboBoxModel communicates changes of the selected item like this
        selectedDelegate.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                for (ListDataListener listener : listToTableListener.keySet())
                    listener.contentsChanged(new ListDataEvent(this, CONTENTS_CHANGED, -1, -1));
            }
        });
    }

    public int getSize() {
        return modelDelegate.getRowCount();
    }

    @SuppressWarnings("unchecked")
    public E getElementAt(int index) {
        return (E) modelDelegate.getValueAt(index, -1);
    }

    public void addListDataListener(ListDataListener listener) {
        TableModelToListDataListenerAdapter adapter = new TableModelToListDataListenerAdapter(listener);
        listToTableListener.put(listener, adapter);
        modelDelegate.addTableModelListener(adapter);
    }

    public void removeListDataListener(ListDataListener listener) {
        TableModelListener adapter = listToTableListener.get(listener);
        if (adapter != null)
            modelDelegate.removeTableModelListener(adapter);
    }

    public Object getSelectedItem() {
        return selectedDelegate.getItem();
    }

    @SuppressWarnings("unchecked")
    public void setSelectedItem(Object anItem) {
        selectedDelegate.setItem((E) anItem);
    }

    private static class TableModelToListDataListenerAdapter implements TableModelListener {
        private final ListDataListener delegate;

        private TableModelToListDataListenerAdapter(ListDataListener delegate) {
            this.delegate = delegate;
        }

        public void tableChanged(TableModelEvent e) {
            switch (e.getType()) {
                case INSERT:
                    delegate.intervalAdded(new ListDataEvent(this, INTERVAL_ADDED, e.getFirstRow(), e.getLastRow()));
                    break;
                case DELETE:
                    delegate.intervalRemoved(new ListDataEvent(this, INTERVAL_REMOVED, e.getFirstRow(), e.getLastRow()));
                    break;
                case UPDATE:
                    delegate.contentsChanged(new ListDataEvent(this, CONTENTS_CHANGED, e.getFirstRow(), e.getLastRow()));
                    break;
                default:
                    throw new IllegalArgumentException("Event " + e + " cannot be handled");
            }
        }
    }
}
