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

package slash.navigation.converter.gui.helpers;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReleaseHighlightsTest {
    private static final String RELEASE_PAGE_HTML =
            "<html><body><main class=\"page\"><div class=\"wrap\"><article class=\"prose\"><h1>Release 3.7</h1>\n" +
            "<ul>\n" +
            "<li>adds web bullet one</li>\n" +
            "<li>adds web bullet <a href=\"/x\">two</a> with a link</li>\n" +
            "<li>fixes web bullet three &amp; more</li>\n" +
            "</ul></article>\n" +
            "<div class=\"ad-slot\">ignored</div></div></main>\n" +
            "<ul><li>footer nav item, must not be picked up</li></ul>" +
            "</body></html>";

    @Test
    public void extractsBulletsFromReleasePageArticle() {
        List<String> highlights = ReleaseHighlights.extractFromHtml(RELEASE_PAGE_HTML, 5);
        assertEquals(Arrays.asList("adds web bullet one", "adds web bullet two with a link", "fixes web bullet three & more"), highlights);
    }

    @Test
    public void extractFromHtmlStopsAtMax() {
        assertEquals(Arrays.asList("adds web bullet one"), ReleaseHighlights.extractFromHtml(RELEASE_PAGE_HTML, 1));
    }

    @Test
    public void extractFromHtmlWithoutArticleYieldsNoHighlights() {
        assertTrue(ReleaseHighlights.extractFromHtml("<html><body>no article here</body></html>", 5).isEmpty());
    }

    @Test
    public void releasePageUrlPicksHostByLocale() {
        assertEquals("https://www.routeconverter.com/releases/3-6/", ReleaseHighlights.releasePageUrl("3.6", Locale.ENGLISH));
        assertEquals("https://www.routeconverter.de/releases/3-6/", ReleaseHighlights.releasePageUrl("3.6", Locale.GERMAN));
    }

    @Test
    public void usesInjectedPageFetcher() {
        List<String> highlights = ReleaseHighlights.first("3.7", Locale.ENGLISH, url -> RELEASE_PAGE_HTML);
        assertEquals(Arrays.asList("adds web bullet one", "adds web bullet two with a link", "fixes web bullet three & more"), highlights);
    }

    @Test
    public void noHighlightsWhenPageFetchFails() {
        assertTrue(ReleaseHighlights.first("3.7", Locale.ENGLISH, url -> null).isEmpty());
    }

    @Test
    public void noHighlightsWhenPageFetcherThrows() {
        assertTrue(ReleaseHighlights.first("3.7", Locale.ENGLISH, url -> {
            throw new RuntimeException("network down");
        }).isEmpty());
    }
}
