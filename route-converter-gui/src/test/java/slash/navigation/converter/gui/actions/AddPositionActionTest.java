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

package slash.navigation.converter.gui.actions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.navigation.converter.gui.actions.AddPositionAction.segmentRowToBisect;

/**
 * Locks the "+" (new position) behaviour that regressed in forum report #1329: a new intermediate
 * position must land on the route (bisect a real segment between two existing positions), never at the
 * map centre, regardless of whether the selected position is in the middle, at the end, or absent.
 *
 * @author Christian Pesch
 */
public class AddPositionActionTest {

    @Test
    public void testSelectedMiddleRowBisectsThatSegment() {
        // a mid-list selection bisects the segment after it, toward the next position in travel direction
        assertEquals(4, segmentRowToBisect(4, 23));
        assertEquals(0, segmentRowToBisect(0, 23));
    }

    @Test
    public void testSelectedLastRowBisectsTheSegmentBefore() {
        // the last position has no following segment, so bisect the one before it - still on the route
        assertEquals(21, segmentRowToBisect(22, 23));
        assertEquals(0, segmentRowToBisect(1, 2));
    }

    @Test
    public void testEmptySelectionInsertsIntoTheLastSegment() {
        // run() maps an empty selection to the last row; that must bisect the final segment, not seed
        for (int rowCount = 2; rowCount <= 50; rowCount++)
            assertEquals(rowCount - 2, segmentRowToBisect(rowCount - 1, rowCount));
    }

    @Test
    public void testTooFewPositionsSeedsAtMapCenter() {
        // fewer than two positions: nothing to bisect, caller seeds at the map centre
        assertEquals(-1, segmentRowToBisect(0, 1));
        assertEquals(-1, segmentRowToBisect(-1, 0));
        assertEquals(-1, segmentRowToBisect(0, 0));
    }

    @Test
    public void testResultIsAlwaysAValidOnRouteSegment() {
        // the invariant that fixes #1329: for any selected row in a route of >= 2 positions, the chosen
        // segment start is a real segment [0, rowCount - 2], so the midpoint lies between two positions
        for (int rowCount = 2; rowCount <= 100; rowCount++)
            for (int row = 0; row < rowCount; row++) {
                int segmentRow = segmentRowToBisect(row, rowCount);
                assertTrue("row=" + row + " rowCount=" + rowCount + " -> " + segmentRow,
                        segmentRow >= 0 && segmentRow <= rowCount - 2);
            }
    }
}
