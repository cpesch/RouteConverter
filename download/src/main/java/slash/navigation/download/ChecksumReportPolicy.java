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

import static slash.common.io.Transfer.roundMillisecondsToSecondPrecision;

/**
 * Decides whether the checksum of a {@link Download} that failed may be reported to the catalog
 * server as a new known-good build.
 * <p>
 * A failed download only carries a reportable checksum if the transfer provably completed and the
 * failure came from validation: the response announced a Content-Length the written file matches
 * exactly, and a Last-Modified the file still carries. That is the genuine "the upstream rebuilt
 * the file and the catalog is stale" case the catalog wants to learn about. Anything else — a
 * truncated transfer, a response without a Content-Length, a transport error, a different
 * Last-Modified — describes content the server never announced, and reporting its checksum would
 * pollute the catalog's known-good checksums (see GitHub #382).
 *
 * @author Christian Pesch
 */
public class ChecksumReportPolicy {

    private ChecksumReportPolicy() {}

    /**
     * @param state the terminal state of the download
     * @param announcedContentLength the Content-Length announced by the HTTP response, if any
     * @param actualContentLength the size of the file that was written
     * @param announcedLastModified the Last-Modified announced by the HTTP response, if any
     * @param actualLastModified the last-modified of the file that was written
     * @return true iff this checksum is trustworthy enough to be reported to the server as a
     * new known-good build
     */
    public static boolean isReportableChecksum(State state, Long announcedContentLength, Long actualContentLength,
                                               Long announcedLastModified, Long actualLastModified) {
        // only a validation failure happens on a completely transferred file; a transport error
        // leaves the content in an unknown state whose checksum is meaningless
        if (state != State.ChecksumError)
            return false;

        // without an announced Content-Length the transfer cannot be proven complete, and a size
        // that differs from it means the content is not the announced one
        if (announcedContentLength == null || actualContentLength == null ||
                !announcedContentLength.equals(actualContentLength))
            return false;

        // without an announced Last-Modified the file cannot be proven to be the announced build;
        // both sides are truncated to second precision, as HTTP dates and file systems carry no
        // milliseconds
        return announcedLastModified != null && actualLastModified != null &&
                roundMillisecondsToSecondPrecision(announcedLastModified) ==
                        roundMillisecondsToSecondPrecision(actualLastModified);
    }
}
