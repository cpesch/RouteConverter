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

import slash.navigation.gpx.GpxUtil;
import slash.navigation.gpx.binding11.ExtensionsType;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.binding11.MetadataType;
import slash.navigation.gpx.binding11.ObjectFactory;
import slash.navigation.gpx.routecatalog10.UserextensionType;
import slash.navigation.rest.Credentials;
import slash.navigation.rest.Get;
import slash.navigation.rest.Post;
import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.rest.exception.UnAuthorizedException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Logger;

import static slash.common.io.Files.writeToTempFile;
import static slash.navigation.rest.Helper.asUtf8;

/**
 * Encapsulates REST access to the RouteFeedback service of RouteConverter.
 *
 * @author Christian Pesch
 */

public class RouteFeedback {
    private static final Logger log = Logger.getLogger(RouteFeedback.class.getName());

    private static final String USERS_URI = "users/";
    private static final String ERROR_REPORT_URI = "error-report/";

    private final String rootUrl;
    private final Credentials credentials;

    public RouteFeedback(String rootUrl, Credentials credentials) {
        this.rootUrl = rootUrl;
        this.credentials = credentials;
    }

    private static final ObjectFactory gpxFactory = new ObjectFactory();
    private static final slash.navigation.gpx.routecatalog10.ObjectFactory rcFactory = new slash.navigation.gpx.routecatalog10.ObjectFactory();

    private static GpxType createGpxType() {
        GpxType gpxType = gpxFactory.createGpxType();
        gpxType.setCreator("RouteFeedback Client");
        gpxType.setVersion("1.1");
        return gpxType;
    }

    private static String toXml(GpxType gpxType) {
        StringWriter writer = new StringWriter();
        try {
            GpxUtil.marshal11(gpxType, writer);
        } catch (JAXBException e) {
            throw new RuntimeException("Cannot marshall " + gpxType + ": " + e.getMessage(), e);
        }
        return writer.toString();
    }

    GpxType fetchGpx(String url) throws IOException {
        log.fine("Fetching gpx from " + url);
        Get get = new Get(url);
        String result = get.execute();
        if (get.isSuccessful())
            try {
                return GpxUtil.unmarshal11(result);
            } catch (JAXBException e) {
                IOException io = new IOException("Cannot unmarshall " + result + ": " + e.getMessage());
                io.setStackTrace(e.getStackTrace());
                throw io;
            }
        else
            return null;
    }

    private static String createUserXml(String userName, String password, String firstName, String lastName, String email) {
        MetadataType metadataType = gpxFactory.createMetadataType();
        metadataType.setName(asUtf8(userName));

        UserextensionType userextensionType = rcFactory.createUserextensionType();
        userextensionType.setEmail(asUtf8(email));
        userextensionType.setFirstname(asUtf8(firstName));
        userextensionType.setLastname(asUtf8(lastName));
        userextensionType.setPassword(asUtf8(password));

        ExtensionsType extensionsType = gpxFactory.createExtensionsType();
        extensionsType.getAny().add(userextensionType);
        metadataType.setExtensions(extensionsType);

        GpxType gpxType = createGpxType();
        gpxType.setMetadata(metadataType);

        return toXml(gpxType);
    }

    private String getUsersUrl() {
        return rootUrl + USERS_URI;
    }

    private Post prepareAddUser(String userName, String password, String firstName, String lastName, String email) throws IOException {
        log.fine("Adding " + userName + "," + firstName + "," + lastName + "," + email);
        String xml = createUserXml(userName, password, firstName, lastName, email);
        Post request = new Post(getUsersUrl(), credentials);
        request.addFile("file", writeToTempFile(xml));
        return request;
    }

    public String addUser(String userName, String password, String firstName, String lastName, String email) throws IOException {
        Post request = prepareAddUser(userName, password, firstName, lastName, email);
        String result = request.execute();
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

    private Post prepareSendErrorReport(String log, String description, File file) throws IOException {
        RouteFeedback.log.fine("Sending error report with log \"" + log + "\", description \"" + description +
                "\"" + (file != null ? ", file " + file.getAbsolutePath() : ""));
        Post request = new Post(getErrorReportUrl(), credentials);
        request.addString("log", log);
        request.addString("description", description);
        if (file != null)
            request.addFile("file", file);
        return request;
    }

    public String sendErrorReport(String log, String description, File file) throws IOException {
        Post request = prepareSendErrorReport(log, description, file);
        String result = request.execute();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot send error report " + file.getAbsolutePath(), getErrorReportUrl());
        if (!request.isSuccessful())
            throw new IOException("POST on " + getErrorReportUrl() + " with log " + log.length() + " characters" +
                    ", description \"" + description + "\", file " + file + " not successful: " + result);
        return request.getLocation();
    }
}
