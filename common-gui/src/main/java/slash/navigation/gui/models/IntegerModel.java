/*
 *
 *     This file is part of RouteConverter.
 *
 *     RouteConverter is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     RouteConverter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with RouteConverter; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *     Copyright (C) 2007 Christian Pesch. All Rights Reserved.
 * /
 */

package slash.navigation.gui.models;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.prefs.Preferences;

import static java.util.prefs.Preferences.MAX_KEY_LENGTH;
import static slash.common.io.Transfer.trim;

/**
 * A model for a {@link Integer}.
 *
 * @author Christian Pesch
 */

public class IntegerModel {
    private static final Preferences preferences = Preferences.userNodeForPackage(IntegerModel.class);

    private final String preferencesName;
    private final Integer defaultValue;

    private final EventListenerList listenerList = new EventListenerList();

    public IntegerModel(String preferencesName, Integer defaultValue) {
        this.preferencesName = preferencesName;
        this.defaultValue = defaultValue;
    }

    public Integer getInteger() {
        return preferences.getInt(preferencesName, defaultValue);
    }

    public void setInteger(Integer integerValue) {
        preferences.putInt(trim(preferencesName, MAX_KEY_LENGTH), integerValue);
        fireChanged();
    }

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
