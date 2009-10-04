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

import slash.common.util.CompactCalendar;
import slash.navigation.util.Conversion;

/**
 * Represents a Gauss Krueger position in a route.
 *
 * @author Christian Pesch
 */

public class GkPosition extends BaseNavigationPosition {
    private double right, height;
    private String comment;

    public GkPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        super(elevation, speed, time);
        if (longitude != null && latitude != null) {
            double[] gk = Conversion.wgs84LongitudeLatitudeToGaussKruegerRightHeight(longitude, latitude);
            setRight(gk[0]);
            setHeight(gk[1]);
        }
        setComment(comment);
    }

    public GkPosition(double right, double height, String comment) {
        super(null, null, null);
        this.right = right;
        this.height = height;
        setComment(comment);
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


    public Double getLongitude() {
        return Conversion.gaussKruegerRightHeightToWgs84LongitudeLatitude(right, height)[0];
    }

    public void setLongitude(Double longitude) {
        double[] gk = Conversion.wgs84LongitudeLatitudeToGaussKruegerRightHeight(longitude, getLatitude());
        setRight(gk[0]);
    }

    public Double getLatitude() {
        return Conversion.gaussKruegerRightHeightToWgs84LongitudeLatitude(right, height)[1];
    }

    public void setLatitude(Double latitude) {
        double[] gk = Conversion.wgs84LongitudeLatitudeToGaussKruegerRightHeight(getLongitude(), latitude);
        setHeight(gk[1]);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
                !(elevation != null ? !elevation.equals(that.elevation) : that.elevation != null) &&
                !(time != null ? !time.equals(that.time) : that.time != null);
    }

    public int hashCode() {
        int result;
        long temp;
        temp = right != +0.0d ? Double.doubleToLongBits(right) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = height != +0.0d ? Double.doubleToLongBits(height) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (elevation != null ? elevation.hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        return result;
    }
}
