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

package slash.navigation.catalog.domain;

import slash.navigation.catalog.domain.exception.DuplicateNameException;
import slash.navigation.catalog.domain.exception.NotFoundException;
import slash.navigation.catalog.domain.exception.NotOwnerException;
import slash.navigation.catalog.domain.exception.UnAuthorizedException;
import slash.navigation.gpx.GpxUtil;
import slash.navigation.gpx.binding11.*;
import slash.navigation.gpx.routecatalog10.UserextensionType;
import slash.navigation.util.Files;
import slash.navigation.util.InputOutput;
import slash.navigation.rest.*;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.logging.Logger;

/**
 * Encapsulates REST access to the route service.
 *
 * @author Christian Pesch
 */

public class RouteService {
    protected static Logger log = Logger.getLogger(RouteService.class.getName());

    public static final String ROOT_CATEGORY_URI = "categories/.gpx";
    public static final String ROUTES_URI = "routes/";
    public static final String FILES_URI = "files/";
    public static final String USERS_URI = "users/";

    private String rootUrl;
    private String userName, password;

    public RouteService(String rootUrl, String userName, String password) {
        this.rootUrl = rootUrl;
        setAuthentication(userName, password);
    }

    public RouteService(String rootUrl) {
        this(rootUrl, "anonymous", null);
    }

    public void setAuthentication(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public Category getRootCategory() {
        return new Category(this, rootUrl + ROOT_CATEGORY_URI);
    }

    GpxType fetchGpx(String url) throws IOException {
        log.fine(System.currentTimeMillis() + " fetching gpx from " + url);
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

    File createTempFile(String fileName) throws IOException {
        if (fileName == null)
            fileName = "route.file";
        String decodedName = Helper.decodeUri(fileName);
        String prefix = Files.removeExtension(decodedName);
        if (prefix.length() < 3)
            prefix = "rcc" + prefix;
        File file = File.createTempFile(prefix, Files.getExtension(decodedName));
        File tmp = new File(file.getParentFile(), decodedName);
        if (!tmp.exists()) {
            if (file.renameTo(tmp))
                file = tmp;
        }
        return file;
    }

    File fetchFile(String url) throws IOException {    // TODO use InputStream
        log.fine(System.currentTimeMillis() + " fetching " + url);
        Get get = new Get(url);
        InputStream in = get.executeAsStream();
        if (get.isSuccessful()) {
            String attachmentFileName = get.getAttachmentFileName();
            File file = createTempFile(attachmentFileName);
            file.deleteOnExit();
            InputOutput inOut = new InputOutput(in, new FileOutputStream(file));
            inOut.start();
            inOut.close();
            return file;
        } else
            return null;
    }

    private static final ObjectFactory gpxFactory = new ObjectFactory();
    private static final slash.navigation.gpx.routecatalog10.ObjectFactory rcFactory = new slash.navigation.gpx.routecatalog10.ObjectFactory();

    public static GpxType createGpxType() {
        GpxType gpxType = gpxFactory.createGpxType();
        gpxType.setCreator("RouteCatalog Client");
        gpxType.setVersion("1.1");
        return gpxType;
    }

    public static String createCategoryXml(String parentUrl, String name) throws UnsupportedEncodingException {
        MetadataType metadataType = gpxFactory.createMetadataType();
        metadataType.setName(Helper.asUtf8(name));
        if (parentUrl != null)
            metadataType.setKeywords(Helper.asUtf8(Helper.decodeUri(parentUrl)));

        GpxType gpxType = createGpxType();
        gpxType.setMetadata(metadataType);
        return toXml(gpxType);
    }

    public static String createRouteXml(String category, String description, String fileUrl) throws UnsupportedEncodingException {
        MetadataType metadataType = gpxFactory.createMetadataType();
        metadataType.setDesc(Helper.asUtf8(description));
        metadataType.setKeywords(Helper.asUtf8(Helper.decodeUri(category)));

        GpxType gpxType = createGpxType();
        gpxType.setMetadata(metadataType);

        RteType rteType = gpxFactory.createRteType();
        LinkType linkType = gpxFactory.createLinkType();
        linkType.setHref(fileUrl);
        rteType.getLink().add(linkType);
        gpxType.getRte().add(rteType);

        return toXml(gpxType);
    }

    public static String createUserXml(String userName, String password, String firstName, String lastName, String email) throws UnsupportedEncodingException {
        MetadataType metadataType = gpxFactory.createMetadataType();
        metadataType.setName(Helper.asUtf8(userName));

        UserextensionType userextensionType = rcFactory.createUserextensionType();
        userextensionType.setEmail(Helper.asUtf8(email));
        userextensionType.setFirstname(Helper.asUtf8(firstName));
        userextensionType.setLastname(Helper.asUtf8(lastName));
        userextensionType.setPassword(Helper.asUtf8(password));

        ExtensionsType extensionsType = gpxFactory.createExtensionsType();
        extensionsType.getAny().add(userextensionType);
        metadataType.setExtensions(extensionsType);

        GpxType gpxType = createGpxType();
        gpxType.setMetadata(metadataType);

        return toXml(gpxType);
    }

    public static String toXml(GpxType gpxType) {
        StringWriter writer = new StringWriter();
        try {
            GpxUtil.marshal11(gpxType, writer);
        } catch (JAXBException e) {
            throw new RuntimeException("Cannot marshall " + gpxType + ": " + e.getMessage(), e);
        }
        return writer.toString();
    }

    private File writeToTempFile(String string) throws IOException {
        File tempFile = File.createTempFile("rcclient", ".xml");
        tempFile.deleteOnExit();
        FileWriter fileWriter = new FileWriter(tempFile);
        fileWriter.write(string);
        fileWriter.close();
        return tempFile;
    }

    private String ensureEndsWithSlash(String url) {
        if (!url.endsWith("/"))
            url += "/";
        return url;
    }

    private String removeDotGpx(String url) {
        if (url.endsWith(".gpx"))
            url = url.substring(0, url.length() - 4);
        return url;
    }

    private Delete prepareDelete(String url) {
        log.fine(System.currentTimeMillis() + " deleting " + url);
        Delete request = new Delete(url);
        request.setAuthentication(userName, password);
        return request;
    }

    private Post prepareAddCategory(String categoryUrl, String name) throws IOException {
        categoryUrl = ensureEndsWithSlash(removeDotGpx(categoryUrl)); // // TODO removeDotGpx is silly
        log.fine(System.currentTimeMillis() + " adding " + name + " to " + categoryUrl);
        String xml = createCategoryXml(null, name);
        Post request = new Post(categoryUrl);
        request.setAuthentication(userName, password);
        request.setParameter("file", writeToTempFile(xml));
        return request;
    }

    String addCategory(String categoryUrl, String name) throws IOException {
        Post request = prepareAddCategory(categoryUrl, name);
        String result = request.execute();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot add category " + name, categoryUrl);
        if (request.isNotFound())
            throw new NotFoundException("Cannot add category " + name, categoryUrl);
        if (request.isForbidden())
            throw new DuplicateNameException("Cannot add category " + name, categoryUrl);
        if (!request.isSuccessful())
            throw new IOException("POST on " + categoryUrl + " with payload " + name + " not successful: " + result);
        return request.getLocation();
    }

    private Put prepareUpdateCategory(String categoryUrl, String parentUrl, String name) throws IOException {
        log.fine(System.currentTimeMillis() + " updating " + categoryUrl + " to " + parentUrl + " with name " + name);
        String xml = createCategoryXml(parentUrl, name);
        Put request = new Put(categoryUrl);
        request.setAuthentication(userName, password);
        request.setParameter("file", writeToTempFile(xml));
        return request;
    }

    String updateCategory(String categoryUrl, String parentUrl, String name) throws IOException {
        Put request = prepareUpdateCategory(categoryUrl, parentUrl, name);
        String result = request.execute();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot update category to " + name, categoryUrl);
        if (request.isNotFound())
            throw new NotFoundException("Cannot update category to " + name, categoryUrl);
        if (request.isForbidden())
            throw new NotOwnerException("Cannot update category to " + name, categoryUrl);
        if (!request.isSuccessful())
            throw new IOException("PUT on " + categoryUrl + " with payload " + name + " not successful: " + result);
        return request.getLocation();
    }

    void deleteCategory(String categoryUrl) throws IOException {
        Delete request = prepareDelete(categoryUrl);
        String result = request.execute();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot delete category", categoryUrl);
        if (request.isNotFound())
            throw new NotFoundException("Cannot delete category", categoryUrl);
        if (request.isForbidden())
            throw new NotOwnerException("Cannot delete category", categoryUrl);
        if (!request.isSuccessful())
            throw new IOException("DELETE on " + categoryUrl + " not successful: " + result);
    }

    private String getFilesUrl() {
        return rootUrl + FILES_URI;
    }

    private Post prepareAddFile(File file) throws IOException {
        log.fine(System.currentTimeMillis() + " adding " + file.getAbsolutePath());
        Post request = new Post(getFilesUrl());
        request.setAuthentication(userName, password);
        request.setParameter("file", file);
        return request;
    }

    String addFile(File file) throws IOException {
        Post request = prepareAddFile(file);
        String result = request.execute();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot add file " + file.getAbsolutePath(), getFilesUrl());
        if (request.isForbidden())
            throw new DuplicateNameException("Cannot add file " + file.getAbsolutePath(), getFilesUrl());
        if (!request.isSuccessful())
            throw new IOException("POST on " + getFilesUrl() + " with file " + file.getAbsolutePath() + " not successful: " + result);
        return request.getLocation();
    }

    void deleteFile(String fileUrl) throws IOException {
        if (!fileUrl.startsWith(rootUrl)) {
            log.fine("Ignoring delete on " + fileUrl + " since it's not part of " + rootUrl);
            return;
        }

        Delete request = prepareDelete(fileUrl);
        String result = request.execute();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot delete file", fileUrl);
        if (request.isNotFound())
            throw new NotFoundException("Cannot delete file", fileUrl);
        if (request.isForbidden())
            throw new NotOwnerException("Cannot delete file", fileUrl);
        if (!request.isSuccessful())
            throw new IOException("DELETE on " + fileUrl + " not successful: " + result);
    }

    private String getRoutesUrl() {
        return rootUrl + ROUTES_URI;
    }

    private Post prepareAddRoute(String categoryUrl, String description, String fileUrl) throws IOException {
        log.fine(System.currentTimeMillis() + " adding " + fileUrl + " to category " + categoryUrl + " with description " + description);
        String xml = createRouteXml(categoryUrl, description, fileUrl);
        Post request = new Post(getRoutesUrl());
        request.setAuthentication(userName, password);
        request.setParameter("file", writeToTempFile(xml));
        return request;
    }

    private Put prepareUpdateRoute(String categoryUrl, String routeUrl, String description, String fileUrl) throws IOException {
        log.fine(System.currentTimeMillis() + " updating " + routeUrl + " to " + categoryUrl + "," + description + "," + fileUrl);
        String xml = createRouteXml(categoryUrl, description, fileUrl);
        Put request = new Put(routeUrl);
        request.setAuthentication(userName, password);
        request.setParameter("file", writeToTempFile(xml));
        return request;
    }

    String addRouteAndFile(String categoryUrl, String description, File file) throws IOException {
        // 1. POST for File
        String fileUrl = addFile(file);

        // 2. POST for Route with category, description and file
        return addRoute(categoryUrl, description, fileUrl);
    }

    public String addRoute(String categoryUrl, String description, String fileUrl) throws IOException {
        Post request = prepareAddRoute(categoryUrl, description, fileUrl);
        String result = request.execute();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot add route " + description, getRoutesUrl());
        if (request.isForbidden())
            throw new NotOwnerException("Cannot add route " + description, getRoutesUrl());
        if (!request.isSuccessful())
            throw new IOException("POST on " + getRoutesUrl() + " with route " + description + "," + categoryUrl + "," + fileUrl + " not successful: " + result);
        return request.getLocation();
    }

    void updateRoute(String categoryUrl, String routeUrl, String description, String fileUrl) throws IOException {
        Put request = prepareUpdateRoute(categoryUrl, routeUrl, description, fileUrl);
        String result = request.execute();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot update route to " + description, routeUrl);
        if (request.isNotFound())
            throw new NotFoundException("Cannot update route to " + description, routeUrl);
        if (request.isForbidden())
            throw new NotOwnerException("Cannot update route to " + description, routeUrl);
        if (!request.isSuccessful())
            throw new IOException("PUT on " + routeUrl + " with payload " + description + " not successful: " + result);
    }

    void deleteRoute(String routeUrl) throws IOException {
        Delete request = prepareDelete(routeUrl);
        String result = request.execute();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot delete route", routeUrl);
        if (request.isNotFound())
            throw new NotFoundException("Cannot delete route", routeUrl);
        if (request.isForbidden())
            throw new NotOwnerException("Cannot delete route", routeUrl);
        if (!request.isSuccessful())
            throw new IOException("DELETE on " + routeUrl + " not successful: " + result);
    }


    private String getUsersUrl() {
        return rootUrl + USERS_URI;
    }

    private Post prepareAddUser(String userName, String password, String firstName, String lastName, String email) throws IOException {
        log.fine(System.currentTimeMillis() + " adding " + userName + "," + firstName + "," + lastName + "," + email);
        String xml = createUserXml(userName, password, firstName, lastName, email);
        Post request = new Post(getUsersUrl());
        request.setAuthentication(userName, password);
        request.setParameter("file", writeToTempFile(xml));
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
}
