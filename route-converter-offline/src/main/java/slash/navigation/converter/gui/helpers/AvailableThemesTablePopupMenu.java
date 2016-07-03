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

import slash.navigation.maps.impl.LocalThemesTableModel;

import javax.swing.*;

import static slash.navigation.converter.gui.models.LocalNames.THEMES;
import static slash.navigation.gui.helpers.JMenuHelper.createItem;

/**
 * Creates a {@link JPopupMenu} for a {@link LocalThemesTableModel}.
 *
 * @author Christian Pesch
 */

public class AvailableThemesTablePopupMenu extends AbstractTablePopupMenu {
    public AvailableThemesTablePopupMenu(JTable table) {
        super(table, THEMES);
    }

    protected JPopupMenu doCreatePopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createItem("apply-theme"));
        return menu;
    }
}
