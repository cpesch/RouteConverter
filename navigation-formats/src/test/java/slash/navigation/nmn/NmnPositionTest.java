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
package slash.navigation.nmn;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the description reconstruction and parsing of {@link NmnPosition}.
 *
 * @author Christian Pesch
 */
public class NmnPositionTest {

    @Test
    public void descriptionReconstructsFromStructuredFields() {
        NmnPosition position = new NmnPosition(13.0, 52.0, "12345", "Berlin", "Hauptstr", "5");

        assertEquals("12345 Berlin, Hauptstr 5", position.getDescription());
    }

    @Test
    public void descriptionIsNullWhenAllPartsAreNull() {
        NmnPosition position = new NmnPosition(13.0, 52.0, (String) null, null, null, null);

        assertNull(position.getDescription());
    }

    @Test
    public void setDescriptionParsesStructuredText() {
        NmnPosition position = new NmnPosition(13.0, 52.0, (String) null, null, null, null);
        // the pattern's number group requires >= 2 characters after the space
        position.setDescription("12345 Berlin,Hauptstr 55");

        assertEquals("12345", position.getZip());
        assertEquals("Berlin", position.getCity());
        assertEquals("Hauptstr", position.getStreet());
        assertEquals("55", position.getNumber());
        assertFalse(position.isUnstructured());
    }

    @Test
    public void setDescriptionLeavesPlainTextUnstructured() {
        NmnPosition position = new NmnPosition(13.0, 52.0, (String) null, null, null, null);
        position.setDescription("Berlin");

        assertEquals("Berlin", position.getCity());
        assertNull(position.getStreet());
        assertNull(position.getNumber());
        assertTrue(position.isUnstructured());
    }

    @Test
    public void isUnstructuredReflectsStreetAndNumberPresence() {
        assertFalse(new NmnPosition(13.0, 52.0, "12345", "Berlin", "Hauptstr", "5").isUnstructured());
        assertTrue(new NmnPosition(13.0, 52.0, "12345", "Berlin", null, null).isUnstructured());
    }
}
