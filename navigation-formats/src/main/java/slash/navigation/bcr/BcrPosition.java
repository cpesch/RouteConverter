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

package slash.navigation.bcr;

import slash.common.type.CompactCalendar;
import slash.navigation.base.MercatorPosition;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.tour.TourPosition;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;

import static slash.common.io.Transfer.trim;
import static slash.navigation.common.NavigationConversion.bcrAltitudeToElevationMeters;
import static slash.navigation.common.NavigationConversion.elevationMetersToBcrAltitude;

/**
 * Represents a position in a Map &amp; Guide Tourenplaner Route (.bcr) file.
 * <p>Currently, the metrics of the altitude field is unclear.
 * Numbers range in the area of 210 billion plus something.
 *
 * @author Christian Pesch
 */

public class BcrPosition extends MercatorPosition {
    public static final int NO_ALTITUDE_DEFINED = 999999999;
    static final String STREET_DEFINES_CENTER_SYMBOL = "@";
    static final String STREET_DEFINES_CENTER_NAME = "Zentrum";
    static final String ZIPCODE_DEFINES_NOTHING = "WP";
    private static final DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance(Locale.GERMAN);

    static {
        decimalFormat.applyPattern("###,##0.00");
    }

    private long altitude;
    private String zipCode, street, type; // description = city

    public BcrPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        super(longitude, latitude, elevation, speed, time, description);
        this.altitude = asAltitude(elevation);
    }

    public BcrPosition(long x, long y, Double elevation, String description) {
        this(x, y, asAltitude(elevation), description);
    }

    public BcrPosition(long x, long y, long altitude, String description) {
        super(x, y, null, null, null, description);
        this.altitude = altitude;
    }

    private static long asAltitude(Double elevation) {
        return elevation != null ? elevationMetersToBcrAltitude(elevation) : NO_ALTITUDE_DEFINED;
    }


    public Double getElevation() {
        return altitude != NO_ALTITUDE_DEFINED ? bcrAltitudeToElevationMeters(getAltitude()) : null;
    }

    public void setElevation(Double elevation) {
        this.altitude = asAltitude(elevation);
    }

    public boolean isUnstructured() {
        return getZipCode() == null && getStreet() == null && getType() == null;
    }

    public String getDescription() {
        String result = (getZipCode() != null ? getZipCode() + " " : "") +
                (getCity() != null ? getCity() : "") +
                (getStreet() != null ? ", " + getStreet() : "");
        return result.length() > 0 ? result : null;
    }

    public void setDescription(String description) {
        this.zipCode = null;
        this.description = description;
        this.street = null;
        this.type = null;

        if (description == null)
            return;

        Matcher matcher = MTP0809Format.DESCRIPTION_PATTERN.matcher(description);
        if (matcher.matches()) {
            zipCode = trim(matcher.group(1));
            if (ZIPCODE_DEFINES_NOTHING.equals(zipCode)) {
                zipCode = null;
            }
            this.description = trim(matcher.group(2));
            if (zipCode != null && this.description == null) {
                this.description = zipCode;
                zipCode = null;
            }
            street = trim(matcher.group(3));
            if (street != null && STREET_DEFINES_CENTER_SYMBOL.equals(street))
                street = STREET_DEFINES_CENTER_NAME;
            this.type = trim(matcher.group(4));
        }
    }

    public long getAltitude() {
        return altitude;
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

    public String getType() {
        return type;
    }


    public BcrPosition asMTPPosition() {
        return this;
    }

    public GoPalPosition asGoPalRoutePosition() {
        return new GoPalPosition(getX(), getY(), null, null, getZipCode(), getCity(), null, getStreet(), null, null);
    }

    public TourPosition asTourPosition() {
        return new TourPosition(getX(), getY(), getZipCode(), getCity(), getStreet(), null, null, false, new HashMap<String, String>());
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BcrPosition that = (BcrPosition) o;

        return altitude == that.altitude &&
                !(x != null ? !x.equals(that.x) : that.x != null) &&
                !(y != null ? !y.equals(that.y) : that.y != null) &&
                !(description != null ? !description.equals(that.description) : that.description != null) &&
                !(street != null ? !street.equals(that.street) : that.street != null) &&
                !(type != null ? !type.equals(that.type) : that.type != null) &&
                !(zipCode != null ? !zipCode.equals(that.zipCode) : that.zipCode != null);
    }

    public int hashCode() {
        int result;
        result = (x != null ? x.hashCode() : 0);
        result = 31 * result + (y != null ? y.hashCode() : 0);
        result = 31 * result + (int) (altitude ^ (altitude >>> 32));
        result = 31 * result + (zipCode != null ? zipCode.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (street != null ? street.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
