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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.kml.Kml22Format;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the probe timing summary logged after reading a container.
 *
 * @author Christian Pesch
 */
public class ProbeTimingsTest {
    @AfterEach
    public void tearDown() {
        ProbeTimings.discard();
    }

    @Test
    public void summarizesEntriesFormatsAndPairs() {
        assertTrue(ProbeTimings.start("takeout.zip"));
        assertFalse(ProbeTimings.start("inner.zip"));

        ProbeTimings.startEntry("Takeout/a.gpx");
        ProbeTimings.recordProbe(new Gpx10Format(), 3_000_000);
        ProbeTimings.recordProbe(new Kml22Format(), 7_000_000);
        ProbeTimings.endEntry(10_000_000);
        ProbeTimings.startEntry("Takeout/b.kml");
        ProbeTimings.recordProbe(new Kml22Format(), 20_000_000);
        ProbeTimings.endEntry(20_000_000);

        String summary = summary();
        assertTrue(summary.contains("takeout.zip: 2 entries probed"), summary);
        assertTrue(summary.indexOf("20 ms  Takeout/b.kml") < summary.indexOf("10 ms  Takeout/a.gpx"), summary);
        assertTrue(summary.contains("27 ms  Kml22Format (2 probes)"), summary);
        assertTrue(summary.contains("3 ms  Gpx10Format (1 probes)"), summary);
        assertTrue(summary.contains("20 ms  Takeout/b.kml | Kml22Format"), summary);
    }

    @Test
    public void restoresTheOuterEntryWhenANestedEntryFinishes() {
        assertTrue(ProbeTimings.start("takeout.zip"));

        ProbeTimings.startEntry("inner.zip");
        ProbeTimings.startEntry("inner.zip/a.gpx");
        ProbeTimings.recordProbe(new Gpx10Format(), 4_000_000);
        ProbeTimings.endEntry(4_000_000);
        // control returns here from the nested archive's own read(): the outer entry's
        // time must be attributed to "inner.zip", not to the last nested entry it processed
        ProbeTimings.endEntry(10_000_000);

        String summary = summary();
        assertTrue(summary.contains("10 ms  inner.zip"), summary);
        assertTrue(summary.contains("4 ms  inner.zip/a.gpx"), summary);
    }

    @Test
    public void recordsNothingOutsideAContainer() {
        ProbeTimings.recordProbe(new Gpx10Format(), 3_000_000);
        ProbeTimings.startEntry("a.gpx");
        ProbeTimings.endEntry(1);
        assertTrue(ProbeTimings.start("later.zip"));
        assertTrue(summary().contains("later.zip: 0 entries probed"));
    }

    private static String summary() {
        return ProbeTimings.currentSummary();
    }

    @Test
    public void keepsOnlyTheSlowestPairs() {
        assertTrue(ProbeTimings.start("big.zip"));
        for (int i = 0; i < 1000; i++) {
            ProbeTimings.startEntry("entry" + i);
            ProbeTimings.recordProbe(new Gpx10Format(), i * 1_000_000L);
        }
        String summary = summary();
        String pairs = summary.substring(summary.indexOf("Slowest entry | format pairs"));
        assertEquals(ProbeTimings.TOP, pairs.split("\n").length - 1, summary);
        assertTrue(pairs.contains("999 ms  entry999 | Gpx10Format"), summary);
        assertFalse(pairs.contains("entry984 |"), summary);
    }
}
