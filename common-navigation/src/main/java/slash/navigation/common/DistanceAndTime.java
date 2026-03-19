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

/**
 * Combines distance in meters and time in milliseconds between two {@link NavigationPosition}s.
 *
 * @author Christian Pesch
 */
public record DistanceAndTime(Double distance, Long timeInMillis) {
    public static final DistanceAndTime ZERO = new DistanceAndTime(0.0, 0L);

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DistanceAndTime that = (DistanceAndTime) o;

        if (distance() != null ? !distance().equals(that.distance()) : that.distance() != null)
            return false;
        return timeInMillis() != null ? timeInMillis().equals(that.timeInMillis()) : that.timeInMillis() == null;
    }

    public int hashCode() {
        int result = distance() != null ? distance().hashCode() : 0;
        result = 31 * result + (timeInMillis() != null ? timeInMillis().hashCode() : 0);
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[distance=" + distance() + ", time=" + timeInMillis() + "]";
    }
}
