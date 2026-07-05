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

package slash.navigation.converter.cmdline;

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.common.NavigationPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static slash.navigation.base.RouteCharacteristics.Route;

/**
 * BRouter-backed {@link RouteLengthComputer} for the {@code analyze} command
 * (specs/00055 P2b). Route-characteristic position lists (planned routes) are
 * routed on-road with BRouter and reported as {@code routed}; every other list
 * (recorded tracks, loose waypoints) is delegated to the point-to-point
 * {@link PointToPointLengthComputer}.
 * <p>
 * A Route list is routed in a single call over all of its coordinates, and the
 * total on-road distance is reported. If the route cannot be routed (no segment
 * coverage, routing error, timeout, missing profile) the whole list falls back
 * to the straight-line {@code beeline} length, so the label never over-promises
 * and a partially-covered route is never reported as a mix. Routing failures
 * never propagate: {@link #computeLength} never throws for a routing problem, so
 * the analyzer always emits its JSON.
 * <p>
 * The actual routing is behind the {@link RouteRouter} seam. The production seam
 * is {@link BRouterRouteRouter}, which drives BRouter's
 * {@code RoutingContext}/{@code RoutingEngine} over the whole waypoint list like
 * {@code slash.navigation.brouter.BRouter#getRouteBetween}. Tests inject a fake
 * {@link RouteRouter} to exercise the routed/fallback logic without real
 * {@code .rd5} segments.
 *
 * @author Christian Pesch
 */
public class BRouterRouteLengthComputer implements RouteLengthComputer {
    private static final Logger log = Logger.getLogger(BRouterRouteLengthComputer.class.getName());

    private final RouteLengthComputer fallback = new PointToPointLengthComputer();
    private final RouteRouter routeRouter;

    /**
     * Routes a whole position list in one call.
     */
    interface RouteRouter {
        /**
         * @param longitudes the route's point longitudes, in order
         * @param latitudes  the route's point latitudes, in order (parallel to
         *                   {@code longitudes})
         * @return the total on-road distance in metres through all points, or
         *         {@code null} if the route cannot be routed (no coverage,
         *         error, timeout) — routing is all-or-nothing
         */
        Double routeRoute(double[] longitudes, double[] latitudes);
    }

    /**
     * Production constructor: routes with BRouter against the given {@code .rd5}
     * segments directory using the bundled default profile.
     */
    public BRouterRouteLengthComputer(java.io.File segmentsDirectory) {
        this(new BRouterRouteRouter(segmentsDirectory));
    }

    BRouterRouteLengthComputer(RouteRouter routeRouter) {
        this.routeRouter = routeRouter;
    }

    public LengthResult computeLength(BaseRoute<?, ?> route) {
        if (route.getCharacteristics() != Route)
            return fallback.computeLength(route);
        if (route.getPositionCount() < 2)
            return null;

        @SuppressWarnings("unchecked")
        List<BaseNavigationPosition> positions = ((BaseRoute<BaseNavigationPosition, ?>) route).getPositions();
        List<NavigationPosition> withCoordinates = new ArrayList<>();
        for (NavigationPosition position : positions) {
            if (position.getLongitude() != null && position.getLatitude() != null)
                withCoordinates.add(position);
        }
        if (withCoordinates.size() < 2)
            return fallback.computeLength(route);

        double[] longitudes = new double[withCoordinates.size()];
        double[] latitudes = new double[withCoordinates.size()];
        for (int i = 0; i < withCoordinates.size(); i++) {
            longitudes[i] = withCoordinates.get(i).getLongitude();
            latitudes[i] = withCoordinates.get(i).getLatitude();
        }

        Double routedMeters = safeRoute(longitudes, latitudes);
        if (routedMeters == null) {
            log.info("BRouter could not route the list; falling back to beeline for this list");
            return fallback.computeLength(route);
        }
        return new LengthResult(routedMeters, "routed");
    }

    private Double safeRoute(double[] longitudes, double[] latitudes) {
        try {
            return routeRouter.routeRoute(longitudes, latitudes);
        } catch (Throwable t) {
            // never let a routing error crash the analyze run
            log.warning("BRouter routing failed: " + t);
            return null;
        }
    }
}
