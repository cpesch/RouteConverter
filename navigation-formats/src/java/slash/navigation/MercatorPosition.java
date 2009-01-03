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

package slash.navigation;

import slash.navigation.util.Conversion;

import java.util.Calendar;

/**
 * Represents a Mercator position in a route.
 *
 * @author Christian Pesch
 */

public class MercatorPosition extends BaseNavigationPosition {
    protected Long x, y;
    protected Double elevation;
    protected String comment;
    protected Calendar time;

    public MercatorPosition(Double longitude, Double latitude, Double elevation, Calendar time, String comment) {
        this(asX(longitude), asY(latitude), elevation, time, comment);
    }

    public MercatorPosition(Long x, Long y, Double elevation, Calendar time, String comment) {
        this.x = x;
        this.y = y;
        this.elevation = elevation;
        this.time = time;
        setComment(comment);
    }

    private static Long asX(Double longitude) {
        return longitude != null ? Conversion.wgs84LongitudeToMercatorX(longitude) : null;
    }

    private static Long asY(Double latitude) {
        return latitude != null ? Conversion.wgs84LatitudeToMercatorY(latitude) : null;
    }

    public Double getLongitude() {
        return x != null ? Conversion.mercatorXToWgs84Longitude(x) : null;
    }

    public void setLongitude(Double longitude) {
        this.x = asX(longitude);
    }

    public Double getLatitude() {
        return y != null ? Conversion.mercatorYToWgs84Latitude(y) : null;
    }

    public void setLatitude(Double latitude) {
        this.y = asY(latitude);
    }

    public Long getX() {
        return x;
    }

    public Long getY() {
        return y;
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
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setStartDate(Calendar startDate) {
        if (time != null) {
            time.set(Calendar.YEAR, startDate.get(Calendar.YEAR));
            time.set(Calendar.MONTH, startDate.get(Calendar.MONTH));
            time.set(Calendar.DAY_OF_MONTH, startDate.get(Calendar.DAY_OF_MONTH));
        }
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MercatorPosition that = (MercatorPosition) o;

        return !(comment != null ? !comment.equals(that.comment) : that.comment != null) &&
                !(elevation != null ? !elevation.equals(that.elevation) : that.elevation != null) &&
                !(x != null ? !x.equals(that.x) : that.x != null) &&
                !(y != null ? !y.equals(that.y) : that.y != null) &&
                !(time != null ? !time.equals(that.time) : that.time != null);
    }

    public int hashCode() {
        int result;
        result = (x != null ? x.hashCode() : 0);
        result = 31 * result + (y != null ? y.hashCode() : 0);
        result = 31 * result + (elevation != null ? elevation.hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        return result;
    }
}