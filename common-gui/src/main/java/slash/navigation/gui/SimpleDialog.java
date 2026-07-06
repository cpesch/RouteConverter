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
import java.util.prefs.Preferences;

import static java.util.prefs.Preferences.userNodeForPackage;

/**
 * The base of all simple {@link JDialog}s.
 *
 * @author Christian Pesch
 */

public abstract class SimpleDialog extends JDialog {
    private final Preferences preferences = userNodeForPackage(getClass());
    private WindowBounds windowBounds;

    public SimpleDialog(Window owner, String name) {
        super(owner);
        setName(name);
    }

    public void showWithPreferences() {
        pack();
        windowBounds = new WindowBounds(this, preferences, getName() + "-");
        windowBounds.restore(getOwner());
        windowBounds.installLivePersistence();
        setVisible(true);
    }

    private boolean disposed = false;

    public void dispose() {
        if (windowBounds != null) {
            windowBounds.putLocation();
            windowBounds.putSize();
        }
        super.dispose();
        disposed = true;
    }

    public void setVisible(boolean visible) {
        if(disposed) return;
        super.setVisible(visible);
    }
}
