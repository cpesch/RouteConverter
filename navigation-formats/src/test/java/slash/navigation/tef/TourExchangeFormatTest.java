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

package slash.navigation.tef;

import org.junit.jupiter.api.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import slash.navigation.base.UnreadableFileException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class TourExchangeFormatTest {
    private final TourExchangeFormat format = new TourExchangeFormat();

    private static String tef(String comment, String version, String itemCount, String... items) {
        StringBuilder buffer = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buffer.append("<TEF Comment=\"").append(comment).append("\" Version=\"").append(version).append("\">\n");
        buffer.append("<Header Name=\" von A nach B \" Software=\"MOTORRAD Tourenplaner\"/>\n");
        buffer.append("<WaypointList").append(itemCount != null ? " ItemCount=\"" + itemCount + "\"" : "").append(">\n");
        for (String item : items)
            buffer.append(item).append("\n");
        buffer.append("</WaypointList>\n</TEF>\n");
        return buffer.toString();
    }

    private static String item(String attributes, String x, String y) {
        return "<Item " + attributes + "><Point x=\"" + x + "\" y=\"" + y + "\"/></Item>";
    }

    private List<GpxRoute> read(String xml) throws IOException {
        return read(xml.getBytes(UTF_8));
    }

    private List<GpxRoute> read(byte[] bytes) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(bytes), context);
        return context.getRoutes();
    }

    private String standardTef() {
        return tef("TourExchangeFormat", "2.0", "4",
                item("Altitude=\"6\" PointDescription=\"Start\" SegDescription=\"Blumenau\"", "10.032", "53.56949"),
                item("Altitude=\"13\" Instruction=\"rechts abbiegen\" SegDescription=\"B434\"", "10,04", "53,57"),
                item("PointDescription=\"Via\" ViaStation=\"true\"", "10.05", "53.58"),
                item("PointDescription=\"Ziel\"", "10.06", "53.59"));
    }

    @Test
    public void properties() {
        assertEquals(".tef", format.getExtension());
        assertEquals("Tour Exchange Format (*.tef)", format.getName());
        assertTrue(format.isSupportsReading());
        assertTrue(format.isSupportsWriting());
        assertFalse(format.isSupportsMultipleRoutes());
    }

    @Test
    public void commentIsChecked() throws IOException {
        assertTrue(read(tef("SomethingElse", "2.0", "1", item("PointDescription=\"A\"", "1", "2"))).isEmpty());
        assertEquals(2, read(tef("tourexchangeformat", "2.0", "1", item("PointDescription=\"A\"", "1", "2"))).size());
    }

    @Test
    public void rootMustBeTef() throws IOException {
        assertTrue(read("<?xml version=\"1.0\"?><gpx version=\"1.0\"><wpt lat=\"1\" lon=\"2\"/></gpx>").isEmpty());
        assertTrue(read("no xml at all").isEmpty());
        assertTrue(read("").isEmpty());
    }

    @Test
    public void itemCountMismatchMakesTheLastItemTheDestination() throws IOException {
        // tef_xml.cc stops with "waypoint count differs"
        for (String itemCount : new String[]{"3", null, "1", "x"}) {
            List<GpxRoute> routes = read(tef("TourExchangeFormat", "2.0", itemCount,
                    item("PointDescription=\"A\"", "1", "2"), item("PointDescription=\"B\"", "3", "4")));
            assertEquals(2, routes.size(), itemCount);
            assertEquals(2, routes.get(0).getPositionCount(), itemCount);
            assertEquals("A", routes.get(0).getPosition(0).getDescription());
            assertEquals("B", routes.get(0).getPosition(1).getDescription());
            assertEquals(2, routes.get(1).getPositionCount(), itemCount);
        }
    }

    @Test
    public void itemsWithoutValidCoordinatesAreSkipped() throws IOException {
        // tef_xml.cc reads them as positions at 0/0
        List<GpxRoute> routes = read(tef("TourExchangeFormat", "2.0", "6",
                item("PointDescription=\"A\"", "1", "2"),
                "<Item PointDescription=\"NoPoint\"/>",
                item("PointDescription=\"TwoCommas\"", "10,5,1", "2"),
                item("PointDescription=\"Text\"", "abc", "2"),
                item("PointDescription=\"OutOfRange\"", "181", "2"),
                item("PointDescription=\"B\"", "3", "4")));
        assertEquals(2, routes.get(0).getPositionCount());
        assertEquals("B", routes.get(0).getPosition(1).getDescription());
        assertEquals(2, routes.get(1).getPositionCount());
        assertEquals(3.0, routes.get(1).getPosition(1).getLongitude(), 0.0);
    }

    @Test
    public void altitudeIsReadLikeTheCoordinates() throws IOException {
        // tef_xml.cc reads an empty altitude and a decimal comma as 0
        List<GpxRoute> routes = read(tef("TourExchangeFormat", "2.0", "4",
                item("Altitude=\"\" PointDescription=\"A\"", "1", "2"),
                item("Altitude=\"12,5\" PointDescription=\"B\"", "1", "2"),
                item("Altitude=\"abc\" PointDescription=\"C\"", "1", "2"),
                item("Altitude=\"-3.5\" PointDescription=\"D\"", "1", "2")));
        GpxRoute route = routes.get(1);
        assertNull(route.getPosition(0).getElevation());
        assertEquals(12.5, route.getPosition(1).getElevation(), 0.0);
        assertNull(route.getPosition(2).getElevation());
        assertEquals(-3.5, route.getPosition(3).getElevation(), 0.0);
    }

    @Test
    public void whitespaceBeforeTheXmlDeclarationIsAccepted() throws IOException {
        assertEquals(2, read("\n  \r\n" + standardTef()).size());
        assertEquals(2, read(("\ufeff\n " + standardTef()).getBytes(UTF_8)).size());
        assertEquals(2, read(("\ufeff" + standardTef()).getBytes(UTF_8)).size());
    }

    @Test
    public void bytesThatAreNoUtf8DoNotPrintToStandardError() throws IOException {
        PrintStream original = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, UTF_8));
        try {
            String xml = "<TEF Comment=\"TourExchangeFormat\"><Header Name=\"K\u00e4se\"/>";
            assertThrows(UnreadableFileException.class, () -> read(xml.getBytes(ISO_8859_1)));
            assertTrue(read("no xml \u00e4".getBytes(ISO_8859_1)).isEmpty());
        } finally {
            System.setErr(original);
        }
        assertEquals("", captured.toString(UTF_8));
    }

    @Test
    public void readsEncodings() throws IOException {
        String xml = tef("TourExchangeFormat", "2.0", "2", item("PointDescription=\"K\u00e4se\"", "1", "2"),
                item("PointDescription=\"B\"", "3", "4"));
        // no declaration and no UTF-8: Windows-1252 (D22)
        String undeclared = xml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", "");
        assertEquals("K\u00e4se", read(undeclared.getBytes(ISO_8859_1)).get(0).getPosition(0).getDescription());
        assertEquals("K\u00e4se", read(undeclared.getBytes(UTF_8)).get(0).getPosition(0).getDescription());
        String declared = xml.replace("UTF-8", "ISO-8859-1");
        assertEquals("K\u00e4se", read(declared.getBytes(ISO_8859_1)).get(0).getPosition(0).getDescription());
        byte[] utf16 = ("\ufeff" + xml.replace("UTF-8", "UTF-16")).getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
        assertEquals("K\u00e4se", read(utf16).get(0).getPosition(0).getDescription());
        assertTrue(read(xml.replace("UTF-8", "NO-SUCH-ENCODING").getBytes(UTF_8)).isEmpty());
    }

    @Test
    public void damagedFileKeepsTheItemsBeforeTheDamage() throws IOException {
        String xml = standardTef();
        String cut = xml.substring(0, xml.indexOf("<Item PointDescription=\"Ziel\""));
        List<GpxRoute> routes = read(cut + "<Item");
        assertEquals(2, routes.size());
        assertEquals(3, routes.get(1).getPositionCount());
        // the last item read is the destination
        assertEquals(2, routes.get(0).getPositionCount());
    }

    @Test
    public void recognizedFileWithoutItemsIsUnreadable() {
        String xml = standardTef();
        assertThrows(UnreadableFileException.class, () -> read(xml.substring(0, xml.indexOf("<Item"))));
        assertThrows(UnreadableFileException.class, () -> read(tef("TourExchangeFormat", "2.0", "0")));
        assertThrows(UnreadableFileException.class, () -> read(tef("TourExchangeFormat", "2.0", "1", "<Item PointDescription=\"A\"/>")));
    }

    @Test
    public void unknownElevationIsNotWritten() throws IOException {
        String xml = write(route("Tour", position(1, 2, null, null, "A"), position(3, 4, 12.0, null, "B")));
        assertFalse(xml.contains("Altitude=\"0\""), xml);
        assertTrue(xml.contains("<Item Altitude=\"12\" "), xml);
        List<GpxRoute> routes = read(xml);
        assertNull(routes.get(1).getPosition(0).getElevation());
        assertEquals(12.0, routes.get(1).getPosition(1).getElevation(), 0.0);
    }

    @Test
    public void firstLastAndViaStationsAreWaypointsAndEveryItemIsRoutePoint() throws IOException {
        List<GpxRoute> routes = read(standardTef());
        assertEquals(2, routes.size());

        GpxRoute waypoints = routes.get(0);
        assertEquals(Waypoints, waypoints.getCharacteristics());
        assertEquals(3, waypoints.getPositionCount());
        assertEquals("Blumenau; Start", waypoints.getPosition(0).getDescription());
        assertEquals("Via", waypoints.getPosition(1).getDescription());
        assertEquals("Ziel", waypoints.getPosition(2).getDescription());

        GpxRoute route = routes.get(1);
        assertEquals(Route, route.getCharacteristics());
        assertEquals("von A nach B", route.getName());
        assertEquals(1, route.getDescription().size());
        assertEquals("MOTORRAD Tourenplaner", route.getDescription().get(0));
        assertEquals(4, route.getPositionCount());
        assertEquals("B434; rechts abbiegen", route.getPosition(1).getDescription());
        assertEquals(6.0, route.getPosition(0).getElevation(), 0.0);
        assertEquals(13.0, route.getPosition(1).getElevation(), 0.0);
        assertNull(route.getPosition(2).getElevation());
        assertNull(route.getPosition(0).getTime());
    }

    @Test
    public void laterOfPointDescriptionAndInstructionWins() throws IOException {
        List<GpxRoute> routes = read(tef("TourExchangeFormat", "2.0", "1",
                item("PointDescription=\"Point\" Instruction=\"Instruction\"", "1", "2")));
        assertEquals("Instruction", routes.get(1).getPosition(0).getDescription());
        routes = read(tef("TourExchangeFormat", "2.0", "1",
                item("Instruction=\"Instruction\" PointDescription=\"Point\"", "1", "2")));
        assertEquals("Point", routes.get(1).getPosition(0).getDescription());
    }

    @Test
    public void commaDecimalsAreAccepted() throws IOException {
        GpxPosition position = read(standardTef()).get(1).getPosition(1);
        assertEquals(10.04, position.getLongitude(), 0.0);
        assertEquals(53.57, position.getLatitude(), 0.0);
    }

    @Test
    public void attributeNamesAreCaseInsensitive() throws IOException {
        List<GpxRoute> routes = read(tef("TourExchangeFormat", "2.0", "1",
                item("pointdescription=\"A\" ALTITUDE=\"5\"", "1", "2")));
        assertEquals("A", routes.get(1).getPosition(0).getDescription());
        assertEquals(5.0, routes.get(1).getPosition(0).getElevation(), 0.0);
    }

    @Test
    public void descriptionOfVersion1IsKept() throws IOException {
        List<GpxRoute> routes = read(tef("TourExchangeFormat", "1.0", "1",
                item("PointDescription=\"Old\" SegDescription=\"Seg\"", "1", "2")));
        assertEquals("Seg; Old", routes.get(1).getPosition(0).getDescription());
    }

    // fix_notes() of tef_xml.cc is compiled out (#if OMG) in 1.5.4 as well as 1.8.0: the example from
    // its source comment is not rewritten to "Wittlicher Strasse (L34)"
    @Test
    public void fixNotesExampleFromSourceCommentIsNotRewritten() throws IOException {
        List<GpxRoute> routes = read(tef("TourExchangeFormat", "2.0", "1",
                item("SegDescription=\"L34\\Wittlicher Strasse\" PointDescription=\"Wittlicher Strasse (  \"", "1", "2")));
        assertEquals("L34\\Wittlicher Strasse; Wittlicher Strasse (", routes.get(1).getPosition(0).getDescription());
    }

    @Test
    public void unnamedItemsGetSynthesizedShortnames() throws IOException {
        List<GpxRoute> routes = read(tef("TourExchangeFormat", "2.0", "4",
                item("PointDescription=\"Start\" SegDescription=\"Named\"", "1", "2"),
                item("Instruction=\"links abbiegen\"", "1", "2"),
                item("ViaStation=\"true\"", "1", "2"),
                item("SegDescription=\"Ziel\"", "1", "2")));
        GpxRoute waypoints = routes.get(0), route = routes.get(1);
        assertEquals(3, waypoints.getPositionCount());
        assertEquals("Named; Start", waypoints.getPosition(0).getDescription());
        assertEquals("WPT002", waypoints.getPosition(1).getDescription());
        assertEquals("Ziel", waypoints.getPosition(2).getDescription());
        assertEquals("Named; Start", route.getPosition(0).getDescription());
        assertEquals("RPT002; links abbiegen", route.getPosition(1).getDescription());
        assertEquals("WPT002", route.getPosition(2).getDescription());
        assertEquals("Ziel", route.getPosition(3).getDescription());
    }

    @Test
    public void viaItemWithDescriptionButNoShortnameStaysUnnamed() throws IOException {
        List<GpxRoute> routes = read(tef("TourExchangeFormat", "2.0", "3",
                item("SegDescription=\"A\"", "1", "2"),
                item("PointDescription=\"bei Ort\" ViaStation=\"true\"", "1", "2"),
                item("SegDescription=\"B\"", "1", "2")));
        assertEquals("bei Ort", routes.get(0).getPosition(1).getDescription());
        assertEquals("bei Ort", routes.get(1).getPosition(1).getDescription());
    }

    // writing

    private static GpxPosition position(double longitude, double latitude, Double elevation, CompactCalendar time, String description) {
        return new GpxPosition(longitude, latitude, elevation, null, time, description);
    }

    private static GpxRoute route(String name, GpxPosition... positions) {
        List<GpxPosition> list = new ArrayList<>(List.of(positions));
        return new GpxRoute(new Gpx10Format(), Route, name, null, list);
    }

    private String write(GpxRoute route) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(route, out, 0, route.getPositionCount());
        return out.toString(UTF_8);
    }

    private List<GpxRoute> readBack(String xml) throws IOException {
        return read(xml);
    }

    @Test
    public void writtenShapeMatchesTourenplanerTemplate() throws IOException {
        CompactCalendar t1 = CompactCalendar.fromMillis(1181640165000L); // 2007-06-12T09:22:45Z
        CompactCalendar t2 = CompactCalendar.fromMillis(1181640225000L);
        String xml = write(route("Tour",
                position(10.032, 53.56949, 6.4, t1, "Hamburg"),
                position(10.0334, 53.5701, null, null, "Mitte"),
                position(10.04, 53.58, 43.6, t2, "Ziel")));

        String[] lines = xml.split("\n");
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", lines[0]);
        assertEquals("<TEF Comment=\"TourExchangeFormat\" Version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">", lines[1]);
        assertTrue(lines[2].matches("<Header Datasource=\"RoutePlanningProgramm\" Name=\"Tour\" Software=\"RouteConverter [^\"]+\" CreationTime=\"\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d[+-]\\d\\d:\\d\\d\">"), lines[2]);
        assertEquals("  <Setup CoordinateSystem=\"GeoDecimal\" Measure=\"metric\"/>", lines[3]);
        assertTrue(lines[4].matches("  <Route Distance=\"\\d+\" Duration=\"60\"/>"), lines[4]);
        assertEquals("</Header>", lines[5]);
        assertEquals("<WaypointList ItemCount=\"3\">", lines[6]);

        assertTrue(lines[7].matches("<Item Altitude=\"6\" PointDist=\"0\" PointDescription=\"Hamburg\" Prio=\"-2\" SegDist=\"\\d+\" SegTime=\"0\" TimeStamp=\"2007-06-12T09:22:45\"><Point x=\"10.032\" y=\"53.56949\"/></Item>"), lines[7]);
        assertTrue(lines[8].matches("<Item PointDist=\"\\d+\" PointDescription=\"Mitte\" Prio=\"4\" SegDist=\"\\d+\" SegTime=\"0\" ViaStation=\"true\"><Point x=\"10.0334\" y=\"53.5701\"/></Item>"), lines[8]);
        assertTrue(lines[9].matches("<Item Altitude=\"44\" PointDist=\"\\d+\" PointDescription=\"Ziel\" Prio=\"-4\" SegDist=\"0\" SegTime=\"0\" TimeStamp=\"2007-06-12T09:23:45\"><Point x=\"10.04\" y=\"53.58\"/></Item>"), lines[9]);
        assertEquals("</WaypointList>", lines[10]);
        assertEquals("</TEF>", lines[11]);
        assertFalse(xml.contains("Instruction"));
        assertFalse(xml.contains("Turn"));
        assertFalse(xml.contains("SegDescription"));
    }

    @Test
    public void coordinatesHaveFiveDecimalsWithoutTrailingZeros() throws IOException {
        String xml = write(route("x", position(-0.123456789, 1.0, null, null, "a"), position(179.9999951, -45.000004, null, null, "b")));
        assertTrue(xml.contains("<Point x=\"-0.12346\" y=\"1\"/>"), xml);
        assertTrue(xml.contains("<Point x=\"180\" y=\"-45\"/>"), xml);
    }

    @Test
    public void coordinatesRoundTheDecimalValueHalfUp() throws IOException {
        // 11.123455 and 2.000025 are stored just below the half; rounding the binary expansion would
        // give 11.12345 and 2.00002, rounding the decimal value gives what the user sees
        String xml = write(route("x", position(11.123455, 2.000025, null, null, "a"), position(1.0, 1.0, null, null, "b")));
        assertTrue(xml.contains("<Point x=\"11.12346\" y=\"2.00003\"/>"), xml);
    }

    @Test
    public void timeStampOnlyWhenKnownAndDurationZeroWithoutTimes() throws IOException {
        String xml = write(route("x", position(1, 2, null, null, "a"), position(3, 4, null, null, "b")));
        assertFalse(xml.contains("TimeStamp"));
        assertTrue(xml.contains("Duration=\"0\""));
    }

    @Test
    public void emptyNameIsDerivedFromFirstAndLastDescription() throws IOException {
        String xml = write(route(null, position(1, 2, null, null, "Hamburg"), position(3, 4, null, null, "Berlin")));
        assertTrue(xml.contains(" Name=\"von Hamburg nach Berlin\" "), xml);
    }

    @Test
    public void writtenRouteIsReadBack() throws IOException {
        GpxRoute written = route("Tour", position(10.03212, 53.56949, 6.4, null, "Hamburg; Uhlenhorst"),
                position(10.0334, 53.5701, 13.0, null, "Mitte"), position(11.04, 52.58, null, null, "Ziel"));
        List<GpxRoute> routes = readBack(write(written));
        assertEquals(2, routes.size());
        for (GpxRoute route : routes)
            assertEquals(3, route.getPositionCount());
        GpxRoute route = routes.get(1);
        assertEquals("Tour", route.getName());
        for (int i = 0; i < 3; i++) {
            GpxPosition expected = written.getPosition(i), actual = route.getPosition(i);
            assertEquals(expected.getLongitude(), actual.getLongitude(), 0.000005);
            assertEquals(expected.getLatitude(), actual.getLatitude(), 0.000005);
            assertEquals(expected.getDescription(), actual.getDescription());
            if (expected.getElevation() != null)
                assertEquals(expected.getElevation(), actual.getElevation(), 0.5);
            else
                assertNull(actual.getElevation());
            assertEquals(expected.getDescription(), routes.get(0).getPosition(i).getDescription());
        }
    }

    @Test
    public void nameWithSpecialCharactersSurvives() throws IOException {
        String name = "Tour \"Süd\" & <Nord> Übergang ß";
        String description = "Café \"Zur Post\" & Söhne <1>";
        String xml = write(route(name, position(1, 2, null, null, description), position(3, 4, null, null, "Ä")));
        assertNotNull(xml);
        List<GpxRoute> routes = readBack(xml);
        assertEquals(name, routes.get(1).getName());
        assertEquals(description, routes.get(1).getPosition(0).getDescription());
        assertEquals("Ä", routes.get(1).getPosition(1).getDescription());
    }

    @Test
    public void textWithXmlIllegalCharactersKeepsTheFileReadable() throws IOException {
        String hostile = "Na,me\r\nx\ty\0z\uFFFE\uFFFF\uD800w; de,sc\r\nq";
        String xml = write(route("Tour\0\uFFFE" + hostile, position(10.0, 53.5, null, null, hostile),
                position(10.1, 53.6, null, null, "B"), position(10.2, 53.7, null, null, "C")));
        List<GpxRoute> routes = readBack(xml);
        assertEquals(2, routes.size());
        assertEquals(3, routes.get(1).getPositionCount());
        String description = routes.get(1).getPosition(0).getDescription();
        assertTrue(description.contains("Na,me"), description);
        assertTrue(description.contains("x\ty z"), description);
        assertFalse(description.indexOf('\0') != -1 || description.indexOf('\uFFFE') != -1 || description.indexOf('\uD800') != -1);
    }
}
