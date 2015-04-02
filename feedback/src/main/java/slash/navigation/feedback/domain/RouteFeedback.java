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

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourcesUtil;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.gpx.GpxUtil;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.rest.*;
import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.rest.exception.UnAuthorizedException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Locale.getDefault;
import static slash.common.io.Files.printArrayToDialogString;
import static slash.navigation.gpx.GpxUtil.unmarshal11;

/**
 * Encapsulates REST access to the RouteFeedback service of RouteConverter.
 *
 * @author Christian Pesch
 */

public class RouteFeedback {
    private static final Logger log = Logger.getLogger(RouteFeedback.class.getName());

    private static final String ERROR_REPORT_URI = "error-report/";
    private static final String UPDATE_CHECK_URI = "update-check/";
    private static final String USERS_URI = "users/";

    private final String rootUrl;
    private final String apiUrl;
    private final Credentials credentials;

    public RouteFeedback(String rootUrl, String apiUrl, Credentials credentials) {
        this.rootUrl = rootUrl;
        this.apiUrl = apiUrl;
        this.credentials = credentials;
    }

    GpxType fetchGpx(String url) throws IOException {
        log.fine("Fetching gpx from " + url);
        Get get = new Get(url);
        String result = get.executeAsString();
        if (get.isSuccessful())
            try {
                return unmarshal11(result);
            } catch (JAXBException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        else
            return null;
    }

    private String getUsersUrl() {
        return rootUrl + USERS_URI;
    }

    public String addUser(String userName, String password, String firstName, String lastName, String email) throws IOException {
        log.fine("Adding " + userName + "," + firstName + "," + lastName + "," + email);
        String xml = GpxUtil.createXml(userName, password, firstName, lastName, email);
        Post request = new Post(getUsersUrl(), credentials);
        request.addFile("file", xml.getBytes());

        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot add user " + userName, getUsersUrl());
        if (request.isForbidden())
            throw new DuplicateNameException("Cannot add user " + userName, getUsersUrl());
        if (!request.isSuccessful())
            throw new IOException("POST on " + getUsersUrl() + " with payload " + userName + "," + firstName + "," + lastName + "," + email + " not successful: " + result);
        return request.getLocation();
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
                                 String webstartVersion, long startTime) throws IOException {
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
        if (webstartVersion != null)
            request.addString("webstartVersion", webstartVersion);
        return request.executeAsString().replace("\"", "");
    }

    private String getDataSourcesUrl(String dataSourceId) {
        return apiUrl + "v1/datasources/" + dataSourceId + "/";
    }

    public String sendChecksums(DataSource dataSource, Map<FileAndChecksum, List<FileAndChecksum>> fileToFragments, String... filterUrls) throws IOException {
        String xml = DataSourcesUtil.createXml(dataSource, fileToFragments, filterUrls);
        log.info(format("Sending checksums for %s filtered with %s:\n%s", fileToFragments, printArrayToDialogString(filterUrls), xml));
        String dataSourcesUrl = getDataSourcesUrl(dataSource.getId());
        Put request = new Put(dataSourcesUrl, credentials);
        request.addFile("file", xml.getBytes());

        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot send checksums ", dataSourcesUrl);
        if (!request.isSuccessful())
            throw new IOException("PUT on " + dataSourcesUrl + " for data source " + dataSource + " not successful: " + result);

        log.info(format("Sent checksum for %s filtered with %s with result:\n%s", fileToFragments, printArrayToDialogString(filterUrls), result));
        return result;
    }
}
