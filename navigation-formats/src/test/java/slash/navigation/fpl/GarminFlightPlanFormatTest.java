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
import static slash.navigation.base.WaypointType.UserWaypoint;
import static slash.navigation.fpl.CountryCode.*;
import static slash.navigation.fpl.GarminFlightPlanFormat.*;

/**
 * Tests the pure identifier/description/country/waypoint helpers of {@link GarminFlightPlanFormat}.
 *
 * @author Christian Pesch
 */
public class GarminFlightPlanFormatTest {

    private static GarminFlightPlanPosition position(String identifier) {
        return new GarminFlightPlanPosition(1.0, 2.0, null, identifier, null);
    }

    @Test
    public void hasValidIdentifierRequiresUppercaseAlphanumericUpToSix() {
        assertTrue(hasValidIdentifier("KJFK"));
        assertFalse(hasValidIdentifier("kjfk"));       // not uppercase
        assertFalse(hasValidIdentifier("AB-CD"));      // punctuation
        assertFalse(hasValidIdentifier("ABCDEFG"));    // 7 > 6
        assertFalse(hasValidIdentifier(null));
    }

    @Test
    public void createValidIdentifierUppercasesStripsAndTruncates() {
        assertEquals("ABCD", createValidIdentifier("ab-cd"));
        assertEquals("ABCDEF", createValidIdentifier("abcdefghij"));
        assertNull(createValidIdentifier(null));
    }

    @Test
    public void hasValidDescriptionAllowsSpacesUpToTwentyFive() {
        assertTrue(hasValidDescription("HELLO WORLD"));
        assertFalse(hasValidDescription("Hello"));     // not uppercase
        assertFalse(hasValidDescription("THIS DESCRIPTION IS FAR TOO LONG")); // > 25
    }

    @Test
    public void createValidDescriptionUppercasesStripsPunctuationAndTruncates() {
        assertEquals("HELLO WORLD", createValidDescription("hello world!"));
        assertEquals(25, createValidDescription("abcdefghijklmnopqrstuvwxyz0123456789").length());
    }

    @Test
    public void createValidWaypointTypeIsAirportForFourCharacterIdentifiers() {
        assertEquals(Airport, createValidWaypointType(position("KJFK")));
        assertEquals(UserWaypoint, createValidWaypointType(position("KJF")));
    }

    @Test
    public void createValidCountryCodeUsesTheUnitedStatesRuleThenTheTwoLetterPrefix() {
        assertEquals(United_States, createValidCountryCode(position("KJFK"))); // starts with K
        assertEquals(Austria, createValidCountryCode(position("LOWW")));       // LO
        assertEquals(Germany1, createValidCountryCode(position("EDDF")));      // ED
    }

    @Test
    public void userWaypointsHaveNoCountryCode() {
        // "KJF" is 3 chars -> UserWaypoint -> None, despite the leading 'K'
        assertEquals(None, createValidCountryCode(position("KJF")));
    }

    @Test
    public void positionConstructorDerivesWaypointTypeAndCountryCode() {
        GarminFlightPlanPosition airport = position("EDDF");

        assertEquals(Airport, airport.getWaypointType());
        assertEquals(Germany1, airport.getCountryCode());
    }
}
