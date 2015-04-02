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
import slash.navigation.common.BoundingBox;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourcesUtil;
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.File;
import slash.navigation.datasources.binding.CatalogType;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.FileType;
import slash.navigation.datasources.binding.ObjectFactory;
import slash.navigation.download.Checksum;
import slash.navigation.download.tools.base.BaseDataSourcesServerTool;
import slash.navigation.graphhopper.PbfUtil;
import slash.navigation.rest.Get;
import slash.navigation.rest.Head;
import slash.navigation.rest.Post;
import slash.navigation.rest.exception.ForbiddenException;
import slash.navigation.rest.exception.UnAuthorizedException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static slash.common.io.Files.generateChecksum;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.datasources.DataSourcesUtil.asChecksumType;
import static slash.navigation.maps.helpers.MapUtil.writeFile;
import static slash.navigation.maps.helpers.MapUtil.writePartialFile;

/**
 * Updates the resources from the DataSources catalog from websites
 *
 * @author Christian Pesch
 */

public class UpdateCatalog extends BaseDataSourcesServerTool {
    private static final Logger log = Logger.getLogger(ScanWebsite.class.getName());
    private static final long PEEK_MAP_HEADER_SIZE = 4 * 4096L;
    private static final long PEEK_PBF_HEADER_SIZE = 256L;
    private static final long MAXIMUM_DOWNLOAD_SIZE = 16 * 1024L;
    private static final int MAXIMUM_FILE_UPDATE_COUNT = 10;
    private static final String DOT_MAP = ".map";
    private static final String DOT_PBF = ".pbf";
    private static final String DOT_ZIP = ".zip";

    private void update() throws IOException, JAXBException {
        DataSource source = loadDataSource(getId());
        Map<Downloadable, Checksum> downloadableToChecksum = new HashMap<>();
        for (File file : source.getFiles()) {
            if (isDownload(file)) {
                Checksum checksum = extractChecksum(source.getBaseUrl(), file.getUri());
                if (checksum != null)
                    downloadableToChecksum.put(file, checksum);
            } else {
                Checksum checksum = extractContentLengthAndLastModified(source.getBaseUrl(), file.getUri());
                if (checksum != null)
                    downloadableToChecksum.put(file, checksum);
            }

            /*
            if (file.getUri().endsWith(DOT_MAP)) {
                ;
            } else if (file.getUri().endsWith(DOT_PBF)) {
                ;
            } else if (file.getUri().endsWith(DOT_ZIP)) {
                ;
            } else
                ;
            */

            if(downloadableToChecksum.size() > MAXIMUM_FILE_UPDATE_COUNT)      {
                updateUris(source, downloadableToChecksum);
                downloadableToChecksum.clear();
            }
        }

        if (downloadableToChecksum.size() > 0)
            updateUris(source, downloadableToChecksum);
    }

    private boolean isDownload(Downloadable downloadable) {
        return downloadable.getLatestChecksum() != null &&
                downloadable.getLatestChecksum().getContentLength() != null &&
                downloadable.getLatestChecksum().getContentLength() < MAXIMUM_DOWNLOAD_SIZE;
    }

    private Checksum extractContentLengthAndLastModified(String baseUrl, String uri) {
        log.info("Extracting content length and last modified from " + baseUrl + uri);
        try {
            Head request = new Head(baseUrl + uri);
            request.executeAsString();
            if (request.isSuccessful()) {
                return new Checksum(request.getLastModified() != null ? fromMillis(request.getLastModified()) : null, request.getContentLength(), null);
            }
        } catch (IOException e) {
            log.warning("Error while extracting content length and last modified: " + e.getMessage());
        }
        return null;
    }

    private Checksum extractChecksum(String baseUrl, String uri) {
        log.info("Extracting checksum from " + baseUrl + uri);
        try {
            Get request = new Get(baseUrl + uri);
            InputStream inputStream = request.executeAsStream();
            java.io.File file = writeFile(inputStream);
            closeQuietly(inputStream);
            String sha1 = generateChecksum(file);
            long contentLength = file.length();
            if(!file.delete())
                throw new IOException("Cannot delete " + file);
            return new Checksum(request.getLastModified() != null ? fromMillis(request.getLastModified()) : null, contentLength, sha1);
        } catch (IOException e) {
            log.warning("Error while extracting checksum: " + e.getMessage());
        }
        return null;
    }


    private BoundingBox extractBoundingBox(String baseUrl, String uri, int index) throws IOException {
        System.out.println(getClass().getSimpleName() + ": Extracting bounding box from " + baseUrl + uri + " (" + index + ")");
        try {
            Get get = new Get(baseUrl + uri);
            get.setRange(0L, PEEK_PBF_HEADER_SIZE);
            InputStream inputStream = get.executeAsStream();
            BoundingBox boundingBox = PbfUtil.extractBoundingBox(inputStream);
            inputStream.close();
            closeQuietly(inputStream);
            get.release();
            return boundingBox;
        } catch (IOException e) {
            System.err.println(getClass().getSimpleName() + ": " + e.getMessage());
        }
        return null;
    }

    private java.io.File downloadMapHeader(String baseUrl, String uri, long fileSize) throws IOException {
        System.out.println(getClass().getSimpleName() + ": Downloading map header from " + baseUrl + uri);
        Get get = new Get(baseUrl + uri);
        get.setRange(0L, PEEK_MAP_HEADER_SIZE);
        InputStream inputStream = get.executeAsStream();
        java.io.File file = writePartialFile(inputStream, fileSize);
        closeQuietly(inputStream);
        return file;
    }

    private DatasourceType asDatasourceType(DataSource dataSource, Map<Downloadable, Checksum> downloadableToChecksum) {
        ObjectFactory objectFactory = new ObjectFactory();

        DatasourceType datasourceType = objectFactory.createDatasourceType();
        datasourceType.setId(dataSource.getId());
        datasourceType.setName(dataSource.getName());
        datasourceType.setBaseUrl(dataSource.getBaseUrl());
        datasourceType.setDirectory(dataSource.getDirectory());

        for (Map.Entry<Downloadable, Checksum> entry : downloadableToChecksum.entrySet()) {
            if(entry.getKey() instanceof File) {
                FileType fileType = objectFactory.createFileType();
                fileType.setUri(entry.getKey().getUri());
                fileType.getChecksum().add(asChecksumType(entry.getValue()));
                // BoundingBox
                // Fragments
                datasourceType.getFile().add(fileType);
            }

            // TODO Fragments, ZIPs, Maps, Themes
        }
        return datasourceType;
    }

    private String createXml(DataSource dataSource, Map<Downloadable, Checksum> downloadableToChecksum) throws IOException {
        slash.navigation.datasources.binding.ObjectFactory objectFactory = new slash.navigation.datasources.binding.ObjectFactory();

        CatalogType catalogType = objectFactory.createCatalogType();
        DatasourceType datasourceType = asDatasourceType(dataSource, downloadableToChecksum);
        catalogType.getDatasource().add(datasourceType);

        return DataSourcesUtil.toXml(catalogType);
    }

    private String updateUris(DataSource dataSource, Map<Downloadable, Checksum> downloadableToChecksum) throws IOException {
        String xml = createXml(dataSource, downloadableToChecksum);
        log.info(format("Updating URIs %s:\n%s", downloadableToChecksum, xml));
        String dataSourcesUrl = getDataSourcesUrl();
        Post request = new Post(dataSourcesUrl, getCredentials());
        request.addFile("file", xml.getBytes());
        request.setAccept("application/xml");
        request.setSocketTimeout(900 * 1000);

        String result = request.executeAsString();
        log.info(format("Updated URIs %s with result:\n%s", downloadableToChecksum.size(), result));
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot add uris " + downloadableToChecksum, dataSourcesUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Cannot add uris " + downloadableToChecksum, dataSourcesUrl);
        if (!request.isSuccessful())
            throw new IOException("POST on " + dataSourcesUrl + " with payload " + downloadableToChecksum + " not successful: " + result);
        return result;
    }

    private void run(String[] args) throws Exception {
        CommandLine line = parseCommandLine(args);
        setId(line.getOptionValue(ID_ARGUMENT));
        setDatasourcesServer(line.getOptionValue(DATASOURCES_SERVER_ARGUMENT));
        setDatasourcesUserName(line.getOptionValue(DATASOURCES_USERNAME_ARGUMENT));
        setDatasourcesPassword(line.getOptionValue(DATASOURCES_PASSWORD_ARGUMENT));
        update();
        System.exit(0);
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private CommandLine parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName(ID_ARGUMENT).hasArgs().isRequired().withLongOpt("id").
                withDescription("ID of the data source").create());
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
        new UpdateCatalog().run(args);
    }
}
