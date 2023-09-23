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

package slash.navigation.gopal;

import slash.common.type.CompactCalendar;
import slash.navigation.base.MercatorPosition;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.tour.TourPosition;

import java.util.HashMap;
import java.util.Objects;

/**
 * Represents a position in a GoPal 3 or 5 Route (.xml) file.
 *
 * @author Christian Pesch
 */

public class GoPalPosition extends MercatorPosition { // TODO eliminate this class
    private Short country, houseNumber;
    private String state, zipCode, suburb, street, sideStreet; // description = city

    public GoPalPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        super(longitude, latitude, elevation, speed, time, description);
    }

    public GoPalPosition(Long x, Long y, Short country, String state, String zipCode, String city, String suburb, String street, String sideStreet, Short houseNo) {
        super(x, y, null, null, null, city);
        this.state = state;
        this.country = country;
        this.zipCode = zipCode;
        this.suburb = suburb;
        this.street = street;
        this.sideStreet = sideStreet;
        this.houseNumber = houseNo;
    }

    public String getDescription() {
        String result = (getZipCode() != null ? getZipCode() + " " : "") +
                (getCity() != null ? getCity() : "") +
                (getStreet() != null ? ", " + getStreet() : "") +
                (getHouseNumber() != null ? " " + getHouseNumber() : "");
        return !result.isEmpty() ? result : null;
    }

    public void setDescription(String description) {
        this.state = null;
        this.country = null;
        this.zipCode = null;
        this.description = description;
        this.suburb = null;
        this.street = null;
        this.sideStreet = null;
        this.houseNumber = null;
        // TODO parse description like BcrPosition#setDescription
    }

    public Short getCountry() {
        return country;
    }

    public String getState() {
        return state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getCity() {
        return description;
    }

    public String getSuburb() {
        return suburb;
    }

    public String getStreet() {
        return street;
    }

    public String getSideStreet() {
        return sideStreet;
    }

    public Short getHouseNumber() {
        return houseNumber;
    }


    public BcrPosition asMTPPosition() {
        return new BcrPosition(getX(), getY(), getElevation(), getDescription());
    }

    public GoPalPosition asGoPalRoutePosition() {
        return this;
    }

    public TourPosition asTourPosition() {
        return new TourPosition(getX(), getY(), getZipCode(), getCity(), getStreet(), Short.toString(getHouseNumber()), null, false, new HashMap<String, String>());
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GoPalPosition that = (GoPalPosition) o;

        return Objects.equals(x, that.x) &&
                Objects.equals(y, that.y) &&
                Objects.equals(country, that.country) &&
                Objects.equals(zipCode, that.zipCode) &&
                Objects.equals(description, that.description) &&
                Objects.equals(suburb, that.suburb) &&
                Objects.equals(street, that.street) &&
                Objects.equals(sideStreet, that.sideStreet) &&
                Objects.equals(houseNumber, that.houseNumber);
    }

    public int hashCode() {
        int result;
        result = (x != null ? x.hashCode() : 0);
        result = 31 * result + (y != null ? y.hashCode() : 0);
        result = 31 * result + (country != null ? country.hashCode() : 0);
        result = 31 * result + (zipCode != null ? zipCode.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (suburb != null ? suburb.hashCode() : 0);
        result = 31 * result + (street != null ? street.hashCode() : 0);
        result = 31 * result + (sideStreet != null ? sideStreet.hashCode() : 0);
        result = 31 * result + (houseNumber != null ? houseNumber.hashCode() : 0);
        return result;
    }
}
