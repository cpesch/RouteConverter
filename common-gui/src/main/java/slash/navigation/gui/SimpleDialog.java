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

package slash.navigation.gui;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.Integer.MAX_VALUE;
import static java.util.logging.Logger.getLogger;
import static java.util.prefs.Preferences.userNodeForPackage;
import static slash.navigation.gui.SingleFrameApplication.*;

/**
 * The base of all simple {@link JDialog}s.
 *
 * @author Christian Pesch
 */

public abstract class SimpleDialog extends JDialog {
    private static final Logger log = getLogger(SimpleDialog.class.getName());
    private final Preferences preferences = userNodeForPackage(getClass());

    public SimpleDialog(Window owner, String name) {
        super(owner);
        setName(name);
    }

    public void restoreLocation() {
        Rectangle bounds = getOwner().getGraphicsConfiguration().getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(getOwner().getGraphicsConfiguration());

        int width = crop(getName() + "width", getPreferenceWidth(),
                (int) bounds.getX() - (insets.left + insets.right),
                (int) bounds.getWidth() - (insets.left + insets.right));
        int height = crop(getName() + "height", getPreferenceHeight(),
                (int) bounds.getY() - (insets.top + insets.bottom),
                (int) bounds.getHeight() - (insets.top + insets.bottom));
        if (width > 120 && height > 60)
            setSize(width, height);
        log.info("Dialog " + getName() + " size is " + getSize());

        int x = crop(getName() + "x", getPreferencesX(),
                (int) bounds.getX() + insets.left,
                (int) bounds.getX() + insets.left + (int) bounds.getWidth() - insets.right - width);
        int y = crop(getName() + "y", getPreferencesY(),
                (int) bounds.getY() + insets.top,
                (int) bounds.getY() + insets.top + (int) bounds.getHeight() - insets.bottom - height);
        if (x != -1 && y != -1)
            setLocation(x, y);
        else
            setLocationRelativeTo(getOwner());
        log.info("Dialog " + getName() + " location is " + getLocation());
    }

    private int getPreferencesX() {
        return preferences.getInt(getName() + "-" + X_PREFERENCE, -1);
    }

    private int getPreferencesY() {
        return preferences.getInt(getName() + "-" + Y_PREFERENCE, -1);
    }

    private int getPreferenceHeight() {
        return crop("preferencesHeight", preferences.getInt(getName() + "-" + HEIGHT_PREFERENCE, -1), 0, MAX_VALUE);
    }

    private int getPreferenceWidth() {
        return crop("preferenceWidth", preferences.getInt(getName() + "-" + WIDTH_PREFERENCE, -1), 0, MAX_VALUE);
    }

    private void putPreferencesLocation() {
        int x = getLocation().x;
        int y = getLocation().y;
        if(getPreferencesX() == x && getPreferencesY() == y)
            return;

        preferences.putInt(getName() + "-" + X_PREFERENCE, x);
        preferences.putInt(getName() + "-" + Y_PREFERENCE, y);
        log.fine("Storing dialog " + getName() + " location as " + getLocation());
    }

    private void putPreferencesSize() {
        int width = getSize().width;
        int height = getSize().height;
        if(getPreferenceWidth() == width && getPreferenceHeight() == height)
            return;

        preferences.putInt(getName() + "-" + WIDTH_PREFERENCE, width);
        preferences.putInt(getName() + "-" + HEIGHT_PREFERENCE, height);
        log.fine("Storing dialog " + getName() + " size as " + getSize());
    }

    public void dispose() {
        putPreferencesLocation();
        putPreferencesSize();
        super.dispose();
    }
}