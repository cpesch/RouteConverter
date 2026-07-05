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

package slash.navigation.converter.gui.comparators;

import slash.navigation.common.DistanceAndTime;
import slash.navigation.converter.gui.models.RouteMetadataSource;
import slash.navigation.routes.impl.RouteModel;

import java.util.Comparator;

/**
 * Creates {@link Comparator}s that sort the {@link RouteModel}s of the browse routes table
 * when a column header is clicked. Name and creator sort case-insensitively; length and
 * duration sort numerically on the underlying {@link DistanceAndTime} metadata, with missing
 * values (rendered as {@code –}) sorting last.
 *
 * @author Christian Pesch
 */

public class RouteModelComparators {
    private static final Comparator<String> CASE_INSENSITIVE = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);

    public static Comparator<RouteModel> byName() {
        return Comparator.comparing(RouteModelComparators::name, CASE_INSENSITIVE);
    }

    public static Comparator<RouteModel> byCreator() {
        return Comparator.comparing(RouteModelComparators::creator, CASE_INSENSITIVE);
    }

    public static Comparator<RouteModel> byDistance(RouteMetadataSource source) {
        return Comparator.comparingDouble(route -> distance(source, route));
    }

    public static Comparator<RouteModel> byDuration(RouteMetadataSource source) {
        return Comparator.comparingLong(route -> duration(source, route));
    }

    static String name(RouteModel route) {
        try {
            return route.route().getDescription();
        } catch (Exception e) {
            return null;
        }
    }

    static String creator(RouteModel route) {
        try {
            return route.route().getCreator();
        } catch (Exception e) {
            return null;
        }
    }

    static double distance(RouteMetadataSource source, RouteModel route) {
        DistanceAndTime distanceAndTime = source.getDistanceAndTime(route.getUrl());
        if (distanceAndTime == null || distanceAndTime.distance() == null || distanceAndTime.distance() <= 0.0)
            return Double.POSITIVE_INFINITY;
        return distanceAndTime.distance();
    }

    static long duration(RouteMetadataSource source, RouteModel route) {
        DistanceAndTime distanceAndTime = source.getDistanceAndTime(route.getUrl());
        if (distanceAndTime == null || distanceAndTime.timeInMillis() == null || distanceAndTime.timeInMillis() <= 0)
            return Long.MAX_VALUE;
        return distanceAndTime.timeInMillis();
    }
}
