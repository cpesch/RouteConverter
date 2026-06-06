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

package slash.navigation.download.executor;

import org.junit.Test;
import slash.navigation.download.Checksum;
import slash.navigation.download.Download;
import slash.navigation.download.FileAndChecksum;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.State.Queued;

/**
 * Unit tests for {@link DownloadExecutorComparator}.
 *
 * @author Christian Pesch
 */
public class DownloadExecutorComparatorTest {

    private static final DownloadExecutorComparator COMPARATOR = new DownloadExecutorComparator();

    private static final long TS_EARLY = 1_000_000_000_000L;
    private static final long TS_LATE = TS_EARLY + 86_400_000L;

    private static File tempFile() {
        try {
            File f = File.createTempFile("comparator-test", ".tmp");
            f.deleteOnExit();
            return f;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static DownloadExecutor executorWithTimestamp(long millis) {
        Checksum checksum = new Checksum(fromMillis(millis), 100L, "sha");
        FileAndChecksum fac = new FileAndChecksum(tempFile(), checksum);
        Download d = new Download("desc", "http://example.com/" + millis, Copy, fac, null, null, Queued, tempFile());
        return new DownloadExecutor(d, null);
    }

    private static DownloadExecutor executorWithNoChecksum() {
        FileAndChecksum fac = new FileAndChecksum(tempFile(), null);
        Download d = new Download("desc", "http://example.com/no-checksum", Copy, fac, null, null, Queued, tempFile());
        return new DownloadExecutor(d, null);
    }

    // --- sorting order ---

    @Test
    public void earlierTimestampComesFirst() {
        DownloadExecutor early = executorWithTimestamp(TS_EARLY);
        DownloadExecutor late = executorWithTimestamp(TS_LATE);

        List<Runnable> list = new ArrayList<>();
        list.add(late);
        list.add(early);
        list.sort(COMPARATOR);

        assertTrue(list.get(0) == early);
        assertTrue(list.get(1) == late);
    }

    @Test
    public void noChecksumSortsBeforeChecksumEntry() {
        // Runnables without a checksum get -1 from compare, so they sort to the front
        DownloadExecutor withChecksum = executorWithTimestamp(TS_EARLY);
        DownloadExecutor withoutChecksum = executorWithNoChecksum();

        int result = COMPARATOR.compare(withoutChecksum, withChecksum);
        assertTrue(result < 0);
    }

    @Test
    public void nonDownloadExecutorRunnableSortsBeforeAny() {
        Runnable plainRunnable = () -> {};
        DownloadExecutor withChecksum = executorWithTimestamp(TS_EARLY);

        int resultPlainFirst = COMPARATOR.compare(plainRunnable, withChecksum);
        assertTrue(resultPlainFirst < 0);
    }

    @Test
    public void nonDownloadExecutorRunnableAsSecondArgSortsAfterAny() {
        Runnable plainRunnable = () -> {};
        DownloadExecutor withChecksum = executorWithTimestamp(TS_EARLY);

        int resultChecksumFirst = COMPARATOR.compare(withChecksum, plainRunnable);
        assertTrue(resultChecksumFirst > 0);
    }

    @Test
    public void sameTimestampComparesEqual() {
        DownloadExecutor e1 = executorWithTimestamp(TS_EARLY);
        DownloadExecutor e2 = executorWithTimestamp(TS_EARLY);
        assertEquals(0, COMPARATOR.compare(e1, e2));
    }

    private static void assertEquals(int expected, int actual) {
        assertTrue("Expected " + expected + " but was " + actual, expected == actual);
    }
}

