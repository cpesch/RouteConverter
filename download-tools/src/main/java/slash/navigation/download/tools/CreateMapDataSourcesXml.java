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

import slash.navigation.common.BoundingBox;
import slash.navigation.download.datasources.binding.FileType;
import slash.navigation.download.datasources.binding.FragmentType;
import slash.navigation.download.datasources.binding.MapType;
import slash.navigation.maps.helpers.MapUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.io.File.createTempFile;
import static java.lang.String.format;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copyLarge;
import static slash.common.io.Files.getExtension;

/**
 * Creates a map data sources XML from file system mirror.
 *
 * @author Christian Pesch
 */

public class CreateMapDataSourcesXml extends BaseDataSourcesXmlGenerator {
    private static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

    private BoundingBox extractBoundingBox(InputStream inputStream) throws IOException {
        File file = extractFile(inputStream);
        BoundingBox result = MapUtil.extractBoundingBox(file);
        if(!file.delete())
            throw new IOException(format("Could not delete temporary map file '%s'", file));
        return result;
    }

    private File extractFile(InputStream inputStream) throws IOException {
        File file = createTempFile("mapfromzip", ".map");
        FileOutputStream outputStream = new FileOutputStream(file);
        copyLarge(inputStream, outputStream, new byte[DEFAULT_BUFFER_SIZE]);
        closeQuietly(outputStream);
        return file;
    }

    protected void parseFile(File file, List<FileType> fileTypes, List<FragmentType> fragmentTypes, List<MapType> mapTypes, File baseDirectory) throws IOException {
        String uri = relativizeUri(file, baseDirectory);

        String extension = getExtension(file);
        if(".map".equals(extension)) {
            System.out.println(getClass().getSimpleName() + ": " + uri);
            mapTypes.add(createMapType(uri, file, MapUtil.extractBoundingBox(file), true, false));

        } else if (".zip".endsWith(extension)) {
            ZipInputStream zipInputStream = null;
            try {
                zipInputStream = new ZipInputStream(new FileInputStream(file));
                ZipEntry entry = zipInputStream.getNextEntry();
                while (entry != null) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".map")) {
                        System.out.println(getClass().getSimpleName() + ": " + entry.getName() + " maps to " + uri);
                        mapTypes.add(createMapType(uri, file, extractBoundingBox(zipInputStream), true, false));

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
