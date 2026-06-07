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

import org.junit.Test;

import static org.junit.Assert.*;
import static slash.navigation.bcr.BcrPosition.NO_ALTITUDE_DEFINED;

public class BcrPositionTest {

    // Mercator X/Y for Hamburg approx
    private static final long HAMBURG_X = 1118968L;
    private static final long HAMBURG_Y = 7097077L;

    // --- unstructured description ---

    @Test
    public void testPlainDescriptionIsUnstructured() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        assertTrue(pos.isUnstructured());
    }

    @Test
    public void testPlainDescriptionGetCity() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        assertEquals("Hamburg", pos.getCity());
    }

    @Test
    public void testPlainDescriptionGetZipCodeIsNull() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        assertNull(pos.getZipCode());
    }

    @Test
    public void testPlainDescriptionGetStreetIsNull() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        assertNull(pos.getStreet());
    }

    @Test
    public void testPlainDescriptionGetTypeIsNull() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        assertNull(pos.getType());
    }

    // --- getDescription on unstructured position ---

    @Test
    public void testGetDescriptionCityOnly() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        assertEquals("Hamburg", pos.getDescription());
    }

    @Test
    public void testGetDescriptionNullCityReturnsNull() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, null);
        assertNull(pos.getDescription());
    }

    // --- structured description via setDescription ---

    @Test
    public void testSetDescriptionStructuredParsesZipAndCity() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, null);
        // BCR pattern: zipCode,city,street,0,
        pos.setDescription("20095,Hamburg,Mönckebergstraße,0,");
        assertEquals("20095", pos.getZipCode());
        assertEquals("Hamburg", pos.getCity());
        assertEquals("Mönckebergstraße", pos.getStreet());
        assertFalse(pos.isUnstructured());
    }

    @Test
    public void testGetDescriptionWithZipAndCityAndStreet() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, null);
        pos.setDescription("20095,Hamburg,Mönckebergstraße,0,");
        String desc = pos.getDescription();
        assertNotNull(desc);
        assertTrue(desc.contains("Hamburg"));
        assertTrue(desc.contains("20095"));
        assertTrue(desc.contains("Mönckebergstraße"));
    }

    // --- setDescription clears structured fields ---

    @Test
    public void testSetDescriptionClearsStructuredFields() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, null);
        pos.setDescription("20095,Hamburg,Main Street,0,");
        // Now reset with plain text
        pos.setDescription("Frankfurt");
        assertNull(pos.getZipCode());
        assertNull(pos.getStreet());
        assertTrue(pos.isUnstructured());
    }

    // --- altitude / elevation ---

    @Test
    public void testNoAltitudeDefinedConstant() {
        assertEquals(999999999, NO_ALTITUDE_DEFINED);
    }

    @Test
    public void testGetElevationReturnsNullForNoAltitude() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        assertNull(pos.getElevation());
    }

    @Test
    public void testGetAltitudeReturnsNoAltitudeDefined() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        assertEquals(NO_ALTITUDE_DEFINED, pos.getAltitude());
    }

    @Test
    public void testSetElevationAndGetBack() {
        BcrPosition pos = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        pos.setElevation(100.0);
        assertNotNull(pos.getElevation());
        // BCR altitude conversion is lossy; just verify it's reasonable
        assertTrue(pos.getElevation() > 50 && pos.getElevation() < 200);
    }

    @Test
    public void testConstructorFromLonLatAndElevation() {
        BcrPosition pos = new BcrPosition(10.0, 50.0, 100.0, null, null, "Test");
        assertNotNull(pos.getLongitude());
        assertNotNull(pos.getLatitude());
        assertNotNull(pos.getElevation());
    }

    // --- equals / hashCode ---

    @Test
    public void testEqualsAndHashCodeSameValues() {
        BcrPosition a = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        BcrPosition b = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualDifferentCity() {
        BcrPosition a = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Hamburg");
        BcrPosition b = new BcrPosition(HAMBURG_X, HAMBURG_Y, NO_ALTITUDE_DEFINED, "Berlin");
        assertNotEquals(a, b);
    }
}

