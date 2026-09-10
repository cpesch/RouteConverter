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

package slash.navigation.converter.gui.helpers;

import org.junit.Test;

import slash.common.system.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.converter.gui.helpers.UpdatePolicy.Nudge.HIGHLIGHTS;
import static slash.navigation.converter.gui.helpers.UpdatePolicy.Nudge.HIGHLIGHTS_NO_SKIP;

public class UpdatePolicyTest {
    private static final Version V3_4 = new Version("3.4");
    private static final Version V3_5 = new Version("3.5"); // 1 release behind 3.4
    private static final Version V3_6 = new Version("3.6"); // 2 releases behind 3.4
    private static final Version V3_3 = new Version("3.3");

    @Test
    public void oneReleaseBehindShowsHighlightsWithSkip() {
        assertEquals(HIGHLIGHTS, UpdatePolicy.decide(V3_4, V3_5));
    }

    @Test
    public void twoReleasesBehindHidesSkip() {
        assertEquals(HIGHLIGHTS_NO_SKIP, UpdatePolicy.decide(V3_4, V3_6));
    }

    @Test
    public void threeReleasesBehindHidesSkip() {
        assertEquals(HIGHLIGHTS_NO_SKIP, UpdatePolicy.decide(V3_3, V3_6));
    }

    /** Highlights are no longer earned by repeated offers: the first offer already shows them. */
    @Test
    public void everyDecisionShowsHighlights() {
        for (Version latest : new Version[]{V3_5, V3_6})
            for (Version mine : new Version[]{V3_3, V3_4})
                assertTrue(UpdatePolicy.decide(mine, latest).name().startsWith("HIGHLIGHTS"));
    }

    @Test
    public void releasesBehindIgnoresPatchVersion() {
        assertEquals(1, UpdatePolicy.releasesBehind(new Version("3.4.1"), V3_5));
    }

    @Test
    public void releasesBehindIgnoresPreReleaseSuffix() {
        assertEquals(0, UpdatePolicy.releasesBehind(new Version("3.7-SNAPSHOT"), new Version("3.7")));
        assertEquals(1, UpdatePolicy.releasesBehind(new Version("3.6-SNAPSHOT"), new Version("3.7")));
    }

    @Test
    public void snapshotIsNotNudgedAsSeveralReleasesBehind() {
        assertEquals(HIGHLIGHTS, UpdatePolicy.decide(new Version("3.6-SNAPSHOT"), V3_6));
    }

    @Test
    public void isEndOfLifeBelowMajor3() {
        assertTrue(UpdatePolicy.isEndOfLife(new Version("2.30")));
        assertTrue(UpdatePolicy.isEndOfLife(new Version("2.9")));
    }

    @Test
    public void isNotEndOfLifeFromMajor3() {
        assertFalse(UpdatePolicy.isEndOfLife(V3_3));
        assertFalse(UpdatePolicy.isEndOfLife(new Version("3.7-SNAPSHOT")));
        assertFalse(UpdatePolicy.isEndOfLife(new Version("4.0")));
    }
}
