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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.download.ChecksumReportPolicy.isReportableChecksum;
import static slash.navigation.download.State.ChecksumError;
import static slash.navigation.download.State.Failed;
import static slash.navigation.download.State.NoFileError;
import static slash.navigation.download.State.Succeeded;

/**
 * Unit tests for {@link ChecksumReportPolicy}: the checksum of a download that failed validation
 * may only be reported to the catalog server when the transfer provably completed — the response
 * announced a Content-Length the file matches and a Last-Modified the file still carries, and the
 * failure is a validation failure, not a transport failure (see GitHub #382).
 *
 * @author Christian Pesch
 */
public class ChecksumReportPolicyTest {
    private static final long WORLD_MAP_SIZE = 3_276_950L;
    private static final long TRUNCATED_SIZE = 1_662_901L;
    private static final long LAST_MODIFIED = 1_700_000_000_123L;
    // a genuinely different build: whole seconds apart, not just sub-second noise the
    // second-precision comparison is designed to ignore
    private static final long LAST_MODIFIED_OTHER = 1_700_000_001_456L;

    private static final Long ANNOUNCED = WORLD_MAP_SIZE;
    private static final Long ANNOUNCED_LAST_MODIFIED = LAST_MODIFIED;

    @Test
    public void genuineRebuildIsReported() {
        // upstream rebuilt the file; catalog is stale but the download is complete and correct
        assertTrue(isReportableChecksum(ChecksumError, ANNOUNCED, WORLD_MAP_SIZE,
                ANNOUNCED_LAST_MODIFIED, LAST_MODIFIED));
    }

    @Test
    public void truncatedDownloadIsNotReported() {
        // interrupted transfer: the file is smaller than the announced Content-Length
        assertFalse(isReportableChecksum(ChecksumError, ANNOUNCED, TRUNCATED_SIZE,
                ANNOUNCED_LAST_MODIFIED, LAST_MODIFIED));
    }

    @Test
    public void largerThanAnnouncedDownloadIsNotReported() {
        assertFalse(isReportableChecksum(ChecksumError, ANNOUNCED, WORLD_MAP_SIZE + 1,
                ANNOUNCED_LAST_MODIFIED, LAST_MODIFIED));
    }

    @Test
    public void missingAnnouncedContentLengthIsNotReported() {
        // without a Content-Length completeness cannot be proven
        assertFalse(isReportableChecksum(ChecksumError, null, WORLD_MAP_SIZE,
                ANNOUNCED_LAST_MODIFIED, LAST_MODIFIED));
    }

    @Test
    public void missingActualContentLengthIsNotReported() {
        assertFalse(isReportableChecksum(ChecksumError, ANNOUNCED, null,
                ANNOUNCED_LAST_MODIFIED, LAST_MODIFIED));
    }

    @Test
    public void differingLastModifiedIsNotReported() {
        // sizes match, but the file is not the build the server announced
        assertFalse(isReportableChecksum(ChecksumError, ANNOUNCED, WORLD_MAP_SIZE,
                ANNOUNCED_LAST_MODIFIED, LAST_MODIFIED_OTHER));
    }

    @Test
    public void missingAnnouncedLastModifiedIsNotReported() {
        assertFalse(isReportableChecksum(ChecksumError, ANNOUNCED, WORLD_MAP_SIZE,
                null, LAST_MODIFIED));
    }

    @Test
    public void missingActualLastModifiedIsNotReported() {
        assertFalse(isReportableChecksum(ChecksumError, ANNOUNCED, WORLD_MAP_SIZE,
                ANNOUNCED_LAST_MODIFIED, null));
    }

    @Test
    public void transportFailureIsNotReported() {
        // even with matching sizes, a transport error leaves the content in an unknown state
        assertFalse(isReportableChecksum(Failed, ANNOUNCED, WORLD_MAP_SIZE,
                ANNOUNCED_LAST_MODIFIED, LAST_MODIFIED));
    }

    @Test
    public void missingFileErrorIsNotReported() {
        assertFalse(isReportableChecksum(NoFileError, ANNOUNCED, WORLD_MAP_SIZE,
                ANNOUNCED_LAST_MODIFIED, LAST_MODIFIED));
    }

    @Test
    public void successIsNeverRoutedThroughThePredicate() {
        // Succeeded is reported by the unchanged succeeded() path, not by this predicate
        assertFalse(isReportableChecksum(Succeeded, ANNOUNCED, WORLD_MAP_SIZE,
                ANNOUNCED_LAST_MODIFIED, LAST_MODIFIED));
    }

    @Test
    public void lastModifiedComparisonIgnoresMillisecondPrecision() {
        // HTTP dates and file mtimes carry no milliseconds; sub-second drift must not reject a report
        assertTrue(isReportableChecksum(ChecksumError, ANNOUNCED, WORLD_MAP_SIZE,
                ANNOUNCED_LAST_MODIFIED, LAST_MODIFIED + 42));
    }
}
