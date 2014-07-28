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

package slash.navigation.download.actions;

import slash.common.type.CompactCalendar;

/**
 * A checksum, size, timestamp.
 *
 * @author Christian Pesch
 */

public class Checksum {
    private final String checksum;
    private final Long size;
    private final CompactCalendar timestamp;

    public Checksum(String checksum, Long size, CompactCalendar timestamp) {
        this.checksum = checksum;
        this.size = size;
        this.timestamp = timestamp;
    }

    public String getChecksum() {
        return checksum;
    }

    public Long getSize() {
        return size;
    }

    public CompactCalendar getTimestamp() {
        return timestamp;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Checksum checksum1 = (Checksum) o;

        return !(checksum != null ? !checksum.equals(checksum1.checksum) : checksum1.checksum != null) &&
                !(size != null ? !size.equals(checksum1.size) : checksum1.size != null) &&
                !(timestamp != null ? !timestamp.equals(checksum1.timestamp) : checksum1.timestamp != null);
    }

    public int hashCode() {
        int result = checksum != null ? checksum.hashCode() : 0;
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[checksum=" + getChecksum() + ", size=" + getSize() +
                ", timestamp=" + getTimestamp() + "]";
    }
}
