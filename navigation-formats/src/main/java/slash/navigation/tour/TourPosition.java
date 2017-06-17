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

package slash.navigation.tour;

import slash.common.type.CompactCalendar;
import slash.navigation.base.MercatorPosition;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.gopal.GoPalPosition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a position in a Falk Navigator (.tour) file.
 *
 * @author Christian Pesch
 */

public class TourPosition extends MercatorPosition {
    private boolean home;
    private String name, zipCode, street, houseNo; // description = city
    private Map<String, String> nameValues = new HashMap<>();

    public TourPosition(Double longitude, Double latitude,  Double elevation, Double speed, CompactCalendar time, String description) {
        super(longitude, latitude, elevation, speed, time, description);
    }

    public TourPosition(Long x, Long y, String zipCode, String city, String street, String houseNo, String name, boolean home, Map<String, String> nameValues) {
        super(x, y, null, null, null, city);
        this.name = name;
        this.zipCode = zipCode;
        this.street = street;
        this.houseNo = houseNo;
        this.home = home;
        this.nameValues = nameValues;
    }

    public String getDescription() {
        String result = (getZipCode() != null ? getZipCode() + " " : "") +
                (getCity() != null ? getCity() : "") +
                (getStreet() != null ? ", " + getStreet() : "") +
                (getHouseNo() != null ? " " + getHouseNo() : "");
        if (getName() != null)
            result += (result.length() > 0 ? ", " : "") + getName();
        return result.length() > 0 ? result : null;
    }

    public void setDescription(String description) {
        this.name = null;
        this.zipCode = null;
        this.description = description;
        this.street = null;
        this.houseNo = null;
        // TODO parse description like BcrPosition#setdescription
    }

    public String getName() {
        return name;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getCity() {
        return description;
    }

    public String getStreet() {
        return street;
    }

    public String getHouseNo() {
        return houseNo;
    }

    public boolean isHome() {
        return home;
    }

    public String get(String name) {
        return nameValues.get(name);
    }

    void put(String name, String value) {
        nameValues.put(name, value);
    }

    public Set<String> keySet() {
        return nameValues.keySet();
    }


    public BcrPosition asMTPPosition() {
        return new BcrPosition(getX(), getY(), getElevation(), getDescription());
    }

    public GoPalPosition asGoPalRoutePosition() {
        short houseNo = 0;
        try {
            houseNo = Short.parseShort(getHouseNo());   // TODO eliminate this
        } catch (NumberFormatException e) {
            // intentionally left empty
        }
        return new GoPalPosition(getX(), getY(), null, null, getZipCode(), getCity(), null, getStreet(), null, houseNo);
    }

    public TourPosition asTourPosition() {
        return this;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TourPosition that = (TourPosition) o;

        return !(x != null ? !x.equals(that.x) : that.x != null) &&
                !(y != null ? !y.equals(that.y) : that.y != null) &&
                !(name != null ? !name.equals(that.name) : that.name != null) &&
                !(zipCode != null ? !zipCode.equals(that.zipCode) : that.zipCode != null) &&
                !(description != null ? !description.equals(that.description) : that.description != null) &&
                !(street != null ? !street.equals(that.street) : that.street != null) &&
                !(houseNo != null ? !houseNo.equals(that.houseNo) : that.houseNo != null) &&
                !(nameValues != null ? !nameValues.equals(that.nameValues) : that.nameValues != null);
    }

    public int hashCode() {
        int result;
        result = (x != null ? x.hashCode() : 0);
        result = 31 * result + (y != null ? y.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (zipCode != null ? zipCode.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (street != null ? street.hashCode() : 0);
        result = 31 * result + (houseNo != null ? houseNo.hashCode() : 0);
        result = 31 * result + (nameValues != null ? nameValues.hashCode() : 0);
        return result;
    }
}
