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
package slash.common.io;

import org.junit.Test;

import java.io.File;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static slash.common.io.Files.*;

/**
 * Covers the pure filename/path helpers of {@link Files} that the existing
 * {@code FilesTest} leaves untouched.
 *
 * @author Christian Pesch
 */
public class FilesExtensionTest {

    @Test
    public void getExtensionReturnsLowercasedSuffixOrEmpty() {
        assertEquals(".gpx", getExtension("track.gpx"));
        assertEquals(".gpx", getExtension("TRACK.GPX"));
        assertEquals(".c", getExtension("a.b.c"));
        assertEquals("", getExtension("noextension"));
    }

    @Test
    public void removeExtensionStripsTheSuffix() {
        assertEquals("track", removeExtension("track.gpx"));
        assertEquals("noextension", removeExtension("noextension"));
    }

    @Test
    public void setExtensionReplacesTheSuffix() {
        assertEquals("track.kml", setExtension("track.gpx", ".kml"));
        assertEquals("noextension.gpx", setExtension("noextension", ".gpx"));
    }

    @Test
    public void extractFileNameReturnsThePartAfterTheLastSlash() {
        assertEquals("c.gpx", extractFileName("/a/b/c.gpx"));
        assertEquals("plain.gpx", extractFileName("plain.gpx"));
        assertEquals("", extractFileName("a/b/"));
    }

    @Test
    public void compareOrdersByNormalizedAbsolutePath() {
        assertTrue(compare(new File("/tmp/aaa"), new File("/tmp/bbb")) < 0);
        assertTrue(compare(new File("/tmp/bbb"), new File("/tmp/aaa")) > 0);
        assertEquals(0, compare(new File("/tmp/same"), new File("/tmp/same")));
    }

    @Test
    public void asDialogStringHandlesNullEmptyAndJoins() {
        assertEquals("null", asDialogString(null, false));
        assertEquals("none", asDialogString(emptyList(), false));
        assertEquals("'a'", asDialogString(asList("a"), false));
        assertEquals("'a' and\n'b'", asDialogString(asList("a", "b"), false));
        assertEquals("'a',\n'b' and\n'c'", asDialogString(asList("a", "b", "c"), false));
    }
}
