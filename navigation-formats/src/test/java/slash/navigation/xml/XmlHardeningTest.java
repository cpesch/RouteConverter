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

    Copyright (C) 2026 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.xml;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.kml.Kml20Format;
import slash.navigation.kml.Kml21Format;
import slash.navigation.kml.Kml22BetaFormat;
import slash.navigation.kml.Kml22Format;
import slash.navigation.rtz.RtzFormat;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Hardening of the XML read path for an untrusted upload (the online converter): a DOCTYPE with
 * an external entity or external DTD must neither read a local file nor open a network
 * connection, and an entity expansion bomb must be rejected quickly.
 */
public class XmlHardeningTest {
    private static final String SECRET = "TOP-SECRET-MARKER-4711";
    private final NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());
    private ServerSocket server;
    private final AtomicInteger connections = new AtomicInteger();
    private File secretFile;

    @Before
    public void setUp() throws IOException {
        server = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
        Thread acceptor = new Thread(() -> {
            try {
                while (true) {
                    Socket socket = server.accept();
                    connections.incrementAndGet();
                    socket.close();
                }
            } catch (IOException e) {
                // server closed
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
        secretFile = File.createTempFile("secret", ".txt");
        Files.writeString(secretFile.toPath(), SECRET);
    }

    @After
    public void tearDown() throws IOException {
        server.close();
        //noinspection ResultOfMethodCallIgnored
        secretFile.delete();
    }

    private static final String GPX11 = "<gpx version=\"1.1\" creator=\"x\" xmlns=\"http://www.topografix.com/GPX/1/1\">" +
            "<wpt lat=\"1\" lon=\"2\"><name>&xxe;</name><desc>&xxe;</desc></wpt></gpx>";
    private static final String GPX10 = "<gpx version=\"1.0\" creator=\"x\" xmlns=\"http://www.topografix.com/GPX/1/0\">" +
            "<wpt lat=\"1\" lon=\"2\"><name>&xxe;</name><desc>&xxe;</desc></wpt></gpx>";
    private static final String KML = "<kml xmlns=\"%s\"><Document><name>&xxe;</name>" +
            "<Placemark><name>&xxe;</name><description>&xxe;</description><Point><coordinates>2,1,0</coordinates></Point></Placemark></Document></kml>";
    private static final String RTZ = "<route version=\"1.0\" xmlns=\"http://www.cirm.org/RTZ/1/0\"><routeInfo routeName=\"&xxe;\"/>" +
            "<waypoints><waypoint id=\"1\" name=\"&xxe;\"><position lat=\"1\" lon=\"2\"/></waypoint></waypoints></route>";
    private static final String TCX1 = "<TrainingCenterDatabase xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1\">" +
            "<Courses><Course><Name>&xxe;</Name><Lap><TotalTimeSeconds>1</TotalTimeSeconds></Lap><Track><Trackpoint><Position>" +
            "<LatitudeDegrees>1</LatitudeDegrees><LongitudeDegrees>2</LongitudeDegrees></Position></Trackpoint></Track></Course></Courses></TrainingCenterDatabase>";
    private static final String TCX2 = TCX1.replace("/v1", "/v2");


    private List<Object[]> cases() {
        return asList(
                new Object[]{new Gpx11Format(), GPX11},
                new Object[]{new Gpx10Format(), GPX10},
                new Object[]{new Kml22Format(), KML.replace("%s", "http://www.opengis.net/kml/2.2")},
                new Object[]{new Kml22BetaFormat(), KML.replace("%s", "http://earth.google.com/kml/2.2")},
                new Object[]{new Kml21Format(), KML.replace("%s", "http://earth.google.com/kml/2.1")},
                new Object[]{new Kml20Format(), KML.replace("%s", "http://earth.google.com/kml/2.0")},
                new Object[]{new RtzFormat(), RTZ},
                new Object[]{new Tcx1Format(), TCX1},
                new Object[]{new Tcx2Format(), TCX2});
    }

    private String read(NavigationFormat<?> format, String xml) throws IOException {
        File file = File.createTempFile("hardening", ".xml");
        try {
            Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
            ParserResult result;
            try {
                result = parser.read(file, singletonList(format));
            } catch (IOException | RuntimeException e) {
                return "";
            }
            if (result == null || !result.isSuccessful())
                return "";
            StringBuilder content = new StringBuilder();
            for (BaseRoute<?, ?> route : result.getAllRoutes()) {
                content.append(route.getName()).append('\n');
                for (Object position : route.getPositions())
                    content.append(position).append('\n');
            }
            return content.toString();
        } finally {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    @Test
    public void externalFileEntityIsNotResolved() throws IOException {
        String doctype = "<!DOCTYPE x [<!ENTITY xxe SYSTEM \"" + secretFile.toURI() + "\">]>";
        for (Object[] c : cases()) {
            String xml = "<?xml version=\"1.0\"?>" + doctype + c[1];
            String content = read((NavigationFormat<?>) c[0], xml);
            assertFalse(c[0] + " leaked the local file", content.contains(SECRET));
        }
    }

    @Test
    public void externalNetworkEntityIsNotFetched() throws IOException {
        String url = "http://127.0.0.1:" + server.getLocalPort() + "/entity";
        String doctype = "<!DOCTYPE x [<!ENTITY xxe SYSTEM \"" + url + "\">]>";
        for (Object[] c : cases())
            read((NavigationFormat<?>) c[0], "<?xml version=\"1.0\"?>" + doctype + c[1]);
        assertEquals("external entity was fetched over the network", 0, connections.get());
    }

    @Test
    public void externalDtdIsNotFetched() throws IOException {
        String url = "http://127.0.0.1:" + server.getLocalPort() + "/evil.dtd";
        String doctype = "<!DOCTYPE x SYSTEM \"" + url + "\">";
        for (Object[] c : cases())
            read((NavigationFormat<?>) c[0], "<?xml version=\"1.0\"?>" + doctype + ((String) c[1]).replace("&xxe;", "x"));
        assertEquals("external DTD was fetched over the network", 0, connections.get());
    }

    @Test
    public void entityExpansionBombIsRejectedQuickly() throws IOException {
        StringBuilder doctype = new StringBuilder("<!DOCTYPE x [<!ENTITY l0 \"lollollollollollollollollollol\">");
        for (int i = 1; i <= 9; i++) {
            doctype.append("<!ENTITY l").append(i).append(" \"");
            for (int j = 0; j < 10; j++)
                doctype.append("&l").append(i - 1).append(';');
            doctype.append("\">");
        }
        doctype.append("]>");
        long start = System.currentTimeMillis();
        for (Object[] c : cases()) {
            String content = read((NavigationFormat<?>) c[0],
                    "<?xml version=\"1.0\"?>" + doctype + ((String) c[1]).replace("&xxe;", "&l9;"));
            assertTrue(c[0] + " expanded the bomb", content.length() < 1_000_000);
        }
        assertTrue("bomb took too long", System.currentTimeMillis() - start < 10_000);
    }
}
