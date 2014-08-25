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

import slash.navigation.download.Checksum;
import slash.navigation.download.Download;
import slash.navigation.download.FileAndChecksum;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static slash.common.io.Files.generateChecksum;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Validates a {@link Download}
 *
 * @author Christian Pesch
 */

public class Validator {
    private static final Logger log = getLogger(Validator.class.getName());
    private final Download download;

    public Validator(Download download) {
        this.download = download;
    }

    private Checksum createChecksum(File file) throws IOException {
        return new Checksum(fromMillis(file.lastModified()), file.length(), generateChecksum(file));
    }

    public void validate() throws IOException {
        if (!existTargets())
            return;

        download.getFile().setActualChecksum(createChecksum(getFileTarget()));
        List<FileAndChecksum> fragments = download.getFragments();
        if (fragments != null) {
            for (FileAndChecksum fragment : fragments) {
                fragment.setActualChecksum(createChecksum(fragment.getFile()));
            }
        }
    }

    private File getFileTarget() {
        File file = download.getFile().getFile();
        return file.isFile() ? file : download.getTempFile();
    }

    private boolean isChecksumValid(FileAndChecksum file) throws IOException {
        Checksum expected = file.getExpectedChecksum();
        if (expected == null)
            return true;

        Checksum actual = file.getActualChecksum();
        boolean lastModifiedEquals = expected.getLastModified() == null ||
                expected.getLastModified().equals(actual.getLastModified());
        if (!lastModifiedEquals)
            log.warning(format("%s has last modified %s but expected %s", file.getFile(), actual.getLastModified(), expected.getLastModified()));
        boolean contentLengthEquals = expected.getContentLength() == null ||
                expected.getContentLength().equals(actual.getContentLength());
        if (!contentLengthEquals)
            log.warning(format("%s has %d bytes but expected %d", file.getFile(), actual.getContentLength(), expected.getContentLength()));
        boolean sha1Equals = expected.getSHA1() == null ||
                expected.getSHA1().equals(actual.getSHA1());
        if (!sha1Equals)
            log.warning(format("%s has SHA-1 %s but expected %s", file.getFile(), actual.getSHA1(), expected.getSHA1()));
        // TODO solve timezone problems first before making lastModifiedEquals relevant again
        boolean valid = /*lastModifiedEquals &&*/ contentLengthEquals && sha1Equals;
        if (valid)
            log.info(format("%s has valid checksum", file.getFile()));
        return valid;
    }

    public boolean isChecksumValid() throws IOException {
        if (!isChecksumValid(download.getFile()))
            return false;

        List<FileAndChecksum> fragments = download.getFragments();
        if (fragments != null)
            for (FileAndChecksum fragment : fragments) {
                if (!isChecksumValid(fragment))
                    return false;
            }
        return true;
    }

    public boolean existTargets() {
        if (!download.getFile().getFile().exists())
            return false;
        List<FileAndChecksum> fragments = download.getFragments();
        if (fragments != null)
            for (FileAndChecksum fragment : fragments) {
                if (!fragment.getFile().exists())
                    return false;
            }
        return true;
    }
}
