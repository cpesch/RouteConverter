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
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.File;
import slash.navigation.datasources.Map;
import slash.navigation.datasources.binding.*;
import slash.navigation.download.Checksum;
import slash.navigation.download.tools.base.BaseDataSourcesServerTool;
import slash.navigation.graphhopper.PbfUtil;
import slash.navigation.maps.helpers.MapUtil;
import slash.navigation.rest.Get;
import slash.navigation.rest.Head;
import slash.navigation.rest.Post;
import slash.navigation.rest.exception.ForbiddenException;
import slash.navigation.rest.exception.UnAuthorizedException;

import javax.xml.bind.JAXBException;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static slash.common.io.Files.generateChecksum;
import static slash.common.io.Transfer.formatTime;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.datasources.DataSourcesUtil.*;
import static slash.navigation.maps.helpers.MapUtil.*;

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
    private static final int MAXIMUM_UPDATE_COUNT = 10;
    private static final String DOT_MAP = ".map";
    private static final String DOT_PBF = ".pbf";
    private static final String DOT_ZIP = ".zip";


    private void update() throws IOException, JAXBException {
        DataSource source = loadDataSource(getId());

        DatasourceType datasourceType = asDatasourceType(source);
        for (File file : source.getFiles()) {
            String url = source.getBaseUrl() + file.getUri();
            if (isDownload(file)) {
                Checksum checksum = extractChecksum(url);
                if (checksum != null)
                    datasourceType.getFile().add(createFileType(file.getUri(), checksum, null));

            } else {
                Checksum checksum = extractContentLengthAndLastModified(url);
                if (checksum != null) {
                    FileType fileType = createFileType(file.getUri(), checksum, null);
                    datasourceType.getFile().add(fileType);

                    if (file.getUri().endsWith(DOT_PBF)) {
                        BoundingBox boundingBox = extractBoundingBox(url);
                        if (boundingBox != null)
                            fileType.setBoundingBox(asBoundingBoxType(boundingBox));
                    }
                }
            }

            updatePartially(datasourceType);
        }

        for (Map map : source.getMaps()) {
            String url = source.getBaseUrl() + map.getUri();
            Checksum checksum = extractContentLengthAndLastModified(url);
            if (checksum != null) {
                java.io.File file = downloadPartial(url, checksum.getContentLength());

                if (map.getUri().endsWith(DOT_ZIP)) {
                    MapType mapType = createMapType(map.getUri(), checksum, null);
                    datasourceType.getMap().add(mapType);

                    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file))) {
                        ZipEntry entry = zipInputStream.getNextEntry();
                        while (entry != null) {
                            if (!entry.isDirectory() && entry.getName().endsWith(DOT_MAP)) {
                                log.info("Found map " + entry.getName() + " in " + map.getUri());
                                mapType.getFragment().add(createFragmentType(entry.getName(), entry.getTime(), entry.getSize()));

                                BoundingBox boundingBox = MapUtil.extractBoundingBox(zipInputStream, entry.getSize());
                                if (boundingBox != null)
                                    mapType.setBoundingBox(asBoundingBoxType(boundingBox));

                                // do not close zip input stream and cope with partially copied zips
                                try {
                                    zipInputStream.closeEntry();
                                } catch (EOFException e) {
                                    // intentionally left empty
                                }
                            }

                            try {
                                entry = zipInputStream.getNextEntry();
                            } catch (EOFException e) {
                                entry = null;
                            }
                        }
                    }

                } else {
                    log.info("Found map " + map.getUri());
                    BoundingBox boundingBox = MapUtil.extractBoundingBox(file);
                    datasourceType.getMap().add(createMapType(map.getUri(), checksum, boundingBox));
                }

                if (!file.delete())
                    throw new IOException(format("Could not delete temporary map file '%s'", file));
            }

            updatePartially(datasourceType);
        }

        updateUris(datasourceType);
    }

    private void updatePartially(DatasourceType datasourceType) throws IOException {
        int count = datasourceType.getFile().size() + datasourceType.getMap().size() + datasourceType.getTheme().size();
        if(count >= MAXIMUM_UPDATE_COUNT) {
            updateUris(datasourceType);

            datasourceType.getFile().clear();
            datasourceType.getMap().clear();
            datasourceType.getTheme().clear();
        }
    }

    private boolean isDownload(Downloadable downloadable) {
        return downloadable.getLatestChecksum() != null &&
                downloadable.getLatestChecksum().getContentLength() != null &&
                downloadable.getLatestChecksum().getContentLength() < MAXIMUM_DOWNLOAD_SIZE;
    }

    private Checksum extractContentLengthAndLastModified(String url) {
        log.info("Extracting content length and last modified from " + url);
        try {
            Head request = new Head(url);
            request.executeAsString();
            if (request.isSuccessful()) {
                return new Checksum(request.getLastModified() != null ? fromMillis(request.getLastModified()) : null, request.getContentLength(), null);
            }
        } catch (IOException e) {
            log.warning("Error while extracting content length and last modified: " + e.getMessage());
        }
        return null;
    }

    private Checksum extractChecksum(String url) {
        log.info("Extracting checksum from " + url);
        try {
            Get request = new Get(url);
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

    private java.io.File downloadPartial(String url, long fileSize) throws IOException {
        log.info("Downloading " + PEEK_MAP_HEADER_SIZE + " bytes from " + url);
        try {
            Get get = new Get(url);
            get.setRange(0L, PEEK_MAP_HEADER_SIZE);
            InputStream inputStream = get.executeAsStream();
            java.io.File file = writePartialFile(inputStream, fileSize);
            closeQuietly(inputStream);
            get.release();
            return file;
        } catch (IOException e) {
            log.warning("Error while extracting bounding box: " + e.getMessage());
        }
        return null;
    }

    private BoundingBox extractBoundingBox(String url) throws IOException {
        log.info("Extracting bounding box from " + url);
        try {
            Get get = new Get(url);
            get.setRange(0L, PEEK_PBF_HEADER_SIZE);
            InputStream inputStream = get.executeAsStream();
            BoundingBox boundingBox = PbfUtil.extractBoundingBox(inputStream);
            inputStream.close();
            closeQuietly(inputStream);
            get.release();
            return boundingBox;
        } catch (IOException e) {
            log.warning("Error while extracting bounding box: " + e.getMessage());
        }
        return null;
    }

    private FileType createFileType(String uri, Checksum checksum, BoundingBox boundingBox) throws IOException {
        FileType fileType = new ObjectFactory().createFileType();
        fileType.setUri(uri);
        fileType.setBoundingBox(asBoundingBoxType(boundingBox));
        fileType.getChecksum().add(asChecksumType(checksum));
        return fileType;
    }

    private MapType createMapType(String uri, Checksum checksum, BoundingBox boundingBox) throws IOException {
        MapType mapType = new ObjectFactory().createMapType();
        mapType.setUri(uri);
        mapType.setBoundingBox(asBoundingBoxType(boundingBox));
        mapType.getChecksum().add(asChecksumType(checksum));
        return mapType;
    }

    private FragmentType createFragmentType(String key, Long lastModified, Long contentLength) throws IOException {
        FragmentType fragmentType = new ObjectFactory().createFragmentType();
        fragmentType.setKey(key);
        fragmentType.getChecksum().add(createChecksumType(lastModified, contentLength));
        return fragmentType;
    }

    private ChecksumType createChecksumType(Long lastModified, Long contentLength, InputStream inputStream) throws IOException {
        ChecksumType result = new ChecksumType();
        result.setLastModified(lastModified != null ? formatTime(fromMillis(lastModified), true) : null);
        result.setContentLength(contentLength);
        if (inputStream != null)
            result.setSha1(generateChecksum(inputStream));
        return result;
    }

    private ChecksumType createChecksumType(Long lastModified, Long contentLength) throws IOException {
        return createChecksumType(lastModified, contentLength, null);
    }

    private String createXml(DatasourceType datasourceType) throws IOException {
        CatalogType catalogType = new ObjectFactory().createCatalogType();
        catalogType.getDatasource().add(datasourceType);
        return toXml(catalogType);
    }

    private String updateUris(DatasourceType dataSourceType) throws IOException {
        String xml = createXml(dataSourceType);
        log.info(format("Updating URIs:\n%s", xml));
        String dataSourcesUrl = getDataSourcesUrl();
        Post request = new Post(dataSourcesUrl, getCredentials());
        request.addFile("file", xml.getBytes());
        request.setAccept("application/xml");
        request.setSocketTimeout(900 * 1000);

        String result = request.executeAsString();
        log.info(format("Updated URIs with result:\n%s", result));
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot add uris " + dataSourceType, dataSourcesUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Cannot add uris " + dataSourceType, dataSourcesUrl);
        if (!request.isSuccessful())
            throw new IOException("POST on " + dataSourcesUrl + " with payload " + dataSourceType + " not successful: " + result);
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
