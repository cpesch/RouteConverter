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

package slash.navigation.tcx;

import slash.navigation.Wgs84Position;
import slash.navigation.util.Conversion;

import java.math.BigDecimal;
import java.util.Calendar;

/**
 * Represents a position in a Training Center Database (.tcx) file.
 *
 * @author Christian Pesch
 */

public class TcxPosition extends Wgs84Position { // TODO same as GpxPosition

    public TcxPosition(Double longitude, Double latitude, Double elevation, Calendar time, String comment) {
        super(longitude, latitude, elevation, null, time, comment);
    }

    public TcxPosition(BigDecimal longitude, BigDecimal latitude, BigDecimal elevation, Calendar time, String comment) {
        this(Conversion.formatDouble(longitude), Conversion.formatDouble(latitude),
                Conversion.formatDouble(elevation), time, comment);
    }

    public TcxPosition asTcxPosition() {
        return this;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TcxPosition that = (TcxPosition) o;

        return !(elevation != null ? !elevation.equals(that.elevation) : that.elevation != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(comment != null ? !comment.equals(that.comment) : that.comment != null) &&
                !(time != null ? !time.equals(that.time) : that.time != null);
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (elevation != null ? elevation.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        return result;
    }
}
