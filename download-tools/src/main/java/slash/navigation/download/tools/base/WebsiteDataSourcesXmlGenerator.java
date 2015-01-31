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
import java.util.*;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;

/**
 * Base for generation of data sources XML from websites.
 *
 * @author Christian Pesch
 */

public abstract class WebsiteDataSourcesXmlGenerator extends BaseDataSourcesXmlGenerator {

    public void run(String[] args) throws Exception {
        if (args.length < 6) {
            System.err.println(getClass().getSimpleName() + ": <id> <name> <startUrl> <baseUrl> <directory> <writeXmlFile> [<startindex>] [<endindex>]");
            System.exit(20);
        }

        long start = currentTimeMillis();
        DatasourceType datasourceType = new ObjectFactory().createDatasourceType();
        datasourceType.setId(args[0]);
        datasourceType.setName(args[1]);
        String baseUrl = args[2];
        datasourceType.setBaseUrl(baseUrl);
        datasourceType.setDirectory(args[4]);

        List<String> collectedUris = collectUris(args[3], baseUrl);
        System.out.println("Collected URIs: " + collectedUris + " (" + collectedUris.size() + " elements)");

        int startIndex = args.length > 7 ? parseInt(args[6]) : 0;
        int endIndex = args.length > 8 ? parseInt(args[7]) : MAX_VALUE;
        endIndex = min(collectedUris.size(), endIndex);
        List<String> parsingUris = collectedUris.subList(startIndex, endIndex);
        System.out.println("Parsing URIs from " + startIndex + " to " + endIndex + " (" + parsingUris.size() + " elements)");

        List<FileType> fileTypes = new ArrayList<>();
        List<ThemeType> themeTypes = new ArrayList<>();
        List<MapType> mapTypes = new ArrayList<>();

        parseUris(baseUrl, parsingUris, fileTypes, mapTypes, themeTypes);

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

    private void recursiveCollect(String startUrl, String baseUrl, String uri, Set<String> uris, Set<String> visitedUris) throws IOException {
        if(visitedUris.contains(uri))
            return;
        visitedUris.add(uri);

        // avoid server overload
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // intentionally do nothing
        }

        System.out.println(getClass().getSimpleName() + ": Downloading Webpage from " + startUrl + uri);
        Get get = new Get(startUrl + uri);
        String result = get.executeAsString();
        // System.out.println(result);

        AnchorParser parser = new AnchorParser();
        List<String> anchors = parser.parseAnchors(result);

        for (String anchor : anchors) {
            // System.out.println(anchor);

            if (anchor.startsWith("./"))
                anchor = anchor.substring(2);

            if (isRecurseAnchor(anchor)) {
                String nextPath = createPath(uri, anchor);
                if (nextPath.startsWith(startUrl))
                    nextPath = nextPath.substring(startUrl.length());
                recursiveCollect(startUrl, baseUrl, nextPath, uris, visitedUris);
            } else if (isIncludeAnchor(anchor)) {
                String nextUri = createUri(uri, anchor);
                if (nextUri.startsWith(baseUrl))
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

    private List<String> collectUris(String startUrl, String baseUrl) throws IOException {
        Set<String> uris = new HashSet<>();
        recursiveCollect(startUrl, baseUrl, "", uris, new HashSet<String>());

        String[] sortedUris = uris.toArray(new String[uris.size()]);
        sort(sortedUris);
        return asList(sortedUris);
    }

    protected abstract boolean isRecurseAnchor(String anchor);

    protected abstract boolean isIncludeAnchor(String anchor);

    private void parseUris(String baseUrl, List<String> uris, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes) throws IOException {
        System.out.println(getClass().getSimpleName() + ": Parsing " + uris.size() + " URIs");
        for (int i = 0; i < uris.size(); i++)
            parseUri(baseUrl, uris.get(i), i, fileTypes, mapTypes, themeTypes);
    }

    protected ContentLengthAndLastModified extractContentLengthAndLastModified(String baseUrl, String uri, int index) {
        // avoid server overload
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // intentionally do nothing
        }
        System.out.println(getClass().getSimpleName() + ": Extracting content length and last modified from " + baseUrl + uri + " (" + index + ")");
        try {
            Head head = new Head(baseUrl + uri);
            head.executeAsString();
            if (head.isSuccessful()) {
                return new ContentLengthAndLastModified(head.getContentLength(), head.getLastModified());
            }
        } catch (IOException e) {
            System.err.println(getClass().getSimpleName() + ": " + e.getMessage());
        }
        return null;
    }

    protected abstract void parseUri(String baseUrl, String uri, int index, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes) throws IOException;
}
