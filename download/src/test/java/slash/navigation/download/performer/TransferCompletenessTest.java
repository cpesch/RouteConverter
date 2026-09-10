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
package slash.navigation.download.performer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Pesch
 */
public class TransferCompletenessTest {

    @Test
    public void testResumeRangeEnd() {
        assertEquals(Long.valueOf(3276949L), TransferCompleteness.resumeRangeEnd(3276950L));
        assertNull(TransferCompleteness.resumeRangeEnd(null));
    }

    @Test
    public void testExpectedBytesOnDisk() {
        assertEquals(Long.valueOf(3276950L), TransferCompleteness.expectedBytesOnDisk(0, 3276950L));
        assertEquals(Long.valueOf(3276950L), TransferCompleteness.expectedBytesOnDisk(1662900, 1614050L));
        assertNull(TransferCompleteness.expectedBytesOnDisk(5, null));
    }

    @Test
    public void testIsComplete() {
        assertTrue(TransferCompleteness.isComplete(3276950, 3276950L));
        assertFalse(TransferCompleteness.isComplete(1662901, 3276950L));
        assertFalse(TransferCompleteness.isComplete(3276951, 3276950L));
        assertTrue(TransferCompleteness.isComplete(42, null));
    }

    @Test
    public void testKeepForResume() {
        assertTrue(TransferCompleteness.keepForResume(1662901, 3276950L));
        assertFalse(TransferCompleteness.keepForResume(3276951, 3276950L));
        assertFalse(TransferCompleteness.keepForResume(42, null));
    }
}
