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
package slash.navigation.converter.gui.models;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link UrlDocument}.
 *
 * @author Christian Pesch
 */
public class UrlDocumentTest {

    private static UrlDocument document(String value) {
        UrlDocument document = new UrlDocument();
        document.setString(value);
        return document;
    }

    @Test
    public void emptyDocumentHasNoShortUrl() {
        assertNull(new UrlDocument().getShortUrl());
    }

    @Test
    public void whitespaceOnlyHasNoShortUrl() {
        assertNull(document("   ").getShortUrl());
    }

    @Test
    public void shortUrlIsTheLastPathFragment() {
        assertEquals("file.gpx", document("http://host/path/file.gpx").getShortUrl());
    }

    @Test
    public void stringRoundTrips() {
        assertEquals("http://host/x", document("http://host/x").getString());
    }

    @Test
    public void overlongFragmentIsTruncatedWithEllipsis() {
        String longName = "a".repeat(80) + ".gpx";
        String shortUrl = document("http://host/" + longName).getShortUrl();

        assertNotNull(shortUrl);
        assertTrue(shortUrl.startsWith("..."));
        assertEquals(60, shortUrl.length());
        assertTrue(shortUrl.endsWith(".gpx"));
    }
}
