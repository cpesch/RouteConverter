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
package slash.navigation.gpx;

import org.junit.Test;
import slash.navigation.gpx.binding10.Gpx;
import slash.navigation.gpx.binding11.GpxType;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;
import static slash.navigation.gpx.GpxUtil.*;

/**
 * Tests for {@link GpxUtil} marshal/unmarshal round-trips for GPX 1.0 and GPX 1.1.
 *
 * @author Christian Pesch
 */
public class GpxUtilTest {

    // ---- namespace URI constants ----

    @Test
    public void testNamespaceUriConstants() {
        assertEquals("http://www.topografix.com/GPX/1/0", GPX_10_NAMESPACE_URI);
        assertEquals("http://www.topografix.com/GPX/1/1", GPX_11_NAMESPACE_URI);
        assertEquals("http://www.garmin.com/xmlschemas/GpxExtensions/v3", GARMIN_EXTENSIONS_3_NAMESPACE_URI);
        assertEquals("http://www.garmin.com/xmlschemas/TrackPointExtension/v1", GARMIN_TRACKPOINT_EXTENSIONS_1_NAMESPACE_URI);
        assertEquals("http://www.garmin.com/xmlschemas/TrackPointExtension/v2", GARMIN_TRACKPOINT_EXTENSIONS_2_NAMESPACE_URI);
        assertEquals("http://www.garmin.com/xmlschemas/TripExtensions/v1", GARMIN_TRIP_EXTENSIONS_1_NAMESPACE_URI);
        assertEquals("https://osmand.net/docs/technical/osmand-file-formats/osmand-gpx", OSMAND_EXTENSIONS_NAMESPACE_URI);
        assertEquals("http://trekbuddy.net/2009/01/gpx/nmea", TREKBUDDY_EXTENSIONS_0984_NAMESPACE_URI);
    }

    // ---- GPX 1.0 ----

    private static final String GPX_10_DOC =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" version=\"1.0\" creator=\"test\"/>";

    @Test
    public void testUnmarshal10FromReader() throws IOException {
        Gpx gpx = unmarshal10(new StringReader(GPX_10_DOC));
        assertNotNull(gpx);
        assertEquals("1.0", gpx.getVersion());
        assertEquals("test", gpx.getCreator());
    }

    @Test
    public void testUnmarshal10FromStream() throws IOException {
        Gpx gpx = unmarshal10(new ByteArrayInputStream(GPX_10_DOC.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(gpx);
        assertEquals("1.0", gpx.getVersion());
    }

    @Test
    public void testMarshal10RoundTrip() throws IOException, JAXBException {
        Gpx gpx = unmarshal10(new StringReader(GPX_10_DOC));
        assertNotNull(gpx);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal10(gpx, out);
        String xml = out.toString(StandardCharsets.UTF_8);
        assertTrue("marshalled XML should contain gpx element", xml.contains("gpx"));
        assertTrue("marshalled XML should contain GPX 1.0 namespace", xml.contains(GPX_10_NAMESPACE_URI));
    }

    @Test(expected = IOException.class)
    public void testUnmarshal10BadXml() throws IOException {
        unmarshal10(new StringReader("<notvalid/>"));
    }

    // ---- GPX 1.1 ----

    private static final String GPX_11_DOC =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"test\"/>";

    @Test
    public void testUnmarshal11FromString() throws IOException {
        GpxType gpx = unmarshal11(GPX_11_DOC);
        assertNotNull(gpx);
        assertEquals("1.1", gpx.getVersion());
        assertEquals("test", gpx.getCreator());
    }

    @Test
    public void testUnmarshal11FromReader() throws IOException {
        GpxType gpx = unmarshal11(new StringReader(GPX_11_DOC));
        assertNotNull(gpx);
        assertEquals("1.1", gpx.getVersion());
    }

    @Test
    public void testUnmarshal11FromStream() throws IOException {
        GpxType gpx = unmarshal11(new ByteArrayInputStream(GPX_11_DOC.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(gpx);
        assertEquals("1.1", gpx.getVersion());
    }

    @Test
    public void testMarshal11ToOutputStream() throws IOException, JAXBException {
        GpxType gpx = unmarshal11(GPX_11_DOC);
        assertNotNull(gpx);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal11(gpx, out);
        String xml = out.toString(StandardCharsets.UTF_8);
        assertTrue("marshalled XML should contain gpx element", xml.contains("gpx"));
    }

    @Test
    public void testToXml() throws IOException {
        GpxType gpx = unmarshal11(GPX_11_DOC);
        assertNotNull(gpx);
        String xml = toXml(gpx);
        assertNotNull(xml);
        assertTrue("toXml result should contain gpx element", xml.contains("gpx"));
        assertTrue("toXml result should contain version", xml.contains("1.1"));
    }

    @Test(expected = IOException.class)
    public void testUnmarshal11BadXml() throws IOException {
        unmarshal11("<notvalid/>");
    }

    // ---- trekbuddy nmea extensions (binding must be registered in newContext11) ----

    @Test
    public void testUnmarshal11BindsTrekbuddyNmeaExtensions() throws IOException {
        String gpx =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"trekbuddy-test\">" +
                "<trk><trkseg>" +
                "<trkpt lat=\"48.0\" lon=\"11.0\">" +
                "<extensions xmlns:nmea=\"http://trekbuddy.net/2009/01/gpx/nmea\">" +
                "<nmea:speed>12.5</nmea:speed>" +
                "<nmea:course>270.0</nmea:course>" +
                "</extensions>" +
                "</trkpt>" +
                "</trkseg></trk>" +
                "</gpx>";
        GpxType type = unmarshal11(gpx);
        assertNotNull(type);

        // With slash.navigation.gpx.trekbuddy.ObjectFactory registered in newContext11(), the
        // lax @XmlAnyElement extensions bind nmea:speed/course to JAXBElement<BigDecimal>;
        // without it they would fall back to a generic DOM Element (dead binding).
        List<Object> any = type.getTrk().get(0).getTrkseg().get(0).getTrkpt().get(0).getExtensions().getAny();
        BigDecimal speed = null, course = null;
        for (Object o : any) {
            if (o instanceof JAXBElement) {
                JAXBElement<?> element = (JAXBElement<?>) o;
                if ("speed".equals(element.getName().getLocalPart()))
                    speed = (BigDecimal) element.getValue();
                else if ("course".equals(element.getName().getLocalPart()))
                    course = (BigDecimal) element.getValue();
            }
        }
        assertEquals("nmea:speed should bind to a JAXBElement<BigDecimal>", new BigDecimal("12.5"), speed);
        assertEquals("nmea:course should bind to a JAXBElement<BigDecimal>", new BigDecimal("270.0"), course);
    }

    // ---- every registered extension ObjectFactory must produce a live binding ----

    private static String gpx11WithTrkptExtension(String prefix, String namespace, String elementXml) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"ext-test\">" +
                "<trk><trkseg>" +
                "<trkpt lat=\"48.0\" lon=\"11.0\">" +
                "<extensions xmlns:" + prefix + "=\"" + namespace + "\">" + elementXml + "</extensions>" +
                "</trkpt>" +
                "</trkseg></trk>" +
                "</gpx>";
    }

    private static boolean trkptExtensionBindsTo(GpxType type, String namespace, String localName) {
        List<Object> any = type.getTrk().get(0).getTrkseg().get(0).getTrkpt().get(0).getExtensions().getAny();
        for (Object o : any)
            if (o instanceof JAXBElement) {
                javax.xml.namespace.QName name = ((JAXBElement<?>) o).getName();
                if (namespace.equals(name.getNamespaceURI()) && localName.equals(name.getLocalPart()))
                    return true;
            }
        return false;
    }

    @Test
    public void testAllTopLevelExtensionBindingsAreRegistered() throws IOException {
        // namespace, xmlns prefix, element XML, top-level element local name (one per ObjectFactory in newContext11)
        String[][] cases = {
                {GARMIN_EXTENSIONS_3_NAMESPACE_URI, "gpxx", "<gpxx:TrackPointExtension/>", "TrackPointExtension"},
                {GARMIN_TRACKPOINT_EXTENSIONS_1_NAMESPACE_URI, "tpx1", "<tpx1:TrackPointExtension/>", "TrackPointExtension"},
                {GARMIN_TRACKPOINT_EXTENSIONS_2_NAMESPACE_URI, "tpx2", "<tpx2:TrackPointExtension/>", "TrackPointExtension"},
                {GARMIN_TRIP_EXTENSIONS_1_NAMESPACE_URI, "trp", "<trp:Trip/>", "Trip"},
                {TREKBUDDY_EXTENSIONS_0984_NAMESPACE_URI, "nmea", "<nmea:speed>12.5</nmea:speed>", "speed"},
        };
        for (String[] c : cases) {
            String namespace = c[0], prefix = c[1], elementXml = c[2], localName = c[3];
            GpxType type = unmarshal11(gpx11WithTrkptExtension(prefix, namespace, elementXml));
            assertNotNull(type);
            assertTrue(localName + " in " + namespace + " must bind to a JAXBElement (ObjectFactory registered in newContext11), not fall back to a DOM Element",
                    trkptExtensionBindsTo(type, namespace, localName));
        }
    }

    // ---- osmand track-appearance extensions (global elements, directly in <extensions>) ----

    private static final String GPX_11_OSMAND =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"osmand-test\" " +
            "xmlns:osmand=\"https://osmand.net/docs/technical/osmand-file-formats/osmand-gpx\">" +
            "<trk><trkseg>" +
            "<trkpt lat=\"52.397799\" lon=\"4.575998\"/>" +
            "</trkseg></trk>" +
            "<extensions>" +
            "<osmand:color>#4e4eff</osmand:color>" +
            "<osmand:width>bold</osmand:width>" +
            "<osmand:show_arrows>true</osmand:show_arrows>" +
            "<osmand:split_type>distance</osmand:split_type>" +
            "<osmand:split_interval>2000.0</osmand:split_interval>" +
            "</extensions>" +
            "</gpx>";

    @Test
    public void testUnmarshal11BindsOsmandTrackAppearanceExtensions() throws IOException {
        GpxType type = unmarshal11(GPX_11_OSMAND);
        assertNotNull(type);
        assertNotNull("gpx <extensions> should be present", type.getExtensions());

        // osmand writes its appearance elements directly in <extensions> (no wrapper). With the
        // global element decls (un-scoped in newContext11's osmand.ObjectFactory), they bind to
        // typed JAXBElements instead of falling back to DOM Elements.
        String color = null, width = null;
        Boolean showArrows = null;
        for (Object o : type.getExtensions().getAny()) {
            if (o instanceof JAXBElement) {
                JAXBElement<?> element = (JAXBElement<?>) o;
                String localName = element.getName().getLocalPart();
                if ("color".equals(localName))
                    color = (String) element.getValue();
                else if ("width".equals(localName))
                    width = (String) element.getValue();
                else if ("show_arrows".equals(localName))
                    showArrows = (Boolean) element.getValue();
            }
        }
        assertEquals("osmand:color must bind to JAXBElement<String>, not a DOM Element", "#4e4eff", color);
        assertEquals("bold", width);
        assertEquals(Boolean.TRUE, showArrows);
    }

    @Test
    public void testOsmandTrackAppearanceRoundTrip() throws IOException, JAXBException {
        GpxType type = unmarshal11(GPX_11_OSMAND);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal11(type, out);
        String xml = out.toString(StandardCharsets.UTF_8);
        assertTrue("round-trip should preserve the osmand namespace", xml.contains(OSMAND_EXTENSIONS_NAMESPACE_URI));
        assertTrue("round-trip should preserve osmand:color value", xml.contains("#4e4eff"));
        assertTrue("round-trip should preserve osmand:width value", xml.contains("bold"));
        assertTrue("round-trip should preserve osmand:split_interval value", xml.contains("2000.0"));
    }

    @Test
    public void testRealOsmandExportBindsTrackAppearance() throws IOException {
        // from-osmand.gpx is taken verbatim from a real OsmAnd~ 5.1.9 export.
        GpxType type;
        try (java.io.InputStream in = getClass().getResourceAsStream("/from-osmand.gpx")) {
            assertNotNull("from-osmand.gpx fixture must be on the test classpath", in);
            type = unmarshal11(in);
        }
        assertNotNull(type);
        assertNotNull("real osmand export has a top-level <extensions>", type.getExtensions());

        String color = null, width = null;
        Boolean showArrows = null;
        Object coloringType = null, splitType = null;
        boolean sawWidthElement = false;
        for (Object o : type.getExtensions().getAny()) {
            if (o instanceof JAXBElement) {
                JAXBElement<?> e = (JAXBElement<?>) o;
                switch (e.getName().getLocalPart()) {
                    case "color" -> color = (String) e.getValue();
                    case "width" -> { sawWidthElement = true; width = (String) e.getValue(); }
                    case "show_arrows" -> showArrows = (Boolean) e.getValue();
                    case "coloring_type" -> coloringType = e.getValue();
                    case "split_type" -> splitType = e.getValue();
                    default -> { }
                }
            }
        }
        assertEquals("osmand:color binds typed", "#4e4eff", color);
        assertEquals(Boolean.FALSE, showArrows);
        assertTrue("empty <osmand:width/> still binds (no parse failure)", sawWidthElement);
        assertTrue("empty width value is null or blank", width == null || width.isEmpty());
        assertNotNull("coloring_type binds to its enum", coloringType);
        assertEquals("solid", String.valueOf(coloringType).toLowerCase());
        assertNotNull("split_type binds to its enum", splitType);
    }

    // ---- garmin TripExtensions v1 (trip1) ----

    @Test
    public void testTrip1TripBindsAndRoundTrips() throws IOException, JAXBException {
        String gpx =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"trip1-test\" " +
                "xmlns:trp=\"http://www.garmin.com/xmlschemas/TripExtensions/v1\">" +
                "<rte>" +
                "<extensions><trp:Trip><trp:TransportationMode>Automotive</trp:TransportationMode></trp:Trip></extensions>" +
                "<rtept lat=\"48.0\" lon=\"11.0\"/>" +
                "</rte>" +
                "</gpx>";
        GpxType type = unmarshal11(gpx);
        String mode = null;
        for (Object o : type.getRte().get(0).getExtensions().getAny()) {
            if (o instanceof JAXBElement) {
                Object v = ((JAXBElement<?>) o).getValue();
                if (v instanceof slash.navigation.gpx.trip1.TripExtensionT trip)
                    mode = trip.getTransportationMode();
            }
        }
        assertEquals("trp:Trip must bind to TripExtensionT (trip1 registered)", "Automotive", mode);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal11(type, out);
        String xml = out.toString(StandardCharsets.UTF_8);
        assertTrue("round-trip preserves the TripExtensions namespace", xml.contains(GARMIN_TRIP_EXTENSIONS_1_NAMESPACE_URI));
        assertTrue("round-trip preserves TransportationMode", xml.contains("Automotive"));
    }

    @Test
    public void testRealGarminBasecampBindsTripAndRoutePointExtensions() throws IOException {
        // from-garmin-basecamp.gpx is a real "Garmin Desktop App" export (GPSBabel reference).
        GpxType type;
        try (java.io.InputStream in = getClass().getResourceAsStream("/from-garmin-basecamp.gpx")) {
            assertNotNull("from-garmin-basecamp.gpx must be on the test classpath", in);
            type = unmarshal11(in);
        }
        String calcMode = null;
        boolean routePointExtension = false;
        for (slash.navigation.gpx.binding11.WptType rtept : type.getRte().get(0).getRtept()) {
            for (Object o : rtept.getExtensions().getAny()) {
                if (o instanceof JAXBElement) {
                    Object v = ((JAXBElement<?>) o).getValue();
                    if (v instanceof slash.navigation.gpx.trip1.ViaPointExtensionT vp)
                        calcMode = vp.getCalculationMode();
                    else if (v instanceof slash.navigation.gpx.garmin3.RoutePointExtensionT)
                        routePointExtension = true;
                }
            }
        }
        assertEquals("trp:ViaPoint binds to ViaPointExtensionT (trip1)", "FasterTime", calcMode);
        assertTrue("gpxx:RoutePointExtension binds to RoutePointExtensionT (garmin3)", routePointExtension);
    }

    @Test
    public void testRealGarminTrackPointExtensionV2Binds() throws IOException {
        GpxType type;
        try (java.io.InputStream in = getClass().getResourceAsStream("/from-garmin-trackpoint.gpx")) {
            assertNotNull("from-garmin-trackpoint.gpx must be on the test classpath", in);
            type = unmarshal11(in);
        }
        Short hr = null, cad = null;
        Double atemp = null;
        slash.navigation.gpx.binding11.WptType trkpt =
                type.getTrk().get(0).getTrkseg().get(0).getTrkpt().get(0);
        for (Object o : trkpt.getExtensions().getAny()) {
            if (o instanceof JAXBElement) {
                Object v = ((JAXBElement<?>) o).getValue();
                if (v instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT tpx) {
                    hr = tpx.getHr();
                    cad = tpx.getCad();
                    atemp = tpx.getAtemp();
                }
            }
        }
        assertEquals("gpxtpx:hr binds", Short.valueOf((short) 118), hr);
        assertEquals("gpxtpx:cad binds", Short.valueOf((short) 84), cad);
        assertEquals("gpxtpx:atemp binds", Double.valueOf(21.0), atemp);
    }

    @Test
    public void testTrekbuddyNmeaExtensionsRoundTrip() throws IOException, JAXBException {
        String gpx =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"trekbuddy-test\">" +
                "<trk><trkseg>" +
                "<trkpt lat=\"48.0\" lon=\"11.0\">" +
                "<extensions xmlns:nmea=\"http://trekbuddy.net/2009/01/gpx/nmea\">" +
                "<nmea:speed>12.5</nmea:speed>" +
                "</extensions>" +
                "</trkpt>" +
                "</trkseg></trk>" +
                "</gpx>";
        GpxType type = unmarshal11(gpx);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal11(type, out);
        String xml = out.toString(StandardCharsets.UTF_8);
        assertTrue("round-trip should preserve the trekbuddy nmea namespace", xml.contains(TREKBUDDY_EXTENSIONS_0984_NAMESPACE_URI));
        assertTrue("round-trip should preserve the nmea:speed value", xml.contains("12.5"));
    }

    // ---- NamespaceFilter ----

    @Test
    public void testNamespaceFilterRemapsGarminTrackPointExtensionV1() throws IOException {
        // Use a GPX 1.1 doc that uses the old garmin extension namespace that NamespaceFilter remaps
        String gpxWithOldNs =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"filter-test\">" +
                "<trk><trkseg>" +
                "<trkpt lat=\"48.0\" lon=\"11.0\">" +
                "<extensions xmlns:gpxtpx=\"https://www8.garmin.com/xmlschemas/TrackPointExtensionv1.xsd\">" +
                "<gpxtpx:TrackPointExtension/>" +
                "</extensions>" +
                "</trkpt>" +
                "</trkseg></trk>" +
                "</gpx>";
        // Should parse without error; NamespaceFilter silently remaps the old URI
        GpxType gpx = unmarshal11(gpxWithOldNs);
        assertNotNull(gpx);
        assertEquals("filter-test", gpx.getCreator());
    }

}
