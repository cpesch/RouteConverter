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

package slash.navigation.googlemaps;

import jakarta.xml.bind.JAXBException;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.MapDescriptor;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.googlemaps.directions.DirectionsResponse;
import slash.navigation.rest.Get;
import slash.navigation.rest.exception.ServiceUnavailableException;
import slash.navigation.routing.BaseRoutingService;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.TravelMode;
import slash.navigation.routing.TravelRestrictions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static slash.navigation.googlemaps.GoogleUtil.unmarshalDirections;
import static slash.navigation.routing.RoutingResult.Validity.Invalid;
import static slash.navigation.routing.RoutingResult.Validity.PointNotFound;
import static slash.navigation.routing.RoutingResult.Validity.Valid;

/**
 * Encapsulates REST access to the Google Directions API Service. Used as
 * last resort, after BRouter and GraphHopper -- registered but not
 * preferred; see RouteConverter#initializeRoutingServices().
 *
 * @author Christian Pesch
 */

public class GoogleRoutingService extends BaseRoutingService {
    private static final Logger log = Logger.getLogger(GoogleRoutingService.class.getName());
    private static final TravelMode CAR = new TravelMode("car");
    private static final TravelMode BIKE = new TravelMode("bike");
    private static final TravelMode FOOT = new TravelMode("foot");
    private static final List<TravelMode> TRAVEL_MODES = asList(BIKE, CAR, FOOT);
    private final GoogleApiClient apiClient = new GoogleApiClient();

    public String getName() {
        return "Google";
    }

    public boolean isInitialized() {
        return true;
    }

    public boolean isDownload() {
        return false;
    }

    public List<TravelMode> getAvailableTravelModes() {
        return TRAVEL_MODES;
    }

    public TravelRestrictions getAvailableTravelRestrictions() {
        // Google Directions API can avoid tolls, highways and ferries, but not
        // bridges or tunnels specifically
        return new TravelRestrictions(false, true, true, true, false);
    }

    public TravelMode getPreferredTravelMode() {
        return CAR;
    }

    public String getPath() {
        throw new UnsupportedOperationException();
    }

    public void setPath(String path) {
        throw new UnsupportedOperationException();
    }

    private String getDirectionsUrl(NavigationPosition from, NavigationPosition to,
                                     TravelMode travelMode, TravelRestrictions travelRestrictions) {
        StringBuilder payload = new StringBuilder();
        payload.append("origin=").append(from.getLatitude()).append(',').append(from.getLongitude());
        payload.append("&destination=").append(to.getLatitude()).append(',').append(to.getLongitude());
        payload.append("&mode=").append(toGoogleTravelMode(travelMode));
        String avoid = toAvoidParam(travelRestrictions);
        if (!avoid.isEmpty())
            payload.append("&avoid=").append(avoid);
        return apiClient.getGoogleApiUrl("directions", payload.toString());
    }

    private String toGoogleTravelMode(TravelMode travelMode) {
        if (travelMode == null)
            return "driving";
        if (travelMode.name().equals(BIKE.name()))
            return "bicycling";
        if (travelMode.name().equals(FOOT.name()))
            return "walking";
        return "driving";
    }

    private String toAvoidParam(TravelRestrictions travelRestrictions) {
        if (travelRestrictions == null)
            return "";
        List<String> avoid = new ArrayList<>();
        if (travelRestrictions.avoidTolls())
            avoid.add("tolls");
        if (travelRestrictions.avoidMotorways())
            avoid.add("highways");
        if (travelRestrictions.avoidFerries())
            avoid.add("ferries");
        return String.join(",", avoid);
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to,
                                          TravelMode travelMode, TravelRestrictions travelRestrictions) {
        String url = getDirectionsUrl(from, to, travelMode, travelRestrictions);
        Get get = apiClient.get(url);
        log.info("Getting route between " + from + " and " + to);
        try {
            fireRouting(0);
            String result = get.executeAsString();
            if (get.isSuccessful()) {
                DirectionsResponse response = unmarshalDirections(result);
                if (response != null) {
                    String status = response.getStatus();
                    if (status.equals("NOT_FOUND") || status.equals("ZERO_RESULTS"))
                        return new RoutingResult(null, null, PointNotFound);
                    apiClient.checkForError(getClass().getSimpleName(), url, status);
                    return extractRouteResult(response);
                }
            }
        } catch (IOException | JAXBException e) {
            log.severe("Error getting route between " + from + " and " + to + ": " + e);
        }
        return new RoutingResult(null, null, Invalid);
    }

    private RoutingResult extractRouteResult(DirectionsResponse response) {
        List<NavigationPosition> positions = new ArrayList<>();
        double distance = 0.0;
        long durationSeconds = 0;
        for (DirectionsResponse.Route route : response.getRoute()) {
            for (DirectionsResponse.Route.Leg leg : route.getLeg()) {
                if (leg.getDistance() != null && leg.getDistance().getValue() != null)
                    distance += leg.getDistance().getValue().doubleValue();
                if (leg.getDuration() != null && leg.getDuration().getValue() != null)
                    durationSeconds += leg.getDuration().getValue().longValue();
                for (DirectionsResponse.Route.Leg.Step step : leg.getStep()) {
                    if (positions.isEmpty())
                        positions.add(toPosition(step.getStartLocation()));
                    positions.add(toPosition(step.getEndLocation()));
                }
            }
        }
        if (positions.isEmpty())
            return new RoutingResult(null, null, PointNotFound);
        return new RoutingResult(positions, new DistanceAndTime(distance, durationSeconds * 1000L), Valid);
    }

    private NavigationPosition toPosition(DirectionsResponse.Route.Leg.Step.Location location) {
        return new SimpleNavigationPosition(location.getLng().doubleValue(), location.getLat().doubleValue());
    }

    public NavigationPosition getSnapToRoadPosition(NavigationPosition position) {
        return null;
    }

    public DownloadFuture downloadRoutingDataFor(String mapIdentifier, List<LongitudeAndLatitude> longitudeAndLatitudes) {
        throw new UnsupportedOperationException();
    }

    public long calculateRemainingDownloadSize(List<MapDescriptor> mapDescriptors) {
        throw new UnsupportedOperationException();
    }

    public void downloadRoutingData(List<MapDescriptor> mapDescriptors) {
        throw new UnsupportedOperationException();
    }

    public Map<BoundingBox, Boolean> getCoverageTiles(BoundingBox area) {
        return Collections.emptyMap();
    }
}
