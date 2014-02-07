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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.Integer.MAX_VALUE;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static slash.common.io.Files.lastPathFragment;

/**
 * Extracts a {@link Download} to a target directory.
 *
 * @author Christian Pesch
 */
public class Extractor {
    private final CopierListener listener;

    public Extractor(CopierListener listener) {
        this.listener = listener;
    }

    public void extract(File tempFile, File destination) throws IOException {
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(new FileInputStream(tempFile));
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    File extracted = new File(destination, lastPathFragment(entry.getName(), MAX_VALUE));
                    FileOutputStream output = new FileOutputStream(extracted);

                    new Copier(listener).copy(zipInputStream, output, 0, entry.getSize());

                    // do not close zip input stream
                    closeQuietly(output);
                    zipInputStream.closeEntry();
                }
                entry = zipInputStream.getNextEntry();
            }
        }
        finally {
            if (zipInputStream != null)
                closeQuietly(zipInputStream);
        }
    }
}
