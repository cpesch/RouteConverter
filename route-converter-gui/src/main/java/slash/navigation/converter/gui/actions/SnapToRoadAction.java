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
package slash.navigation.converter.gui.actions;

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.helpers.RoutingServiceFacade;
import slash.navigation.converter.gui.models.PositionColumnValues;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.panels.ConvertPanel;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.routing.RoutingService;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static slash.navigation.converter.gui.models.PositionColumns.LATITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LONGITUDE_COLUMN_INDEX;
import static slash.navigation.gui.events.Range.revert;

/**
 * {@link Action} that snaps a {@link BaseNavigationPosition} to the next road.
 *
 * @author Christian Pesch
 */

public class SnapToRoadAction extends FrameAction {
    private final JTable table;
    private final PositionsModel positionsModel;
    private final RoutingServiceFacade routingServiceFacade;
    private final ConvertPanel convertPanel;

    public SnapToRoadAction(JTable table, PositionsModel positionsModel, RoutingServiceFacade routingServiceFacade, ConvertPanel convertPanel) {
        this.table = table;
        this.positionsModel = positionsModel;
        this.routingServiceFacade = routingServiceFacade;
        this.convertPanel = convertPanel;
    }

    private void updatePosition(NavigationPosition position, NavigationPosition roadPosition) {
        positionsModel.edit(positionsModel.getIndex(position),
                new PositionColumnValues(asList(LONGITUDE_COLUMN_INDEX, LATITUDE_COLUMN_INDEX),
                        asList(roadPosition.getLongitude(), roadPosition.getLatitude())), true, true);
    }

    public void run() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length > 0) {
            for (final int selectedRow : selectedRows) {
                NavigationPosition position = positionsModel.getPosition(selectedRow);
                NavigationPosition roadPosition = routingServiceFacade.getRoutingService().getSnapToRoadPosition(position);
                if (roadPosition != null) {
                    updatePosition(position, roadPosition);
                }
            }
            convertPanel.selectPositions(selectedRows);
        }
    }
}
