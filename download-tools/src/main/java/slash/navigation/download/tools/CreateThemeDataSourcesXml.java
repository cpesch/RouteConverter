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
import slash.navigation.datasources.binding.FragmentType;
import slash.navigation.datasources.binding.MapType;
import slash.navigation.datasources.binding.ThemeType;
import slash.navigation.download.tools.base.WebsiteDataSourcesXmlGenerator;
import slash.navigation.rest.Get;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Creates a map theme data sources XML from websites.
 *
 * @author Christian Pesch
 */

public class CreateThemeDataSourcesXml extends WebsiteDataSourcesXmlGenerator {
    protected boolean isIncludeAnchor(String anchor) {
        return anchor.endsWith(".zip") && !anchor.endsWith(".map.zip");
    }

    protected boolean isRecurseAnchor(String anchor) {
        return !anchor.equals("index.html") &&
                !anchor.startsWith("/") && !anchor.startsWith("..") && !anchor.startsWith("http") &&
                (anchor.endsWith("/") || anchor.endsWith(".html"));
    }

    protected void parseUri(String baseUrl, String uri, int index, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes) throws IOException {
        Get get = new Get(baseUrl + uri);
        InputStream inputStream = get.executeAsStream();

        List<FragmentType> fragmentTypes = new ArrayList<>();
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(inputStream);
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    System.out.println(getClass().getSimpleName() + ": theme file " + entry.getName() + " found in " + uri + " (" + index + ")");
                    fragmentTypes.add(createFragmentType(entry.getName(), entry, zipInputStream));

                    // do not close zip input stream
                    zipInputStream.closeEntry();
                }

                entry = zipInputStream.getNextEntry();
            }
        } finally {
            if (zipInputStream != null)
                closeQuietly(zipInputStream);
        }

        closeQuietly(inputStream);

        Get checksumGet = new Get(baseUrl + uri);
        InputStream checksumInputStream = checksumGet.executeAsStream();

        ThemeType themeType = createThemeType(uri, checksumGet.getLastModified(), checksumGet.getContentLength(), checksumInputStream, null);
        themeType.getFragment().addAll(sortFragmentTypes(fragmentTypes));
        themeTypes.add(themeType);

        closeQuietly(checksumInputStream);
    }

    public static void main(String[] args) throws Exception {
        new CreateThemeDataSourcesXml().run(args);
    }
}
