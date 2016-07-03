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
package slash.navigation.mapview.mapsforge.helpers;

import slash.navigation.gui.Application;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.mapview.mapsforge.AwtGraphicMapView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;
import static slash.navigation.mapview.mapsforge.models.LocalNames.MAP;

/**
 * Opens a {@link JPopupMenu} upon mouse clicks of the {@link AwtGraphicMapView}.
 *
 * @author Christian Pesch
 */

public class MapViewPopupMenu extends MouseAdapter {
    private final Component component;
    private final JPopupMenu popupMenu;

    public MapViewPopupMenu(Component component, JPopupMenu popupMenu) {
        this.component = component;
        this.popupMenu = popupMenu;
        component.addMouseListener(this);
    }

    public void mousePressed(MouseEvent e) {
        if (isLeftMouseButton(e)) {
            ActionManager actionManager = Application.getInstance().getContext().getActionManager();
            actionManager.setLocalName(MAP);

            boolean shiftKey = e.isShiftDown();
            boolean altKey = e.isAltDown();
            boolean ctrlKey = e.isControlDown();
            if (!shiftKey && !altKey && !ctrlKey)
                actionManager.run("select-position");
            else if (shiftKey && !altKey && !ctrlKey)
                actionManager.run("extend-selection");
            else if (!shiftKey && !altKey && ctrlKey)
                actionManager.run("add-position");
            else if (!shiftKey && altKey && ctrlKey)
                actionManager.run("delete-position-from-map");

        } else if (isRightMouseButton(e)) {
            popupMenu.show(component, e.getX(), e.getY());
        }
    }
}
