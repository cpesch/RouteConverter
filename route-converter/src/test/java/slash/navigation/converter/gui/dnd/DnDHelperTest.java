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

package slash.navigation.converter.gui.dnd;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static slash.navigation.converter.gui.dnd.DnDHelper.extractDescription;
import static slash.navigation.converter.gui.dnd.DnDHelper.extractUrl;

/**
 * Tests for {@link DnDHelper}.
 * The separator character is '\n' (newline).
 */
public class DnDHelperTest {

    private static final String SEP = "\n";

    // --- extractUrl ---

    @Test
    public void testExtractUrlNoSeparatorReturnsWholeString() {
        assertEquals("https://example.com/file.gpx", extractUrl("https://example.com/file.gpx"));
    }

    @Test
    public void testExtractUrlWithSeparatorReturnsPartBeforeSeparator() {
        assertEquals("https://example.com/file.gpx",
                extractUrl("https://example.com/file.gpx" + SEP + "My Route"));
    }

    @Test
    public void testExtractUrlSeparatorAtStartReturnsEmptyString() {
        assertEquals("", extractUrl(SEP + "description only"));
    }

    @Test
    public void testExtractUrlEmptyStringReturnsEmpty() {
        assertEquals("", extractUrl(""));
    }

    @Test
    public void testExtractUrlMultipleSeparatorsReturnsUntilFirstOne() {
        assertEquals("url",
                extractUrl("url" + SEP + "desc1" + SEP + "desc2"));
    }

    // --- extractDescription ---

    @Test
    public void testExtractDescriptionNoSeparatorReturnsEmpty() {
        assertEquals("", extractDescription("https://example.com/file.gpx"));
    }

    @Test
    public void testExtractDescriptionWithSeparatorReturnsPartAfterSeparator() {
        assertEquals("My Route",
                extractDescription("https://example.com/file.gpx" + SEP + "My Route"));
    }

    @Test
    public void testExtractDescriptionSeparatorAtEndReturnsEmpty() {
        assertEquals("", extractDescription("url" + SEP));
    }

    @Test
    public void testExtractDescriptionEmptyStringReturnsEmpty() {
        assertEquals("", extractDescription(""));
    }

    @Test
    public void testExtractDescriptionMultipleSeparatorsReturnsEverythingAfterFirst() {
        assertEquals("desc1" + SEP + "desc2",
                extractDescription("url" + SEP + "desc1" + SEP + "desc2"));
    }
}

