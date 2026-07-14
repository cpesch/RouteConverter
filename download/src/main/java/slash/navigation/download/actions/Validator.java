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

        download.getFile().setActualChecksum(createChecksum(getFileTarget(), true));
        List<FileAndChecksum> fragments = download.getFragments();
        if (fragments != null)
            for (FileAndChecksum fragment : fragments)
                fragment.setActualChecksum(createChecksum(fragment.getFile(), true));

        calculatedChecksums = true;
    }

    private boolean isChecksumValid(FileAndChecksum file) {
        if (file.getFile().isDirectory())
            return true;

        List<Checksum> expectedChecksums = file.getExpectedChecksums();
        if (expectedChecksums.isEmpty())
            return true;

        Checksum actual = file.getActualChecksum();
        if (actual == null)
            return false;

        // A file is valid if it matches ANY known-good checksum. Upstreams that rebuild a file on a
        // schedule (e.g. BRouter segment tiles) produce a new content hash per build, so the catalog
        // may record several valid checksums for one URI; a download of any of those builds is correct
        // and must not be re-fetched (see GitHub #155).
        for (Checksum expected : expectedChecksums)
            if (matches(expected, actual)) {
                log.fine(format("%s has valid checksum", file.getFile()));
                return true;
            }

        Checksum latest = Checksum.getLatestChecksum(expectedChecksums);
        log.warning(format("%s has SHA-1 %s / %s bytes but matched none of %d known-good checksum(s) (latest expected SHA-1 %s / %s bytes)",
                file.getFile(), actual.getSHA1(), actual.getContentLength(), expectedChecksums.size(),
                latest != null ? latest.getSHA1() : null, latest != null ? latest.getContentLength() : null));
        return false;
    }

    private boolean matches(Checksum expected, Checksum actual) {
        if (expected == null)
            return true;

        boolean contentLengthEquals = expected.getContentLength() == null ||
                expected.getContentLength().equals(actual.getContentLength());
        boolean sha1Equals = expected.getSHA1() == null ||
                expected.getSHA1().equals(actual.getSHA1());

        if (expected.getSHA1() != null)
            // SHA-1 is authoritative: a matching content hash means the file is correct,
            // independent of its last-modified time (the server may not even send one, so a
            // re-downloaded good file routinely has a different mtime than the stored checksum).
            // A mismatching SHA-1 means the file is definitively wrong (see specs/00054).
            return sha1Equals && contentLengthEquals;

        // no authoritative hash: fall back to size + last-modified, with the "locally later
        // than remote" heuristic to avoid endless re-downloads when the local file is newer
        // than the (possibly stale) queued checksum:
        // - the very first download after a file update, once this process updated the server checksum
        // - the server checksum was updated but the queue still holds the first download's value
        boolean lastModifiedEquals = expected.getLastModified() == null ||
                expected.getLastModified().equals(actual.getLastModified());
        boolean localLaterThanRemote = actual.laterThan(expected);
        return lastModifiedEquals && contentLengthEquals || localLaterThanRemote;
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
