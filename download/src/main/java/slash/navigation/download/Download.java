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
import java.util.Date;

import static slash.common.type.CompactCalendar.fromDate;
import static slash.navigation.download.DownloadState.Queued;

/**
 * A file to download
 *
 * @author Christian Pesch
 */

public class Download {
    private static final int UNKNOWN_EXPECTED_BYTES = 1024 * 1024 * 1024;
    private String url;
    private File target;
    private CompactCalendar creationDate = fromDate(new Date());
    private DownloadState state = Queued;
    private long processedBytes;
    private Long expectedBytes;
    private String description;

    public Download(String description, String url, File target) {
        this.description = description;
        this.url = url;
        this.target = target;
    }

    public String getDescription() {
        return description;
    }

    public String getURL() {
        return url;
    }

    public File getTarget() {
        return target;
    }

    public CompactCalendar getCreationDate() {
        return creationDate;
    }

    public DownloadState getState() {
        return state;
    }

    public void setState(DownloadState state) {
        this.state = state;
    }

    public int getPercentage() {
        long totalBytes = expectedBytes != null ? expectedBytes : UNKNOWN_EXPECTED_BYTES;
        return new Double((double) processedBytes / totalBytes * 100).intValue();
    }

    public void setProcessedBytes(long processedBytes) {
        this.processedBytes = processedBytes;
    }

    public void setExpectedBytes(Long expectedBytes) {
        this.expectedBytes = expectedBytes;
    }
}
