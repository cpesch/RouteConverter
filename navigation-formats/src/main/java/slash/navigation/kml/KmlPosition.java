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

/**
 * Represents a position in a Google Earth (.kml) file.
 *
 * @author Christian Pesch
 */

public class KmlPosition extends Wgs84Position {

    public KmlPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        super(longitude, latitude, elevation, speed, time, comment);
    }

    public KmlPosition asKmlPosition() {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KmlPosition that = (KmlPosition) o;

        return !(getElevation() != null ? !getElevation().equals(that.getElevation()) : that.getElevation() != null) &&
                !(comment != null ? !comment.equals(that.comment) : that.comment != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(getTime() != null ? !getTime().equals(that.getTime()) : that.getTime() != null);
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (getElevation() != null ? getElevation().hashCode() : 0);
        result = 31 * result + (getTime() != null ? getTime().hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        return result;
    }
}
