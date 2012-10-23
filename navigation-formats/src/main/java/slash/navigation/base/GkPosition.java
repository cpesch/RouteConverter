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

import static slash.navigation.util.Conversion.gaussKruegerRightHeightToWgs84LongitudeLatitude;
import static slash.navigation.util.Conversion.wgs84LongitudeLatitudeToGaussKruegerRightHeight;

/**
 * Represents a Gauss Krueger position in a route.
 *
 * @author Christian Pesch
 */

public class GkPosition extends BaseNavigationPosition {
    private double right, height;
    private String comment;
    private Double elevation;
    private Double speed;
    private CompactCalendar time;

    public GkPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        setElevation(elevation);
        setSpeed(speed);
        setTime(time);
        if (longitude != null && latitude != null) {
            double[] gk = wgs84LongitudeLatitudeToGaussKruegerRightHeight(longitude, latitude);
            setRight(gk[0]);
            setHeight(gk[1]);
        }
        setComment(comment);
    }

    public GkPosition(double right, double height, String comment) {
        this.right = right;
        this.height = height;
        setComment(comment);
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
                !(comment != null ? !comment.equals(that.comment) : that.comment != null) &&
                !(getElevation() != null ? !getElevation().equals(that.getElevation()) : that.getElevation() != null) &&
                !(getTime() != null ? !getTime().equals(that.getTime()) : that.getTime() != null);
    }

    public int hashCode() {
        int result;
        long temp;
        temp = right != +0.0d ? Double.doubleToLongBits(right) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = height != +0.0d ? Double.doubleToLongBits(height) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (getElevation() != null ? getElevation().hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (getTime() != null ? getTime().hashCode() : 0);
        return result;
    }
}
