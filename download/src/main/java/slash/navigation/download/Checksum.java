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

import slash.common.type.CompactCalendar;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static slash.common.io.Files.generateChecksum;
import static slash.common.io.Transfer.roundMillisecondsToSecondPrecision;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * A last modified timestamp, content length and SHA-1 checksum.
 *
 * @author Christian Pesch
 */

public class Checksum {
    private final CompactCalendar lastModified;
    private final Long contentLength;
    private final String sha1;

    public Checksum(CompactCalendar lastModified, Long contentLength, String sha1) {
        this.lastModified = lastModified;
        this.contentLength = contentLength;
        this.sha1 = sha1;
    }

    public CompactCalendar getLastModified() {
        return lastModified;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public String getSHA1() {
        return sha1;
    }

    public boolean laterThan(Checksum other) {
        return other == null || getLastModified() != null && other.getLastModified() != null &&
                getLastModified().after(other.getLastModified());
    }

    public static Checksum getLatestChecksum(List<Checksum> checksums) {
        Checksum latest = null;
        for (Checksum checksum : checksums) {
            if (latest == null || checksum.laterThan(latest))
                latest = checksum;
        }
        return latest;
    }

    public static Checksum createChecksum(File file) throws IOException {
        return file != null && file.exists() ?
                new Checksum(fromMillis(roundMillisecondsToSecondPrecision(file.lastModified())), file.length(), generateChecksum(file)) : null;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Checksum checksum = (Checksum) o;

        return !(contentLength != null ? !contentLength.equals(checksum.contentLength) : checksum.contentLength != null) &&
                !(lastModified != null ? !lastModified.equals(checksum.lastModified) : checksum.lastModified != null) &&
                !(sha1 != null ? !sha1.equals(checksum.sha1) : checksum.sha1 != null);
    }

    public int hashCode() {
        int result = lastModified != null ? lastModified.hashCode() : 0;
        result = 31 * result + (contentLength != null ? contentLength.hashCode() : 0);
        result = 31 * result + (sha1 != null ? sha1.hashCode() : 0);
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[lastModified=" + getLastModified() +
                ", contentLength=" + getContentLength() + ", sha1=" + getSHA1() + "]";
    }
}
