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

import static slash.navigation.common.UnitConversion.feetToMeters;
import static slash.navigation.common.UnitConversion.kiloMeterToNauticMiles;
import static slash.navigation.common.UnitConversion.kiloMeterToStatuteMiles;
import static slash.navigation.common.UnitConversion.meterToFeets;
import static slash.navigation.common.UnitConversion.nauticMilesToKiloMeter;
import static slash.navigation.common.UnitConversion.statuteMilesToKiloMeter;

/**
 * Enumeration of supported unit systems.
 *
 * @author Christian Pesch
 */

public enum UnitSystem {
    Metric("km", "m", "km/h", new UnitTransfer() {
        public Double distanceToUnit(Double distance) {
            return distance;
        }
        public Double distanceToDefault(Double distance) {
            return distance;
        }
        public Double valueToUnit(Double value) {
            return value;
        }
        public Double valueToDefault(Double value) {
            return value;
        }
    }),

    Statute("mi", "ft", "mi/h", new UnitTransfer() {
        public Double distanceToUnit(Double distance) {
            return distance != null ? kiloMeterToStatuteMiles(distance) : null;
        }
        public Double distanceToDefault(Double distance) {
            return distance != null ? statuteMilesToKiloMeter(distance) : null;
        }
        public Double valueToUnit(Double value) {
            return value != null ? meterToFeets(value) : null;
        }
        public Double valueToDefault(Double value) {
            return value != null ? feetToMeters(value) : null;
        }
    }),

    Nautic("nm", "m", "knots", new UnitTransfer() {
        public Double distanceToUnit(Double distance) {
            return distance != null ? kiloMeterToNauticMiles(distance) : null;
        }
        public Double distanceToDefault(Double distance) {
            return distance != null ? nauticMilesToKiloMeter(distance) : null;
        }
        public Double valueToUnit(Double value) {
            return value;
        }
        public Double valueToDefault(Double value) {
            return value;
        }
    });

    private String distanceName, elevationName, speedName;
    private UnitTransfer unitTransfer;

    UnitSystem(String distanceName, String elevationName, String speedName, UnitTransfer unitTransfer) {
        this.distanceName = distanceName;
        this.elevationName = elevationName;
        this.speedName = speedName;
        this.unitTransfer = unitTransfer;
    }

    public String getDistanceName() {
        return distanceName;
    }

    public String getElevationName() {
        return elevationName;
    }

    public String getSpeedName() {
        return speedName;
    }

    public Double distanceToUnit(Double distance) {
        return unitTransfer.distanceToUnit(distance);
    }

    @SuppressWarnings("UnusedDeclaration")
    public Double distanceToDefault(Double distance) {
        return unitTransfer.distanceToDefault(distance);
    }

    public Double valueToUnit(Double value) {
        return unitTransfer.valueToUnit(value);
    }

    public Double valueToDefault(Double value) {
        return unitTransfer.valueToDefault(value);
    }
}
