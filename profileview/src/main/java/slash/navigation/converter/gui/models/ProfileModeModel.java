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

import slash.navigation.converter.gui.profileview.ProfileMode;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.prefs.Preferences;

import static slash.navigation.converter.gui.profileview.ProfileMode.Elevation;

/**
 * A model for {@link ProfileMode}.
 *
 * @author Christian Pesch
 */

public class ProfileModeModel {
    private static final Preferences preferences = Preferences.userNodeForPackage(UnitSystemModel.class);
    private static final String PROFILE_MODE_PREFERENCE = "profileMode";

    private EventListenerList listenerList = new EventListenerList();

    public ProfileMode getProfileMode() {
        try {
            return ProfileMode.valueOf(preferences.get(PROFILE_MODE_PREFERENCE, Elevation.toString()));
        } catch (IllegalArgumentException e) {
            return Elevation;
        }
    }

    public void setProfileMode(ProfileMode profileMode) {
        preferences.put(PROFILE_MODE_PREFERENCE, profileMode.toString());
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
