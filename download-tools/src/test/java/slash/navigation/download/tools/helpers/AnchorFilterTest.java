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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class AnchorFilterTest {
    private final AnchorFilter filter = new AnchorFilter();

    @Test
    public void testFilterAbsoluteURLs() {
        String ABSOLUTE_URL = "http://www.routeconverter.com";
        String ANOTHER_ABSOLUTE_URL = "http://www.routeconverter.de";
        String RELATIVE_URL = "page.html";
        assertEquals(singletonList(RELATIVE_URL), filter.filterAnchors(ABSOLUTE_URL, asList(ABSOLUTE_URL, ANOTHER_ABSOLUTE_URL, RELATIVE_URL), null, null, null));
    }

    @Test
    public void testFilterIndexHtml() {
        String ABSOLUTE_URL = "http://www.routeconverter.com";
        String URI = "page.html";
        String INDEX_URI = "index.html";
        assertEquals(singletonList(URI), filter.filterAnchors(ABSOLUTE_URL, asList(URI, INDEX_URI), null, null, null));
    }

    @Test
    public void testFilterExtensions() {
        String ABSOLUTE_URL = "http://www.routeconverter.com";
        String URI = "page.html";
        String ZIP_URI = "page.zip";
        assertEquals(singletonList(URI), filter.filterAnchors(ABSOLUTE_URL, asList(URI, ZIP_URI), createSet(".html"), null, null));
    }

    private Set<String> createSet(String string) {
        return new HashSet<>(singletonList(string));
    }

    @Test
    public void testFilterIncludes() {
        String ABSOLUTE_URL = "http://www.routeconverter.com";
        String URI = "page.html";
        String ZIP_URI = "page.zip";
        assertEquals(singletonList(URI), filter.filterAnchors(ABSOLUTE_URL, asList(URI, ZIP_URI), null, createSet(".*\\.html"), null));
    }

    @Test
    public void testFilterExcludes() {
        String ABSOLUTE_URL = "http://www.routeconverter.com";
        String URI = "page.html";
        String ZIP_URI = "page.zip";
        assertEquals(singletonList(URI), filter.filterAnchors(ABSOLUTE_URL, asList(URI, ZIP_URI), null, null, createSet(".*\\.zip")));
    }

    @Test
    public void testStripsDotSlashPrefix() {
        String BASE = "http://www.routeconverter.com/";
        // "./page.html" should become "page.html" and pass through
        assertEquals(singletonList("page.html"), filter.filterAnchors(BASE, singletonList("./page.html"), null, null, null));
    }

    @Test
    public void testStripsQueryString() {
        String BASE = "http://www.routeconverter.com/";
        // "file.zip?v=1" should become "file.zip"
        assertEquals(singletonList("file.zip"), filter.filterAnchors(BASE, singletonList("file.zip?v=1"), null, null, null));
    }

    @Test
    public void testFiltersParentPathAnchors() {
        String BASE = "http://www.routeconverter.com/sub/";
        // ".." relative anchors should be excluded
        assertEquals(new ArrayList<>(), filter.filterAnchors(BASE, singletonList("../other.html"), null, null, null));
    }

    @Test
    public void testStripsBaseUrlPrefixFromAbsoluteUrl() {
        String BASE = "http://example.com/files/";
        String FULL = BASE + "germany.zip";
        // the full absolute URL should be relativised to "germany.zip"
        assertEquals(singletonList("germany.zip"), filter.filterAnchors(BASE, singletonList(FULL), null, null, null));
    }
}
