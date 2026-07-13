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

import slash.navigation.base.BaseRoute;

/**
 * Computes the length of a single position list for the {@code analyze} command.
 * <p>
 * This is the extension seam for BRouter (specs/00055 P3): the default
 * {@link PointToPointLengthComputer} measures recorded geometry point-to-point
 * (track length) and reports planned Route/Waypoints lists as {@code straight-line}.
 * A future BRouter-backed implementation will route Route-type lists that fall
 * inside its rd5 segment coverage and report {@code routed} on-road distances,
 * falling back to straight-line outside coverage. Nothing else in the analyzer needs
 * to change: swap the {@code RouteLengthComputer} instance in
 * {@link FileAnalyzer}.
 *
 * @author Christian Pesch
 */
public interface RouteLengthComputer {
    /**
     * @param route a single position list from the parsed file
     * @return the length in metres and its kind, or {@code null} if the list has
     *         no computable length (e.g. fewer than two positions with coordinates)
     */
    LengthResult computeLength(BaseRoute<?, ?> route);

    /**
     * @param meters length of the position list in metres
     * @param kind   {@code "track"}, {@code "straight-line"} or {@code "routed"}
     */
    record LengthResult(double meters, String kind) {
    }
}
