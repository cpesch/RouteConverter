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
 * A Route list is routed leg by leg between consecutive positions that carry
 * coordinates, and the leg distances are summed. If <em>any</em> leg cannot be
 * routed (no segment coverage, routing error, timeout, missing profile) the
 * whole list falls back to the straight-line {@code beeline} length, so the
 * label never over-promises and a partially-covered route is never reported as
 * a mix. Routing failures never propagate: {@link #computeLength} never throws
 * for a routing problem, so the analyzer always emits its JSON.
 * <p>
 * The actual leg routing is behind the {@link LegRouter} seam. The production
 * seam is {@link BRouterLegRouter}, which drives BRouter's
 * {@code RoutingContext}/{@code RoutingEngine} exactly like
 * {@code slash.navigation.brouter.BRouter#getRouteBetween}. Tests inject a fake
 * {@link LegRouter} to exercise the summation and fallback logic without real
 * {@code .rd5} segments.
 *
 * @author Christian Pesch
 */
public class BRouterRouteLengthComputer implements RouteLengthComputer {
    private static final Logger log = Logger.getLogger(BRouterRouteLengthComputer.class.getName());

    private final RouteLengthComputer fallback = new PointToPointLengthComputer();
    private final LegRouter legRouter;

    /**
     * Routes a single leg between two coordinates.
     */
    interface LegRouter {
        /**
         * @return the on-road distance in metres, or {@code null} if the leg
         *         cannot be routed (no coverage, error, timeout)
         */
        Double routeLeg(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude);
    }

    /**
     * Production constructor: routes with BRouter against the given {@code .rd5}
     * segments directory using the bundled default profile.
     */
    public BRouterRouteLengthComputer(java.io.File segmentsDirectory) {
        this(new BRouterLegRouter(segmentsDirectory));
    }

    BRouterRouteLengthComputer(LegRouter legRouter) {
        this.legRouter = legRouter;
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

        double routedMeters = 0;
        for (int i = 1; i < withCoordinates.size(); i++) {
            NavigationPosition from = withCoordinates.get(i - 1);
            NavigationPosition to = withCoordinates.get(i);
            Double legMeters = safeRouteLeg(from, to);
            if (legMeters == null) {
                log.info("BRouter could not route a leg; falling back to beeline for this list");
                return fallback.computeLength(route);
            }
            routedMeters += legMeters;
        }
        return new LengthResult(routedMeters, "routed");
    }

    private Double safeRouteLeg(NavigationPosition from, NavigationPosition to) {
        try {
            return legRouter.routeLeg(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude());
        } catch (Throwable t) {
            // never let a routing error crash the analyze run
            log.warning("BRouter leg routing failed: " + t);
            return null;
        }
    }
}
