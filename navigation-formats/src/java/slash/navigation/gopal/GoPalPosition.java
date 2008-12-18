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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.gopal;

import slash.navigation.MercatorPosition;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.tour.TourPosition;

import java.util.HashMap;

/**
 * Represents a position in a GoPal Route (.xml) file.
 *
 * @author Christian Pesch
 */

public class GoPalPosition extends MercatorPosition {
    private Short country, houseNo;
    private String zipCode, street; // comment = city

    public GoPalPosition(Double longitude, Double latitude, String comment) {
        super(longitude, latitude, null, null, comment);
    }

    public GoPalPosition(Long x, Long y, Short country, String zipCode, String city, String street, Short houseNo) {
        super(x, y, null, null, city);
        this.country = country;
        this.zipCode = zipCode;
        this.street = street;
        this.houseNo = houseNo;
    }

    public String getComment() {
        String result = (getZipCode() != null ? getZipCode() + " " : "") +
                (getCity() != null ? getCity() : "") +
                (getStreet() != null ? ", " + getStreet() : "") +
                (getHouseNo() != null ? " " + getHouseNo() : "");
        return result.length() > 0 ? result : null;
    }

    public void setComment(String comment) {
        this.country = null;
        this.zipCode = null;
        this.comment = comment;
        this.street = null;
        this.houseNo = null;
        // TODO parse comment like BcrPosition#setComment
    }

    public Short getCountry() {
        return country;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getCity() {
        return comment;
    }

    public String getStreet() {
        return street;
    }

    public Short getHouseNo() {
        return houseNo;
    }


    public BcrPosition asBcrPosition() {
        return new BcrPosition(getX(), getY(), getElevation(), getComment());
    }

    public GoPalPosition asGoPalRoutePosition() {
        return this;
    }

    public TourPosition asTourPosition() {
        return new TourPosition(getX(), getY(), getZipCode(), getCity(), getStreet(), Short.toString(getHouseNo()), null, new HashMap<String, String>());
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GoPalPosition that = (GoPalPosition) o;

        return !(x != null ? !x.equals(that.x) : that.x != null) &&
                !(y != null ? !y.equals(that.y) : that.y != null) &&
                !(country != null ? !country.equals(that.country) : that.country != null) &&
                !(zipCode != null ? !zipCode.equals(that.zipCode) : that.zipCode != null) &&
                !(comment != null ? !comment.equals(that.comment) : that.comment != null) &&
                !(street != null ? !street.equals(that.street) : that.street != null) &&
                !(houseNo != null ? !houseNo.equals(that.houseNo) : that.houseNo != null);
    }

    public int hashCode() {
        int result;
        result = (x != null ? x.hashCode() : 0);
        result = 31 * result + (y != null ? y.hashCode() : 0);
        result = 31 * result + (country != null ? country.hashCode() : 0);
        result = 31 * result + (zipCode != null ? zipCode.hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (street != null ? street.hashCode() : 0);
        result = 31 * result + (houseNo != null ? houseNo.hashCode() : 0);
        return result;
    }
}
