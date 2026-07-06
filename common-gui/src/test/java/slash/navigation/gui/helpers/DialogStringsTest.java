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

package slash.navigation.gui.helpers;

import org.junit.Test;

import java.io.File;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static slash.navigation.gui.helpers.DialogStrings.formatList;

/**
 * Tests for the pure list-formatting core of {@link DialogStrings}.
 *
 * @author Christian Pesch
 */
public class DialogStringsTest {
    private static final String NONE = "none", AND = "and", MORE = "and {0} more";

    private static String format(java.util.List<?> items) {
        return formatList(items, 10, false, NONE, AND, MORE);
    }

    @Test
    public void nullAndEmptyGiveNone() {
        assertEquals(NONE, formatList(null, 10, false, NONE, AND, MORE));
        assertEquals(NONE, format(emptyList()));
    }

    @Test
    public void joinsWithLocalizedAnd() {
        assertEquals("'a'", format(asList("a")));
        assertEquals("'a' and\n'b'", format(asList("a", "b")));
        assertEquals("'a',\n'b' and\n'c'", format(asList("a", "b", "c")));
    }

    @Test
    public void capsWithMoreSuffixAndNoAnd() {
        // 4 items, max 2 -> two shown, no "and" before the cap, then "and 2 more"
        assertEquals("'i1',\n'i2',\nand 2 more",
                formatList(asList("i1", "i2", "i3", "i4"), 2, false, NONE, AND, MORE));
    }

    @Test
    public void exactlyAtLimitIsNotCapped() {
        assertEquals("'a',\n'b' and\n'c'",
                formatList(asList("a", "b", "c"), 3, false, NONE, AND, MORE));
    }

    @Test
    public void shortensOnlyPathsWhenRequested() {
        String longPath = "/very/long/directory/structure/that/exceeds/the/limit/somefile.gpx";
        String shortened = formatList(asList(longPath), 10, true, NONE, AND, MORE);
        assertEquals(true, shortened.length() < ("'" + longPath + "'").length());

        // without the flag a plain string (name/url) is left intact
        assertEquals("'" + longPath + "'", format(asList(longPath)));
    }

    @Test
    public void shortensFileItemsEvenWithoutFlag() {
        File file = new File("/very/long/directory/structure/that/exceeds/the/limit/somefile.gpx");
        String result = format(asList(file));
        assertEquals(true, result.length() < ("'" + file + "'").length());
    }
}
