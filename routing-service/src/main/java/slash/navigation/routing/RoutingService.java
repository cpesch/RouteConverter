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

/**
 * Interface for a service that determines the routing between two given positions.
 *
 * @author Christian Pesch
 */

public interface RoutingService {
    String getName();
    boolean isInitialized();
    boolean isDownload();
    boolean isSupportTurnpoints();
    boolean isSupportAvoidFerries();
    boolean isSupportAvoidHighways();
    boolean isSupportAvoidTolls();
    List<TravelMode> getAvailableTravelModes();
    TravelMode getPreferredTravelMode();
    String getPath();
    void setPath(String path);

    RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode);

    DownloadFuture downloadRoutingDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes);
    long calculateRemainingDownloadSize(List<BoundingBox> boundingBoxes);
    void downloadRoutingData(List<BoundingBox> boundingBoxes);
}
