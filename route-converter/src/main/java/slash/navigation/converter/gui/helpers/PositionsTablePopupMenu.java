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

import slash.navigation.converter.gui.panels.ConvertPanel;

import javax.swing.*;

import static slash.navigation.converter.gui.models.LocalActionConstants.POSITIONS;
import static slash.navigation.gui.helpers.JMenuHelper.createItem;
import static slash.navigation.gui.helpers.JMenuHelper.createMenu;

/**
 * Creates a {@link JPopupMenu} for the positions table of the {@link ConvertPanel}.
 *
 * @author Christian Pesch
 */

public class PositionsTablePopupMenu extends AbstractTablePopupMenu {

    public PositionsTablePopupMenu(JTable table) {
        super(table, POSITIONS);
    }

    protected JPopupMenu doCreatePopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createItem("cut"));
        menu.add(createItem("copy"));
        menu.add(createItem("paste"));
        menu.addSeparator();
        menu.add(createItem("find-place"));
        JMenu completeMenu = createMenu("complete");
        completeMenu.add(createItem("add-coordinates"));
        completeMenu.add(createItem("add-elevation"));
        completeMenu.add(createItem("add-address"));
        completeMenu.add(createItem("add-speed"));
        completeMenu.add(createItem("add-time"));
        completeMenu.add(createItem("add-number"));
        menu.add(completeMenu);
        menu.addSeparator();
        menu.add(createItem("split-positionlist"));
        menu.add(createMenu("merge-positionlist"));
        menu.add(createItem("import-positionlist"));
        menu.add(createItem("export-positionlist"));
        return menu;
    }
}
