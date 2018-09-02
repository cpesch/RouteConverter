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

package slash.navigation.converter.gui.helpers;

import slash.navigation.gui.Application;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import static java.awt.event.KeyEvent.VK_CONTEXT_MENU;
import static java.awt.event.MouseEvent.BUTTON1_MASK;
import static java.awt.event.MouseEvent.MOUSE_CLICKED;
import static java.awt.event.MouseEvent.MOUSE_PRESSED;
import static java.awt.event.MouseEvent.MOUSE_RELEASED;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * Helps to make popups for tables useable.
 *
 * @author Christian Pesch
 */

public abstract class AbstractTablePopupMenu {
    private final JTable table;
    private final String localName;
    private JPopupMenu popupMenu;
    private MouseEvent lastMouseEvent;

    public AbstractTablePopupMenu(JTable table, String localName) {
        this.table = table;
        this.localName = localName;
    }

    protected abstract JPopupMenu doCreatePopupMenu();

    public JPopupMenu createPopupMenu() {
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
        }, getKeyStroke(VK_CONTEXT_MENU, 0), WHEN_IN_FOCUSED_WINDOW);
        this.popupMenu = doCreatePopupMenu();
        return popupMenu;
    }

    private void showPopup(final MouseEvent e) {
        if (table.getCellEditor() != null)
            table.getCellEditor().cancelCellEditing();

        invokeLater(new Runnable() {
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
            table.dispatchEvent(new MouseEvent((Component) e.getSource(), MOUSE_PRESSED, e.getWhen(),
                    BUTTON1_MASK, e.getX(), e.getY(),
                    e.getClickCount(), false));
            table.dispatchEvent(new MouseEvent((Component) e.getSource(), MOUSE_RELEASED, e.getWhen(),
                    BUTTON1_MASK, e.getX(), e.getY(),
                    e.getClickCount(), false));
            table.dispatchEvent(new MouseEvent((Component) e.getSource(), MOUSE_CLICKED, e.getWhen(),
                    BUTTON1_MASK, e.getX(), e.getY(),
                    e.getClickCount(), false));
        }
    }

    private class MouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            Application.getInstance().getContext().getActionManager().setLocalName(localName);

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

    private class MouseMotionListener extends MouseMotionAdapter {
        public void mouseMoved(MouseEvent e) {
            lastMouseEvent = e;
        }
    }
}
