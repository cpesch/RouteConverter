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
        log.info("Screen size is " + bounds);
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(getOwner().getGraphicsConfiguration());
        log.info("Insets are " + insets);

        int x = crop(getName() + "-x", preferences.getInt(getName() + "-" + X_PREFERENCE, -1),
                (int) bounds.getX() + insets.left,
                (int) bounds.getX() + insets.left + (int) bounds.getWidth() - insets.right - getWidth());
        int y = crop(getName() + "-y", preferences.getInt(getName() + "-" + Y_PREFERENCE, -1),
                (int) bounds.getY() + insets.top,
                (int) bounds.getY() + insets.top + (int) bounds.getHeight() - insets.bottom - getHeight());
        if (x != -1 && y != -1)
            setLocation(x, y);
        else
            setLocationRelativeTo(getOwner());
        log.info("Dialog " + getName() + " location is " + getLocation());
    }

    public void dispose() {
        preferences.putInt(getName() + "-" + X_PREFERENCE, getLocation().x);
        preferences.putInt(getName() + "-" + Y_PREFERENCE, getLocation().y);
        log.info("Storing dialog " + getName() + " location as " + getLocation());
        super.dispose();
    }
}