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

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.List;
import java.util.prefs.Preferences;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.prefs.Preferences.MAX_KEY_LENGTH;

/**
 * A model for a list of {@link Item}s.
 *
 * @author Christian Pesch
 */

public abstract class ItemSelectionModel<T extends Item> {
    private static final Preferences preferences = Preferences.userNodeForPackage(ItemSelectionModel.class);
    private final String preferenceName;
    private final boolean defaultValue;

    private EventListenerList listenerList = new EventListenerList();

    public ItemSelectionModel(String preferenceName, boolean defaultValue) {
        this.preferenceName = preferenceName;
        this.defaultValue = defaultValue;
    }

    private String getKey(T item) {
        return preferenceName + itemToString(item);
    }

    public boolean isSelected(T item) {
        return preferences.getBoolean(getKey(item), defaultValue);
    }

    public void toggleSelected(T item) {
        preferences.putBoolean(getKey(item), !isSelected(item));
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
