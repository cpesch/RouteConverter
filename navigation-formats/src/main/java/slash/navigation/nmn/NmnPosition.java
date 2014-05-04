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

package slash.navigation.nmn;

import slash.common.type.CompactCalendar;
import slash.navigation.base.Wgs84Position;

import java.util.regex.Matcher;

import static slash.common.io.Transfer.escape;
import static slash.common.io.Transfer.trim;
import static slash.navigation.nmn.NmnFormat.DESCRIPTION_PATTERN;
import static slash.navigation.nmn.NmnFormat.SEPARATOR;

/**
 * Represents a position in a Navigon Mobile Navigator (.rte) file.
 *
 * @author Christian Pesch
 */

public class NmnPosition extends Wgs84Position {
    private String zip, street, number; // description = city

    public NmnPosition(Double longitude, Double latitude, String zip, String city, String street, String number) {
        super(longitude, latitude, null, null, null, city);
        this.zip = zip;
        this.street = street;
        this.number = number;
    }

    public NmnPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        super(longitude, latitude, elevation, speed, time, description);
    }

    public boolean isUnstructured() {
        return getStreet() == null && getNumber() == null;
    }

    public String getDescription() {
        String result = (getZip() != null ? getZip() + " " : "") +
                (getCity() != null ? getCity() : "") +
                (getStreet() != null ? ", " + getStreet() : "") +
                (getNumber() != null ? " " + getNumber() : "");
        return result.length() > 0 ? result : null;
    }

    public void setDescription(String description) {
        this.description = description;
        this.street = null;
        this.number = null;

        if (description == null)
            return;

        Matcher matcher = DESCRIPTION_PATTERN.matcher(escape(description, SEPARATOR, ';'));
        if (matcher.matches()) {
            this.description = trim(matcher.group(2));
            zip = trim(matcher.group(1));
            street = trim(matcher.group(3));
            number = trim(matcher.group(4));
        }
    }

    public String getZip() {
        return zip;
    }

    public String getCity() {
        return description;
    }

    public String getStreet() {
        return street;
    }

    public String getNumber() {
        return number;
    }


    public NmnPosition asNmnPosition() {
        return this;
    }

    public Wgs84Position asWgs84Position() {
        return new Wgs84Position(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }
    

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NmnPosition that = (NmnPosition) o;

        return !(description != null ? !description.equals(that.description) : that.description != null) &&
                !(street != null ? !street.equals(that.street) : that.street != null) &&
                !(number != null ? !number.equals(that.number) : that.number != null) &&
                !(getElevation() != null ? !getElevation().equals(that.getElevation()) : that.getElevation() != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(hasTime() ? !getTime().equals(that.getTime()) : that.hasTime());
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (getElevation() != null ? getElevation().hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (street != null ? street.hashCode() : 0);
        result = 31 * result + (number != null ? number.hashCode() : 0);
        result = 31 * result + (hasTime() ? getTime().hashCode() : 0);
        return result;
    }
}
