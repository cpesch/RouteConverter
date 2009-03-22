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

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

/**
 * Creates a {@link JPopupMenu} for the {@link JTable} of the {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

public class TablePopupMenu {
    private JPopupMenu popupMenu = new JPopupMenu();
    private PositionAugmenter augmenter;

    public TablePopupMenu(JFrame frame, final JTable table, final PositionsModel positionsModel) {
        augmenter = new PositionAugmenter(frame);

        JMenuItem buttonAddElevation = new JMenuItem(RouteConverter.BUNDLE.getString("add-elevation-popup"));
        buttonAddElevation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int[] selectedRows = table.getSelectedRows();
                if (selectedRows.length > 0) {
                    augmenter.addElevations(positionsModel, selectedRows);
                }
            }
        });
        popupMenu.add(buttonAddElevation);

        JMenuItem buttonAddComment = new JMenuItem(RouteConverter.BUNDLE.getString("add-comment-popup"));
        buttonAddComment.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int[] selectedRows = table.getSelectedRows();
                if (selectedRows.length > 0) {
                    augmenter.addComments(positionsModel, selectedRows);
                }
            }
        });
        popupMenu.add(buttonAddComment);

        JMenuItem buttonAddSpeed = new JMenuItem(RouteConverter.BUNDLE.getString("add-speed-popup"));
        buttonAddSpeed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int[] selectedRows = table.getSelectedRows();
                if (selectedRows.length > 0) {
                    augmenter.addSpeeds(positionsModel, selectedRows);
                }
            }
        });
        popupMenu.add(buttonAddSpeed);

        // cannot use table.setComponentPopupMenu(popupMenu); since it does ensure a selection
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                ensureSelection(table, e);
                showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                ensureSelection(table, e);
                showPopup(e);
            }
        });
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
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

    private void ensureSelection(JTable table, MouseEvent e) {
        if (e.isPopupTrigger()) {
            if (!table.hasFocus())
                table.requestFocus();
            if (table.getSelectedRowCount() == 0) {
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
}

