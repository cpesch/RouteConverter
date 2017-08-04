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
import static slash.navigation.download.Checksum.createChecksum;

/**
 * Validates a {@link Download}
 *
 * @author Christian Pesch
 */

public class Validator {
    private static final Logger log = getLogger(Validator.class.getName());
    private final Download download;
    private boolean calculatedChecksums;
    private Boolean existsTargets, checksumsValid;

    public Validator(Download download) {
        this.download = download;
    }

    public boolean isExistsTargets() {
        determineExistTargets();
        return existsTargets;
    }

    public boolean isChecksumsValid() throws IOException {
        determineChecksumsValid();
        return checksumsValid;
    }


    private File getFileTarget() {
        File file = download.getFile().getFile();
        return file.isFile() ? file : download.getTempFile();
    }

    private void determineExistTargets() {
        if (existsTargets != null)
            return;

        existsTargets = true;

        if (!download.getFile().getFile().exists()) {
            log.warning(format("%s does not exist", download.getFile()));
            existsTargets = false;
        }

        List<FileAndChecksum> fragments = download.getFragments();
        if (fragments != null) {
            for (FileAndChecksum fragment : fragments) {
                if (!fragment.getFile().exists()) {
                    log.warning(format("%s does not exist", fragment));
                    existsTargets = false;
                }
            }
        }
    }

    public void calculateChecksums() throws IOException {
        if (calculatedChecksums)
            return;

        download.getFile().setActualChecksum(createChecksum(getFileTarget()));
        List<FileAndChecksum> fragments = download.getFragments();
        if (fragments != null)
            for (FileAndChecksum fragment : fragments)
                fragment.setActualChecksum(createChecksum(fragment.getFile()));

        calculatedChecksums = true;
    }

    private boolean isChecksumValid(FileAndChecksum file) {
        if (file.getFile().isDirectory())
            return true;

        Checksum expected = file.getExpectedChecksum();
        if (expected == null)
            return true;

        Checksum actual = file.getActualChecksum();
        if (actual == null)
            return false;

        boolean localLaterThanRemote = file.getActualChecksum().laterThan(file.getExpectedChecksum());
        if (localLaterThanRemote)
            // 2 reasons:
            // - this was the very first download after a file update and this process updated the checksum on the server
            // - the checksum on the server was updated but in the download queue there is still the checksum from the first download
            log.info(format("%s is locally later than remote", file.getFile()));

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
        boolean valid = lastModifiedEquals && contentLengthEquals && sha1Equals || localLaterThanRemote;
        if (valid)
            log.info(format("%s has valid checksum", file.getFile()));
        return valid;
    }

    private void determineChecksumsValid() throws IOException {
        if (checksumsValid != null)
            return;

        calculateChecksums();

        if (!isChecksumValid(download.getFile())) {
            checksumsValid = false;
            return;
        }

        List<FileAndChecksum> fragments = download.getFragments();
        if (fragments != null)
            for (FileAndChecksum fragment : fragments)
                if (!isChecksumValid(fragment)) {
                    checksumsValid = false;
                    return;
                }
        checksumsValid = true;
    }

    public void expectedChecksumIsCurrentChecksum() throws IOException {
        calculateChecksums();

        download.getFile().setExpectedChecksum(download.getFile().getActualChecksum());
    }
}
