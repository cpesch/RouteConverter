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


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceService;
import slash.navigation.datasources.File;
import slash.navigation.datasources.Map;
import slash.navigation.datasources.Theme;
import slash.navigation.download.tools.base.BaseDownloadTool;
import slash.navigation.download.tools.helpers.AnchorFilter;
import slash.navigation.download.tools.helpers.AnchorParser;
import slash.navigation.rest.Get;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;

/**
 * Scans a website for resources for the DataSources catalog.
 *
 * @author Christian Pesch
 */

public class ScanWebsite extends BaseDownloadTool {
    private static final Logger log = Logger.getLogger(ScanWebsite.class.getName());
    private static final String BASE_URL_ARGUMENT = "baseUrl";
    private static final String EXTENSION_ARGUMENT = "extension";
    private static final String ID_ARGUMENT = "id";

    private String url, baseUrl, id;
    private Set<String> extensions;

    private String appendURIs(String uri, String anchor) {
        int index = uri.lastIndexOf('/');
        return index != -1 ? uri.substring(0, index + 1) + anchor : anchor;
    }

    private void recursiveCollect(String uri, Set<String> uris, Set<String> visitedUris) throws IOException {
        if (visitedUris.contains(uri))
            return;
        visitedUris.add(uri);

        log.info("Downloading " + url + uri);
        Get get = new Get(url + uri);
        String result = get.executeAsString();

        List<String> anchors = new AnchorParser().parseAnchors(result.replaceAll("<area", "<a"));

        List<String> includes = new AnchorFilter().filterAnchors(baseUrl, anchors, extensions);
        for (String anchor : includes) {
            uris.add(anchor);
        }

        List<String> recurse = new AnchorFilter().filterAnchors(baseUrl, anchors, new HashSet<>(asList(".html", "/")));
        for (String anchor : recurse) {
            // create the anchor relative to the current uri
            String nextUri = appendURIs(uri, anchor);
            recursiveCollect(nextUri, uris, visitedUris);
        }
    }

    private List<String> collectUris() throws IOException {
        Set<String> uris = new HashSet<>();
        recursiveCollect("", uris, new HashSet<String>());

        String[] sortedUris = uris.toArray(new String[uris.size()]);
        sort(sortedUris);
        return asList(sortedUris);
    }

    private Set<String> collectURIs(DataSource source) {
        Set<String> result = new HashSet<>();
        for(File file : source.getFiles())
            result.add(file.getUri());
        for(Map map : source.getMaps())
            result.add(map.getUri());
        for(Theme theme : source.getThemes())
            result.add(theme.getUri());
        return result;
    }

    private void scan() throws IOException, JAXBException {
        List<String> collectedUris = collectUris();
        log.info("Collected URIs: " + collectedUris + " (" + collectedUris.size() + " elements)");

        DataSourceService service = loadDataSources(getDataSourcesTarget());
        DataSource source = service.getDataSourceById(id);
        if (source == null)
            throw new IllegalArgumentException("Unknown data source: " + id);
        if (!url.equals(source.getBaseUrl()))
            throw new IllegalArgumentException("Data source URL: " + source.getBaseUrl() + " doesn't match URL: " + url);

        Set<String> files = collectURIs(source);

        Set<String> addedUris = new HashSet<>(collectedUris);
        addedUris.removeAll(files);

        Set<String> removedUris = new HashSet<>(files);
        removedUris.removeAll(collectedUris);

        log.info("Added URIs: " + addedUris + " (" + addedUris.size() + " elements)");
        log.info("Removed URIs: " + removedUris + " (" + removedUris.size() + " elements)");
    }

    private void run(String[] args) throws Exception {
        CommandLine line = parseCommandLine(args);
        String[] extensionArguments = line.getOptionValues(EXTENSION_ARGUMENT);
        extensions = extensionArguments != null ? new HashSet<>(asList(extensionArguments)) : null;
        url = line.getOptionValue(URL_ARGUMENT);
        baseUrl = line.getOptionValue(BASE_URL_ARGUMENT);
        if (baseUrl == null)
            baseUrl = url;
        id = line.getOptionValue(ID_ARGUMENT);
        scan();
        System.exit(0);
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private CommandLine parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName(URL_ARGUMENT).hasArgs(1).isRequired().withLongOpt("url").
                withDescription("URL to scan for resources").create());
        options.addOption(OptionBuilder.withArgName(BASE_URL_ARGUMENT).hasArgs(1).withLongOpt("baseUrl").
                withDescription("URL to use as a base for resources").create());
        options.addOption(OptionBuilder.withArgName(EXTENSION_ARGUMENT).hasArgs().withLongOpt("extension").
                withDescription("Extensions to scan for").create());
        options.addOption(OptionBuilder.withArgName(ID_ARGUMENT).hasArgs().isRequired().withLongOpt("id").
                withDescription("ID of the data source").create());
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(getClass().getSimpleName(), options);
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        new ScanWebsite().run(args);
    }
}
