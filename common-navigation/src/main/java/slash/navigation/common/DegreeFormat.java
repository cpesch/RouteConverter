/*
 *
 *     This file is part of RouteConverter.
 *
 *     RouteConverter is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     RouteConverter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with RouteConverter; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *     Copyright (C) 2007 Christian Pesch. All Rights Reserved.
 * /
 */

package slash.navigation.common;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static slash.common.io.Transfer.parseDouble;
import static slash.navigation.common.NavigationConversion.formatPositionAsString;
import static slash.navigation.common.UnitConversion.*;

/**
 * Enumeration of supported degree formats.
 *
 * @author Christian Pesch
 */

public enum DegreeFormat {
    Degrees(new DegreeTransfer() {
        public String longitudeToDegrees(Double longitude) {
            return formatPositionAsString(longitude);
        }
        public String latitudeToDegrees(Double latitude) {
            return longitudeToDegrees(latitude);
        }
        public Double parseLongitude(String string) {
            return parseDouble(string);
        }
        public Double parseLatitude(String string) {
            return parseDouble(string);
        }
    }),

    Degrees_Minutes(new DegreeTransfer() {
        public String longitudeToDegrees(Double longitude) {
            return longitude2ddmm(longitude);
        }
        public String latitudeToDegrees(Double latitude) {
            return latitude2ddmm(latitude);
        }
        public Double parseLongitude(String string) {
            Double longitude = ddmm2longitude(string);
            // failed parsing results in a null value but the cell editing requires exceptions like for Double.parseDouble
            if(longitude == null)
                throw new NumberFormatException("Could not parse " + string);
            return longitude;
        }
        public Double parseLatitude(String string) {
            Double latitude = ddmm2latitude(string);
            // failed parsing results in a null value but the cell editing requires exceptions like for Double.parseDouble
            if(latitude == null)
                throw new NumberFormatException("Could not parse " + string);
            return latitude;
        }
    }),

    Degrees_Minutes_Seconds(new DegreeTransfer() {
        public String longitudeToDegrees(Double longitude) {
            return longitude2ddmmss(longitude);
        }
        public String latitudeToDegrees(Double latitude) {
            return latitude2ddmmss(latitude);
        }
        public Double parseLongitude(String string) {
            Double longitude = ddmmss2longitude(string);
            // failed parsing results in a null value but the cell editing requires exceptions like for Double.parseDouble
            if(longitude == null)
                throw new NumberFormatException("Could not parse " + string);
            return longitude;
        }
        public Double parseLatitude(String string) {
            Double latitude = ddmmss2latitude(string);
            // failed parsing results in a null value but the cell editing requires exceptions like for Double.parseDouble
            if(latitude == null)
                throw new NumberFormatException("Could not parse " + string);
            return latitude;
        }
    });

    private final DegreeTransfer degreeTransfer;

    DegreeFormat(DegreeTransfer degreeTransfer) {
        this.degreeTransfer = degreeTransfer;
    }

    public String longitudeToDegrees(Double longitude) {
        return degreeTransfer.longitudeToDegrees(longitude);
    }

    public String latitudeToDegrees(Double latitude) {
        return degreeTransfer.latitudeToDegrees(latitude);
    }

    public Double parseLongitude(String string) {
        return degreeTransfer.parseLongitude(string);
    }

    public Double parseLatitude(String string) {
        return degreeTransfer.parseLatitude(string);
    }


    public static List<DegreeFormat> getDegreeFormatsWithPreferredDegreeFormat(DegreeFormat preferredDegreeFormat) {
        List<DegreeFormat> degreeFormats = new ArrayList<>(asList(DegreeFormat.values()));
        if (preferredDegreeFormat != null) {
            degreeFormats.remove(preferredDegreeFormat);
            degreeFormats.add(0, preferredDegreeFormat);
        }
        return degreeFormats;
    }
}
