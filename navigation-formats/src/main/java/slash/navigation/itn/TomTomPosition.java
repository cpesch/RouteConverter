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

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.Wgs84Position;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.nmea.NmeaPosition;

import static slash.navigation.base.RouteComments.parseDescription;

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
    private Double elevation;
    private Double speed;
    private CompactCalendar time;

    public TomTomPosition(Integer longitude, Integer latitude, String description) {
        this.longitude = longitude;
        this.latitude = latitude;
        setDescription(description);
    }

    public TomTomPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        setElevation(elevation);
        setSpeed(speed);
        setTime(time);
        setLongitude(longitude);
        setLatitude(latitude);
        setDescription(description);
        // there could be an elevation/time already parsed from description or one given as a parameter
        if (getElevation() == null || elevation != null)
            setElevation(elevation);
        if (!hasTime() || time != null)
            setTime(time);
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

    public String getDescription() {
        return city;
    }

    public void setDescription(String description) {
        this.city = description;
        this.reason = null;
        if (description == null)
            return;

        parseDescription(this, description);
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public CompactCalendar getTime() {
        return time;
    }

    public void setTime(CompactCalendar time) {
        this.time = time;
    }


    private static Integer asInt(Double aDouble) {
        return aDouble != null ? (int) (aDouble * INTEGER_FACTOR) : null;
    }

    private static Double asDouble(Integer anInteger) {
        return anInteger != null ? anInteger / INTEGER_FACTOR : null;
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
                !(hasTime() ? !getTime().equals(that.getTime()) : that.hasTime());
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (getElevation() != null ? getElevation().hashCode() : 0);
        result = 31 * result + (heading != null ? heading.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        result = 31 * result + (hasTime() ? getTime().hashCode() : 0);
        return result;
    }
}
