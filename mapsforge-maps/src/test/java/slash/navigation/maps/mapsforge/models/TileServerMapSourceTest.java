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
package slash.navigation.maps.mapsforge.models;

import org.junit.Test;
import org.mapsforge.core.model.Tile;
import slash.navigation.maps.tileserver.TileServer;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static slash.navigation.maps.mapsforge.models.TileServerMapSource.appendApiKey;
import static slash.navigation.maps.mapsforge.models.TileServerMapSource.isPlainApiKey;
import static slash.navigation.maps.mapsforge.models.TileServerMapSource.userAgent;

public class TileServerMapSourceTest {

    @Test
    public void testNullApiKeyLeavesUrlUnchanged() {
        assertEquals("https://example.com/tile", appendApiKey("https://example.com/tile", null, false));
    }

    @Test
    public void testNonMapboxWithoutQueryAppendsApiKey() {
        assertEquals("https://example.com/tile?apikey=key", appendApiKey("https://example.com/tile", "key", false));
    }

    @Test
    public void testNonMapboxWithExistingQueryAppendsApiKey() {
        assertEquals("https://example.com/tile?a=b&apikey=key", appendApiKey("https://example.com/tile?a=b", "key", false));
    }

    @Test
    public void testMapboxWithoutQueryAppendsAccessToken() {
        assertEquals("https://example.com/tile?access_token=key", appendApiKey("https://example.com/tile", "key", true));
    }

    @Test
    public void testMapboxWithExistingQueryAppendsAccessToken() {
        assertEquals("https://example.com/tile?a=b&access_token=key", appendApiKey("https://example.com/tile?a=b", "key", true));
    }

    @Test
    public void testAppendApiKeyEncodesCharactersThatWouldBreakTheQueryString() {
        String malformedApiKey = "https://attacker.example/?redirect=1 2";
        String expected = "https://example.com/tile?apikey=" + URLEncoder.encode(malformedApiKey, StandardCharsets.UTF_8);
        assertEquals(expected, appendApiKey("https://example.com/tile", malformedApiKey, false));
    }

    @Test
    public void testIsPlainApiKeyRejectsKeysThatLookLikeAUrlOrTemplate() {
        assertFalse(isPlainApiKey("https://attacker.example/?apikey=1"));
        assertFalse(isPlainApiKey("key/with/slashes"));
        assertFalse(isPlainApiKey("key:with:colons"));
        assertFalse(isPlainApiKey("key with space"));
        assertFalse(isPlainApiKey("{apikey}"));
    }

    @Test
    public void testIsPlainApiKeyAcceptsPlainTokens() {
        assertTrue(isPlainApiKey(null));
        assertTrue(isPlainApiKey("abcDEF123-_."));
    }

    private static TileServer tileServer(String urlPattern) {
        return new TileServer("test", "Test Tile Server", urlPattern, List.of("tiles.example.org"),
                true, 0, 18, "copyright", "Copyright Text");
    }

    private static Tile tile() {
        return new Tile(0, 0, (byte) 0, 256);
    }

    private interface TileUrlCalls {
        void run() throws Exception;
    }

    private static List<LogRecord> recordLogs(TileUrlCalls calls) throws Exception {
        Logger logger = Logger.getLogger(TileServerMapSource.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            public void publish(LogRecord record) {
                records.add(record);
            }

            public void flush() {
            }

            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            calls.run();
        } finally {
            logger.removeHandler(handler);
        }
        return records;
    }

    private static long warnings(List<LogRecord> records) {
        return records.stream().filter(r -> r.getLevel() == Level.WARNING).count();
    }

    @Test
    public void testGetTileUrlWithMalformedApiKeyLogsOneWarningAndDoesNotThrow() throws Exception {
        TileServerMapSource tileSource = new TileServerMapSource(tileServer("https://tiles.example.org/tile.png"));
        tileSource.setApiKey("https://attacker.example/?redirect=1 2");

        List<LogRecord> records = recordLogs(() -> {
            URL first = tileSource.getTileUrl(tile());
            URL second = tileSource.getTileUrl(tile());

            assertTrue(first.toString().startsWith("https://tiles.example.org/tile.png"));
            assertEquals(first.toString(), second.toString());
        });

        assertEquals(1, warnings(records));
    }

    @Test
    public void testGetTileUrlWithBrokenUrlPatternThrowsCheckedExceptionWithoutTheApiKey() throws Exception {
        TileServerMapSource tileSource = new TileServerMapSource(tileServer("https://tiles.example.org/broken pattern/tile.png"));
        String apiKey = "f00dfeedcafe1234";
        tileSource.setApiKey(apiKey);

        List<MalformedURLException> exceptions = new ArrayList<>();
        List<LogRecord> records = recordLogs(() -> {
            exceptions.add(assertThrows(MalformedURLException.class, () -> tileSource.getTileUrl(tile())));
            exceptions.add(assertThrows(MalformedURLException.class, () -> tileSource.getTileUrl(tile())));
        });

        assertEquals(1, warnings(records));
        assertFalse(records.get(0).getMessage().contains(apiKey));
        for (MalformedURLException exception : exceptions) {
            assertTrue(exception.getMessage().contains("invalid tile URL pattern"));
            assertFalse(exception.getMessage().contains(apiKey));
            // the IllegalArgumentException quotes the whole URL including the key, so it must not be chained
            assertNull(exception.getCause());
        }
    }

    @Test
    public void testGetTileUrlWithKeyNeedingEncodingProducesAValidUri() throws Exception {
        TileServerMapSource tileSource = new TileServerMapSource(tileServer("https://tiles.example.org/tile.png"));
        tileSource.setApiKey("key with space & special=chars");

        URL url = tileSource.getTileUrl(tile());

        assertTrue(url.toString().contains("apikey="));
        assertFalse(url.toString().contains(" "));
    }

    private static final String ROUTE_CONVERTER_USER_AGENT =
            "RouteConverter/" + System.getProperty("rest", "3.0") + " (+https://www.routeconverter.com/)";

    @Test
    public void testIdentifiesAsRouteConverterToOrdinaryTileServers() {
        assertEquals(ROUTE_CONVERTER_USER_AGENT, userAgent(new String[]{"tile.openstreetmap.org"}));
    }

    @Test
    public void testIdentifiesAsJavaToOutdoorActiveHosts() {
        // OutdoorActive 404s any User-Agent containing "routeconverter", so those hosts get the JDK default
        assertTrue(userAgent(new String[]{"w0.oastatic.com"}).startsWith("Java/"));
    }

    @Test
    public void testIdentifiesAsJavaToOutdoorActiveComHosts() {
        // the CH Swissmap and Outdoor Topo entries use outdooractive.com, which blocks identically
        assertTrue(userAgent(new String[]{"w0.outdooractive.com"}).startsWith("Java/"));
    }

    @Test
    public void testIdentifiesAsJavaWhenAnyHostIsOutdoorActive() {
        assertTrue(userAgent(new String[]{"w0.example.org", "w1.oastatic.com"}).startsWith("Java/"));
    }

    @Test
    public void testOutdoorActiveMatchIsCaseInsensitive() {
        assertTrue(userAgent(new String[]{"W2.OaStatic.Com"}).startsWith("Java/"));
    }

    @Test
    public void testLookalikeHostsStillGetTheRouteConverterUserAgent() {
        assertEquals(ROUTE_CONVERTER_USER_AGENT, userAgent(new String[]{"notoastatic.com"}));
        assertEquals(ROUTE_CONVERTER_USER_AGENT, userAgent(new String[]{"oastatic.com.example.org"}));
        assertEquals(ROUTE_CONVERTER_USER_AGENT, userAgent(new String[]{"myoutdooractive.com"}));
    }
}
