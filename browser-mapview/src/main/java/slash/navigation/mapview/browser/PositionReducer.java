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

package slash.navigation.mapview.browser;

import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.mapview.MapView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static slash.navigation.base.RouteCalculations.getSignificantPositions;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Helps to reduce the amount of positions for rending routes, tracks, waypoint lists.
 *
 * @author Christian Pesch
 */

class PositionReducer {
    private static final Preferences preferences = Preferences.userNodeForPackage(PositionReducer.class);
    private static final Logger log = Logger.getLogger(MapView.class.getName());

    private static final double[] THRESHOLD_PER_ZOOM = new double[]{
            120000,
            70000,
            40000,
            20000,
            10000,    // level 4
            2700,
            2100,
            1500,
            800,      // level 8
            500,
            225,
            125,
            80,
            45,
            20,
            10,
            4,
            1         // level 17
    };
    private static final int MAXIMUM_ZOOM_FOR_SIGNIFICANCE_CALCULATION = THRESHOLD_PER_ZOOM.length;

    private final Callback callback;
    private final Map<Integer, List<NavigationPosition>> reducedPositions = new HashMap<>(THRESHOLD_PER_ZOOM.length);
    private BoundingBox visible;

    PositionReducer(Callback callback) {
        this.callback = callback;
    }

    public List<NavigationPosition> reducePositions(List<NavigationPosition> positions, RouteCharacteristics characteristics, boolean showWaypointDescription) {
        List<NavigationPosition> result = filterPositionsWithoutCoordinates(positions);

        // if it's more than one segment, reduce the positions
        if (result.size() > getMaximumSegmentLength(characteristics)) {
            int zoom = callback.getZoom();
            result = reducedPositions.get(zoom);
            if (result == null) {
                result = reducePositions(positions, zoom, characteristics, showWaypointDescription);
                reducedPositions.put(zoom, result);
            }
        }
        return result;
    }

    public List<NavigationPosition> reduceSelectedPositions(List<NavigationPosition> positions, int[] indices) {
        List<NavigationPosition> result = filterPositionsWithoutCoordinates(positions);

        // reduce selected result if they're not selected
        result = filterSelectedPositions(result, indices);

        // reduce the number of selected result by a visibility heuristic
        int maximumSelectionCount = preferences.getInt("maximumSelectionCount", 5 * 10);
        if (result.size() > maximumSelectionCount) {
            double visibleSelectedPositionAreaFactor = preferences.getDouble("visibleSelectionAreaFactor", 1.25);
            result = filterVisiblePositions(result, visibleSelectedPositionAreaFactor, true);
        }

        // reduce the number of visible result by a JS-stability heuristic
        if (result.size() > maximumSelectionCount)
            result = filterEveryNthPosition(result, maximumSelectionCount);

        return result;
    }

    public boolean hasFilteredVisibleArea() {
        return visible != null;
    }

    public boolean isWithinVisibleArea(BoundingBox boundingBox) {
        return !hasFilteredVisibleArea() || visible.contains(boundingBox);
    }

    public void clear() {
        reducedPositions.clear();
        visible = null;
    }

    interface Callback {
        int getZoom();
        NavigationPosition getNorthEastBounds();
        NavigationPosition getSouthWestBounds();
    }

    int getMaximumSegmentLength(RouteCharacteristics characteristics) {
        switch (characteristics) {
            case Route:
                return preferences.getInt("maximumRouteSegmentLength", 24);
            case Track:
                return preferences.getInt("maximumTrackSegmentLength", 40);
            case Waypoints:
                return preferences.getInt("maximumWaypointSegmentLength", 15);
            default:
                throw new IllegalArgumentException("RouteCharacteristics " + characteristics + " is not supported");
        }
    }

    private int getMaximumPositionCount(RouteCharacteristics characteristics, boolean showWaypointDescription) {
        switch (characteristics) {
            case Route:
                return preferences.getInt("maximumRoutePositionCount", 30 * getMaximumSegmentLength(characteristics));
            case Track:
                return preferences.getInt("maximumTrackPositionCount", 50 * getMaximumSegmentLength(characteristics));
            case Waypoints:
                return preferences.getInt("maximumWaypointPositionCount", (showWaypointDescription ? 5 : 50) * getMaximumSegmentLength(characteristics));
            default:
                throw new IllegalArgumentException("RouteCharacteristics " + characteristics + " is not supported");
        }
    }

    List<NavigationPosition> filterVisiblePositions(List<NavigationPosition> positions, int zoom) {
        double visiblePositionAreaFactor = preferences.getDouble("visiblePositionAreaFactor", 3.0);
        double factor = max(visiblePositionAreaFactor * (zoom - MAXIMUM_ZOOM_FOR_SIGNIFICANCE_CALCULATION), 1) * visiblePositionAreaFactor;
        return filterVisiblePositions(positions, factor, false);
    }

    private List<NavigationPosition> reducePositions(List<NavigationPosition> positions, int zoom, RouteCharacteristics characteristics, boolean showWaypointDescription) {
        int maximumPositionCount = getMaximumPositionCount(characteristics, showWaypointDescription);
        int positionCountBeforeReduction = positions.size();

        // reduce the number of result to those that are visible for tracks and waypoint lists
        if (positions.size() > maximumPositionCount && !characteristics.equals(Route))
            positions = filterVisiblePositions(positions, zoom);

        // reduce the number of result by selecting every Nth to limit significance computation time
        int maximumSignificantPositionCount = preferences.getInt("maximumSignificantPositionCount", 50000);
        if (positions.size() > maximumSignificantPositionCount)
            positions = filterEveryNthPosition(positions, maximumSignificantPositionCount);

        // determine significant result for routes and tracks for this zoom level if there are too many positions
        if (!characteristics.equals(Waypoints))
            positions = filterSignificantPositions(positions, zoom);

        // reduce the number of result to ensure browser stability
        if (positions.size() > maximumPositionCount)
            positions = filterEveryNthPosition(positions, maximumPositionCount);

        int positionCountAfterReduction = positions.size();
        if (positionCountAfterReduction < positionCountBeforeReduction) {
            visible = new BoundingBox(positions);
        } else {
            visible = null;
        }

        return positions;
    }

    private List<NavigationPosition> filterPositionsWithoutCoordinates(List<NavigationPosition> positions) {
        long start = currentTimeMillis();

        List<NavigationPosition> result = new ArrayList<>();
        for (NavigationPosition position : positions) {
            if (position.hasCoordinates())
                result.add(position);
        }

        long end = currentTimeMillis();
        if (positions.size() != result.size())
            log.info(format("Filtered positions without coordinates to reduce %d positions to %d in %d milliseconds",
                    positions.size(), result.size(), (end - start)));
        return result;
    }

    private List<NavigationPosition> filterSignificantPositions(List<NavigationPosition> positions, int zoom) {
        long start = currentTimeMillis();

        List<NavigationPosition> result = new ArrayList<>();
        if (zoom < MAXIMUM_ZOOM_FOR_SIGNIFICANCE_CALCULATION) {
            double threshold = THRESHOLD_PER_ZOOM[zoom];
            int[] significantPositions = getSignificantPositions(positions, threshold);
            for (int significantPosition : significantPositions) {
                result.add(positions.get(significantPosition));
            }
            log.info(format("Zoom %d smaller than %d: for threshold %f use %d significant positions",
                    zoom, MAXIMUM_ZOOM_FOR_SIGNIFICANCE_CALCULATION, threshold, significantPositions.length));
        } else {
            // on all zoom about MAXIMUM_ZOOM_FOR_SIGNIFICANCE_CALCULATION
            // use all positions since the calculation is too expensive
            result.addAll(positions);
            log.info("Zoom " + zoom + " large: use all " + positions.size() + " positions");
        }

        long end = currentTimeMillis();
        if (positions.size() != result.size())
            log.info(format("Filtered significant positions to reduce %d positions to %d in %d milliseconds",
                    positions.size(), result.size(), (end - start)));
        return result;
    }

    List<NavigationPosition> filterVisiblePositions(List<NavigationPosition> positions,
                                                    double threshold, boolean includeFirstAndLastPosition) {
        long start = currentTimeMillis();

        NavigationPosition northEast = callback.getNorthEastBounds();
        NavigationPosition southWest = callback.getSouthWestBounds();
        if (northEast == null || southWest == null)
            return positions;

        double width = Math.abs(northEast.getLongitude() - southWest.getLongitude()) * threshold;
        double height = Math.abs(southWest.getLatitude() - northEast.getLatitude()) * threshold;
        northEast.setLongitude(northEast.getLongitude() + width);
        northEast.setLatitude(northEast.getLatitude() + height);
        southWest.setLongitude(southWest.getLongitude() - width);
        southWest.setLatitude(southWest.getLatitude() - height);
        BoundingBox boundingBox = new BoundingBox(northEast, southWest);

        List<NavigationPosition> result = new ArrayList<>();

        if (includeFirstAndLastPosition)
            result.add(positions.get(0));

        int firstIndex = includeFirstAndLastPosition ? 1 : 0;
        int lastIndex = includeFirstAndLastPosition ? positions.size() - 1 : positions.size();

        NavigationPosition previousPosition = positions.get(firstIndex);
        boolean previousPositionVisible = previousPosition.hasCoordinates() && boundingBox.contains(previousPosition);

        for (int i = firstIndex; i < lastIndex; i += 1) {
            NavigationPosition position = positions.get(i);
            if(!position.hasCoordinates())
                continue;

            boolean visible = boundingBox.contains(position);
            if (visible) {
                // if the previous position was not visible but the current position is visible:
                // add the previous position to render transition from non-visible to visible area
                if (!previousPositionVisible && previousPosition.hasCoordinates())
                    result.add(previousPosition);
                result.add(position);
            } else {
                // if the previous position was visible but the current position is not visible:
                // add the current position to render transition from visible to non-visible area
                if (previousPositionVisible)
                    result.add(position);
            }

            previousPositionVisible = visible;
            previousPosition = position;
        }

        if (includeFirstAndLastPosition)
            result.add(positions.get(positions.size() - 1));

        long end = currentTimeMillis();
        if (positions.size() != result.size())
            log.info(format("Filtered visible positions with a threshold of %f to reduce %d positions to %d in %d milliseconds",
                    threshold, positions.size(), result.size(), (end - start)));
        return result;
    }

    List<NavigationPosition> filterEveryNthPosition(List<NavigationPosition> positions, int maximumPositionCount) {
        long start = currentTimeMillis();

        List<NavigationPosition> result = new ArrayList<>();
        result.add(positions.get(0));

        double increment = (positions.size() - 1) / (double) (maximumPositionCount - 1);
        for (double i = (increment + 1.0); i < positions.size() - 1; i += increment)
            result.add(positions.get((int) i));

        result.add(positions.get(positions.size() - 1));

        long end = currentTimeMillis();
        if (positions.size() != result.size())
            log.info(format("Filtered every %fth position to reduce %d positions to %d in %d milliseconds",
                    increment, positions.size(), result.size(), (end - start)));
        return result;
    }

    private List<NavigationPosition> filterSelectedPositions(List<NavigationPosition> positions, int[] selectedIndices) {
        long start = currentTimeMillis();

        List<NavigationPosition> result = new ArrayList<>();
        for (int selectedIndex : selectedIndices) {
            if (selectedIndex >= positions.size())
                continue;
            result.add(positions.get(selectedIndex));
        }

        long end = currentTimeMillis();
        if (positions.size() != result.size())
            log.info(format("Filtered selected positions to reduce %d positions to %d in %d milliseconds",
                    selectedIndices.length, result.size(), (end - start)));
        return result;
    }
}
