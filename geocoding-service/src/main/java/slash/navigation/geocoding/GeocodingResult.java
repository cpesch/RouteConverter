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


import java.util.Objects;

/**
 * A geocoding result together with the name of the service that produced it.
 *
 * @author Christian Pesch
 */
public class GeocodingResult {
    private final CategorizedNavigationPosition position;
    private final String geocodingServiceName;

    public GeocodingResult(CategorizedNavigationPosition position, String geocodingServiceName) {
        this.position = position;
        this.geocodingServiceName = geocodingServiceName;
    }

    public CategorizedNavigationPosition getPosition() {
        return position;
    }

    public String getGeocodingServiceName() {
        return geocodingServiceName;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeocodingResult that)) return false;
        return Objects.equals(position, that.position) && Objects.equals(geocodingServiceName, that.geocodingServiceName);
    }

    public int hashCode() {
        return Objects.hash(position, geocodingServiceName);
    }

    public String toString() {
        return "GeocodingResult[position=" + position + ", geocodingServiceName=" + geocodingServiceName + "]";
    }
}

