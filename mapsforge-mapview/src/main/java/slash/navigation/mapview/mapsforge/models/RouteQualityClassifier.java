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
package slash.navigation.mapview.mapsforge.models;

import static slash.navigation.mapview.mapsforge.models.RouteQuality.Detour;
import static slash.navigation.mapview.mapsforge.models.RouteQuality.Invalid;
import static slash.navigation.mapview.mapsforge.models.RouteQuality.Valid;

/**
 * Classifies a routed route leg as {@link RouteQuality#Valid}, {@link RouteQuality#Detour} or
 * {@link RouteQuality#Invalid} by comparing the routed distance to the straight-line distance
 * between its endpoints.
 *
 * @author Christian Pesch
 */

public class RouteQualityClassifier {
    private static final double DETOUR_RATIO = 5.0;
    private static final double DETOUR_MIN_EXCESS_METERS = 1000.0;

    private RouteQualityClassifier() {
    }

    public static RouteQuality classify(boolean routable, Double straightLineMeters, Double routedMeters) {
        if (!routable)
            return Invalid;

        if (straightLineMeters != null && straightLineMeters > 0 && routedMeters != null &&
                routedMeters / straightLineMeters > DETOUR_RATIO &&
                (routedMeters - straightLineMeters) > DETOUR_MIN_EXCESS_METERS)
            return Detour;

        return Valid;
    }
}
