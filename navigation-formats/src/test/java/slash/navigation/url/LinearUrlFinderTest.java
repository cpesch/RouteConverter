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
        assertSame("http://a.google.b/maps?x\u2028 tail");
        assertSame("http://a.google.b/maps?x\u2028tail");
        assertSame("head\u0085 http://a.google.b/maps?x");
        assertSame("http://a.google.b/maps\n?x");
        assertSame("http://a.google.b\u2029/maps?x");
        assertSame("http://x.org/geohack?a\u0085b");
    }

    @Test
    public void testSameAsPatternOnRandomTexts() {
        String[] tokens = {"http://", "https://", "http", "://", ".google.", "google", ".org/geohack", "/maps", "maps", "?", "x",
                "a", ".", "/", " ", "\t", "\u000B", "\f", "\u0085", "\u2028", "\u2029", "\n", "\r", "ftp://", "%", "&"};
        Random random = new Random(20261124L);
        for (int i = 0; i < 20000; i++) {
            StringBuilder buffer = new StringBuilder();
            int count = random.nextInt(14);
            for (int j = 0; j < count; j++)
                buffer.append(tokens[random.nextInt(tokens.length)]);
            assertSame(buffer.toString());
        }
    }

    // valid parts appear more often, so that many texts match each pattern
    private static final String[] SCHEMES = {"http://", "https://", "http://", "https://", "http:/", "ftp://"};
    private static final String[] HOSTS = {"", "a", "www", "x.y", "www"};
    private static final String[] MIDDLES = {".google.", ".org/geohack", ".google.", ".org/geohack", ".google", ".org/geo"};
    private static final String[] GAPS = {"", "c", "com", "de/x", " ", "\t"};
    private static final String[] MARKERS = {"/maps", "?", "/maps", "?", "/map", "/maps?", "?a=?b"};
    private static final String[] TAILS = {"", "x", "?q=1,2", "/dir/a/b/", "params=1_N", "x", "?q=1,2", " ", "\u2028"};
    private static final String[] NOISE = {"", " ", "see ", "<a href=\"", "\"> and ", "\t", " ", "", "/maps", "?", ".google.", "\n"};

    private static String pick(Random random, String[] values) {
        return values[random.nextInt(values.length)];
    }

    // texts made of one to four URL-like runs (scheme, host, middle, gap, marker, tail) between noise,
    // so that many of them match the patterns, often with several candidates for the scheme and marker
    @Test
    public void testSameAsPatternOnUrlLikeTexts() {
        Random random = new Random(20261126L);
        int texts = 20000, googleMapsMatches = 0, geoHackMatches = 0;
        for (int i = 0; i < texts; i++) {
            StringBuilder buffer = new StringBuilder(pick(random, NOISE));
            int urls = 1 + random.nextInt(4);
            for (int j = 0; j < urls; j++)
                buffer.append(pick(random, SCHEMES)).append(pick(random, HOSTS)).append(pick(random, MIDDLES)).
                        append(pick(random, GAPS)).append(pick(random, MARKERS)).append(pick(random, TAILS)).
                        append(pick(random, NOISE));
            String text = buffer.toString();
            assertSame(text);
            if (reference(GOOGLE_MAPS, text) != null)
                googleMapsMatches++;
            if (reference(GEO_HACK, text) != null)
                geoHackMatches++;
        }
        // guard against a generator that only produces texts no pattern matches
        assertTrue(googleMapsMatches > texts / 10, "Google Maps matches: " + googleMapsMatches);
        assertTrue(geoHackMatches > texts / 10, "GeoHack matches: " + geoHackMatches);
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
