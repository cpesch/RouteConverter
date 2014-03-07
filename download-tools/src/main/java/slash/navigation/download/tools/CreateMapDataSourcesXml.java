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
package slash.navigation.download.tools;

import slash.navigation.download.datasources.binding.FileType;
import slash.navigation.download.datasources.binding.FragmentType;
import slash.navigation.download.datasources.binding.ObjectFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static slash.common.io.Files.generateChecksum;
import static slash.common.io.Files.getExtension;

/**
 * Creates a map data sources XML from file system mirror.
 *
 * @author Christian Pesch
 */

public class CreateMapDataSourcesXml extends BaseDataSourcesXmlGenerator {

    protected void parseFile(File file, List<FragmentType> fragmentTypes, List<FileType> fileTypes, File baseDirectory) throws IOException {
        String uri = relativizeUri(file, baseDirectory);
        String fileChecksum = generateChecksum(file);

        String extension = getExtension(file);
        if(".map".equals(extension)) {
            System.out.println(getClass().getSimpleName() + ": " + uri);

            FileType fileType = new ObjectFactory().createFileType();
            fileType.setUri(uri);
            fileType.setSize(file.length());
            fileType.setChecksum(fileChecksum);
            fileTypes.add(fileType);

        } else if (".zip".endsWith(extension)) {
            ZipInputStream zipInputStream = null;
            try {
                zipInputStream = new ZipInputStream(new FileInputStream(file));
                ZipEntry entry = zipInputStream.getNextEntry();
                while (entry != null) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".map")) {
                        System.out.println(getClass().getSimpleName() + ": " + entry.getName() + " maps to " + uri);

                        FileType fileType = new ObjectFactory().createFileType();
                        fileType.setUri(uri);
                        fileType.setSize(file.length());
                        fileType.setChecksum(fileChecksum);
                        fileTypes.add(fileType);

                        // do not close zip input stream
                        zipInputStream.closeEntry();
                    }
                    entry = zipInputStream.getNextEntry();
                }
            } finally {
                if (zipInputStream != null)
                    closeQuietly(zipInputStream);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new CreateMapDataSourcesXml().run(args);
    }
}
