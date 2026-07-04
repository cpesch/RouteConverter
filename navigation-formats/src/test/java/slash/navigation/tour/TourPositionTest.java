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
package slash.navigation.tour;

import org.junit.Test;
import slash.navigation.gopal.GoPalPosition;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Tests description reconstruction and conversions of {@link TourPosition}.
 *
 * @author Christian Pesch
 */
public class TourPositionTest {

    private static TourPosition position(String zip, String city, String street, String houseNo, String name) {
        return new TourPosition(1L, 2L, zip, city, street, houseNo, name, false, new HashMap<>());
    }

    @Test
    public void descriptionReconstructsAllFieldsIncludingName() {
        assertEquals("12345 Berlin, Hauptstr 5, Home",
                position("12345", "Berlin", "Hauptstr", "5", "Home").getDescription());
    }

    @Test
    public void descriptionIsJustTheNameWhenNothingElseSet() {
        assertEquals("Home", position(null, null, null, null, "Home").getDescription());
    }

    @Test
    public void descriptionIsNullWhenEmpty() {
        assertNull(position(null, null, null, null, null).getDescription());
    }

    @Test
    public void setDescriptionStoresCityAndClearsTheRest() {
        TourPosition position = position("12345", "Berlin", "Hauptstr", "5", "Home");
        position.setDescription("elsewhere");

        assertEquals("elsewhere", position.getCity());
        assertNull(position.getStreet());
        assertNull(position.getHouseNo());
        assertNull(position.getName());
    }

    @Test
    public void asGoPalRoutePositionParsesTheHouseNumber() {
        GoPalPosition converted = position("12345", "Berlin", "Hauptstr", "5", null).asGoPalRoutePosition();

        assertEquals(Short.valueOf((short) 5), converted.getHouseNumber());
    }

    @Test
    public void asGoPalRoutePositionFallsBackToZeroForNonNumericHouseNumber() {
        assertEquals(Short.valueOf((short) 0),
                position("12345", "Berlin", "Hauptstr", "abc", null).asGoPalRoutePosition().getHouseNumber());
        assertEquals(Short.valueOf((short) 0),
                position("12345", "Berlin", "Hauptstr", null, null).asGoPalRoutePosition().getHouseNumber());
    }

    @Test
    public void asTourPositionReturnsItself() {
        TourPosition position = position("1", "c", "s", "1", "n");

        assertSame(position, position.asTourPosition());
    }

    @Test
    public void equalPositionsAreEqual() {
        TourPosition one = position("12345", "Berlin", "Hauptstr", "5", "Home");
        TourPosition same = position("12345", "Berlin", "Hauptstr", "5", "Home");
        TourPosition different = position("12345", "Berlin", "Hauptstr", "6", "Home");

        assertEquals(one, same);
        assertEquals(one.hashCode(), same.hashCode());
        assertNotEquals(one, different);
    }
}
