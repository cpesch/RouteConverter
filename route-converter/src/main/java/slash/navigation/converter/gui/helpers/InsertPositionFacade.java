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
        int[] selectedRows = r.getPositionsView().getSelectedRows();
        r.clearSelection();

        RoutingService routingService = r.getRoutingServiceFacade().getRoutingService();
        if (routingService instanceof GoogleDirections && r.isMapViewInitialized()) {
            ((GoogleDirections) routingService).insertAllWaypoints(selectedRows);
        } else
            insertWithRoutingService(routingService, selectedRows);
    }

    public void insertOnlyTurnpoints() {
        RouteConverter r = RouteConverter.getInstance();
        int[] selectedRows = r.getPositionsView().getSelectedRows();
        r.clearSelection();

        RoutingService routingService = r.getRoutingServiceFacade().getRoutingService();
        if (routingService instanceof GoogleDirections && r.isMapViewInitialized()) {
            ((GoogleDirections) routingService).insertOnlyTurnpoints(selectedRows);
        } else
            throw new UnsupportedOperationException();
    }

    private void insertWithRoutingService(RoutingService routingService, int[] selectedRows) {
        RouteConverter r = RouteConverter.getInstance();
        List<NavigationPosition> selectedPositions = new ArrayList<NavigationPosition>();
        for (int i = 0; i < selectedRows.length; i++)
            selectedPositions.add(r.getPositionsModel().getPosition(i));

        if (routingService.isDownload()) {
            List<LongitudeAndLatitude> lal = new ArrayList<LongitudeAndLatitude>();
            for (NavigationPosition position : selectedPositions) {
                lal.add(asLongitudeAndLatitude(position));
            }
            routingService.downloadRoutingDataFor(lal);
        }

        TravelMode travelMode = r.getRoutingServiceFacade().getTravelMode();
        for (int i = 0; i < selectedPositions.size(); i++) {
            // skip the very last position without successor
            if (i == r.getPositionsModel().getRowCount() - 1 || i == selectedPositions.size() - 1)
                continue;

            RoutingResult result = routingService.getRouteBetween(selectedPositions.get(i), selectedPositions.get(i + 1), travelMode);
            if (result != null) {
                List<BaseNavigationPosition> positions = new ArrayList<BaseNavigationPosition>();
                for (NavigationPosition position : result.getPositions()) {
                    positions.add(r.getPositionsModel().getRoute().createPosition(position.getLongitude(), position.getLatitude(), position.getElevation(), null, null, null));
                }
                int insertRow = r.getPositionsModel().getIndex(selectedPositions.get(i)) + 1;
                r.getPositionsModel().add(insertRow, positions);

                for (int j = 0; j < positions.size(); j++) {
                    int[] rows = new int[]{insertRow + j};
                    r.getBatchPositionAugmenter().addData(rows, false, true, true);
                }
            }
        }
    }

    private LongitudeAndLatitude asLongitudeAndLatitude(NavigationPosition position) {
        return new LongitudeAndLatitude(position.getLongitude(), position.getLatitude());
    }
}
