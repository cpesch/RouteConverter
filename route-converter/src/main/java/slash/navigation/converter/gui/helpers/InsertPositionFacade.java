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

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Helps to insert positions.
 *
 * @author Christian Pesch
 */

public class InsertPositionFacade {
    public void insertAllWaypoints() {
        RouteConverter r = RouteConverter.getInstance();
        int[] selectedRows = r.getConvertPanel().getPositionsView().getSelectedRows();
        r.clearSelection();

        RoutingService service = r.getRoutingServiceFacade().getRoutingService();
        if (service instanceof GoogleDirectionsService) {
            ((GoogleDirectionsService)service).insertAllWaypoints(selectedRows);
        } else
            insertWithRoutingService(service, selectedRows);
    }

    public void insertOnlyTurnpoints() {
        RouteConverter r = RouteConverter.getInstance();
        int[] selectedRows = r.getConvertPanel().getPositionsView().getSelectedRows();
        r.clearSelection();

        RoutingService service = r.getRoutingServiceFacade().getRoutingService();
        if (service instanceof GoogleDirectionsService) {
            ((GoogleDirectionsService)service).insertOnlyTurnpoints(selectedRows);
        } else
            throw new UnsupportedOperationException();
    }

    private void insertWithRoutingService(RoutingService routingService, int[] selectedRows) {
        RouteConverter r = RouteConverter.getInstance();
        PositionsModel positionsModel = r.getConvertPanel().getPositionsModel();

        List<NavigationPosition> selectedPositions = new ArrayList<>();
        for (int i = 0; i < selectedRows.length; i++)
            selectedPositions.add(positionsModel.getPosition(i));

        if (routingService.isDownload()) {
            List<LongitudeAndLatitude> lal = new ArrayList<>();
            for (NavigationPosition position : selectedPositions) {
                lal.add(new LongitudeAndLatitude(position.getLongitude(), position.getLatitude()));
            }
            routingService.downloadRoutingDataFor(lal);
        }

        TravelMode travelMode = r.getRoutingServiceFacade().getTravelMode();
        for (int i = 0; i < selectedPositions.size(); i++) {
            // skip the very last position without successor
            if (i == positionsModel.getRowCount() - 1 || i == selectedPositions.size() - 1)
                continue;

            RoutingResult result = routingService.getRouteBetween(selectedPositions.get(i), selectedPositions.get(i + 1), travelMode);
            if (result.isValid()) {
                List<BaseNavigationPosition> positions = new ArrayList<>();
                for (NavigationPosition position : result.getPositions()) {
                    positions.add(r.getConvertPanel().getPositionsModel().getRoute().createPosition(position.getLongitude(), position.getLatitude(), position.getElevation(), null, null, null));
                }
                int insertRow = r.getConvertPanel().getPositionsModel().getIndex(selectedPositions.get(i)) + 1;
                r.getConvertPanel().getPositionsModel().add(insertRow, positions);

                for (int j = 0; j < positions.size(); j++) {
                    int[] rows = new int[]{insertRow + j};
                    r.getPositionAugmenter().addData(rows, false, true, true, false, false);
                }
            }
        }
    }
}
