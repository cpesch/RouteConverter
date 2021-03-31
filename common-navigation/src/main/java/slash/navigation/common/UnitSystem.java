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
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static slash.navigation.common.UnitConversion.*;

/**
 * Enumeration of supported unit systems.
 *
 * @author Christian Pesch
 */

public enum UnitSystem {
    Metric("km", "m","m", "km/h", new UnitTransfer() {
        public Double distanceToUnit(Double distance) {
            return distance / METERS_OF_A_KILOMETER;
        }
        public Double shortDistanceToUnit(Double distance) {
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

    Statute("mi", "ft", "ft", "mi/h", new UnitTransfer() {
        public Double distanceToUnit(Double distance) {
            return distance != null ? kiloMeterToStatuteMiles(distance / METERS_OF_A_KILOMETER) : null;
        }
        public Double shortDistanceToUnit(Double distance) {
            return distance != null ? meterToFeets(distance) : null;
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

    Nautic("nm", "ft", "ft", "knots", new UnitTransfer() {
        public Double distanceToUnit(Double distance) {
            return distance != null ? kiloMeterToNauticMiles(distance / METERS_OF_A_KILOMETER) : null;
        }
        public Double shortDistanceToUnit(Double distance) {
            return distance != null ? meterToFeets(distance) : null;
        }
        public Double distanceToDefault(Double distance) {
            return distance != null ? nauticMilesToKiloMeter(distance) : null;
        }
        public Double valueToUnit(Double value) {
            return value != null ? meterToFeets(value) : null;
        }
        public Double valueToDefault(Double value) {
            return value != null ? feetToMeters(value) : null;
        }
    });

    private final String distanceName, shortDistanceName, elevationName, speedName;
    private final UnitTransfer unitTransfer;

    UnitSystem(String distanceName, String shortDistanceName, String elevationName, String speedName, UnitTransfer unitTransfer) {
        this.distanceName = distanceName;
        this.shortDistanceName = shortDistanceName;
        this.elevationName = elevationName;
        this.speedName = speedName;
        this.unitTransfer = unitTransfer;
    }

    public String getDistanceName() {
        return distanceName;
    }

    public String getShortDistanceName() {
        return shortDistanceName;
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

    public Double shortDistanceToUnit(Double distance) {
        return unitTransfer.shortDistanceToUnit(distance);
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


    public static List<UnitSystem> getUnitSystemsWithPreferredUnitSystem(UnitSystem preferredUnitSystem) {
        List<UnitSystem> unitSystems = new ArrayList<>(asList(UnitSystem.values()));
        if (preferredUnitSystem != null) {
            unitSystems.remove(preferredUnitSystem);
            unitSystems.add(0, preferredUnitSystem);
        }
        return unitSystems;
    }
}
