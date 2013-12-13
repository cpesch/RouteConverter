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
public class Extractor implements DownloadProcessor {
    private File target;

    public Extractor(File target) {
        this.target = target;
    }

    public void process(Download download, Copier copier) throws IOException {
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(new FileInputStream(download.getTarget()));
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    File extracted = new File(target, lastPathFragment(entry.getName(), MAX_VALUE));
                    FileOutputStream output = new FileOutputStream(extracted);

                    download.setExpectedBytes(entry.getSize());
                    copier.copy(zipInputStream, output, 0);

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
