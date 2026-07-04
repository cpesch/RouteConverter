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
package slash.navigation.gopal;

import org.junit.Test;
import slash.navigation.tour.TourPosition;

import static org.junit.Assert.*;

/**
 * Tests description reconstruction and conversions of {@link GoPalPosition}.
 *
 * @author Christian Pesch
 */
public class GoPalPositionTest {

    private static GoPalPosition position(String zip, String city, String street, Short houseNumber) {
        return new GoPalPosition(1L, 2L, null, null, zip, city, null, street, null, houseNumber);
    }

    @Test
    public void descriptionReconstructsFromFields() {
        assertEquals("12345 Berlin, Hauptstr 5",
                position("12345", "Berlin", "Hauptstr", (short) 5).getDescription());
    }

    @Test
    public void descriptionIsNullWhenEmpty() {
        assertNull(position(null, null, null, null).getDescription());
    }

    @Test
    public void setDescriptionStoresCityAndClearsTheRest() {
        GoPalPosition position = position("12345", "Berlin", "Hauptstr", (short) 5);
        position.setDescription("elsewhere");

        assertEquals("elsewhere", position.getCity());
        assertNull(position.getStreet());
        assertNull(position.getZipCode());
        assertNull(position.getHouseNumber());
    }

    @Test
    public void asGoPalRoutePositionReturnsItself() {
        GoPalPosition position = position("12345", "Berlin", "Hauptstr", (short) 5);

        assertSame(position, position.asGoPalRoutePosition());
    }

    @Test
    public void asTourPositionCopiesTheAddress() {
        TourPosition converted = position("12345", "Berlin", "Hauptstr", (short) 5).asTourPosition();

        assertEquals("Berlin", converted.getCity());
        assertEquals("Hauptstr", converted.getStreet());
        assertEquals("5", converted.getHouseNo());
    }

    @Test
    public void asTourPositionKeepsNullHouseNumberNull() {
        GoPalPosition position = position("12345", "Berlin", "Hauptstr", null);

        TourPosition converted = position.asTourPosition();

        assertNull(converted.getHouseNo());
        assertEquals("Berlin", converted.getCity());
    }

    @Test
    public void equalPositionsAreEqual() {
        GoPalPosition one = position("12345", "Berlin", "Hauptstr", (short) 5);
        GoPalPosition same = position("12345", "Berlin", "Hauptstr", (short) 5);
        GoPalPosition different = position("12345", "Berlin", "Hauptstr", (short) 6);

        assertEquals(one, same);
        assertEquals(one.hashCode(), same.hashCode());
        assertNotEquals(one, different);
    }
}
