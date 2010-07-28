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
import slash.navigation.converter.gui.models.*;

/**
 * Creates a {@link JPopupMenu} for a {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

public class TablePopupMenu extends AbstractTablePopupMenu {

    public TablePopupMenu(JTable table) {
        super(table);
    }

    protected JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        menu.add(JMenuHelper.createItem("cut"));
        menu.add(JMenuHelper.createItem("copy"));
        menu.add(JMenuHelper.createItem("paste"));
        menu.add(JMenuHelper.createItem("select-all"));
        menu.addSeparator();
        menu.add(JMenuHelper.createItem("new-position"));
        menu.add(JMenuHelper.createItem("delete"));
        menu.addSeparator();
        menu.add(JMenuHelper.createItem("find-place"));
        JMenu completeMenu = JMenuHelper.createMenu("complete");
        completeMenu.add(JMenuHelper.createItem("add-coordinates"));
        completeMenu.add(JMenuHelper.createItem("add-elevation"));
        completeMenu.add(JMenuHelper.createItem("add-postal-address"));
        completeMenu.add(JMenuHelper.createItem("add-populated-place"));
        completeMenu.add(JMenuHelper.createItem("add-speed"));
        completeMenu.add(JMenuHelper.createItem("add-index"));
        menu.add(completeMenu);
        menu.addSeparator();
        menu.add(JMenuHelper.createItem("split-positionlist"));
        menu.add(JMenuHelper.createMenu("merge-positionlist"));
        menu.add(JMenuHelper.createItem("import-positionlist"));

        return menu;
    }
}
