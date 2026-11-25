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

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.gpx.Gpx11Format;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class UrlFormatTest {
    private static final String GOOGLE_MAPS_ROUTE_URL = "http://maps.google.de/maps?f=d&saddr=Hamburg%2FUhlenhorst&daddr=Hauptstra%C3%9Fe%2FL160+to:53.588429,10.419159+to:Breitenfelde%2FNeuenlande&hl=de&geocode=%3BFVy1MQMdDoudAA%3B%3B&mra=dpe&mrcr=0&mrsp=2&sz=11&via=1,2&sll=53.582575,10.30528&sspn=0.234798,0.715485&ie=UTF8&z=11";
    private static final String GOOGLE_MAPS_EMAIL = "Betreff: Route nach/zu Riehler Strasse 190 50735 Koeln (Google Maps)\n" +
            "\n" +
            "> Routenplaner\n" +
            "> Link:\n" +
            "http://maps.google.de/maps?f=d&hl=de&geocode=&saddr=H%C3%B6lderlinstra%C3%9Fe,+51545+Br%C3%B6l,+Oberbergischer+Kreis,+Nordrhein-Westfalen,+Deutschland&daddr=L339%2FWuppertaler+Stra%C3%9Fe+%4050.918890,+7.560880+to%3AK%C3%B6ln,+Riehler+Str.+190&mrcr=2&mra=mr&sll=50.954318,7.311401&sspn=0.142091,0.32135&ie=UTF8&ll=50.952371,7.261276&spn=0.284193,0.6427&z=11&om=1 \n" +
            ">\n" +
            "> Startadresse: Hoelderlinstrasse 51545 Broel\n" +
            "> Zieladresse: Riehler Strasse 190 50735 Koeln";

    private static final String GPX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<gpx version=\"1.1\" creator=\"test\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n" +
            "<wpt lat=\"53.5\" lon=\"10.0\"><name>One</name></wpt>\n" +
            "<wpt lat=\"53.6\" lon=\"10.1\"><name>Two</name></wpt>\n" +
            "</gpx>\n";

    private static final String KML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><name>Links</name>\n" +
            "<NetworkLink><name>Remote</name><Link><href>%s</href></Link></NetworkLink>\n" +
            "</Document></kml>\n";

    @TempDir
    Path temporary;

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private final CountDownLatch release = new CountDownLatch(1);
    private final NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());
    private final UrlFormat format = new UrlFormat();

    @BeforeEach
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/route.gpx", exchange -> {
            requests.incrementAndGet();
            byte[] bytes = GPX.getBytes(UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.createContext("/silent", exchange -> {
            requests.incrementAndGet();
            try {
                release.await();
            } catch (InterruptedException ignored) {
            }
            exchange.close();
        });
        server.createContext("/big-declared", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, 10_000);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(new byte[10_000]);
            }
        });
        server.createContext("/big-chunked", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(new byte[10_000]);
            }
        });
        server.createContext("/self", exchange -> respond(exchange, shortcut(url("/self"))));
        server.createContext("/loop-a", exchange -> respond(exchange, shortcut(url("/loop-b"))));
        server.createContext("/loop-b", exchange -> respond(exchange, shortcut(url("/loop-a"))));
        server.createContext("/kml-self", exchange -> respond(exchange, String.format(KML, url("/kml-self"))));
        server.createContext("/chain-1", exchange -> respond(exchange, shortcut(url("/chain-2"))));
        server.createContext("/chain-2", exchange -> respond(exchange, shortcut(url("/chain-3"))));
        server.createContext("/chain-3", exchange -> respond(exchange, shortcut(url("/chain-4"))));
        server.createContext("/chain-4", exchange -> respond(exchange, shortcut(url("/route.gpx"))));
        server.start();
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        requests.incrementAndGet();
        byte[] bytes = body.getBytes(UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @AfterEach
    public void stopServer() {
        release.countDown();
        server.stop(0);
    }

    private String url(String path) {
        return "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + server.getAddress().getPort() + path;
    }

    private static String shortcut(String url) {
        return "[InternetShortcut]\r\nURL=" + url + "\r\nIconIndex=1\r\n";
    }

    private static void assertNotRead(ParserResult result) {
        if (result.isSuccessful()) {
            assertNotEquals(UrlFormat.class, result.getFormat().getClass());
            assertNotEquals(Gpx11Format.class, result.getFormat().getClass());
            assertEquals(0, result.getTheRoute().getPositionCount());
            assertFalse(result.getFormat().getClass().getSimpleName().startsWith("Kml"));
        }
    }

    @Test
    public void readGoogleMapsUrl() throws IOException {
        ParserResult result = parser.read(GOOGLE_MAPS_ROUTE_URL);
        assertNotNull(result);
        assertEquals(1, result.getAllRoutes().size());
        assertEquals(4, result.getTheRoute().getPositionCount());
        assertEquals(GoogleMapsUrlFormat.class, result.getFormat().getClass());
    }

    @Test
    public void googleMapsLinkInEmailIsStillReadWithoutDownload() throws IOException {
        ParserResult result = parser.read(GOOGLE_MAPS_EMAIL);
        assertEquals(GoogleMapsUrlFormat.class, result.getFormat().getClass());
        assertEquals(3, result.getTheRoute().getPositionCount());
    }

    @Test
    public void shortcutFileIsFollowed() throws IOException {
        Path file = temporary.resolve("route.url");
        Files.writeString(file, shortcut(url("/route.gpx")), UTF_8);

        ParserResult result = parser.read(file.toFile());
        assertTrue(result.isSuccessful());
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
        assertEquals(2, result.getTheRoute().getPositionCount());
        assertEquals(1, requests.get());
    }

    @Test
    public void shortcutWithoutFileNameIsFollowed() throws IOException {
        // e.g. an entry of a ZIP or a stream: detection by content, not by extension
        ParserResult result = parser.read(shortcut(url("/route.gpx")));
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
        assertEquals(2, result.getTheRoute().getPositionCount());
        assertEquals(1, requests.get());
    }

    @Test
    public void shortcutSectionAndKeyAreCaseInsensitiveAndMayHaveABom() throws IOException {
        ParserResult result = parser.read("\uFEFF[internetshortcut]\nurl = " + url("/route.gpx") + "\n");
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
        assertEquals(1, requests.get());
    }

    @Test
    public void textWithUrlsIsNotFollowed() throws IOException {
        String link = url("/route.gpx");
        String[] texts = {
                "README\nSee " + link + " for more\n",
                "From: me@example.com\nSubject: Route\n\nHave a look at <" + link + ">\n",
                "name,link\nHome," + link + "\n",
                "{\"url\": \"" + link + "\"}",
                "<html><body><a href=\"" + link + "\">route</a></body></html>",
                // not in the shortcut section
                "[Other]\nURL=" + link + "\n",
                "[InternetShortcut]\nIconIndex=1\n[Other]\nURL=" + link + "\n",
                // a URL line without a section
                "URL=" + link + "\n",
                link
        };
        for (String text : texts) {
            assertNull(format.findURL(text), text);
            assertNotRead(parser.read(text));
        }
        assertEquals(0, requests.get());
    }

    @Test
    public void textWithUrlsInFileNamedUrlWithoutSectionIsNotFollowed() throws IOException {
        Path file = temporary.resolve("mail.url");
        Files.writeString(file, "Betreff: Route\n\nLink: " + url("/route.gpx") + "\n", UTF_8);
        assertNotRead(parser.read(file.toFile()));
        assertEquals(0, requests.get());
    }

    @Test
    public void onlyHttpAndHttpsAreFollowed() throws IOException {
        Path target = temporary.resolve("target.gpx");
        Files.writeString(target, GPX, UTF_8);

        assertEquals("http://example.com/a?b=c", format.findURL(shortcut("http://example.com/a?b=c")));
        assertEquals("https://example.com/a", format.findURL(shortcut("https://example.com/a")));
        assertEquals("HTTPS://example.com/a", format.findURL(shortcut("HTTPS://example.com/a")));

        String[] urls = {target.toUri().toString(), "file:///etc/hostname", "ftp://example.com/route.gpx",
                "ftps://example.com/route.gpx", "mailto:me@example.com", "news://example.com/group", "jar:file:///x.jar!/y"};
        for (String url : urls) {
            assertNull(format.findURL(shortcut(url)), url);
            assertNotRead(parser.read(shortcut(url)));
        }
    }

    @Test
    public void firstUrlLineOfTheShortcutSectionCounts() {
        assertNull(format.findURL("[InternetShortcut]\nURL=file:///a.gpx\nURL=http://example.com/\n"));
        assertEquals("http://example.com/", format.findURL("[Other]\nURL=file:///a.gpx\n[InternetShortcut]\nURL=http://example.com/\n"));
    }

    @Test
    public void silentServerHitsTheReadTimeout() throws IOException {
        NavigationFormatParser quick = new NavigationFormatParser(new AllNavigationFormatRegistry(), 500, 300, 1024 * 1024);
        long start = System.nanoTime();
        ParserResult result = quick.read(shortcut(url("/silent")));
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertNotRead(result);
        assertEquals(1, requests.get());
        assertTrue(millis < 2000, "took " + millis + " ms");
    }

    @Test
    public void oversizedResponseIsDeclined() throws IOException {
        NavigationFormatParser small = new NavigationFormatParser(new AllNavigationFormatRegistry(), 1000, 1000, 1000);
        assertNotRead(small.read(shortcut(url("/big-declared"))));
        assertNotRead(small.read(shortcut(url("/big-chunked"))));
        assertEquals(2, requests.get());
        // exactly at the limit is fine
        NavigationFormatParser fits = new NavigationFormatParser(new AllNavigationFormatRegistry(), 1000, 1000, GPX.length());
        assertEquals(Gpx11Format.class, fits.read(shortcut(url("/route.gpx"))).getFormat().getClass());
    }

    @Test
    public void kmlNetworkLinkIsStillFollowed() throws IOException {
        ParserResult result = parser.read(String.format(KML, url("/route.gpx")));
        assertTrue(result.isSuccessful());
        assertEquals(1, requests.get());
        assertEquals(2, result.getAllRoutes().stream().mapToInt(r -> r.getPositionCount()).sum());
    }

    @Test
    public void kmlNetworkLinkIsLimitedToo() throws IOException {
        NavigationFormatParser small = new NavigationFormatParser(new AllNavigationFormatRegistry(), 1000, 1000, 1000);
        assertNotRead(small.read(String.format(KML, url("/big-chunked"))));
        // every KML flavour tried follows the link on its own
        assertTrue(requests.get() >= 1 && requests.get() <= 10, "requests " + requests.get());
    }

    @Test
    public void selfReferencingShortcutIsDeclined() throws IOException {
        assertNotRead(parser.read(shortcut(url("/self"))));
        assertEquals(1, requests.get());
    }

    @Test
    public void shortcutLoopIsDeclined() throws IOException {
        assertNotRead(parser.read(shortcut(url("/loop-a"))));
        assertEquals(2, requests.get());
    }

    @Test
    public void tooLongChainOfShortcutsIsDeclined() throws IOException {
        // chain-1 -> chain-2 -> chain-3 -> chain-4 -> route.gpx: the third hop is the last one followed
        assertNotRead(parser.read(shortcut(url("/chain-1"))));
        assertEquals(3, requests.get());
    }

    @Test
    public void chainOfShortcutsWithinTheLimitIsFollowed() throws IOException {
        ParserResult result = parser.read(shortcut(url("/chain-3")));
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
        assertEquals(2, result.getTheRoute().getPositionCount());
        assertEquals(3, requests.get());
    }

    @Test
    public void kmlNetworkLinkToItselfIsDeclined() throws IOException {
        assertNotRead(parser.read(String.format(KML, url("/kml-self"))));
        assertTrue(requests.get() >= 1 && requests.get() <= 10, "requests " + requests.get());
    }
}
