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

package slash.navigation.itn;

import slash.common.io.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.Wgs84Position;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.util.RouteComments;

/**
 * Represents a position in a Tom Tom Route (.itn) file.
 *
 * @author Christian Pesch
 */

public class TomTomPosition extends BaseNavigationPosition {
    static final double INTEGER_FACTOR = 100000.0;

    private Integer longitude, latitude;
    private String city, reason;
    private Double heading;

    public TomTomPosition(Integer longitude, Integer latitude, String comment) {
        super(null, null, null);
        this.longitude = longitude;
        this.latitude = latitude;
        setComment(comment);
    }

    public TomTomPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        super(elevation, speed, time);
        setLongitude(longitude);
        setLatitude(latitude);
        setComment(comment);
        // there could be an elevation/time already parsed from comment or one given as a parameter
        if (getElevation() == null || elevation != null)
            setElevation(elevation);
        if (getTime() == null || time != null)
            setTime(time);
    }


    private static Integer asInt(Double aDouble) {
        return aDouble != null ? (int) (aDouble * INTEGER_FACTOR) : null;
    }

    private static Double asDouble(Integer anInteger) {
        return anInteger != null ? anInteger / INTEGER_FACTOR : null;
    }


    public Double getLongitude() {
        return asDouble(getLongitudeAsInt());
    }

    public void setLongitude(Double longitude) {
        this.longitude = asInt(longitude);
    }

    public Double getLatitude() {
        return asDouble(getLatitudeAsInt());
    }

    public void setLatitude(Double latitude) {
        this.latitude = asInt(latitude);
    }

    public String getComment() {
        return city;
    }

    public void setComment(String comment) {
        this.city = comment;
        this.reason = null;
        if (comment == null)
            return;

        RouteComments.parseComment(this, comment);
    }


    public Integer getLongitudeAsInt() {
        return longitude;
    }

    public Integer getLatitudeAsInt() {
        return latitude;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }
    

    public GpxPosition asGpxPosition() {
        GpxPosition position = super.asGpxPosition();
        position.setHeading(getHeading());
        return position;
    }

    public NmeaPosition asNmeaPosition() {
        NmeaPosition position = super.asNmeaPosition();
        position.setHeading(getHeading());
        return position;
    }

    public TomTomPosition asTomTomRoutePosition() {
        return this;
    }

    public Wgs84Position asWgs84Position() {
        Wgs84Position position = super.asWgs84Position();
        position.setHeading(getHeading());
        return position;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TomTomPosition that = (TomTomPosition) o;

        return !(city != null ? !city.equals(that.city) : that.city != null) &&
                !(getElevation() != null ? !getElevation().equals(that.getElevation()) : that.getElevation() != null) &&
                !(heading != null ? !heading.equals(that.heading) : that.heading != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(reason != null ? !reason.equals(that.reason) : that.reason != null) &&
                !(getTime() != null ? !getTime().equals(that.getTime()) : that.getTime() != null);
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (getElevation() != null ? getElevation().hashCode() : 0);
        result = 31 * result + (heading != null ? heading.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        result = 31 * result + (getTime() != null ? getTime().hashCode() : 0);
        return result;
    }
}
