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

import slash.navigation.download.Download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Files.lastPathFragment;
import static slash.common.io.Files.setLastModified;
import static slash.common.io.InputOutput.closeQuietly;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Extracts a {@link Download} to a target directory.
 *
 * @author Christian Pesch
 */
public class Extractor {
    private static final Logger log = getLogger(Extractor.class.getName());
    private final CopierListener listener;

    public Extractor(CopierListener listener) {
        this.listener = listener;
    }

    private void doExtract(File tempFile, File destination, boolean flatten) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(tempFile))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                if (entry.isDirectory()) {
                    if (!flatten) {
                        File directory = new File(destination, entry.getName());
                        handleDirectory(directory, entry);
                    }

                } else {
                    File extracted;
                    if(flatten)
                        extracted = new File(destination, lastPathFragment(entry.getName(), MAX_VALUE));
                    else {
                        extracted = new File(destination, entry.getName());
                    }
                    File directory = extracted.getParentFile();
                    handleDirectory(directory, entry);

                    log.info(format("Extracting from %s to %s", tempFile, extracted));
                    FileOutputStream output = new FileOutputStream(extracted);
                    new Copier(listener).copy(zipInputStream, output, 0, entry.getSize());
                    // do not close zip input stream
                    closeQuietly(output);
                    setLastModified(extracted, fromMillis(entry.getTime()));

                    zipInputStream.closeEntry();
                }

                entry = zipInputStream.getNextEntry();
            }
        }
    }

    private void handleDirectory(File directory, ZipEntry entry) throws IOException {
        ensureDirectory(directory.getPath());
        setLastModified(directory, fromMillis(entry.getTime()));
    }

    public void flatten(File tempFile, File destination) throws IOException {
        doExtract(tempFile, destination, true);
    }

    public void extract(File tempFile, File destination) throws IOException {
        doExtract(tempFile, destination, false);
    }
}
