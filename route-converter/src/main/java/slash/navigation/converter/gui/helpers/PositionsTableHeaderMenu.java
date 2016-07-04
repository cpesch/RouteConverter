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

import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsTableColumnModel;
import slash.navigation.gui.actions.ActionManager;

import javax.swing.*;
import javax.swing.table.JTableHeader;

import static slash.navigation.converter.gui.models.LocalActionConstants.POSITIONS;
import static slash.navigation.gui.helpers.JMenuHelper.findMenu;

/**
 * Creates a {@link JMenu} and a {@link JPopupMenu} for a {@link PositionsTableColumnModel}.
 *
 * @author Christian Pesch
 */

public class PositionsTableHeaderMenu extends AbstractTableHeaderMenu {
    public PositionsTableHeaderMenu(JTableHeader tableHeader, JMenuBar menuBar, PositionsModel positionsModel,
                                    PositionsTableColumnModel columnModel, ActionManager actionManager) {
        super(columnModel, actionManager, POSITIONS);
        initializeShowColumn(findMenu(menuBar, "view", "show-column"));
        initializeSortPositions(findMenu(menuBar, "positionlist", "sort-positions"), positionsModel);
        initializePopup(tableHeader);
    }
}
