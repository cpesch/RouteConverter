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

package slash.navigation.converter.gui.helper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Helps to make popups for tables useable.
 *
 * @author Christian Pesch
 */

public abstract class TablePopupMenu {
    private final JTable table;
    private final JPopupMenu popupMenu;

    public TablePopupMenu(JTable table) {
        this.table = table;
        this.popupMenu = createPopupMenu();
        initialize();
    }

    protected abstract JPopupMenu createPopupMenu();

    protected void initialize() {
        // cannot use tablePositions.setComponentPopupMenu(popupMenu); since it does ensure a selection
        table.addMouseListener(new MouseListener());
        table.getParent().addMouseListener(new MouseListener());
    }

    private void showPopup(final MouseEvent e) {
        if (e.isPopupTrigger()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            });
        }
    }

    private void ensureSelection(MouseEvent e) {
        if (e.isPopupTrigger()) {
            if (!table.hasFocus())
                table.requestFocus();
            if (table.getSelectedRowCount() < 2) {
                // dispatch event again as a left mouse click for selections
                // (do not try to spare one of the three events)
                table.dispatchEvent(new MouseEvent((Component) e.getSource(), MouseEvent.MOUSE_PRESSED, e.getWhen(),
                        InputEvent.BUTTON1_MASK, e.getX(), e.getY(),
                        e.getClickCount(), false));
                table.dispatchEvent(new MouseEvent((Component) e.getSource(), MouseEvent.MOUSE_RELEASED, e.getWhen(),
                        InputEvent.BUTTON1_MASK, e.getX(), e.getY(),
                        e.getClickCount(), false));
                table.dispatchEvent(new MouseEvent((Component) e.getSource(), MouseEvent.MOUSE_CLICKED, e.getWhen(),
                        InputEvent.BUTTON1_MASK, e.getX(), e.getY(),
                        e.getClickCount(), false));
            }
        }
    }

    private class MouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            ensureSelection(e);
            showPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            ensureSelection(e);
            showPopup(e);
        }
    }
}
