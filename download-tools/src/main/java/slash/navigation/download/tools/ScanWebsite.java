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


import org.apache.commons.cli.*;
import slash.navigation.datasources.*;
import slash.navigation.datasources.binding.*;
import slash.navigation.download.tools.base.BaseDownloadTool;
import slash.navigation.download.tools.helpers.AnchorFilter;
import slash.navigation.download.tools.helpers.AnchorParser;
import slash.navigation.download.tools.helpers.DownloadableType;
import slash.navigation.rest.*;
import slash.navigation.rest.exception.ForbiddenException;
import slash.navigation.rest.exception.UnAuthorizedException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static slash.navigation.datasources.DataSourceManager.loadDataSources;
import static slash.navigation.download.tools.helpers.DownloadableType.File;

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

    private String id, url, baseUrl, datasourcesServer, datasourcesUserName, datasourcesPassword;
    private DownloadableType type;
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
            // create the anchor relative to the current uri
            String nextUri = appendURIs(uri, anchor);
            uris.add(nextUri);
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
        log.info("Collected URIs: " + collectedUris + " (" + collectedUris.size() + " elements)");

        DataSourceService service = loadDataSources(getDataSourcesDirectory());
        DataSource source = service.getDataSourceById(id);
        if (source == null)
            throw new IllegalArgumentException("Unknown data source: " + id);
        if (!url.equals(source.getBaseUrl()) && !baseUrl.equals(source.getBaseUrl()))
            throw new IllegalArgumentException("Data source URL: " + source.getBaseUrl() + " doesn't match URL: " + url);

        Set<String> files = collectURIs(source);

        Set<String> addedUris = new HashSet<>(collectedUris);
        addedUris.removeAll(files);

        Set<String> removedUris = new HashSet<>(files);
        removedUris.removeAll(collectedUris);

        log.info("Added URIs: " + addedUris + " (" + addedUris.size() + " elements)");
        log.info("Removed URIs: " + removedUris + " (" + removedUris.size() + " elements)");

        if (datasourcesServer != null && datasourcesUserName != null && datasourcesPassword != null) {
            if (addedUris.size() > 0)
                addUris(source, addedUris);
            if (removedUris.size() > 0)
                removeUris(source, removedUris);
        }
    }

    private String getDataSourcesUrl() {
        return datasourcesServer + "v1/datasources/" + id + "/";
    }

    private DatasourceType asDatasourceType(DataSource dataSource, Collection<String> uris) {
        ObjectFactory objectFactory = new ObjectFactory();

        DatasourceType datasourceType = objectFactory.createDatasourceType();
        datasourceType.setId(dataSource.getId());
        datasourceType.setName(dataSource.getName());
        datasourceType.setBaseUrl(dataSource.getBaseUrl());
        datasourceType.setDirectory(dataSource.getDirectory());

        for (String uri : uris) {
            switch (type) {
                case File:
                    FileType fileType = objectFactory.createFileType();
                    fileType.setUri(uri);
                    datasourceType.getFile().add(fileType);
                    break;

                case Map:
                    MapType mapType = objectFactory.createMapType();
                    mapType.setUri(uri);
                    datasourceType.getMap().add(mapType);
                    break;

                case Theme:
                    ThemeType themeType = objectFactory.createThemeType();
                    themeType.setUri(uri);
                    datasourceType.getTheme().add(themeType);
                    break;
            }
        }
        return datasourceType;
    }

    private String createXml(DataSource dataSource, Collection<String> uris) throws IOException {
        slash.navigation.datasources.binding.ObjectFactory objectFactory = new slash.navigation.datasources.binding.ObjectFactory();

        CatalogType datasourcesType = objectFactory.createCatalogType();
        DatasourceType datasourceType = asDatasourceType(dataSource, uris);
        datasourcesType.getDatasource().add(datasourceType);

        return DataSourcesUtil.toXml(datasourcesType);
    }

    private Credentials getCredentials() {
        return new SimpleCredentials(datasourcesUserName, datasourcesPassword);
    }

    public String addUris(DataSource dataSource, Collection<String> uris) throws IOException {
        String xml = createXml(dataSource, uris);
        log.info(format("Adding URIs %s:\n%s", uris, xml));
        String dataSourcesUrl = getDataSourcesUrl();
        Post request = new Post(dataSourcesUrl, getCredentials());
        request.addFile("file", xml.getBytes());
        request.setAccept("application/xml");
        request.setSocketTimeout(900 * 1000);

        String result = request.executeAsString();
        log.info(format("Added URIs %s with result:\n%s", uris, result));
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot add uris " + uris, dataSourcesUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Cannot add uris " + uris, dataSourcesUrl);
        if (!request.isSuccessful())
            throw new IOException("POST on " + dataSourcesUrl + " with payload " + uris + " not successful: " + result);
        return result;
    }

    private String removeUris(DataSource dataSource, Set<String> uris) throws IOException {
        String xml = createXml(dataSource, uris);
        log.info(format("Removing URIs %s:\n%s", uris, xml));
        String dataSourcesUrl = getDataSourcesUrl();
        Delete request = new Delete(dataSourcesUrl, getCredentials());
        request.addFile("file", xml.getBytes());
        request.setAccept("application/xml");

        String result = request.executeAsString();
        log.info(format("Removed URIs %s with result:\n%s", uris, result));
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot remove uris " + uris, dataSourcesUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Cannot remove uris " + uris, dataSourcesUrl);
        if (!request.isSuccessful())
            throw new IOException("DELETE on " + dataSourcesUrl + " with payload " + uris + " not successful: " + result);
        return result;
    }


    private void run(String[] args) throws Exception {
        CommandLine line = parseCommandLine(args);
        String[] extensionArguments = line.getOptionValues(EXTENSION_ARGUMENT);
        String typeArgument = line.getOptionValue(TYPE_ARGUMENT);
        id = line.getOptionValue(ID_ARGUMENT);
        url = line.getOptionValue(URL_ARGUMENT);
        baseUrl = line.getOptionValue(BASE_URL_ARGUMENT);
        if (baseUrl == null)
            baseUrl = url;
        extensions = extensionArguments != null ? new HashSet<>(asList(extensionArguments)) : null;
        type = typeArgument != null ? DownloadableType.fromValue(typeArgument) : File;
        datasourcesServer = line.getOptionValue(DATASOURCES_SERVER_ARGUMENT);
        datasourcesUserName = line.getOptionValue(DATASOURCES_USERNAME_ARGUMENT);
        datasourcesPassword = line.getOptionValue(DATASOURCES_PASSWORD_ARGUMENT);
        scan();
        System.exit(0);
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private CommandLine parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName(ID_ARGUMENT).hasArgs().isRequired().withLongOpt("id").
                withDescription("ID of the data source").create());
        options.addOption(OptionBuilder.withArgName(URL_ARGUMENT).hasArgs(1).isRequired().withLongOpt("url").
                withDescription("URL to scan for resources").create());
        options.addOption(OptionBuilder.withArgName(BASE_URL_ARGUMENT).hasArgs(1).withLongOpt("baseUrl").
                withDescription("URL to use as a base for resources").create());
        options.addOption(OptionBuilder.withArgName(EXTENSION_ARGUMENT).hasArgs().withLongOpt("extension").
                withDescription("Extensions to scan for").create());
        options.addOption(OptionBuilder.withArgName(TYPE_ARGUMENT).hasArgs(1).withLongOpt("type").
                withDescription("Type of the resources").create());
        options.addOption(OptionBuilder.withArgName(DATASOURCES_SERVER_ARGUMENT).hasArgs(1).withLongOpt("server").
                withDescription("Data sources server").create());
        options.addOption(OptionBuilder.withArgName(DATASOURCES_USERNAME_ARGUMENT).hasArgs(1).withLongOpt("username").
                withDescription("Data sources server user name").create());
        options.addOption(OptionBuilder.withArgName(DATASOURCES_PASSWORD_ARGUMENT).hasArgs(1).withLongOpt("password").
                withDescription("Data sources server password").create());
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
