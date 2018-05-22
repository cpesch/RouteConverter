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
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.File;
import slash.navigation.datasources.Map;
import slash.navigation.datasources.Theme;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.FileType;
import slash.navigation.datasources.binding.MapType;
import slash.navigation.datasources.binding.ThemeType;
import slash.navigation.datasources.helpers.DataSourcesUtil;
import slash.navigation.download.tools.base.BaseDownloadTool;
import slash.navigation.download.tools.helpers.AnchorFilter;
import slash.navigation.download.tools.helpers.AnchorParser;
import slash.navigation.download.tools.helpers.DownloadableType;
import slash.navigation.rest.Delete;
import slash.navigation.rest.Get;
import slash.navigation.rest.Post;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.lang.System.exit;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static slash.common.io.Transfer.UTF8_ENCODING;
import static slash.navigation.datasources.helpers.DataSourcesUtil.asDatasourceType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createFileType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createMapType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createThemeType;
import static slash.navigation.download.tools.helpers.DownloadableType.File;
import static slash.navigation.rest.HttpRequest.APPLICATION_JSON;

/**
 * Scans a website for resources for the DataSources catalog.
 *
 * @author Christian Pesch
 */

public class ScanWebsite extends BaseDownloadTool {
    private static final Logger log = Logger.getLogger(ScanWebsite.class.getName());
    private static final String BASE_URL_ARGUMENT = "baseUrl";
    private static final String TYPE_ARGUMENT = "type";
    private static final String EXTENSION_ARGUMENT = "extension";
    private static final String INCLUDE_ARGUMENT = "include";
    private static final String EXCLUDE_ARGUMENT = "exclude";

    private String baseUrl;
    private DownloadableType type;
    private Set<String> extensions, includes, excludes;
    private int addCount, removeCount;

    private String appendURIs(String uri, String anchor) {
        int index = uri.lastIndexOf('/');
        return index != -1 ? uri.substring(0, index + 1) + anchor : anchor;
    }

    private void recursiveCollect(String uri, Set<String> uris, Set<String> visitedUris) throws IOException {
        if (visitedUris.contains(uri))
            return;
        visitedUris.add(uri);

        log.info(format("Downloading %s", getUrl() + uri));
        Get get = new Get(getUrl() + uri);
        String result = get.executeAsString();

        List<String> anchors = new AnchorParser().parseAnchors(result.replaceAll("<area", "<a"));

        List<String> included = new AnchorFilter().filterAnchors(baseUrl, anchors, extensions, includes, excludes);
        for (String anchor : included) {
            // create the anchor relative to the current uri
            String nextUri = appendURIs(uri, anchor);
            uris.add(nextUri);
        }

        List<String> recurse = new AnchorFilter().filterAnchors(baseUrl, anchors, new HashSet<>(asList(".html", "/")), null, null);
        for (String anchor : recurse) {
            if((getUrl() + anchor).equals(baseUrl) || baseUrl.endsWith(anchor))
                continue;
            // create the anchor relative to the current uri
            String nextUri = appendURIs(uri, anchor);
            recursiveCollect(nextUri, uris, visitedUris);
        }
    }

    private List<String> collectUris() throws IOException {
        Set<String> uris = new HashSet<>();
        recursiveCollect("", uris, new HashSet<String>());

        String[] sortedUris = uris.toArray(new String[0]);
        sort(sortedUris);
        return asList(sortedUris);
    }

    private Set<String> collectURIs(DataSource source) {
        Set<String> result = new HashSet<>();
        switch (type) {
            case File:
                for (File file : source.getFiles())
                    result.add(file.getUri());
                break;

            case Map:
                for (Map map : source.getMaps())
                    result.add(map.getUri());
                break;

            case Theme:
                for (Theme theme : source.getThemes())
                    result.add(theme.getUri());
                break;
        }
        return result;
    }

    private void scan() throws IOException, JAXBException {
        List<String> collectedUris = collectUris();
        log.info(format("Collected URIs: %s (%d elements)", collectedUris, collectedUris.size()));

        DataSource source = loadDataSource(getId());
        if (!getUrl().equals(source.getBaseUrl()) && !baseUrl.equals(source.getBaseUrl()))
            log.warning("Data source URL: " + source.getBaseUrl() + " doesn't match URL: " + getUrl());

        Set<String> files = collectURIs(source);

        Set<String> addedUris = new HashSet<>(collectedUris);
        addedUris.removeAll(files);

        Set<String> removedUris = new HashSet<>(files);
        removedUris.removeAll(collectedUris);

        if (hasDataSourcesServer()) {
            if (addedUris.size() > 0)
                addUrisInChunks(source, addedUris);
            if (removedUris.size() > 0)
                removeUris(source, removedUris);
        }

        log.info(format("Added %d URIs, removed %d URIs out of %d URIs", addCount, removeCount, collectedUris.size()));
    }

    private String toXml(DataSource dataSource, Collection<String> uris, DownloadableType type) throws IOException {
        DatasourceType datasourceType = asDatasourceType(dataSource);

        for (String uri : uris) {
            switch (type) {
                case File:
                    FileType fileType = createFileType(uri, null, null);
                    datasourceType.getFile().add(fileType);
                    break;

                case Map:
                    MapType mapType = createMapType(uri, null, null);
                    datasourceType.getMap().add(mapType);
                    break;

                case Theme:
                    ThemeType themeType = createThemeType(uri, null, null);
                    datasourceType.getTheme().add(themeType);
                    break;
            }
        }

        return DataSourcesUtil.toXml(datasourceType);
    }

    private void addUrisInChunks(DataSource dataSource, Collection<String> uris) throws IOException {
        Collection<String> chunk = new HashSet<>();
        for(String uri : uris) {
            chunk.add(uri);

            if(chunk.size() >= MAXIMUM_UPDATE_COUNT) {
                addUris(dataSource, chunk);
                chunk.clear();
            }
        }

        if (chunk.size() > 0)
            addUris(dataSource, chunk);
    }

    private String addUris(DataSource dataSource, Collection<String> uris) throws IOException {
        String xml = toXml(dataSource, uris, type);
        log.info(format("Adding URIs:%n%s", xml));
        String dataSourcesUrl = getDataSourcesUrl();
        Post request = new Post(dataSourcesUrl, getCredentials());
        request.addFile("file", xml.getBytes(UTF8_ENCODING));
        request.setAccept(APPLICATION_JSON);
        request.setSocketTimeout(SOCKET_TIMEOUT);

        String result = null;
        try {
            result = request.executeAsString();
            log.info(format("Added URIs with result:%n%s", result));
            addCount += uris.size();
        }
        catch(Exception e) {
            log.severe(format("Cannot add URIs: %s", e));
        }
        return result;
    }

    private String removeUris(DataSource dataSource, Set<String> uris) throws IOException {
        String xml = toXml(dataSource, uris, type);
        log.info(format("Removing URIs:%n%s", xml));
        String dataSourcesUrl = getDataSourcesUrl();
        Delete request = new Delete(dataSourcesUrl, getCredentials());
        request.addFile("file", xml.getBytes(UTF8_ENCODING));
        request.setAccept(APPLICATION_JSON);

        String result = null;
        try {
            result = request.executeAsString();
            log.info(format("Removed URIs with result:%n%s", result));
            removeCount += uris.size();
        }
        catch(Exception e) {
            log.severe(format("Cannot remove URIs: %s", e));
        }
        return result;
    }

    private void run(String[] args) throws Exception {
        CommandLine line = parseCommandLine(args);
        String typeArgument = line.getOptionValue(TYPE_ARGUMENT);
        setId(line.getOptionValue(ID_ARGUMENT));
        setUrl(line.getOptionValue(URL_ARGUMENT));
        baseUrl = line.getOptionValue(BASE_URL_ARGUMENT);
        if (baseUrl == null)
            baseUrl = getUrl();
        String[] extensionArguments = line.getOptionValues(EXTENSION_ARGUMENT);
        extensions = extensionArguments != null ? new HashSet<>(asList(extensionArguments)) : null;
        String[] includeArguments = line.getOptionValues(INCLUDE_ARGUMENT);
        includes = includeArguments != null ? new HashSet<>(asList(includeArguments)) : null;
        String[] excludeArguments = line.getOptionValues(EXCLUDE_ARGUMENT);
        excludes = excludeArguments != null ? new HashSet<>(asList(excludeArguments)) : null;
        type = typeArgument != null ? DownloadableType.fromValue(typeArgument) : File;
        setDataSourcesServer(line.getOptionValue(DATASOURCES_SERVER_ARGUMENT));
        setDataSourcesUserName(line.getOptionValue(DATASOURCES_USERNAME_ARGUMENT));
        setDataSourcesPassword(line.getOptionValue(DATASOURCES_PASSWORD_ARGUMENT));
        scan();
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private CommandLine parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption(Option.builder().argName(ID_ARGUMENT).hasArgs().required().longOpt("id").
                desc("ID of the data source").build());
        options.addOption(Option.builder().argName(URL_ARGUMENT).numberOfArgs(1).required().longOpt("url").
                desc("URL to scan for resources").build());
        options.addOption(Option.builder().argName(BASE_URL_ARGUMENT).numberOfArgs(1).longOpt("baseUrl").
                desc("URL to use as a base for resources").build());
        options.addOption(Option.builder().argName(EXTENSION_ARGUMENT).hasArgs().longOpt("extension").
                desc("Extensions to scan for").build());
        options.addOption(Option.builder().argName(INCLUDE_ARGUMENT).hasArgs().longOpt("include").
                desc("Regex for resources to include").build());
        options.addOption(Option.builder().argName(EXCLUDE_ARGUMENT).hasArgs().longOpt("exclude").
                desc("Regex for resources to exclude").build());
        options.addOption(Option.builder().argName(TYPE_ARGUMENT).numberOfArgs(1).longOpt("type").
                desc("Type of the resources").build());
        options.addOption(Option.builder().argName(DATASOURCES_SERVER_ARGUMENT).numberOfArgs(1).longOpt("server").
                desc("Data sources server").build());
        options.addOption(Option.builder().argName(DATASOURCES_USERNAME_ARGUMENT).numberOfArgs(1).longOpt("username").
                desc("Data sources server user name").build());
        options.addOption(Option.builder().argName(DATASOURCES_PASSWORD_ARGUMENT).numberOfArgs(1).longOpt("password").
                desc("Data sources server password").build());
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
        exit(0);
    }
}
