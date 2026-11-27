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

package slash.navigation.base;

import org.junit.jupiter.api.Test;
import slash.navigation.common.NavigationPosition;
import slash.navigation.topo.NationalGeographicTopo3Format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.navigation.base.RouteCharacteristics.Track;

/**
 * How {@link NavigationFormatParser} reports a source that a format recognized but cannot read.
 */
public class UnreadableFileParserTest {
    private final NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());

    private enum Behaviour { QUIET, READS, UNREADABLE, SALVAGES_THEN_UNREADABLE }

    // GPX-backed, so that the parser converts its routes to GPX as for the native GPSBabel replacements
    private static class TestFormat extends SimpleFormat<Wgs84Route> implements GpxBackedFormat {
        private final String name;
        private final Behaviour behaviour;

        TestFormat(String name, Behaviour behaviour) {
            this.name = name;
            this.behaviour = behaviour;
        }

        public String getName() {
            return name;
        }

        public String getExtension() {
            return ".test";
        }

        public int getMaximumPositionCount() {
            return UNLIMITED_MAXIMUM_POSITION_COUNT;
        }

        public boolean isSupportsMultipleRoutes() {
            return true;
        }


        @SuppressWarnings("unchecked")
        public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
            return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
        }

        private Wgs84Route route() {
            List<Wgs84Position> positions = new ArrayList<>();
            positions.add(new Wgs84Position(10.0, 50.0, null, null, null, "A"));
            return createRoute(Track, name, positions);
        }

        public void read(InputStream source, ParserContext<Wgs84Route> context) throws IOException {
            source.readAllBytes();
            switch (behaviour) {
                case READS -> context.appendRoute(route());
                case UNREADABLE -> throw new UnreadableFileException(name + " is damaged");
                case SALVAGES_THEN_UNREADABLE -> {
                    context.appendRoute(route());
                    throw new UnreadableFileException(name + " is damaged after its first route");
                }
                default -> {
                }
            }
        }

        public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) {
            throw new UnsupportedOperationException();
        }

        public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
            throw new UnsupportedOperationException();
        }
    }

    private ParserResult read(TestFormat... formats) throws IOException {
        return parser.read(new ByteArrayInputStream(new byte[]{1, 2, 3}), asList(formats));
    }

    @Test
    public void testQuietFormatStillYieldsAnEmptyRoute() throws IOException {
        TestFormat quiet = new TestFormat("Quiet", Behaviour.QUIET);
        ParserResult result = read(quiet);
        assertTrue(result.isSuccessful());
        assertSame(quiet, result.getFormat());
        assertEquals(0, result.getTheRoute().getPositionCount());
        assertNull(result.getFailure());
    }

    @Test
    public void testUnreadableIsReportedInsteadOfAnEmptyRoute() throws IOException {
        // the quiet format comes first and would have been taken for the source before
        ParserResult result = read(new TestFormat("Quiet", Behaviour.QUIET), new TestFormat("Damaged", Behaviour.UNREADABLE));
        assertFalse(result.isSuccessful());
        assertEquals("Damaged is damaged", result.getFailure().getMessage());
    }

    @Test
    public void testFirstUnreadableExplains() throws IOException {
        ParserResult result = read(new TestFormat("First", Behaviour.UNREADABLE), new TestFormat("Second", Behaviour.UNREADABLE));
        assertFalse(result.isSuccessful());
        assertEquals("First is damaged", result.getFailure().getMessage());
    }

    @Test
    public void testFormatThatReadsWinsOverUnreadable() throws IOException {
        TestFormat reads = new TestFormat("Reads", Behaviour.READS);
        ParserResult result = read(new TestFormat("Damaged", Behaviour.UNREADABLE), reads);
        assertTrue(result.isSuccessful());
        assertSame(reads, result.getFormat());
        assertEquals(1, result.getTheRoute().getPositionCount());
    }

    @Test
    public void testRoutesSalvagedBeforeUnreadableAreRead() throws IOException {
        TestFormat salvages = new TestFormat("Salvages", Behaviour.SALVAGES_THEN_UNREADABLE);
        ParserResult result = read(salvages, new TestFormat("Reads", Behaviour.READS));
        assertTrue(result.isSuccessful());
        assertSame(salvages, result.getFormat());
        assertEquals(1, result.getAllRoutes().size());
    }

    @Test
    public void testNoFormatRecognizesTheSource() throws IOException {
        ParserResult result = parser.read(new ByteArrayInputStream(new byte[]{1, 2, 3}),
                singletonList(new NationalGeographicTopo3Format()));
        // a foreign source is declined quietly: the TOPO! format is then the empty fallback
        assertTrue(result.isSuccessful());
        assertNull(result.getFailure());
    }

    @Test
    public void testDamagedTopoFile() throws IOException {
        byte[] header = new byte[512];
        byte[] version = "TOPO! Ver. 3.4.3".getBytes();
        header[0] = (byte) version.length;
        System.arraycopy(version, 0, header, 1, version.length);
        ParserResult result = parser.read(new ByteArrayInputStream(header), new NavigationFormatRegistry().getReadFormatsPreferredByExtension(".tpo"));
        assertFalse(result.isSuccessful());
        assertEquals("TOPO! file is damaged: block 1 at byte 512 is beyond the end of the file at 512", result.getFailure().getMessage());
    }
}
