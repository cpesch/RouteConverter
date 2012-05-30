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

import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Helps to make popups for tables useable.
 *
 * @author Christian Pesch
 */

public abstract class AbstractTablePopupMenu {
    private final JTable table;
    private JPopupMenu popupMenu;

    public AbstractTablePopupMenu(JTable table) {
        this.table = table;
    }

    protected abstract JPopupMenu createPopupMenu();

    public JPopupMenu createMenu() {
        // cannot use table.setComponentPopupMenu(popupMenu); since it does ensure a selection
        MouseListener mouseListener = new MouseListener();
        table.addMouseListener(mouseListener);
        table.getParent().addMouseListener(mouseListener);
        table.getParent().addMouseMotionListener(new MouseMotionListener());
        table.registerKeyboardAction(new FrameAction() {
            public void run() {
                ensureSelection(lastMouseEvent, 1);
                showPopup(lastMouseEvent);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.popupMenu = createPopupMenu();
        return popupMenu;
    }

    private void showPopup(final MouseEvent e) {
        if (table.getCellEditor() != null)
            table.getCellEditor().cancelCellEditing();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private void ensureSelection(MouseEvent e, int selectedRowCountMinimum) {
        if (!table.hasFocus())
            table.requestFocus();
        if (table.getSelectedRowCount() < selectedRowCountMinimum) {
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

    private class MouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                ensureSelection(e, 2);
                showPopup(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                ensureSelection(e, 2);
                showPopup(e);
            }
        }
    }

    private MouseEvent lastMouseEvent;

    private class MouseMotionListener extends MouseMotionAdapter {
        public void mouseMoved(MouseEvent e) {
            lastMouseEvent = e;
        }
    }
}
