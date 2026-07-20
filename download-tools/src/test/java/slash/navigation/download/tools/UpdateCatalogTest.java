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

package slash.navigation.download.tools;

import org.junit.Test;
import slash.navigation.download.Checksum;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.download.tools.UpdateCatalog.dropSha1;
import static slash.navigation.download.tools.UpdateCatalog.mergeChecksums;

/**
 * Unit tests for {@link UpdateCatalog}'s checksum-merging helpers (GitHub #190).
 *
 * @author Christian Pesch
 */
public class UpdateCatalogTest {

    private static final long DAY1 = 1_000_000_000_000L;
    private static final long DAY2 = DAY1 + 86_400_000L;

    // --- dropSha1 ---

    @Test
    public void dropSha1ReturnsNullForNullList() {
        assertNull(dropSha1(null));
    }

    @Test
    public void dropSha1StripsSha1ButKeepsSizeAndLastModified() {
        Checksum withSha1 = new Checksum(fromMillis(DAY1), 100L, "deadbeef");
        List<Checksum> result = dropSha1(Collections.singletonList(withSha1));
        assertEquals(1, result.size());
        Checksum stripped = result.get(0);
        assertEquals(fromMillis(DAY1), stripped.getLastModified());
        assertEquals(Long.valueOf(100L), stripped.getContentLength());
        assertNull(stripped.getSHA1());
    }

    @Test
    public void dropSha1LeavesChecksumsWithoutSha1Unchanged() {
        Checksum withoutSha1 = new Checksum(fromMillis(DAY1), 100L, null);
        List<Checksum> result = dropSha1(Collections.singletonList(withoutSha1));
        assertEquals(1, result.size());
        // same instance is reused when there is no SHA-1 to strip
        assertEquals(withoutSha1, result.get(0));
    }

    // --- mergeChecksums combined with dropSha1 (GitHub #190 scenario) ---

    @Test
    public void staleSha1DoesNotSurviveMergeForHeadOnlyDownloadable() {
        // a historical checksum that (however it got there) still carries a SHA-1 that this
        // HEAD-only tool can never recompute/refresh
        Checksum stale = new Checksum(fromMillis(DAY1), 100L, "stale-sha1");
        Checksum freshlyObserved = new Checksum(fromMillis(DAY2), 200L, null);

        List<Checksum> merged = mergeChecksums(dropSha1(Collections.singletonList(stale)), freshlyObserved);

        assertEquals(2, merged.size());
        for (Checksum checksum : merged)
            assertNull(checksum.getSHA1());
    }

    @Test
    public void mergeChecksumsWithoutDroppingSha1KeepsStaleSha1() {
        // without dropSha1, the stale SHA-1 would keep being forwarded forever
        Checksum stale = new Checksum(fromMillis(DAY1), 100L, "stale-sha1");
        Checksum freshlyObserved = new Checksum(fromMillis(DAY2), 200L, null);

        List<Checksum> merged = mergeChecksums(Collections.singletonList(stale), freshlyObserved);

        assertEquals(2, merged.size());
        assertEquals("stale-sha1", merged.get(1).getSHA1());
    }

    @Test
    public void mergeChecksumsPrependsLatestAndKeepsHistory() {
        Checksum c1 = new Checksum(fromMillis(DAY1), 100L, null);
        Checksum latest = new Checksum(fromMillis(DAY2), 200L, null);

        List<Checksum> merged = mergeChecksums(Arrays.asList(c1), latest);

        assertEquals(2, merged.size());
        assertEquals(latest, merged.get(0));
        assertEquals(c1, merged.get(1));
    }
}
