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
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * A model for {@link Color}.
 *
 * @author Christian Pesch
 */
public class ColorModel {
    private static final Preferences preferences = Preferences.userNodeForPackage(ColorModel.class);
    private static final String COLOR_SUFFIX = "-color";

    private EventListenerList listenerList = new EventListenerList();
    private final String preferencesPrefix;
    private final int preferencesDefault;

    public ColorModel(String preferencesPrefix, int preferencesDefault) {
        this.preferencesPrefix = preferencesPrefix;
        this.preferencesDefault = preferencesDefault;
    }

    private Color fromString(String color) {
        return new Color(Integer.parseInt(color), true);
    }

    private String toString(Color color) {
        return Integer.toString(color.getRGB());
    }

    public Color getColor() {
        try {
            return fromString(preferences.get(preferencesPrefix + COLOR_SUFFIX, Integer.toString(preferencesDefault)));
        } catch (IllegalArgumentException e) {
            return new Color(preferencesDefault);
        }
    }

    public void setColor(Color color) {
        preferences.put(preferencesPrefix + COLOR_SUFFIX, toString(color));
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
