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

import slash.common.type.CompactCalendar;
import slash.navigation.download.Checksum;

import java.util.Comparator;

/**
 * Compares {@link DownloadExecutor}s by their last sync date.
 *
 * @author Christian Pesch
 */
public class DownloadExecutorComparator implements Comparator<Runnable> {
    public int compare(Runnable r1, Runnable r2) {
        if (!(r1 instanceof DownloadExecutor))
            return -1;
        if (!(r2 instanceof DownloadExecutor))
            return 1;
        return (int) (getTimeInMillis((DownloadExecutor) r1) - getTimeInMillis((DownloadExecutor) r2));
    }

    private long getTimeInMillis(DownloadExecutor executor) {
        Checksum checksum = executor.getDownload().getFile().getExpectedChecksum();
        if (checksum != null) {
            CompactCalendar lastModified = checksum.getLastModified();
            if (lastModified != null)
                return lastModified.getTimeInMillis();
        }
        return 0;
    }
}
