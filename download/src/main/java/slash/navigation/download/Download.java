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
import java.util.Date;

import static java.io.File.createTempFile;
import static slash.common.io.Externalization.getTempDirectory;
import static slash.common.io.Files.getExtension;
import static slash.common.io.Files.removeExtension;
import static slash.common.type.CompactCalendar.fromDate;
import static slash.navigation.download.State.Queued;

/**
 * A file to download
 *
 * @author Christian Pesch
 */

public class Download {
    private final String description, url, checksum;
    private final Long size;
    private final CompactCalendar creationDate;
    private final Action action;
    private final File target, tempFile;

    private State state;
    private long processedBytes;
    private Long expectedBytes;

    public Download(String description, String url, Long size, String checksum, Action action, File target,
                    CompactCalendar creationDate, State state, File tempFile) {
        this.description = description;
        this.url = url;
        this.size = size;
        this.checksum = checksum;
        this.action = action;
        this.target = target;
        this.creationDate = creationDate;
        this.state = state;
        this.tempFile = tempFile;
    }

    public Download(String description, String url, Long size, String checksum, Action action, File target) {
        this(description, url, size, checksum, action, target, fromDate(new Date()), Queued, newTempFile(target, action));
    }

    private static File newTempFile(File target, Action action) {
        try {
            switch (action) {
                case Copy:
                    return createTempFile(removeExtension(target.getName()) + "-", getExtension(target), getTempDirectory());
                case Extract:
                    return createTempFile(target.getName() + "-", ".zip", getTempDirectory());
                default:
                    throw new IllegalArgumentException("Unknown Action " + action);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot create temp file for " + target, e);
        }
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public Long getSize() {
        return size;
    }

    public String getChecksum() {
        return checksum;
    }

    public Action getAction() {
        return action;
    }

    public File getTarget() {
        return target;
    }

    public CompactCalendar getCreationDate() {
        return creationDate;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public File getTempFile() {
        return tempFile;
    }

    private static final int UNKNOWN_EXPECTED_BYTES = 1024 * 1024 * 1024;

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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Download download = (Download) o;

        return !(url != null ? !url.equals(download.url) : download.url != null);
    }

    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }
}
