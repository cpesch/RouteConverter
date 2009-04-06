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
import slash.navigation.converter.gui.actions.*;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.util.RouteComments;
import slash.navigation.BaseRoute;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Creates a {@link JPopupMenu} for the {@link JTable} of the {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

public class TablePopupMenu {
    private RouteConverter routeConverter;
    private JPopupMenu popupMenu = new JPopupMenu();
    private PositionAugmenter augmenter;

    public TablePopupMenu(RouteConverter routeConverter) {
        this.routeConverter = routeConverter;
        augmenter = new PositionAugmenter(routeConverter.getFrame());
        initialize();
    }

    protected void initialize() {
        JMenuItem buttonAddElevation = new JMenuItem(RouteConverter.BUNDLE.getString("add-elevation"));
        buttonAddElevation.addActionListener(new AddElevationsToPositions(routeConverter.getPositionsTable(), routeConverter.getPositionsModel(), augmenter));
        popupMenu.add(buttonAddElevation);

        JMenuItem buttonAddComment = new JMenuItem(RouteConverter.BUNDLE.getString("add-comment"));
        buttonAddComment.addActionListener(new AddCommentsToPositions(routeConverter.getPositionsTable(), routeConverter.getPositionsModel(), augmenter));
        popupMenu.add(buttonAddComment);

        JMenuItem buttonAddSpeed = new JMenuItem(RouteConverter.BUNDLE.getString("add-speed"));
        buttonAddSpeed.addActionListener(new AddSpeedsToPositions(routeConverter.getPositionsTable(), routeConverter.getPositionsModel(), augmenter));
        popupMenu.add(buttonAddSpeed);

        popupMenu.addSeparator();

        JMenuItem buttonSplitPositionlist = new JMenuItem(new SplitPositionList(routeConverter.getFrame(), routeConverter.getPositionsTable(), routeConverter.getFormatComboBox(), routeConverter.getPositionsModel(), routeConverter.getFormatAndRoutesModel()));
        buttonSplitPositionlist.setText(RouteConverter.BUNDLE.getString("split-positionlist"));
        popupMenu.add(buttonSplitPositionlist);

        final JMenu menuMergePositionlist = new JMenu(RouteConverter.BUNDLE.getString("merge-positionlist"));
        popupMenu.add(menuMergePositionlist);

        routeConverter.getFormatAndRoutesModel().addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    BaseRoute route = routeConverter.getFormatAndRoutesModel().getRoute(i);
                    JMenuItem menuItem = new JMenuItem();
                    menuItem.setAction(new MergePositionList(routeConverter.getFrame(), routeConverter.getPositionsTable(), routeConverter.getPositionListComboBox(), route, routeConverter.getPositionsModel(), routeConverter.getFormatAndRoutesModel()));
                    menuItem.setText(RouteComments.shortenRouteName(route));
                    menuMergePositionlist.add(menuItem, i);
                }
            }

            public void intervalRemoved(ListDataEvent e) {
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    if (i >= 0 && i < menuMergePositionlist.getMenuComponentCount())
                        menuMergePositionlist.remove(i);
                }
            }

            public void contentsChanged(ListDataEvent e) {
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    if (i >= 0 && i < menuMergePositionlist.getMenuComponentCount()) {
                        BaseRoute route = routeConverter.getFormatAndRoutesModel().getRoute(i);
                        JMenuItem menuItem = (JMenuItem) menuMergePositionlist.getMenuComponent(i);
                        menuItem.setText(RouteComments.shortenRouteName(route));
                    }
                }
            }
        });

        // cannot use table.setComponentPopupMenu(popupMenu); since it does ensure a selection
        routeConverter.getPositionsTable().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                ensureSelection(routeConverter.getPositionsTable(), e);
                showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                ensureSelection(routeConverter.getPositionsTable(), e);
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

