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
import slash.navigation.rest.Delete;
import slash.navigation.rest.Get;
import slash.navigation.rest.Post;
import slash.navigation.rest.SimpleCredentials;
import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.rest.exception.UnAuthorizedException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Collection;
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
    private static final String ID_ARGUMENT = "id";
    private static final String BASE_URL_ARGUMENT = "baseUrl";
    private static final String EXTENSION_ARGUMENT = "extension";
    private static final String DATASOURCES_SERVER_ARGUMENT = "server";
    private static final String DATASOURCES_USERNAME_ARGUMENT = "username";
    private static final String DATASOURCES_PASSWORD_ARGUMENT = "password";

    private String id, url, baseUrl, datasourcesServer, datasourcesUserName, datasourcesPassword;
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
        for (File file : source.getFiles())
            result.add(file.getUri());
        for (Map map : source.getMaps())
            result.add(map.getUri());
        for (Theme theme : source.getThemes())
            result.add(theme.getUri());
        return result;
    }

    private void scan() throws IOException, JAXBException {
        List<String> collectedUris = collectUris();
        log.info("Collected URIs: " + collectedUris + " (" + collectedUris.size() + " elements)");

        DataSourceService service = loadDataSources(getDataSourcesDirectory());
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

        if (datasourcesServer != null && datasourcesUserName != null && datasourcesPassword != null) {
            addUris(source, addedUris);
            removeUris(source, removedUris);
        }
    }

    private String getDataSourcesUrl() {
        return datasourcesServer + "/datasources/datasources/";
    }

    private static DatasourceType asDatasourceType(DataSource dataSource, Collection<String> uris) {
        ObjectFactory objectFactory = new ObjectFactory();

        DatasourceType datasourceType = objectFactory.createDatasourceType();
        datasourceType.setId(dataSource.getId());
        datasourceType.setName(dataSource.getName());
        datasourceType.setBaseUrl(dataSource.getBaseUrl());
        datasourceType.setDirectory(dataSource.getDirectory());

        for (String uri : uris) {
            FileType fileType = objectFactory.createFileType();
            // fileType.setBoundingBox(asBoundingBoxType(aFile.getBoundingBox()));
            fileType.setUri(uri);
            // replaceChecksumTypes(fileType.getChecksum(), filterChecksums(aFile, fileToFragments.keySet()));
            // replaceFragmentTypes(fileType.getFragment(), aFile.getFragments(), fileToFragments);
            datasourceType.getFile().add(fileType);
        }

        // TODO: maps, themes, fragments, checksums?
        return datasourceType;
    }

    private String createXml(DataSource dataSource, Collection<String> uris) throws IOException {
        slash.navigation.datasources.binding.ObjectFactory objectFactory = new slash.navigation.datasources.binding.ObjectFactory();

        CatalogType datasourcesType = objectFactory.createCatalogType();
        DatasourceType datasourceType = asDatasourceType(dataSource, uris);
        datasourcesType.getDatasource().add(datasourceType);

        return DataSourcesUtil.toXml(datasourcesType);
    }

    public String addUris(DataSource dataSource, Collection<String> uris) throws IOException {
        log.fine("Adding " + uris);
        String xml = createXml(dataSource, uris);
        String dataSourcesUrl = getDataSourcesUrl();
        Post request = new Post(dataSourcesUrl, new SimpleCredentials(datasourcesUserName, datasourcesPassword));
        request.addFile("file", xml.getBytes());

        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot add uris " + uris, dataSourcesUrl);
        if (request.isForbidden())
            throw new DuplicateNameException("Cannot add uris " + uris, dataSourcesUrl);
        if (!request.isSuccessful())
            throw new IOException("POST on " + dataSourcesUrl + " with payload " + uris + " not successful: " + result);
        return result;
    }

    private String removeUris(DataSource dataSource, Set<String> uris) throws IOException {
        log.fine("Removing " + uris);
        String xml = createXml(dataSource, uris);
        String dataSourcesUrl = getDataSourcesUrl();
        Delete request = new Delete(dataSourcesUrl, new SimpleCredentials(datasourcesUserName, datasourcesPassword));
        // TODO NOBODY request.addFile("file", xml.getBytes());

        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot remove uris " + uris, dataSourcesUrl);
        if (request.isForbidden())
            throw new DuplicateNameException("Cannot remove uris " + uris, dataSourcesUrl);
        if (!request.isSuccessful())
            throw new IOException("DELETE on " + dataSourcesUrl + " with payload " + uris + " not successful: " + result);
        return result;
    }


    private void run(String[] args) throws Exception {
        CommandLine line = parseCommandLine(args);
        String[] extensionArguments = line.getOptionValues(EXTENSION_ARGUMENT);
        id = line.getOptionValue(ID_ARGUMENT);
        url = line.getOptionValue(URL_ARGUMENT);
        baseUrl = line.getOptionValue(BASE_URL_ARGUMENT);
        if (baseUrl == null)
            baseUrl = url;
        extensions = extensionArguments != null ? new HashSet<>(asList(extensionArguments)) : null;
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
