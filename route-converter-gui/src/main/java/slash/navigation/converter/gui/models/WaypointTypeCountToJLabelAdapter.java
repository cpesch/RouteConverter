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

import slash.navigation.base.BaseRoute;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.util.List;
import java.util.Set;

import static java.util.Set.of;
import static slash.navigation.base.WaypointType.Parking;
import static slash.navigation.base.WaypointType.PointOfInterestC;
import static slash.navigation.base.WaypointType.PointOfInterestD;

/**
 * A bidirectional adapter that extracts the Parking and Point Of Interest
 * waypoint counts of a {@link PositionsModel} for display.
 *
 * @author Christian Pesch
 */

public class WaypointTypeCountToJLabelAdapter extends PositionsModelToDocumentAdapter {
    private static final Set<WaypointType> PARKING_TYPES = of(Parking);
    private static final Set<WaypointType> POI_TYPES = of(PointOfInterestC, PointOfInterestD);

    private final JLabel labelParkingCount;
    private final JLabel labelPoiCount;

    public WaypointTypeCountToJLabelAdapter(PositionsModel positionsModel,
                                             JLabel labelParkingCount, JLabel labelPoiCount) {
        super(positionsModel);
        this.labelParkingCount = labelParkingCount;
        this.labelPoiCount = labelPoiCount;
        initialize();
    }

    private void initialize() {
        updateAdapterFromDelegate(new TableModelEvent(getDelegate()));
    }

    protected String getDelegateValue() {
        throw new UnsupportedOperationException();
    }

    public static int countWaypointTypes(List<? extends NavigationPosition> positions, Set<WaypointType> types) {
        int count = 0;
        for (NavigationPosition position : positions) {
            if (!(position instanceof Wgs84Position wgs84Position))
                continue;
            WaypointType waypointType = wgs84Position.getWaypointType();
            if (waypointType != null && types.contains(waypointType))
                count++;
        }
        return count;
    }

    private void updateLabels(int parkingCount, int poiCount) {
        labelParkingCount.setText(Integer.toString(parkingCount));
        labelPoiCount.setText(Integer.toString(poiCount));
    }

    protected void updateAdapterFromDelegate(TableModelEvent e) {
        @SuppressWarnings("rawtypes")
        BaseRoute route = getDelegate().getRoute();
        if (route != null) {
            @SuppressWarnings("unchecked")
            List<NavigationPosition> positions = route.getPositions();
            updateLabels(countWaypointTypes(positions, PARKING_TYPES), countWaypointTypes(positions, POI_TYPES));
        } else {
            updateLabels(0, 0);
        }
    }
}
