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

import slash.navigation.datasources.binding.FileType;
import slash.navigation.datasources.binding.MapType;
import slash.navigation.datasources.binding.ThemeType;
import slash.navigation.download.tools.base.WebsiteDataSourcesXmlGenerator;
import slash.navigation.download.tools.helpers.ContentLengthAndLastModified;

import java.io.IOException;
import java.util.List;

/**
 * Creates a BRouter data sources XML from the website.
 *
 * @author Christian Pesch
 */

public class CreateBrouterDataSourcesXml extends WebsiteDataSourcesXmlGenerator {
    protected boolean isIncludeAnchor(String anchor) {
        return anchor.endsWith(".rd5");
    }

    protected boolean isRecurseAnchor(String anchor) {
        return false;
    }

    protected void parseUri(String baseUrl, String uri, int index, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes) throws IOException {
        ContentLengthAndLastModified meta = extractContentLengthAndLastModified(baseUrl, uri, index);
        if (meta != null)
            fileTypes.add(createFileType(uri, meta.lastModified, meta.contentLength, null));
    }

    public static void main(String[] args) throws Exception {
        new CreateBrouterDataSourcesXml().run(args);
    }
}
