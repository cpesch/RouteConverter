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

package slash.navigation.nmea;

import slash.navigation.BaseNavigationPosition;
import slash.navigation.util.CompactCalendar;
import slash.navigation.util.Conversion;

/**
 * Represents a position in a NMEA 0183 Sentences (.nmea) file.
 *
 * @author Christian Pesch
 */

public class NmeaPosition extends BaseNavigationPosition {
    private Double longitude, latitude;
    private String northOrSouth /*latitude*/, westOrEast /*longitude*/;
    private String comment;

    public NmeaPosition(Double longitude, String westOrEast, Double latitude, String northOrSouth, Double elevation, Double speed, CompactCalendar time, String comment) {
        super(elevation, speed, time);
        this.longitude = longitude;
        this.westOrEast = westOrEast;
        this.latitude = latitude;
        this.northOrSouth = northOrSouth;
        this.comment = comment;
    }

    public NmeaPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        super(elevation, speed, time);
        setLongitude(longitude);
        setLatitude(latitude);
        this.comment = comment;
    }

    private static Double toDegrees(Double ddmm2, String direction) {
        if (ddmm2 == null)
            return null;
        double decimal = Conversion.ddmm2degrees(ddmm2);
        direction = Conversion.trim(direction);
        boolean southOrWest = "S".equals(direction) || "W".equals(direction);
        return southOrWest ? -decimal : decimal;
    }

    public Double getLongitude() {
        return toDegrees(getLongitudeAsDdmm(), westOrEast);
    }

    public void setLongitude(Double longitude) {
        if(longitude == null) {
            this.longitude = null;
            this.westOrEast = null;
        } else {
            double ddmm = Conversion.degrees2ddmm(longitude);
            this.longitude = Math.abs(ddmm);
            this.westOrEast = ddmm >= 0.0 ? "E" : "W";
        }
    }

    public Double getLatitude() {
        return toDegrees(getLatitudeAsDdmm(), northOrSouth);
    }

    public void setLatitude(Double latitude) {
        if(latitude == null) {
            this.latitude = null;
            this.northOrSouth = null;
        } else {
            double ddmm = Conversion.degrees2ddmm(latitude);
            this.latitude = Math.abs(ddmm);
            this.northOrSouth = ddmm >= 0.0 ? "N" : "S";
        }
    }

    public Double getLongitudeAsDdmm() {
        return longitude;
    }

    public String getNorthOrSouth() {
        return northOrSouth;
    }

    public Double getLatitudeAsDdmm() {
        return latitude;
    }

    public String getWestOrEast() {
        return westOrEast;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }


    public NmeaPosition asNmeaPosition() {
        return this;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NmeaPosition that = (NmeaPosition) o;

        return !(comment != null ? !comment.equals(that.comment) : that.comment != null) &&
                !(elevation != null ? !elevation.equals(that.elevation) : that.elevation != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(northOrSouth != null ? !northOrSouth.equals(that.northOrSouth) : that.northOrSouth != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(westOrEast != null ? !westOrEast.equals(that.westOrEast) : that.westOrEast != null) &&
                !(time != null ? !time.equals(that.time) : that.time != null);
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (westOrEast != null ? westOrEast.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (northOrSouth != null ? northOrSouth.hashCode() : 0);
        result = 31 * result + (elevation != null ? elevation.hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        return result;
    }
}
