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
import static slash.navigation.nmn.NmnFormat.COMMENT_PATTERN;

/**
 * Represents a position in a Navigon Mobile Navigator (.rte) file.
 *
 * @author Christian Pesch
 */

public class NmnPosition extends Wgs84Position {
    private String zip, street, number; // comment = city

    public NmnPosition(Double longitude, Double latitude, String zip, String city, String street, String number) {
        super(longitude, latitude, null, null, null, city);
        this.zip = zip;
        this.street = street;
        this.number = number;
    }

    public NmnPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        super(longitude, latitude, elevation, speed, time, comment);
    }

    public boolean isUnstructured() {
        return getStreet() == null && getNumber() == null;
    }

    public String getComment() {
        String result = (getZip() != null ? getZip() + " " : "") +
                (getCity() != null ? getCity() : "") +
                (getStreet() != null ? ", " + getStreet() : "") +
                (getNumber() != null ? " " + getNumber() : "");
        return result.length() > 0 ? result : null;
    }

    public void setComment(String comment) {
        this.comment = comment;
        this.street = null;
        this.number = null;

        if (comment == null)
            return;

        Matcher matcher = COMMENT_PATTERN.matcher(escape(comment, NmnFormat.SEPARATOR, ';'));
        if (matcher.matches()) {
            this.comment = trim(matcher.group(2));
            zip = trim(matcher.group(1));
            street = trim(matcher.group(3));
            number = trim(matcher.group(4));
        }
    }

    public String getZip() {
        return zip;
    }

    public String getCity() {
        return comment;
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
        return new Wgs84Position(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }
    

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NmnPosition that = (NmnPosition) o;

        return !(comment != null ? !comment.equals(that.comment) : that.comment != null) &&
                !(street != null ? !street.equals(that.street) : that.street != null) &&
                !(number != null ? !number.equals(that.number) : that.number != null) &&
                !(getElevation() != null ? !getElevation().equals(that.getElevation()) : that.getElevation() != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(getTime() != null ? !getTime().equals(that.getTime()) : that.getTime() != null);
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (getElevation() != null ? getElevation().hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (street != null ? street.hashCode() : 0);
        result = 31 * result + (number != null ? number.hashCode() : 0);
        result = 31 * result + (getTime() != null ? getTime().hashCode() : 0);
        return result;
    }
}
