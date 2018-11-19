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
    }),

    Degrees_Minutes(new DegreeTransfer() {
        public String longitudeToDegrees(Double longitude) {
            return longitude2ddmm(longitude);
        }
        public String latitudeToDegrees(Double latitude) {
            return latitude2ddmm(latitude);
        }
    }),

    Degrees_Minutes_Seconds(new DegreeTransfer() {
        public String longitudeToDegrees(Double longitude) {
            return longitude2ddmmss(longitude);
        }
        public String latitudeToDegrees(Double latitude) {
            return latitude2ddmmss(latitude);
        }
    });

    private DegreeTransfer degreeTransfer;

    DegreeFormat(DegreeTransfer degreeTransfer) {
        this.degreeTransfer = degreeTransfer;
    }

    public String longitudeToDegrees(Double longitude) {
        return degreeTransfer.longitudeToDegrees(longitude);
    }

    public String latitudeToDegrees(Double latitude) {
        return degreeTransfer.latitudeToDegrees(latitude);
    }
}
