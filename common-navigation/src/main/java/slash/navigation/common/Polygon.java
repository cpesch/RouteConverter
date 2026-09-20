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
package slash.navigation.common;

import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * An area defined by one or more rings of {@link NavigationPosition}s, e.g. a clip polygon of an extract.
 * Rings flagged as hole cut out of the area. Containment uses the even-odd rule, which
 * handles holes without looking at the flag. Deliberately no java.awt.geom: this module
 * must stay free of GUI dependencies.
 *
 * @author Christian Pesch
 */
public record Polygon(List<List<NavigationPosition>> rings, List<Boolean> holes, BoundingBox boundingBox) {

    public static Polygon of(List<List<NavigationPosition>> rings, List<Boolean> holes) {
        if (rings.size() != holes.size())
            throw new IllegalArgumentException("Expected one hole flag per ring but got " + holes.size() + " for " + rings.size() + " rings");
        List<NavigationPosition> all = rings.stream().flatMap(List::stream).toList();
        if (all.isEmpty())
            throw new IllegalArgumentException("Expected at least one position");
        return new Polygon(List.copyOf(rings), List.copyOf(holes), BoundingBox.asBoundingBox(all));
    }

    public boolean contains(NavigationPosition position) {
        double x = position.getLongitude(), y = position.getLatitude();
        if (!boundingBox.contains(position))
            return false;

        boolean inside = false;
        for (List<NavigationPosition> ring : rings) {
            for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
                double xi = ring.get(i).getLongitude(), yi = ring.get(i).getLatitude();
                double xj = ring.get(j).getLongitude(), yj = ring.get(j).getLatitude();
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi)
                    inside = !inside;
            }
        }
        return inside;
    }

    /**
     * A corner test alone misses a narrow polygon crossing the tile, a vertex test alone
     * misses a tile fully inside the polygon, so all three cases are checked.
     */
    public boolean intersects(BoundingBox tile) {
        if (boundingBox.intersect(tile) == null)
            return false;

        double west = tile.southWest().getLongitude(), south = tile.southWest().getLatitude();
        double east = tile.northEast().getLongitude(), north = tile.northEast().getLatitude();

        if (contains(tile.northEast()) || contains(tile.southWest()) ||
                contains(tile.getNorthWest()) || contains(tile.getSouthEast()))
            return true;

        double[][] tileEdges = {{west, south, east, south}, {east, south, east, north},
                {east, north, west, north}, {west, north, west, south}};
        for (List<NavigationPosition> ring : rings) {
            for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
                double xi = ring.get(i).getLongitude(), yi = ring.get(i).getLatitude();
                if (xi >= west && xi <= east && yi >= south && yi <= north)
                    return true;

                double xj = ring.get(j).getLongitude(), yj = ring.get(j).getLatitude();
                for (double[] e : tileEdges)
                    if (segmentsIntersect(xi, yi, xj, yj, e[0], e[1], e[2], e[3]))
                        return true;
            }
        }
        return false;
    }

    private static boolean segmentsIntersect(double ax, double ay, double bx, double by,
                                             double cx, double cy, double dx, double dy) {
        double d1 = direction(cx, cy, dx, dy, ax, ay);
        double d2 = direction(cx, cy, dx, dy, bx, by);
        double d3 = direction(ax, ay, bx, by, cx, cy);
        double d4 = direction(ax, ay, bx, by, dx, dy);
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0)))
            return true;
        return (d1 == 0 && onSegment(cx, cy, dx, dy, ax, ay)) ||
                (d2 == 0 && onSegment(cx, cy, dx, dy, bx, by)) ||
                (d3 == 0 && onSegment(ax, ay, bx, by, cx, cy)) ||
                (d4 == 0 && onSegment(ax, ay, bx, by, dx, dy));
    }

    private static double direction(double ax, double ay, double bx, double by, double px, double py) {
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
    }

    private static boolean onSegment(double ax, double ay, double bx, double by, double px, double py) {
        return px >= min(ax, bx) && px <= max(ax, bx) && py >= min(ay, by) && py <= max(ay, by);
    }
}
