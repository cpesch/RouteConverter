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
import slash.navigation.graphhopper.PbfUtil;
import slash.navigation.rest.Get;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Creates a GraphHopper data sources XML from the website.
 *
 * @author Christian Pesch
 */

public class CreateGraphHopperDataSourcesXml extends WebsiteDataSourcesXmlGenerator {
    private static final long PEEK_HEADER_SIZE = 256L;

    protected boolean isIncludeAnchor(String anchor) {
        return anchor.endsWith("-latest.osm.pbf");
    }

    protected boolean isRecurseAnchor(String anchor) {
        return !anchor.equals("index.html") &&
                !anchor.startsWith("/") && !anchor.startsWith("..") && !anchor.startsWith("http") &&
                (anchor.endsWith("/") || anchor.endsWith(".html"));
    }

    private BoundingBox extractBoundingBox(String baseUrl, String uri) throws IOException {
        System.out.println(getClass().getSimpleName() + ": Extracting bounding box from " + baseUrl + uri);
        Get get = new Get(baseUrl + uri);
        get.setRange(0L, PEEK_HEADER_SIZE);
        InputStream inputStream = get.executeAsStream();
        BoundingBox boundingBox = PbfUtil.extractBoundingBox(inputStream);
        closeQuietly(inputStream);
        get.release();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        return boundingBox;
    }

    protected void parseUri(String baseUrl, String uri, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes) throws IOException {
        ContentLengthAndLastModified meta = extractContentLengthAndLastModified(baseUrl, uri);
        if (meta != null) {
            BoundingBox boundingBox = extractBoundingBox(baseUrl, uri);
            fileTypes.add(createFileType(uri, meta.lastModified, meta.contentLength, boundingBox));
        }
    }

    public static void main(String[] args) throws Exception {
        new CreateGraphHopperDataSourcesXml().run(args);
    }
}
