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
package slash.navigation.converter.gui.models;

import slash.navigation.converter.gui.renderer.PositionsTableHeaderRenderer;
import slash.navigation.converter.gui.renderer.RoutesTableCellRenderer;
import slash.navigation.routes.impl.RouteModel;

import javax.swing.table.TableColumnModel;

import static slash.navigation.converter.gui.models.LocalActionConstants.ROUTES;
import static slash.navigation.gui.helpers.UIHelper.getMaxWidth;
import static slash.navigation.routes.impl.RoutesTableModel.*;

/**
 * Acts as a {@link TableColumnModel} for the {@link RouteModel}s of the browse panel.
 *
 * @author Christian Pesch
 */

public class RoutesTableColumnModel extends AbstractTableColumnModel {
    public RoutesTableColumnModel(RouteMetadataSource routeMetadataSource) {
        super(ROUTES, null);

        PositionsTableHeaderRenderer headerRenderer = new PositionsTableHeaderRenderer();
        RoutesTableCellRenderer cellRenderer = new RoutesTableCellRenderer(routeMetadataSource);
        predefineColumn(NAME_COLUMN, "description", null, true, cellRenderer, headerRenderer);
        predefineColumn(CREATOR_COLUMN, "creator", 80, true, cellRenderer, headerRenderer);
        predefineColumn(LENGTH_COLUMN, "route-length", getMaxWidth("12345 Km", 7), true, cellRenderer, headerRenderer);
        predefineColumn(DURATION_COLUMN, "route-duration", getMaxWidth("999:59:59", 7), true, cellRenderer, headerRenderer);
        initializeColumns();
    }
}
