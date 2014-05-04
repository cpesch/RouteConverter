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

package slash.navigation.base;

import slash.common.type.CompactCalendar;

import static slash.navigation.common.NavigationConversion.gaussKruegerRightHeightToWgs84LongitudeLatitude;
import static slash.navigation.common.NavigationConversion.wgs84LongitudeLatitudeToGaussKruegerRightHeight;

/**
 * Represents a Gauss Krueger position in a route.
 *
 * @author Christian Pesch
 */

public class GkPosition extends BaseNavigationPosition {
    private double right, height;
    private String description;
    private Double elevation;
    private Double speed;
    private CompactCalendar time;

    public GkPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        setElevation(elevation);
        setSpeed(speed);
        setTime(time);
        if (longitude != null && latitude != null) {
            double[] gk = wgs84LongitudeLatitudeToGaussKruegerRightHeight(longitude, latitude);
            setRight(gk[0]);
            setHeight(gk[1]);
        }
        setDescription(description);
    }

    public GkPosition(double right, double height, String description) {
        this.right = right;
        this.height = height;
        setDescription(description);
    }

    public Double getLongitude() {
        return gaussKruegerRightHeightToWgs84LongitudeLatitude(right, height)[0];
    }

    public void setLongitude(Double longitude) {
        double[] gk = wgs84LongitudeLatitudeToGaussKruegerRightHeight(longitude, getLatitude());
        setRight(gk[0]);
    }

    public Double getLatitude() {
        return gaussKruegerRightHeightToWgs84LongitudeLatitude(right, height)[1];
    }

    public void setLatitude(Double latitude) {
        double[] gk = wgs84LongitudeLatitudeToGaussKruegerRightHeight(getLongitude(), latitude);
        setHeight(gk[1]);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Double getRight() {
        return right;
    }

    private void setRight(double right) {
        this.right = right;
    }

    public Double getHeight() {
        return height;
    }

    private void setHeight(double height) {
        this.height = height;
    }

    public GkPosition asGkPosition() {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GkPosition that = (GkPosition) o;

        return Double.compare(that.height, height) == 0 &&
                Double.compare(that.right, right) == 0 &&
                !(description != null ? !description.equals(that.description) : that.description != null) &&
                !(getElevation() != null ? !getElevation().equals(that.getElevation()) : that.getElevation() != null) &&
                !(hasTime() ? !getTime().equals(that.getTime()) : that.hasTime());
    }

    public int hashCode() {
        int result;
        long temp;
        temp = right != +0.0d ? Double.doubleToLongBits(right) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = height != +0.0d ? Double.doubleToLongBits(height) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (getElevation() != null ? getElevation().hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (hasTime() ? getTime().hashCode() : 0);
        return result;
    }
}
