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

/**
 * Pure functions to reason about the completeness of an HTTP transfer: the inclusive
 * range end for a resume request, the total bytes a temp file must have afterward, and
 * whether a transfer that fell short of that total may still serve a later resume.
 *
 * @author Christian Pesch
 */

public final class TransferCompleteness {
    private TransferCompleteness() {
    }

    // inclusive end offset for a resume Range header, null if the total length is unknown
    public static Long resumeRangeEnd(Long contentLength) {
        return contentLength != null ? contentLength - 1 : null;
    }

    // total bytes the temp file must have after this response: announced Content-Length plus the resume offset
    public static Long expectedBytesOnDisk(long resumeOffset, Long responseContentLength) {
        return responseContentLength != null ? resumeOffset + responseContentLength : null;
    }

    // true iff the transfer is complete or completeness cannot be judged (announced length unknown)
    public static boolean isComplete(long bytesOnDisk, Long expectedBytesOnDisk) {
        return expectedBytesOnDisk == null || bytesOnDisk == expectedBytesOnDisk;
    }

    // true iff a failed temp file may be kept for a later resume (shorter than expected), false = delete
    public static boolean keepForResume(long bytesOnDisk, Long expectedBytesOnDisk) {
        return expectedBytesOnDisk != null && bytesOnDisk < expectedBytesOnDisk;
    }
}
