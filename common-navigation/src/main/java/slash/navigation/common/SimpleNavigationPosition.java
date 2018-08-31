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

import slash.common.type.CompactCalendar;

/**
 * A navigation position that supports just longitude, latitude, elevation, description and time.
 *
 * @author Christian Pesch
 */

public class SimpleNavigationPosition implements NavigationPosition {
    private Double longitude, latitude, elevation, speed;
    private String description;
    private CompactCalendar time;

    public SimpleNavigationPosition(Double longitude, Double latitude, Double elevation, String description, CompactCalendar time) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.elevation = elevation;
        this.description = description;
        this.time = time;
    }

    public SimpleNavigationPosition(Double longitude, Double latitude, Double elevation, String description) {
        this(longitude, latitude, elevation, description, null);
    }

    public SimpleNavigationPosition(Double longitude, Double latitude, CompactCalendar time) {
        this(longitude, latitude, null, null, time);
    }

    public SimpleNavigationPosition(Double longitude, Double latitude) {
        this(longitude, latitude, null);
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

    public boolean hasCoordinates() {
        return getLongitude() != null && getLatitude() != null;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public CompactCalendar getTime() {
        return time;
    }

    public void setTime(CompactCalendar time) {
        this.time = time;
    }

    public boolean hasTime() {
        return getTime() != null;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartDate(CompactCalendar startDate) {
        throw new UnsupportedOperationException();
    }

    public Double calculateDistance(NavigationPosition other) {
        throw new UnsupportedOperationException();
    }

    public Double calculateDistance(double longitude, double latitude) {
        throw new UnsupportedOperationException();
    }

    public Double calculateOrthogonalDistance(NavigationPosition pointA, NavigationPosition pointB) {
        throw new UnsupportedOperationException();
    }

    public Double calculateAngle(NavigationPosition other) {
        throw new UnsupportedOperationException();
    }

    public Double calculateElevation(NavigationPosition other) {
        throw new UnsupportedOperationException();
    }

    public Long calculateTime(NavigationPosition other) {
        throw new UnsupportedOperationException();
    }

    public Double calculateSpeed(NavigationPosition other) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleNavigationPosition that = (SimpleNavigationPosition) o;

        return !(description != null ? !description.equals(that.description) : that.description != null) &&
                !(speed != null ? !speed.equals(that.speed) : that.speed != null) &&
                !(elevation != null ? !elevation.equals(that.elevation) : that.elevation != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(time != null ? !time.equals(that.time) : that.time != null);
    }

    public int hashCode() {
        int result = longitude != null ? longitude.hashCode() : 0;
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (elevation != null ? elevation.hashCode() : 0);
        result = 31 * result + (speed != null ? speed.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[longitude=" + longitude + ", latitude=" + latitude + ", description=" + description + "]";
    }
}
