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

import org.junit.Test;

import static org.junit.Assert.*;
import static slash.navigation.base.WaypointType.Airport;
import static slash.navigation.fpl.CountryCode.United_States;

/**
 * Tests derivation and the description fallback of {@link GarminFlightPlanPosition}.
 *
 * @author Christian Pesch
 */
public class GarminFlightPlanPositionTest {

    @Test
    public void identifierConstructorDerivesWaypointTypeAndCountryCode() {
        GarminFlightPlanPosition position = new GarminFlightPlanPosition(1.0, 2.0, null, "KJFK", null);

        assertEquals("KJFK", position.getIdentifier());
        assertEquals(Airport, position.getWaypointType());
        assertEquals(United_States, position.getCountryCode());
    }

    @Test
    public void descriptionFallsBackToIdentifierWaypointTypeAndCountry() {
        GarminFlightPlanPosition position = new GarminFlightPlanPosition(1.0, 2.0, null, "KJFK", null);

        String description = position.getDescription();
        assertTrue(description.startsWith("KJFK,"));
        assertTrue(description.contains(Airport.toString()));
    }

    @Test
    public void explicitDescriptionIsUsedWhenPresent() {
        GarminFlightPlanPosition position = new GarminFlightPlanPosition(1.0, 2.0, null, "KJFK", "My Airport");

        assertEquals("My Airport", position.getDescription());
    }

    @Test
    public void descriptionOnlyConstructorDerivesIdentifierFromIt() {
        GarminFlightPlanPosition position = new GarminFlightPlanPosition(1.0, 2.0, null, "waypoint one");

        // identifier is the letters/numbers, uppercased and truncated to 6
        assertEquals("WAYPOI", position.getIdentifier());
    }
}
