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

package slash.navigation.garmin;

import org.junit.Test;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class GarminPoiDbFormatTest {
    private final GarminPoiDbFormat format = new GarminPoiDbFormat();

    private List<GpxRoute> read(String content) throws IOException {
        return read(content.getBytes(UTF_8));
    }

    private List<GpxRoute> read(byte[] content) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(content), context);
        return context.getRoutes();
    }

    private GpxRoute readSingle(String content) throws IOException {
        List<GpxRoute> routes = read(content);
        assertEquals(1, routes.size());
        return routes.get(0);
    }

    private String write(GpxPosition... positions) throws IOException {
        GpxRoute route = new GpxRoute(new Gpx10Format(), Waypoints, "route", null, asList(positions));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, positions.length);
        return new String(target.toByteArray(), "UTF-8");
    }

    private static GpxPosition position(Double longitude, Double latitude, String description) {
        return new GpxPosition(longitude, latitude, null, null, null, description);
    }

    @Test
    public void parsesDecimalLines() throws IOException {
        GpxRoute route = readSingle("11.12345,47.12345,Alpha,First place\n-8.5,-33.25,Beta,\n");
        assertEquals(Waypoints, route.getCharacteristics());
        assertEquals(2, route.getPositionCount());
        GpxPosition first = route.getPosition(0);
        assertEquals(11.12345, first.getLongitude(), 0.0000001);
        assertEquals(47.12345, first.getLatitude(), 0.0000001);
        assertEquals("Alpha; First place", first.getDescription());
        GpxPosition second = route.getPosition(1);
        assertEquals(-8.5, second.getLongitude(), 0.0000001);
        assertEquals(-33.25, second.getLatitude(), 0.0000001);
        assertEquals("Beta", second.getDescription());
    }

    @Test
    public void parsesHumanReadableCoordinates() throws IOException {
        GpxRoute route = readSingle("E 11 7.407,N 47 7.407,Gamma,Text\n" +
                "W 8.5,S 33.25,Delta,Text\n");
        assertEquals(11.12345, route.getPosition(0).getLongitude(), 0.0000001);
        assertEquals(47.12345, route.getPosition(0).getLatitude(), 0.0000001);
        assertEquals(-8.5, route.getPosition(1).getLongitude(), 0.0000001);
        assertEquals(-33.25, route.getPosition(1).getLatitude(), 0.0000001);
    }

    @Test
    public void toleratesWindowsLineEndingsBlankLinesAndNoTrailingNewline() throws IOException {
        GpxRoute route = readSingle("11.1,47.1,One,A\r\n\r\n12.2,48.2,Two,B");
        assertEquals(2, route.getPositionCount());
        assertEquals("One; A", route.getPosition(0).getDescription());
        assertEquals("Two; B", route.getPosition(1).getDescription());
    }

    @Test
    public void descriptionStartingWithNameIsNotRepeated() throws IOException {
        GpxRoute route = readSingle("11.1,47.1,Cafe,Cafe at the corner\n");
        assertEquals("Cafe at the corner", route.getPosition(0).getDescription());
    }

    @Test
    public void readsUtf8Characters() throws IOException {
        assertEquals("K\u00E4se; \u20AC", read("11.1,47.1,K\u00E4se,\u20AC\n").get(0).getPosition(0).getDescription());
    }

    @Test
    public void stripsIllegalControlCharacters() throws IOException {
        GpxRoute route = readSingle("11.1,47.1,Na\u0001me,Des\u000Bc\n");
        assertEquals("Name; Desc", route.getPosition(0).getDescription());
    }

    @Test
    public void rejectsTwoColumnLine() throws IOException {
        assertTrue(read("11.12345,47.12345\n").isEmpty());
    }

    @Test
    public void acceptsThreeColumnLine() throws IOException {
        assertEquals("Name", readSingle("11.12345,47.12345,Name\n").getPosition(0).getDescription());
    }

    @Test
    public void usesNameAsDescriptionColumnWhenThereIsNoDescription() throws IOException {
        // GPSBabel repeats the uncut shortname as the description, so a name longer than 24 characters survives
        String name = "abcdefghijklmnopqrstuvwxy";
        String written = write(position(11.1, 47.1, name));
        assertEquals("11.10000,47.10000,abcdefghijklmnopqrstuvwx," + name + "\n", written);
        assertEquals(name, readSingle(written).getPosition(0).getDescription());
    }

    @Test
    public void rejectsOutOfRangeCoordinates() throws IOException {
        assertTrue(read("181.0,47.1,Name,Desc\n").isEmpty());
        assertTrue(read("11.1,91.0,Name,Desc\n").isEmpty());
        assertTrue(read("-181.0,47.1,Name,Desc\n").isEmpty());
        assertTrue(read("11.1,-90.5,Name,Desc\n").isEmpty());
    }

    @Test
    public void rejectsFreeText() throws IOException {
        assertTrue(read("This is some free text\nwithout any structure\n").isEmpty());
        assertTrue(read("Dies ist 1 Text,mit 2,Kommas,und 3\n").isEmpty());
        assertTrue(read("Longitude,Latitude,Name,Comment\n").isEmpty());
        assertTrue(read("").isEmpty());
        assertTrue(read("\n\n").isEmpty());
    }

    @Test
    public void oneBadLineRejectsTheWholeFile() throws IOException {
        assertTrue(read("11.1,47.1,One,A\nnot a poi line\n12.2,48.2,Two,B\n").isEmpty());
    }

    @Test
    public void validRouteGuard() {
        GpxRoute valid = new GpxRoute(new Gpx10Format(), Waypoints, null, null, asList(position(11.1, 47.1, "a")));
        assertTrue(format.isValidRoute(valid));
        GpxRoute invalid = new GpxRoute(new Gpx10Format(), Waypoints, null, null, asList(position(null, null, "a")));
        assertFalse(format.isValidRoute(invalid));
    }

    @Test
    public void writesFixedWidthCoordinatesNameAndDescription() throws IOException {
        assertEquals("11.12345,47.12345,Name,Description\n", write(position(11.12345, 47.12345, "Name; Description")));
        assertEquals("09.50000,05.25000,Name,Name\n", write(position(9.5, 5.25, "Name")));
        assertEquals("-8.50000,-33.25000,Name,Name\n", write(position(-8.5, -33.25, "Name")));
        assertEquals("-0.00000,00.00000,,\n", write(position(-0.000001, 0.0, null)));
    }

    @Test
    public void roundsCoordinatesToFiveDecimals() throws IOException {
        assertEquals("11.12346,47.12345,,\n", write(position(11.123456, 47.123454, null)));
    }

    @Test
    public void cutsNameAfter24AndDescriptionAfter50Characters() throws IOException {
        String name = "abcdefghijklmnopqrstuvwxyz0123456789";
        String description = "0123456789012345678901234567890123456789012345678901234567890123456789";
        assertEquals("11.10000,47.10000," + name.substring(0, 24) + "," + description.substring(0, 50) + "\n",
                write(position(11.1, 47.1, name + "; " + description)));
    }

    @Test
    public void removesCommasFromNameAndDescription() throws IOException {
        assertEquals("11.10000,47.10000,Ab,Cd e\n", write(position(11.1, 47.1, "A,b; C,d, e")));
    }

    @Test
    public void commasAreRemovedBeforeCutting() throws IOException {
        String name = "a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z";
        assertEquals("11.10000,47.10000,abcdefghijklmnopqrstuvwx,abcdefghijklmnopqrstuvwxyz\n", write(position(11.1, 47.1, name)));
    }

    @Test
    public void writesNonAsciiAsUtf8() throws IOException {
        assertEquals("11.10000,47.10000,\u00C4 1,\u00C4 1\n", write(position(11.1, 47.1, "\u00C4 1")));
        assertEquals("11.10000,47.10000,\u00DF2,\u00DF2\n", write(position(11.1, 47.1, "\u00DF,2")));
    }

    @Test
    public void writesOnlyTheRequestedRangeAndSkipsPositionsWithoutCoordinates() throws IOException {
        GpxRoute route = new GpxRoute(new Gpx10Format(), Waypoints, null, null,
                asList(position(1.0, 2.0, "a"), position(null, 3.0, "b"), position(4.0, 5.0, "c"), position(6.0, 7.0, "d")));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 1, 3);
        assertEquals("04.00000,05.00000,c,c\n", new String(target.toByteArray(), UTF_8));
    }

    @Test
    public void roundtrip() throws IOException {
        String written = write(position(11.12345, 47.12345, "Alpha; First"), position(-8.5, -33.25, "Beta"));
        GpxRoute route = readSingle(written);
        assertEquals(2, route.getPositionCount());
        assertEquals("Alpha; First", route.getPosition(0).getDescription());
        assertEquals(-33.25, route.getPosition(1).getLatitude(), 0.0000001);
        assertEquals(written, write(route.getPosition(0), route.getPosition(1)));
    }

    @Test
    public void describesTheFormat() {
        assertEquals(".xcsv", format.getExtension());
        assertEquals("Garmin POI Database (*.xcsv)", format.getName());
        assertTrue(format.isSupportsReading());
        assertTrue(format.isSupportsWriting());
        assertFalse(format.isSupportsMultipleRoutes());
    }
}
