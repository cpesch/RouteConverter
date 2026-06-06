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

package slash.navigation.download;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.download.Action.*;
import static slash.navigation.download.State.*;

/**
 * Unit tests for {@link Download}.
 *
 * @author Christian Pesch
 */
public class DownloadTest {

    private static final long TS = 1_000_000_000_000L;

    private static File tempFile() {
        try {
            File f = File.createTempFile("download-test", ".tmp");
            f.deleteOnExit();
            return f;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Download simpleDownload(Action action, State state) {
        FileAndChecksum file = new FileAndChecksum(tempFile(), new Checksum(fromMillis(TS), 100L, "sha1"));
        return new Download("desc", "http://example.com/file", action, file, null, null, state, tempFile());
    }

    // --- getPercentage ---

    @Test
    public void getPercentageIsNullWhenExpectedBytesIsNull() {
        Download d = simpleDownload(Copy, Queued);
        assertNull(d.getPercentage());
    }

    @Test
    public void getPercentageIsZeroAtStart() {
        Download d = simpleDownload(Copy, Queued);
        d.setExpectedBytes(1000L);
        d.setProcessedBytes(0L);
        assertEquals(Integer.valueOf(0), d.getPercentage());
    }

    @Test
    public void getPercentageIs50AtHalf() {
        Download d = simpleDownload(Copy, Queued);
        d.setExpectedBytes(1000L);
        d.setProcessedBytes(500L);
        assertEquals(Integer.valueOf(50), d.getPercentage());
    }

    @Test
    public void getPercentageIs100AtComplete() {
        Download d = simpleDownload(Copy, Queued);
        d.setExpectedBytes(1000L);
        d.setProcessedBytes(1000L);
        assertEquals(Integer.valueOf(100), d.getPercentage());
    }

    @Test
    public void getPercentageIsNullWhenOverflow() {
        Download d = simpleDownload(Copy, Queued);
        d.setExpectedBytes(100L);
        d.setProcessedBytes(200L);
        assertNull(d.getPercentage());
    }

    // --- getChecksum: Copy+Succeeded uses actual checksum ---

    @Test
    public void getChecksumUsesActualWhenSucceededAndCopy() {
        Checksum expected = new Checksum(fromMillis(TS), 100L, "expected-sha");
        Checksum actual = new Checksum(fromMillis(TS + 1000), 200L, "actual-sha");
        FileAndChecksum fac = new FileAndChecksum(tempFile(), expected);
        fac.setActualChecksum(actual);

        Download d = new Download("d", "http://example.com/f", Copy, fac, null, null, Succeeded, tempFile());
        assertSame(actual, d.getChecksum());
    }

    @Test
    public void getChecksumUsesActualWhenNotModifiedAndCopy() {
        Checksum expected = new Checksum(fromMillis(TS), 100L, "expected-sha");
        Checksum actual = new Checksum(fromMillis(TS + 1000), 200L, "actual-sha");
        FileAndChecksum fac = new FileAndChecksum(tempFile(), expected);
        fac.setActualChecksum(actual);

        Download d = new Download("d", "http://example.com/f", Copy, fac, null, null, NotModified, tempFile());
        assertSame(actual, d.getChecksum());
    }

    @Test
    public void getChecksumFallsBackToExpectedWhenNotCopy() {
        Checksum expected = new Checksum(fromMillis(TS), 100L, "expected-sha");
        Checksum actual = new Checksum(fromMillis(TS + 1000), 200L, "actual-sha");
        FileAndChecksum fac = new FileAndChecksum(tempFile(), expected);
        fac.setActualChecksum(actual);

        Download d = new Download("d", "http://example.com/f", Flatten, fac, null, null, Succeeded, tempFile());
        assertSame(expected, d.getChecksum());
    }

    @Test
    public void getChecksumFallsBackToExpectedWhenActualIsNull() {
        Checksum expected = new Checksum(fromMillis(TS), 100L, "expected-sha");
        FileAndChecksum fac = new FileAndChecksum(tempFile(), expected);

        Download d = new Download("d", "http://example.com/f", Copy, fac, null, null, Succeeded, tempFile());
        assertSame(expected, d.getChecksum());
    }

    @Test
    public void getChecksumFallsBackToExpectedWhenQueuedAndCopy() {
        Checksum expected = new Checksum(fromMillis(TS), 100L, "expected-sha");
        Checksum actual = new Checksum(fromMillis(TS + 1000), 200L, "actual-sha");
        FileAndChecksum fac = new FileAndChecksum(tempFile(), expected);
        fac.setActualChecksum(actual);

        Download d = new Download("d", "http://example.com/f", Copy, fac, null, null, Queued, tempFile());
        assertSame(expected, d.getChecksum());
    }

    // --- getSize / getLastModified delegate to getChecksum ---

    @Test
    public void getSizeAndGetLastModifiedFromExpectedChecksum() {
        Checksum expected = new Checksum(fromMillis(TS), 42L, "sha");
        FileAndChecksum fac = new FileAndChecksum(tempFile(), expected);
        Download d = new Download("d", "http://example.com/f", Flatten, fac, null, null, Queued, tempFile());

        assertEquals(Long.valueOf(42L), d.getSize());
        assertEquals(fromMillis(TS), d.getLastModified());
    }

    @Test
    public void getSizeIsNullWhenChecksumIsNull() {
        FileAndChecksum fac = new FileAndChecksum(tempFile(), null);
        Download d = new Download("d", "http://example.com/f", Flatten, fac, null, null, Queued, tempFile());
        assertNull(d.getSize());
        assertNull(d.getLastModified());
    }

    // --- setETag strips -gzip suffix ---

    @Test
    public void setETagStripsGzipSuffix() {
        Download d = simpleDownload(Copy, Queued);
        d.setETag("\"abc123\"-gzip");
        assertEquals("\"abc123\"", d.getETag());
    }

    @Test
    public void setETagPreservesNormalETag() {
        Download d = simpleDownload(Copy, Queued);
        d.setETag("\"abc123\"");
        assertEquals("\"abc123\"", d.getETag());
    }

    @Test
    public void setETagHandlesNull() {
        Download d = simpleDownload(Copy, Queued);
        d.setETag(null);
        assertNull(d.getETag());
    }

    // --- equals and hashCode (URL-based) ---

    @Test
    public void equalsReturnsTrueForSameUrl() {
        Download d1 = new Download("a", "http://same.url/x", Copy,
                new FileAndChecksum(tempFile(), null), null, null, Queued, tempFile());
        Download d2 = new Download("b", "http://same.url/x", Flatten,
                new FileAndChecksum(tempFile(), null), null, null, Running, tempFile());
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    public void equalsReturnsFalseForDifferentUrl() {
        Download d1 = new Download("a", "http://url1/x", Copy,
                new FileAndChecksum(tempFile(), null), null, null, Queued, tempFile());
        Download d2 = new Download("a", "http://url2/x", Copy,
                new FileAndChecksum(tempFile(), null), null, null, Queued, tempFile());
        assertNotEquals(d1, d2);
    }

    // --- getProcessedBytes / getExpectedBytes round-trip ---

    @Test
    public void processedAndExpectedBytesRoundTrip() {
        Download d = simpleDownload(Copy, Queued);
        d.setProcessedBytes(12345L);
        d.setExpectedBytes(99999L);
        assertEquals(12345L, d.getProcessedBytes());
        assertEquals(Long.valueOf(99999L), d.getExpectedBytes());
    }

    // --- fragments ---

    @Test
    public void getFragmentsReturnsSetFragments() {
        FileAndChecksum f1 = new FileAndChecksum(tempFile(), null);
        FileAndChecksum f2 = new FileAndChecksum(tempFile(), null);
        Download d = new Download("d", "http://example.com/f", Flatten,
                new FileAndChecksum(tempFile(), null), Arrays.asList(f1, f2), null, Queued, tempFile());
        assertEquals(2, d.getFragments().size());
    }
}

