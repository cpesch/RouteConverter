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
package slash.navigation.maps.impl;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.prefs.Preferences;

/**
 * A model for a single {@link Object}.
 *
 * @author Christian Pesch
 */

public abstract class ItemModel<T> {
    private static final Preferences preferences = Preferences.userNodeForPackage(ItemModel.class);
    private final String preferenceName;
    private final String defaultValue;

    private EventListenerList listenerList = new EventListenerList();

    public ItemModel(String preferenceName, String defaultValue) {
        this.preferenceName = preferenceName;
        this.defaultValue = defaultValue;
    }

    public T getItem() {
        try {
            T item = stringToItem(preferences.get(preferenceName, defaultValue));
            if (item != null)
                return item;
        } catch (IllegalArgumentException e) {
            // intentionally left empty
        }
        return stringToItem(defaultValue);
    }

    protected abstract T stringToItem(String value);

    public void setItem(T item) {
        preferences.put(preferenceName, itemToString(item));
        fireChanged();
    }

    protected abstract String itemToString(T item);

    protected void fireChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(null);
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
