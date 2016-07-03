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

import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static slash.navigation.common.Bearing.calculateBearing;

/**
 * A routing service that does no routing, i.e. returns beelines.
 *
 * @author Christian Pesch
 */

public class BeelineService implements RoutingService {
    private static final TravelMode BEELINE = new TravelMode("Beeline");

    public String getName() {
        return "Beeline";
    }

    public boolean isInitialized() {
        return true;
    }

    public boolean isDownload() {
        return false;
    }

    public boolean isSupportTurnpoints() {
        return false;
    }

    public boolean isSupportAvoidFerries() {
        return false;
    }

    public boolean isSupportAvoidHighways() {
        return false;
    }

    public boolean isSupportAvoidTolls() {
        return false;
    }

    public List<TravelMode> getAvailableTravelModes() {
        return singletonList(BEELINE);
    }

    public TravelMode getPreferredTravelMode() {
        return BEELINE;
    }

    public String getPath() {
        throw new UnsupportedOperationException();
    }

    public void setPath(String path) {
        throw new UnsupportedOperationException();
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode) {
        return new RoutingResult(asList(from, to), calculateBearing(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude()).getDistance(), 0L, false);
    }

    public DownloadFuture downloadRoutingDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        throw new UnsupportedOperationException();
    }

    public long calculateRemainingDownloadSize(List<BoundingBox> boundingBoxes) {
        throw new UnsupportedOperationException();
    }

    public void downloadRoutingData(List<BoundingBox> boundingBoxes) {
        throw new UnsupportedOperationException();
    }
}
