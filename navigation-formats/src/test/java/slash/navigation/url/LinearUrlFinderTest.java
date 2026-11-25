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

package slash.navigation.url;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;

public class LinearUrlFinderTest {
    private static final Pattern GOOGLE_MAPS = Pattern.compile(".*http[s]?://.+\\.google\\..+/maps([^\\s]+).*");
    private static final Pattern GEO_HACK = Pattern.compile(".*http[s]?://.+\\.org/geohack.*\\?([^\\s]+).*");

    private final LinearUrlFinder googleMaps = new LinearUrlFinder(GOOGLE_MAPS, ".google.", 1, "/maps");
    private final LinearUrlFinder geoHack = new LinearUrlFinder(GEO_HACK, ".org/geohack", 0, "?");

    private static String reference(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private void assertSame(String text) {
        assertEquals(reference(GOOGLE_MAPS, text), googleMaps.find(text), "Google Maps: '" + text + "'");
        assertEquals(reference(GEO_HACK, text), geoHack.find(text), "GeoHack: '" + text + "'");
    }

    @Test
    public void testFindsTail() {
        assertEquals("?q=x", googleMaps.find("see https://www.google.com/maps?q=x for details"));
        assertEquals("params=1_N", geoHack.find("see https://geohack.toolforge.org/geohack.php?params=1_N now"));
        assertNull(googleMaps.find("see https://example.com/maps?q=x"));
        assertNull(geoHack.find("see https://example.org/other?params=1_N"));
    }

    @Test
    public void testTakesLastMarker() {
        assertEquals("?b", googleMaps.find("http://www.google.de/maps?a http://www.google.de/maps?b"));
        assertEquals("b", geoHack.find("http://x.org/geohack?a http://x.org/geohack?b"));
    }

    @Test
    public void testEdgeCases() {
        // scheme needs one character before middle, middle one character before marker
        assertNull(googleMaps.find("http://.google./maps?x"));
        assertEquals("?x", googleMaps.find("http://a.google.b/maps?x"));
        assertNull(googleMaps.find("http://a.google./maps?x"));
        // a marker without non white space after it does not count
        assertNull(googleMaps.find("http://a.google.b/maps "));
        assertNull(googleMaps.find("http://a.google.b/maps"));
        assertEquals("?x", googleMaps.find("http://a.google.b/maps /maps?x"));
        // https and http
        assertEquals("?x", googleMaps.find("https://a.google.b/maps?x"));
        assertNull(googleMaps.find("ftp://a.google.b/maps?x"));
        assertNull(googleMaps.find(""));
    }

    @Test
    public void testLineTerminatorsAreHandledLikeThePattern() {
        assertSame("http://a.google.b/maps?x  tail");
        assertSame("http://a.google.b/maps?x tail");
        assertSame("head\u0085 http://a.google.b/maps?x");
        assertSame("http://a.google.b/maps\n?x");
        assertSame("http://a.google.b /maps?x");
        assertSame("http://x.org/geohack?a\u0085b");
    }

    @Test
    public void testSameAsPatternOnRandomTexts() {
        String[] tokens = {"http://", "https://", "http", "://", ".google.", "google", ".org/geohack", "/maps", "maps", "?", "x",
                "a", ".", "/", " ", "\t", "\u000B", "\f", "\u0085", " ", " ", "\n", "\r", "ftp://", "%", "&"};
        Random random = new Random(20261124L);
        for (int i = 0; i < 20000; i++) {
            StringBuilder buffer = new StringBuilder();
            int count = random.nextInt(14);
            for (int j = 0; j < count; j++)
                buffer.append(tokens[random.nextInt(tokens.length)]);
            assertSame(buffer.toString());
        }
    }

    // a text like an HTML page: many URLs, and google hosts and /maps references that do not complete a match
    static String pageWithManyUrls(int urls, String tail) {
        StringBuilder buffer = new StringBuilder("<html><body>");
        for (int i = 0; i < urls; i++)
            buffer.append("<a href=\"https://www.google.com/policies/terms").append(i).append("\">See the terms</a> and ").
                    append("https://example.org/page/").append(i).append("?x=1 for details. ");
        return buffer.append(tail).toString();
    }

    @Test
    public void testManyUrlsAreFast() {
        // the regular expression needs minutes for this 130 kB
        String text = pageWithManyUrls(1000, "");
        assertTrue(text.length() > 100 * 1024);
        assertTimeout(ofSeconds(2), () -> assertNull(googleMaps.find(text)));
        assertTimeout(ofSeconds(2), () -> assertNull(geoHack.find(text)));
    }

    @Test
    public void testManyUrlsWithTailAreFastAndFound() {
        String googleMapsText = pageWithManyUrls(1000, " https://www.google.com/maps/dir/a/b/ end");
        assertTimeout(ofSeconds(2), () -> assertEquals("/dir/a/b/", googleMaps.find(googleMapsText)));
        String geoHackText = pageWithManyUrls(1000, " https://tools.wmflabs.org/geohack/geohack.php?params=1_N end");
        assertTimeout(ofSeconds(2), () -> assertEquals("params=1_N", geoHack.find(geoHackText)));
    }
}
