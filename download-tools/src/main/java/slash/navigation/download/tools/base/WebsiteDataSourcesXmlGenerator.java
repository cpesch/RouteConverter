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
package slash.navigation.download.tools.base;

import slash.navigation.datasources.binding.*;
import slash.navigation.download.tools.helpers.AnchorParser;
import slash.navigation.download.tools.helpers.ContentLengthAndLastModified;
import slash.navigation.rest.Get;
import slash.navigation.rest.Head;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.System.currentTimeMillis;

/**
 * Base for generation of data sources XML from websites.
 *
 * @author Christian Pesch
 */

public abstract class WebsiteDataSourcesXmlGenerator extends BaseDataSourcesXmlGenerator {

    public void run(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println(getClass().getSimpleName() + ": <id> <name> <startUrl> <baseUrl> <directory> <writeXmlFile>");
            System.exit(20);
        }

        long start = currentTimeMillis();
        DatasourceType datasourceType = new ObjectFactory().createDatasourceType();
        datasourceType.setId(args[0]);
        datasourceType.setName(args[1]);
        String baseUrl = args[3];
        datasourceType.setBaseUrl(baseUrl);
        datasourceType.setDirectory(args[4]);

        Set<String> uris = collectUris(args[2], baseUrl);
        System.out.println("Collected URIs: " + uris);

        List<FileType> fileTypes = new ArrayList<>();
        List<ThemeType> themeTypes = new ArrayList<>();
        List<MapType> mapTypes = new ArrayList<>();

        parseUris(baseUrl, uris, fileTypes, mapTypes, themeTypes);

        datasourceType.getFile().addAll(sortFileTypes(fileTypes));
        datasourceType.getMap().addAll(sortMapTypes(mapTypes));
        datasourceType.getTheme().addAll(sortThemeTypes(themeTypes));

        File writeXmlFile = new File(args[5]);
        writeXml(datasourceType, writeXmlFile);

        long end = currentTimeMillis();
        System.out.println(getClass().getSimpleName() + ": Took " + ((end - start) / 1000) + " seconds to collect " +
                fileTypes.size() + " files, " + mapTypes.size() + " maps and " + themeTypes.size() + " themes");
        System.exit(0);
    }

    private void recursiveCollect(String startUrl, String baseUrl, String uri, Set<String> uris) throws IOException {
        System.out.println(getClass().getSimpleName() + ": Downloading Webpage from " + startUrl + uri);
        Get get = new Get(startUrl + uri);
        String result = get.executeAsString();

        AnchorParser parser = new AnchorParser();
        List<String> anchors = parser.parseAnchors(result);

        for (String anchor : anchors) {
            if (anchor.startsWith("./"))
                anchor = anchor.substring(2);

            if (isRecurseAnchor(anchor)) {
                String nextPath = createPath(uri, anchor);
                recursiveCollect(startUrl, baseUrl, nextPath, uris);
            } else if (isIncludeAnchor(anchor)) {
                String nextUri = createUri(uri, anchor);
                if(nextUri.startsWith(baseUrl))
                    nextUri = nextUri.substring(baseUrl.length());
                uris.add(nextUri);
            }
        }
    }

    private String createPath(String uri, String anchor) {
        int index = uri.lastIndexOf('/');
        return index != -1 ? uri.substring(0, index + 1) + anchor : anchor;
    }

    private String createUri(String uri, String anchor) {
        int index = uri.lastIndexOf('/');
        return index != -1 && !anchor.contains("/") ? uri.substring(0, index + 1) + anchor : anchor;
    }

    private Set<String> collectUris(String startUrl, String baseUrl) throws IOException {
        Set<String> uris = new HashSet<>();
        recursiveCollect(startUrl, baseUrl, "", uris);
        return uris;
    }

    protected abstract boolean isRecurseAnchor(String anchor);

    protected abstract boolean isIncludeAnchor(String anchor);

    private void parseUris(String baseUrl, Set<String> uris, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes) throws IOException {
        System.out.println(getClass().getSimpleName() + ": Parsing " + uris.size() + " URIs");
        for (String uri : uris)
            parseUri(baseUrl, uri, fileTypes, mapTypes, themeTypes);
    }

    protected ContentLengthAndLastModified extractContentLengthAndLastModified(String baseUrl, String uri) throws IOException {
        System.out.println(getClass().getSimpleName() + ": Extracting content length and last modified from " + baseUrl + uri);
        Head head = new Head(baseUrl + uri);
        head.executeAsString();
        if (head.isSuccessful()) {
            return new ContentLengthAndLastModified(head.getContentLength(), head.getLastModified());
        }
        return null;
    }

    protected abstract void parseUri(String baseUrl, String uri, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes) throws IOException;
}
