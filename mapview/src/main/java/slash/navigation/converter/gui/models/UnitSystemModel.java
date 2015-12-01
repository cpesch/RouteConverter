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

import slash.navigation.common.DegreeFormat;
import slash.navigation.common.UnitSystem;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.prefs.Preferences;

import static slash.navigation.common.DegreeFormat.Degrees;
import static slash.navigation.common.UnitSystem.Metric;

/**
 * A model for {@link UnitSystem}.
 *
 * @author Christian Pesch
 */

public class UnitSystemModel {
    private static final Preferences preferences = Preferences.userNodeForPackage(UnitSystemModel.class);
    private static final String UNIT_SYSTEM_PREFERENCE = "unitSystem";
    private static final String DEGREE_FORMAT_PREFERENCE = "degreeFormat";

    private EventListenerList listenerList = new EventListenerList();

    public UnitSystem getUnitSystem() {
        try {
            return UnitSystem.valueOf(preferences.get(UNIT_SYSTEM_PREFERENCE, Metric.toString()));
        } catch (IllegalArgumentException e) {
            return Metric;
        }
    }

    public void setUnitSystem(UnitSystem unitSystem) {
        preferences.put(UNIT_SYSTEM_PREFERENCE, unitSystem.toString());
        fireChanged();
    }

    public DegreeFormat getDegreeFormat() {
        try {
            return DegreeFormat.valueOf(preferences.get(DEGREE_FORMAT_PREFERENCE, Degrees.toString()));
        } catch (IllegalArgumentException e) {
            return Degrees;
        }
    }

    public void setDegreeFormat(DegreeFormat degreeFormat) {
        preferences.put(DEGREE_FORMAT_PREFERENCE, degreeFormat.toString());
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
