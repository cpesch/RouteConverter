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

package slash.navigation.kml;

import slash.common.type.CompactCalendar;
import slash.navigation.base.Wgs84Position;

import java.util.Objects;

/**
 * Represents a position in a Google Earth (.kml) file.
 *
 * @author Christian Pesch
 */

public class KmlPosition extends Wgs84Position {

    public KmlPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        super(longitude, latitude, elevation, speed, time, description);
    }

    public KmlPosition asKmlPosition() {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KmlPosition that = (KmlPosition) o;

        return !(getElevation() != null ? !getElevation().equals(that.getElevation()) : that.getElevation() != null) &&
                !(!Objects.equals(description, that.description)) &&
                !(!Objects.equals(latitude, that.latitude)) &&
                !(!Objects.equals(longitude, that.longitude)) &&
                !(hasTime() ? !getTime().equals(that.getTime()) : that.hasTime());
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (getElevation() != null ? getElevation().hashCode() : 0);
        result = 31 * result + (hasTime() ? getTime().hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
