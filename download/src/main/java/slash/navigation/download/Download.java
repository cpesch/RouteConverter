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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.io.File.createTempFile;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static slash.common.io.Directories.getTemporaryDirectory;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.State.*;

/**
 * A file to download
 *
 * @author Christian Pesch
 */

public class Download {
    private final String description, url;
    private String eTag;
    private final Action action;
    private final FileAndChecksum file;
    private final File tempFile;
    private final List<FileAndChecksum> fragments;

    private State state;
    private long processedBytes;
    private Long expectedBytes;

    public Download(String description, String url, Action action, FileAndChecksum file,
                    List<FileAndChecksum> fragments, String eTag, State state, File tempFile) {
        this.description = description;
        this.url = url;
        this.action = action;
        this.file = file;
        this.fragments = fragments;
        setETag(eTag);
        this.state = state;
        this.tempFile = tempFile;
    }

    public Download(String description, String url, Action action, FileAndChecksum file,
                    List<FileAndChecksum> fragments) {
        this(description, url, action, file, fragments, null, Queued, newTempFile());
    }

    private static File newTempFile() {
        try {
            File file = createTempFile("download", ".tmp", getTemporaryDirectory());
            if (!file.delete())
                throw new IllegalArgumentException("Cannot delete temp file");
            return file;
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot create temp file", e);
        }
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public Action getAction() {
        return action;
    }

    public FileAndChecksum getFile() {
        return file;
    }

    public List<FileAndChecksum> getFragments() {
        return fragments;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag != null ? eTag.replaceAll("-gzip", "") : null;
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

    public Integer getPercentage() {
        return expectedBytes != null ? (int) (processedBytes / (double) expectedBytes * 100.0) : null;
    }

    public long getProcessedBytes() {
        return processedBytes;
    }

    public void setProcessedBytes(long processedBytes) {
        this.processedBytes = processedBytes;
    }

    public Long getExpectedBytes() {
        return expectedBytes;
    }

    public void setExpectedBytes(Long expectedBytes) {
        this.expectedBytes = expectedBytes;
    }

    private static final Set<State> DOWNLOADED = new HashSet<>(asList(NotModified, Succeeded));
    private static final Set<Action> COPY = new HashSet<>(singletonList(Copy));

    private Checksum getChecksum() {
        return DOWNLOADED.contains(getState()) && COPY.contains(getAction()) ?
                file.getActualChecksum() : file.getExpectedChecksum();
    }

    public Long getSize() {
        Checksum checksum = getChecksum();
        return checksum != null ? checksum.getContentLength() : null;
    }

    public CompactCalendar getLastModified() {
        Checksum checksum = getChecksum();
        return checksum != null ? checksum.getLastModified() : null;
    }

    public String toString() {
        return super.toString() + "[url=" + getUrl() + "]";
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
