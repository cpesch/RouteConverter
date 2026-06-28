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

