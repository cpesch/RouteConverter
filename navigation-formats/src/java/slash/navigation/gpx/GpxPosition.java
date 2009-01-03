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

package slash.navigation.gpx;

import slash.navigation.BaseNavigationPosition;
import slash.navigation.util.Conversion;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.regex.Matcher;

/**
 * Represents a position in a GPS Exchange Format (.gpx) file.
 *
 * @author Christian Pesch
 */

public class GpxPosition extends BaseNavigationPosition {
    private Double longitude, latitude, elevation;
    private Calendar time;
    private String city, reason;

    public GpxPosition(Double longitude, Double latitude, Double elevation, Calendar time, String comment) {
        setLongitude(longitude);
        setLatitude(latitude);
        setElevation(elevation);
        setTime(time);
        setComment(comment);
    }

    public GpxPosition(BigDecimal longitude, BigDecimal latitude, BigDecimal elevation, Calendar time, String comment) {
        this(Conversion.formatDouble(longitude), Conversion.formatDouble(latitude),
                Conversion.formatDouble(elevation), time, comment);
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public Calendar getTime() {
        return time;
    }

    public void setTime(Calendar time) {
        this.time = time;
    }

    public String getComment() {
        return city;
    }

    public void setComment(String comment) {
        this.city = comment;
        this.reason = null;
        if (comment == null)
            return;

        Matcher matcher = GpxFormat.TRIPMASTER_PATTERN.matcher(comment);
        if (matcher.matches()) {
            this.reason = Conversion.trim(matcher.group(1));
            this.city = Conversion.trim(matcher.group(3));
        }
    }

    public String getCity() {
        return city;
    }

    public String getReason() {
        return reason;
    }


    public GpxPosition asGpxPosition() {
        return this;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GpxPosition that = (GpxPosition) o;

        return !(city != null ? !city.equals(that.city) : that.city != null) &&
                !(elevation != null ? !elevation.equals(that.elevation) : that.elevation != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(reason != null ? !reason.equals(that.reason) : that.reason != null) && !(time != null ?
                !time.equals(that.time) : that.time != null);
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (elevation != null ? elevation.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        return result;
    }
}
