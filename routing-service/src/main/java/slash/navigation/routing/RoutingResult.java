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

package slash.navigation.routing;

import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.NavigationPosition;

import java.util.List;

/**
 * The result for the routing between two positions.
 *
 * @author Christian Pesch
 */

public class RoutingResult   {
    private final List<NavigationPosition> positions;
    private final DistanceAndTime distanceAndTime;
    private final Validity validity;

    public RoutingResult(List<NavigationPosition> positions, DistanceAndTime distanceAndTime, Validity validity) {
        this.positions = positions;
        this.distanceAndTime = distanceAndTime;
        this.validity = validity;
    }

    public List<NavigationPosition> getPositions() {
        return positions;
    }

    public DistanceAndTime getDistanceAndTime() {
        return distanceAndTime;
    }

    public Validity getValidity() {
        return validity;
    }

    public enum Validity { Valid, Invalid, PointNotFound }
}
