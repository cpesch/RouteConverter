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

import java.io.File;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A file, its expected and actual checksum.
 * <p>
 * A file may carry more than one expected checksum: upstreams that rebuild a file on a schedule
 * (e.g. the BRouter segment tiles) produce a new content hash per build, so the catalog can record
 * several known-good checksums for the same URI. A downloaded file is valid if it matches
 * <em>any</em> of them (see {@link slash.navigation.download.actions.Validator}).
 *
 * @author Christian Pesch
 */

public class FileAndChecksum {
    private final File file;
    private List<Checksum> expectedChecksums;
    private Checksum actualChecksum;

    public FileAndChecksum(File file, Checksum expectedChecksum) {
        this.file = file;
        this.expectedChecksums = expectedChecksum != null ? singletonList(expectedChecksum) : emptyList();
    }

    /**
     * A file with several known-good expected checksums; the download is valid if it matches any of them.
     * A static factory rather than a constructor, so the common {@code (file, null)} call sites stay
     * unambiguous against the {@code (File, Checksum)} constructor.
     */
    public static FileAndChecksum forChecksums(File file, List<Checksum> expectedChecksums) {
        FileAndChecksum result = new FileAndChecksum(file, (Checksum) null);
        result.expectedChecksums = expectedChecksums != null ? expectedChecksums : emptyList();
        return result;
    }

    public File getFile() {
        return file;
    }

    public List<Checksum> getExpectedChecksums() {
        return expectedChecksums;
    }

    public Checksum getExpectedChecksum() {
        return Checksum.getLatestChecksum(expectedChecksums);
    }

    public void setExpectedChecksum(Checksum expectedChecksum) {
        this.expectedChecksums = expectedChecksum != null ? singletonList(expectedChecksum) : emptyList();
    }

    public Checksum getActualChecksum() {
        return actualChecksum;
    }

    public void setActualChecksum(Checksum actualChecksum) {
        this.actualChecksum = actualChecksum;
    }

    public String toString() {
        return getClass().getSimpleName() + "[file=" + getFile() + ", expectedChecksum=" + getExpectedChecksum() +
                ", actualChecksum=" + getActualChecksum() + "]";
    }
}
