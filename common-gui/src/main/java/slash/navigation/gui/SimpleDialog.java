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

/**
 * The base of all simple {@link JDialog}s.
 *
 * @author Christian Pesch
 */

public abstract class SimpleDialog extends JDialog {
    private static final Logger log = Logger.getLogger(SimpleDialog.class.getName());
    private final Preferences preferences = Preferences.userNodeForPackage(getClass());

    public SimpleDialog(Window owner, String name) {
        super(owner);
        setName(name);
    }

    public void restoreLocation() {
        Rectangle bounds = getOwner().getGraphicsConfiguration().getBounds();
        log.info("Screen size is " + bounds);

        int x = SingleFrameApplication.crop(getName() + "-x", preferences.getInt(getName() + "-" + SingleFrameApplication.X_PREFERENCE, -1),
                (int) bounds.getX() - SingleFrameApplication.MAXIMIZE_OFFSET,
                (int) bounds.getX() + (int) bounds.getWidth() + 2 * SingleFrameApplication.MAXIMIZE_OFFSET - getWidth());
        int y = SingleFrameApplication.crop(getName() + "y", preferences.getInt(getName() + "-" + SingleFrameApplication.Y_PREFERENCE, -1),
                (int) bounds.getY() - SingleFrameApplication.MAXIMIZE_OFFSET,
                (int) bounds.getY() + (int) bounds.getHeight() + 2 * SingleFrameApplication.MAXIMIZE_OFFSET - getHeight());
        if (x != -1 && y != -1)
            setLocation(x, y);
        log.info("Dialog " + getName() + " location is " + getLocation());
    }

    public void dispose() {
        log.info("Storing dialog " + getName() + " location as " + getLocation());
        log.info("Storing dialog " + getName() + " size as " + getSize());
        preferences.putInt(getName() + "-" + SingleFrameApplication.X_PREFERENCE, getLocation().x);
        preferences.putInt(getName() + "-" + SingleFrameApplication.Y_PREFERENCE, getLocation().y);
        super.dispose();
    }
}