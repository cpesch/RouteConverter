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

package slash.navigation.fpl;

import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;

import java.math.BigDecimal;

import static java.lang.Math.min;
import static slash.common.io.Transfer.formatDouble;
import static slash.common.io.Transfer.trim;

/**
 * Represents a position in a Garmin Flight Plan (.fpl) file.
 *
 * @author Christian Pesch
 */

public class GarminFlightPlanPosition extends Wgs84Position {
    private String identifier;
    private CountryCode countryCode;

    public GarminFlightPlanPosition(Double longitude, Double latitude, Double elevation, String description) {
        super(longitude, latitude, elevation, null, null, description);
        if(description != null)
            this.identifier = description.substring(0, min(description.length(), 6)).toUpperCase();
    }

    public GarminFlightPlanPosition(BigDecimal longitude, BigDecimal latitude, BigDecimal elevation, String description,
                                    WaypointType waypointType, String identifier, CountryCode countryCode) {
        this(formatDouble(longitude), formatDouble(latitude), formatDouble(elevation), description);
        this.waypointType = waypointType;
        this.identifier = identifier;
        this.countryCode = countryCode;
    }

    public String getDescription() {
        String description = trim(super.getDescription());
        return description != null ? description :
                getIdentifier() + ", " + getWaypointType() + (getCountryCode() != null ? ", " + getCountryCode() : "");
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public CountryCode getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(CountryCode countryCode) {
        this.countryCode = countryCode;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GarminFlightPlanPosition that = (GarminFlightPlanPosition) o;

        return !(countryCode != null ? !countryCode.equals(that.countryCode) : that.countryCode != null) &&
                !(identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) &&
                waypointType == that.waypointType;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (waypointType != null ? waypointType.hashCode() : 0);
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        result = 31 * result + (countryCode != null ? countryCode.hashCode() : 0);
        return result;
    }
}
