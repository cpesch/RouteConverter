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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private Checksum downloadableChecksum;
    private List<Checksum> fragmentChecksums = new ArrayList<>();

    public Validator(Download download) {
        this.download = download;
    }

    private Checksum createChecksum(File file) throws IOException {
        return new Checksum(fromMillis(file.lastModified()), file.length(), generateChecksum(file));
    }

    public void validate() throws IOException {
        if (!existTargets())
            return;

        downloadableChecksum = createChecksum(getFileTarget());
        List<File> fragmentTargets = download.getFragmentTargets();
        if (fragmentTargets != null) {
            for (File fragment : fragmentTargets) {
                fragmentChecksums.add(createChecksum(fragment));
            }
        }

        updateDownloadFromValidator();
    }

    private void updateDownloadFromValidator() {
        if (download.getFileChecksum() == null)
            download.setFileChecksum(downloadableChecksum);
        List<Checksum> fragmentChecksums = download.getFragmentChecksums();
        if (fragmentChecksums != null) {
            for (int i = 0; i < fragmentChecksums.size(); i++) {
                if (fragmentChecksums.get(i) == null)
                    fragmentChecksums.set(i, this.fragmentChecksums.get(i));
            }
        }
    }

    private File getFileTarget() {
        return download.getFileTarget().isFile() ? download.getFileTarget() : download.getTempFile();
    }

    private boolean isChecksumValid(Object object, Checksum expectedChecksum, Checksum actualChecksum) throws IOException {
        if (expectedChecksum == null)
            return true;
        boolean lastModifiedEquals = expectedChecksum.getLastModified() == null ||
                expectedChecksum.getLastModified().equals(actualChecksum.getLastModified());
        if (!lastModifiedEquals)
            log.warning(format("%s has last modified %s but expected %s", object, actualChecksum.getLastModified(), expectedChecksum.getLastModified()));
        boolean contentLengthEquals = expectedChecksum.getContentLength() == null ||
                expectedChecksum.getContentLength().equals(actualChecksum.getContentLength());
        if (!contentLengthEquals)
            log.warning(format("%s has %d bytes but expected %d", object, actualChecksum.getContentLength(), expectedChecksum.getContentLength()));
        boolean sha1Equals = expectedChecksum.getSHA1() == null ||
                expectedChecksum.getSHA1().equals(actualChecksum.getSHA1());
        if (!sha1Equals)
            log.warning(format("%s has SHA-1 %s but expected %s", object, actualChecksum.getSHA1(), expectedChecksum.getSHA1()));
        boolean valid = /*lastModifiedEquals &&*/ contentLengthEquals && sha1Equals;
        if (valid)
            log.info(format("%s has valid checksum", object));
        return valid;
    }

    public boolean isChecksumValid() throws IOException {
        if (!isChecksumValid(getFileTarget(), download.getFileChecksum(), downloadableChecksum))
            return false;

        List<File> fragmentTargets = download.getFragmentTargets();
        List<Checksum> fragmentChecksums = download.getFragmentChecksums();
        if (fragmentTargets != null && fragmentChecksums != null)
            for (int i = 0; i < fragmentTargets.size(); i++) {
                if (!isChecksumValid(fragmentTargets.get(i), fragmentChecksums.get(i), this.fragmentChecksums.get(i)))
                    return false;
            }
        return true;
    }

    public boolean existTargets() {
        if (!download.getFileTarget().exists())
            return false;
        List<File> fragmentTargets = download.getFragmentTargets();
        if (fragmentTargets != null)
            for (File fragment : fragmentTargets) {
                if (!fragment.exists())
                    return false;
            }
        return true;
    }
}
