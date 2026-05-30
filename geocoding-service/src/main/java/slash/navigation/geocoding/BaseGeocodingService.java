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

package slash.navigation.geocoding;


import java.util.ArrayList;
import java.util.List;

/**
 * The base of all {@link GeocodingService} implementations.
 *
 * @author Christian Pesch
 */

public abstract class BaseGeocodingService implements GeocodingService {
    protected List<GeocodingResult> asGeocodingResults(List<CategorizedNavigationPosition> positions) {
        return asGeocodingResults(positions, getName());
    }

    protected List<GeocodingResult> asGeocodingResults(List<CategorizedNavigationPosition> positions, String name) {
        if (positions == null)
            return null;

        List<GeocodingResult> results = new ArrayList<>(positions.size());
        for (CategorizedNavigationPosition position : positions) {
            results.add(new GeocodingResult(position, name));
        }
        return results;
    }
}

