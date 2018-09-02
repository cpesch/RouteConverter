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
package slash.navigation.maps.tileserver.helpers;

import slash.navigation.maps.item.Item;
import slash.navigation.maps.item.ItemTableModel;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import static javax.swing.event.TableModelEvent.DELETE;
import static javax.swing.event.TableModelEvent.INSERT;
import static javax.swing.event.TableModelEvent.UPDATE;

/**
 * Listens to an {@link ItemTableModel} and stores added {@link Item}s in the {@link Preferences}.
 *
 * @author Christian Pesch
 */

public abstract class ItemPreferencesMediator<T extends Item> {
    private static final Preferences preferences = Preferences.userNodeForPackage(ItemPreferencesMediator.class);
    private final ItemTableModel<T> dataModel;
    private final ItemTableModel<T> selectionModel;
    private final String preferenceName;
    private TableModelListener dataListener, selectionListener;

    protected ItemPreferencesMediator(ItemTableModel<T> dataModel, ItemTableModel<T> selectionModel, String preferencesName) {
        this.dataModel = dataModel;
        this.selectionModel = selectionModel;
        this.preferenceName = preferencesName;

        dataListener = new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                switch (e.getType()) {
                    case INSERT:
                        handleDataAdd(e.getFirstRow(), e.getLastRow());
                        break;
                    case DELETE:
                        handleDataRemove(e.getFirstRow(), e.getLastRow());
                        break;
                    case UPDATE:
                        break;
                    default:
                        throw new IllegalArgumentException("Event type " + e.getType() + " is not supported");
                }
            }
        };
        dataModel.addTableModelListener(dataListener);

        selectionListener = new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                switch (e.getType()) {
                    case INSERT:
                        handleSelectionAdd(e.getFirstRow(), e.getLastRow());
                        break;
                    case DELETE:
                        handleSelectionRemove(e.getFirstRow(), e.getLastRow());
                        break;
                    case UPDATE:
                        break;
                    default:
                        throw new IllegalArgumentException("Event type " + e.getType() + " is not supported");
                }
            }
        };
        selectionModel.addTableModelListener(selectionListener);
    }

    public void dispose() {
        dataModel.removeTableModelListener(dataListener);
        dataListener = null;

        selectionModel.removeTableModelListener(selectionListener);
        selectionListener = null;
    }

    private String getKey(T item) {
        return preferenceName + itemToString(item);
    }

    protected abstract String itemToString(T item);

    private void handleDataAdd(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            T item = dataModel.getItem(i);
            if(preferences.getBoolean(getKey(item), false))
                selectionModel.addOrUpdateItem(item);
        }
    }

    private void handleDataRemove(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            T item = dataModel.getItem(i);
            preferences.remove(getKey(item));
        }
    }

    private List<String> keys = new ArrayList<>();

    private void handleSelectionAdd(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            T item = selectionModel.getItem(i);
            String key = getKey(item);
            preferences.putBoolean(key, true);
            keys.add(i, key);
        }
    }

    private void handleSelectionRemove(int firstRow, int lastRow) {
        Set<String> toRemove = new HashSet<>();
        for (int i = firstRow; i < lastRow + 1; i++) {
            String key = keys.get(i);
            preferences.remove(key);
            toRemove.add(key);
        }
        keys.removeAll(toRemove);
    }
}
