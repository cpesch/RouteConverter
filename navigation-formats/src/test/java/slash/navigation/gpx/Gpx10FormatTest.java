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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.RouteCharacteristics;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.gpx.GpxUtil.GPX_10_NAMESPACE_URI;

/**
 * Tests the GPX 1.0 track read/write path, in particular that {@code <trkseg>} boundaries
 * (issue #156) survive it -- {@link Gpx10Format#createTrack} is structurally different from
 * {@link Gpx11Format}'s track writing since it never reuses an origin {@code TrksegType}.
 *
 * @author Christian Pesch
 */

public class Gpx10FormatTest {
    private List<GpxRoute> readGpx(String source) throws Exception {
        ParserContext<GpxRoute> context = new ParserContextImpl<>(null, null);
        new Gpx10Format().read(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), context);
        return context.getRoutes();
    }

    private String writeGpx(List<GpxRoute> routes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new Gpx10Format().write(routes, outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    // Number of <trkseg> children of the first <trk> in the written document.
    private int writtenTrkSegCount(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Element gpx = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .getDocumentElement();
        NodeList trks = gpx.getElementsByTagNameNS(GPX_10_NAMESPACE_URI, "trk");
        assertTrue("expected at least one <trk>", trks.getLength() > 0);
        Element trk = (Element) trks.item(0);
        int count = 0;
        for (Node child = trk.getFirstChild(); child != null; child = child.getNextSibling())
            if (child instanceof Element element && "trkseg".equals(element.getLocalName()))
                count++;
        return count;
    }

    @Test
    public void testMultipleTrkSegsSurviveRoundtrip() throws Exception {
        // issue #156: a <trk> with several <trkseg> elements (e.g. gaps where the GPS was off)
        // must not be flattened into a single <trkseg> on read/write
        String source =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" version=\"1.0\" creator=\"OsmAnd\">" +
                "<trk><trkseg>" +
                "<trkpt lat=\"1.0\" lon=\"1.0\"><time>2020-08-03T07:47:16Z</time></trkpt>" +
                "<trkpt lat=\"1.1\" lon=\"1.1\"><time>2020-08-03T08:04:03Z</time></trkpt>" +
                "</trkseg><trkseg>" +
                "<trkpt lat=\"2.0\" lon=\"2.0\"><time>2020-08-03T08:23:06Z</time></trkpt>" +
                "<trkpt lat=\"2.1\" lon=\"2.1\"><time>2020-08-03T08:35:55Z</time></trkpt>" +
                "</trkseg><trkseg>" +
                "<trkpt lat=\"3.0\" lon=\"3.0\"><time>2020-08-03T08:36:17Z</time></trkpt>" +
                "</trkseg></trk></gpx>";

        List<GpxRoute> routes = readGpx(source);
        assertEquals("all positions of all segments end up in one route", 1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(5, route.getPositionCount());

        // the first position of segment 2 and segment 3 record the boundary, the rest doesn't
        assertFalse(route.getPosition(0).isStartsNewSegment());
        assertFalse(route.getPosition(1).isStartsNewSegment());
        assertTrue(route.getPosition(2).isStartsNewSegment());
        assertFalse(route.getPosition(3).isStartsNewSegment());
        assertTrue(route.getPosition(4).isStartsNewSegment());

        String after = writeGpx(routes);
        assertEquals("the three original <trkseg> boundaries must survive the write", 3, writtenTrkSegCount(after));

        // and again after a second roundtrip
        List<GpxRoute> routes2 = readGpx(after);
        assertEquals(5, routes2.get(0).getPositionCount());
        String after2 = writeGpx(routes2);
        assertEquals(3, writtenTrkSegCount(after2));
    }

    @Test
    public void testSingleTrkSegStillWritesOneSegment() throws Exception {
        String source =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" version=\"1.0\" creator=\"single-segment\">" +
                "<trk><trkseg>" +
                "<trkpt lat=\"1.0\" lon=\"1.0\"/><trkpt lat=\"1.1\" lon=\"1.1\"/>" +
                "</trkseg></trk></gpx>";
        List<GpxRoute> routes = readGpx(source);
        assertFalse(routes.get(0).getPosition(1).isStartsNewSegment());

        String after = writeGpx(routes);
        assertEquals(1, writtenTrkSegCount(after));
    }

    @Test
    public void testSegmentBoundaryIsCarriedForwardPastAPositionWithoutCoordinates() throws Exception {
        // startsNewSegment() is recorded on exactly one GpxPosition (the first of a <trkseg>).
        // createTrack() skips a position lacking valid lat/lon; if that happens to be the one
        // carrying the boundary, the boundary must still apply to the next position actually
        // written instead of silently merging the segment into the previous one.
        GpxPosition first = new GpxPosition(1.0, 1.0, null, null, null, "first");
        GpxPosition boundaryWithoutCoordinates = new GpxPosition(null, null, null, null, null, "boundary");
        boundaryWithoutCoordinates.setStartsNewSegment(true);
        GpxPosition third = new GpxPosition(2.0, 2.0, null, null, null, "third");

        GpxRoute route = new GpxRoute(new Gpx10Format(), RouteCharacteristics.Track,
                "test", null, Arrays.asList(first, boundaryWithoutCoordinates, third));

        String after = writeGpx(Collections.singletonList(route));
        assertEquals("the boundary carried past the skipped position must still start a new <trkseg>",
                2, writtenTrkSegCount(after));
    }
}
