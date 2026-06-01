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
package slash.navigation.download.tools.helpers;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GlobToRegexTest {

    @Test
    public void starDotZipMatchesExpectedFiles() {
        Pattern pattern = Pattern.compile(GlobToRegex.convert("*.zip"));
        assertTrue(pattern.matcher("Germany.zip").matches());
        assertTrue(pattern.matcher("europe/germany.zip").matches());
        assertFalse(pattern.matcher("Germany.zip.tmp").matches());
        assertFalse(pattern.matcher("zip").matches());
    }

    @Test
    public void prefixGlobMatchesLatestPbf() {
        Pattern pattern = Pattern.compile(GlobToRegex.convert("*-latest.osm.pbf"));
        assertTrue(pattern.matcher("germany-latest.osm.pbf").matches());
        assertTrue(pattern.matcher("europe/austria-latest.osm.pbf").matches());
        assertFalse(pattern.matcher("germany.osm.pbf").matches());
    }

    @Test
    public void slashGlobMatchesNestedDummy() {
        Pattern pattern = Pattern.compile(GlobToRegex.convert("*/dummy.brf"));
        assertTrue(pattern.matcher("profiles2/dummy.brf").matches());
        assertTrue(pattern.matcher("brouter.de/brouter/profiles2/dummy.brf").matches());
        assertFalse(pattern.matcher("dummy.brf").matches());
        assertFalse(pattern.matcher("dummy_brf").matches());
    }

    @Test
    public void bareFilenameMatchesItself() {
        Pattern pattern = Pattern.compile(GlobToRegex.convert("freizeitkarte-v5.zip"));
        assertTrue(pattern.matcher("freizeitkarte-v5.zip").matches());
        assertFalse(pattern.matcher("freizeitkarte-v4.zip").matches());
    }

    @Test
    public void escapesRegexMetacharacters() {
        assertEquals(".*\\.zip", GlobToRegex.convert("*.zip"));
        assertEquals("\\(x\\)", GlobToRegex.convert("(x)"));
    }
}
