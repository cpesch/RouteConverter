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

package slash.navigation.converter.gui.models;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.prefs.Preferences;

import static slash.navigation.converter.gui.models.FixMapMode.Automatic;

/**
 * A model for {@link FixMapMode}.
 *
 * @author Christian Pesch
 */
public class FixMapModeModel {
    private static final Preferences preferences = Preferences.userNodeForPackage(FixMapModeModel.class);
    private static final String FIX_MAP_MODE_PREFERENCE = "fixMapMode";

    private final EventListenerList listenerList = new EventListenerList();

    public FixMapMode getFixMapMode() {
        try {
            return FixMapMode.valueOf(preferences.get(FIX_MAP_MODE_PREFERENCE, Automatic.toString()));
        } catch (IllegalArgumentException e) {
            return Automatic;
        }
    }

    public void setFixMapMode(FixMapMode fixMapMode) {
        preferences.put(FIX_MAP_MODE_PREFERENCE, fixMapMode.toString());
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
