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
import slash.navigation.datasources.binding.FileType;
import slash.navigation.datasources.binding.MapType;
import slash.navigation.datasources.binding.ThemeType;
import slash.navigation.download.tools.base.WebsiteDataSourcesXmlGenerator;
import slash.navigation.download.tools.helpers.ContentLengthAndLastModified;
import slash.navigation.rest.Get;

import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static slash.navigation.datasources.DataSourcesUtil.asBoundingBoxType;
import static slash.navigation.maps.helpers.MapUtil.extractBoundingBox;
import static slash.navigation.maps.helpers.MapUtil.writePartialFile;

/**
 * Creates a map data sources XML from websites.
 *
 * @author Christian Pesch
 */

public class CreateMapDataSourcesXml extends WebsiteDataSourcesXmlGenerator {
    private static final long PEEK_HEADER_SIZE = 4 * 4096L;

    protected boolean isIncludeAnchor(String anchor) {
        // TODO freizeitkarte: .map.zip but openandromaps: .zip
        return anchor.endsWith(".map") || anchor.endsWith(".map.zip") || anchor.endsWith(".zip");
    }

    protected boolean isRecurseAnchor(String anchor) {
        return !anchor.equals("index.html") &&
                !anchor.startsWith("/") && !anchor.startsWith("..") && !anchor.startsWith("http") &&
                (anchor.endsWith("/") || anchor.endsWith(".html"));
    }

    private File downloadMapHeader(String baseUrl, String uri, long fileSize) throws IOException {
        System.out.println(getClass().getSimpleName() + ": Downloading map header from " + baseUrl + uri);
        Get get = new Get(baseUrl + uri);
        get.setRange(0L, PEEK_HEADER_SIZE);
        InputStream inputStream = get.executeAsStream();
        File file = writePartialFile(inputStream, fileSize);
        closeQuietly(inputStream);
        return file;
    }

    protected void parseUri(String baseUrl, String uri, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes) throws IOException {
        ContentLengthAndLastModified meta = extractContentLengthAndLastModified(baseUrl, uri);
        if (meta != null) {
            File file = downloadMapHeader(baseUrl, uri, meta.contentLength);

            if (uri.endsWith(".zip")) {
                MapType mapType = createMapType(uri, meta.lastModified, meta.contentLength, null);
                mapTypes.add(mapType);

                ZipInputStream zipInputStream = null;
                try {
                    zipInputStream = new ZipInputStream(new FileInputStream(file));
                    ZipEntry entry = zipInputStream.getNextEntry();
                    while (entry != null) {
                        if (!entry.isDirectory() && entry.getName().endsWith(".map")) {
                            System.out.println(getClass().getSimpleName() + ": Found map " + entry.getName() + " in " + uri);
                            mapType.getFragment().add(createFragmentType(entry.getName(), entry.getTime(), entry.getSize()));

                            BoundingBox boundingBox = extractBoundingBox(zipInputStream, entry.getSize());
                            if (boundingBox != null)
                                mapType.setBoundingBox(asBoundingBoxType(boundingBox));

                            // do not close zip input stream and cope with partially copied zips
                            try {
                                zipInputStream.closeEntry();
                            } catch (EOFException e) {
                                // intentionally left empty
                            }
                        }

                        try {
                            entry = zipInputStream.getNextEntry();
                        } catch (EOFException e) {
                            entry = null;
                        }
                    }
                } finally {
                    if (zipInputStream != null)
                        closeQuietly(zipInputStream);
                }

            } else {
                System.out.println(getClass().getSimpleName() + ": Found map " + uri);
                BoundingBox boundingBox = extractBoundingBox(file);
                mapTypes.add(createMapType(uri, meta.lastModified, meta.contentLength, boundingBox));
            }

            if (!file.delete())
                throw new IOException(format("Could not delete temporary map file '%s'", file));
        }
    }

    public static void main(String[] args) throws Exception {
        new CreateMapDataSourcesXml().run(args);
    }
}
