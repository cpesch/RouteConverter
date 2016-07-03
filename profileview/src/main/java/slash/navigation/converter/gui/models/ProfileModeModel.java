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

import slash.navigation.converter.gui.profileview.XAxisMode;
import slash.navigation.converter.gui.profileview.YAxisMode;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.prefs.Preferences;

import static slash.navigation.converter.gui.profileview.XAxisMode.Distance;
import static slash.navigation.converter.gui.profileview.YAxisMode.Elevation;

/**
 * A model for {@link XAxisMode} and {@link YAxisMode}.
 *
 * @author Christian Pesch
 */

public class ProfileModeModel {
    private static final Preferences preferences = Preferences.userNodeForPackage(ProfileModeModel.class);
    private static final String X_AXIS_MODE_PREFERENCE = "xAxis";
    private static final String Y_AXIS_MODE_PREFERENCE = "yAxis";

    private EventListenerList listenerList = new EventListenerList();

    public XAxisMode getXAxisMode() {
        try {
            return XAxisMode.valueOf(preferences.get(X_AXIS_MODE_PREFERENCE, Distance.toString()));
        } catch (IllegalArgumentException e) {
            return Distance;
        }
    }

    public void setXAxisMode(XAxisMode xAxisMode) {
        preferences.put(X_AXIS_MODE_PREFERENCE, xAxisMode.toString());
        fireChanged();
    }

    public YAxisMode getYAxisMode() {
        try {
            return YAxisMode.valueOf(preferences.get(Y_AXIS_MODE_PREFERENCE, Elevation.toString()));
        } catch (IllegalArgumentException e) {
            return Elevation;
        }
    }

    public void setYAxisMode(YAxisMode yAxisMode) {
        preferences.put(Y_AXIS_MODE_PREFERENCE, yAxisMode.toString());
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
}
