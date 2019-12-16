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

package slash.navigation.feedback.domain;

import slash.navigation.datasources.*;
import slash.navigation.datasources.binding.*;
import slash.navigation.datasources.helpers.DataSourcesUtil;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.rest.Credentials;
import slash.navigation.rest.Delete;
import slash.navigation.rest.Post;
import slash.navigation.rest.Put;
import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.rest.exception.ForbiddenException;
import slash.navigation.rest.exception.UnAuthorizedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Locale.getDefault;
import static slash.navigation.datasources.DataSourceManager.DATASOURCES_URI;
import static slash.navigation.datasources.DataSourceManager.V1;
import static slash.navigation.datasources.helpers.DataSourcesUtil.*;
import static slash.navigation.rest.HttpRequest.APPLICATION_JSON;

/**
 * Encapsulates REST access to the RouteFeedback service of RouteConverter.
 *
 * @author Christian Pesch
 */

public class RouteFeedback {
    private static final Logger log = Logger.getLogger(RouteFeedback.class.getName());

    private static final String ERROR_REPORT_URI = "error-report/";
    private static final String UPDATE_CHECK_URI = "update-check/";
    static final String USER_URI = V1 + "users/";

    private final String rootUrl;
    private final String apiUrl;
    private final Credentials credentials;

    public RouteFeedback(String rootUrl, String apiUrl, Credentials credentials) {
        this.rootUrl = rootUrl;
        this.apiUrl = apiUrl;
        this.credentials = credentials;
    }

    public String addUser(String userName, String password, String firstName, String lastName, String email) throws IOException {
        log.info("Adding user " + userName + "," + firstName + "," + lastName + "," + email);
        Post request = new Post(apiUrl + USER_URI);
        request.setAccept(APPLICATION_JSON);
        request.addString("username", userName);
        request.addString("password", password);
        request.addString("first_name", firstName);
        request.addString("last_name", lastName);
        request.addString("email", email);
        String result = request.executeAsString();
        if (request.isBadRequest())
            throw new ForbiddenException("Cannot add user: " + result, apiUrl + USER_URI);
        if (request.isForbidden())
            throw new DuplicateNameException("User " + userName + " already exists", apiUrl + USER_URI);
        if (!request.isSuccessful())
            throw new IOException("POST on " + (apiUrl + USER_URI) + " with payload " + userName + "," + firstName + "," + lastName + "," + email + " not successful: " + result);
        return request.getLocation();
    }

    void deleteUser(String userUrl) throws IOException {
        log.info("Deleting user " + userUrl);
        Delete request = new Delete(userUrl, credentials);
        request.setAccept(APPLICATION_JSON);
        String result = request.executeAsString();
        if (request.isBadRequest())
            throw new ForbiddenException("Not authorized to delete user", userUrl);
        if (!request.isSuccessful())
            throw new IOException("DELETE on " + userUrl + " not successful: " + result);
    }

    private String getErrorReportUrl() {
        return rootUrl + ERROR_REPORT_URI;
    }

    public String sendErrorReport(String logOutput, String description, java.io.File file) throws IOException {
        log.fine("Sending error report with log \"" + logOutput + "\", description \"" + description + "\"" +
                (file != null ? ", file " + file.getAbsolutePath() : ""));
        Post request = new Post(getErrorReportUrl(), credentials);
        request.addString("log", logOutput);
        request.addString("description", description);
        if (file != null)
            request.addFile("file", file);

        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot send error report " + (file != null ? ", file " + file.getAbsolutePath() : ""), getErrorReportUrl());
        if (!request.isSuccessful())
            throw new IOException("POST on " + getErrorReportUrl() + " with log " + logOutput.length() + " characters" +
                    ", description \"" + description + "\", file " + file + " not successful: " + result);
        return request.getLocation();
    }

    public String checkForUpdate(String routeConverterVersion, String routeConverterBits, long startCount,
                                 String javaVersion, String javaBits,
                                 String osName, String osVersion, String osArch,
                                 long startTime) throws IOException {
        log.fine("Checking for update for version " + routeConverterVersion);
        Post request = new Post(rootUrl + UPDATE_CHECK_URI, credentials);
        request.addString("id", valueOf(startTime));
        request.addString("javaBits", javaBits);
        request.addString("javaVersion", javaVersion);
        request.addString("locale", getDefault().getLanguage());
        request.addString("osArch", osArch);
        request.addString("osName", osName);
        request.addString("osVersion", osVersion);
        request.addString("rcStartCount", Long.toString(startCount));
        request.addString("rcVersion", routeConverterVersion);
        request.addString("rcBits", routeConverterBits);
        return request.executeAsString().replace("\"", "");
    }

    private boolean contains(String[] array, String name) {
        for (String anArray : array) {
            if (name.equals(anArray))
                return true;

        }
        return false;
    }

    private Set<FileAndChecksum> findFile(Fragment fragment, java.util.Map<FileAndChecksum, List<FileAndChecksum>> fileAndChecksumMap) throws IOException {
        Set<FileAndChecksum> result = new HashSet<>();
        for (List<FileAndChecksum> fileAndChecksums : fileAndChecksumMap.values()) {
            for (FileAndChecksum fileAndChecksum : fileAndChecksums) {

                String filePath = asMetaDataComparablePath(fileAndChecksum.getFile());
                if (filePath.contains(fragment.getKey()))
                    result.add(fileAndChecksum);
            }
        }
        return result;
    }

    private List<FragmentType> createFragmentTypes(List<Fragment<Downloadable>> fragments, java.util.Map<FileAndChecksum, List<FileAndChecksum>> fileAndChecksums) throws IOException {
        if (fragments == null)
            return null;

        List<FragmentType> fragmentTypes = new ArrayList<>();
        for (Fragment fragment : fragments)
            fragmentTypes.add(createFragmentType(fragment, findFile(fragment, fileAndChecksums)));
        return fragmentTypes;
    }

    private String toXml(DataSource dataSource, java.util.Map<FileAndChecksum, List<FileAndChecksum>> fileToFragments, String... filterUrls) throws IOException {
        DatasourceType datasourceType = asDatasourceType(dataSource);

        for (File aFile : dataSource.getFiles()) {
            if (!contains(filterUrls, dataSource.getBaseUrl() + aFile.getUri()))
                continue;

            FileType fileType = createFileType(aFile.getUri(), asChecksums(fileToFragments.keySet()), aFile.getBoundingBox());
            List<FragmentType> fragmentTypes = createFragmentTypes(aFile.getFragments(), fileToFragments);
            if (fragmentTypes != null)
                fileType.getFragment().addAll(fragmentTypes);
            datasourceType.getFile().add(fileType);
        }

        for (slash.navigation.datasources.Map map : dataSource.getMaps()) {
            if (!contains(filterUrls, dataSource.getBaseUrl() + map.getUri()))
                continue;

            MapType mapType = createMapType(map.getUri(), asChecksums(fileToFragments.keySet()), map.getBoundingBox());
            List<FragmentType> fragmentTypes = createFragmentTypes(map.getFragments(), fileToFragments);
            if (fragmentTypes != null)
                mapType.getFragment().addAll(fragmentTypes);
            datasourceType.getMap().add(mapType);
        }

        for (Theme theme : dataSource.getThemes()) {
            if (!contains(filterUrls, dataSource.getBaseUrl() + theme.getUri()))
                continue;

            ThemeType themeType = createThemeType(theme.getUri(), asChecksums(fileToFragments.keySet()), theme.getImageUrl());
            List<FragmentType> fragmentTypes = createFragmentTypes(theme.getFragments(), fileToFragments);
            if (fragmentTypes != null)
                themeType.getFragment().addAll(fragmentTypes);
            datasourceType.getTheme().add(themeType);
        }

        return DataSourcesUtil.toXml(datasourceType);
    }

    private String getDataSourcesUrl(String dataSourceId) {
        return apiUrl + DATASOURCES_URI + dataSourceId + "/";
    }

    public String sendChecksums(DataSource dataSource, java.util.Map<FileAndChecksum, List<FileAndChecksum>> fileToFragments, String filterUrl) throws IOException {
        String xml = toXml(dataSource, fileToFragments, filterUrl);
        log.info(format("Sending checksums for %s filtered with %s:%n%s", fileToFragments, filterUrl, xml));
        String dataSourcesUrl = getDataSourcesUrl(dataSource.getId());
        Put request = new Put(dataSourcesUrl, credentials);
        request.setAccept(APPLICATION_JSON);
        request.addFile("file", xml.getBytes(StandardCharsets.UTF_8));

        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot send checksums", dataSourcesUrl);
        if (!request.isSuccessful())
            throw new IOException("PUT on " + dataSourcesUrl + " for data source " + dataSource + " not successful: " + result);

        log.info(format("Sent checksum for %s filtered with %s with result:%n%s", fileToFragments, filterUrl, result));
        return result;
    }
}
