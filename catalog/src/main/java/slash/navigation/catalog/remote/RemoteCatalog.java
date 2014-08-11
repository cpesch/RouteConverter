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

package slash.navigation.catalog.remote;

import slash.navigation.catalog.domain.Catalog;
import slash.navigation.catalog.domain.Category;
import slash.navigation.catalog.domain.exception.NotFoundException;
import slash.navigation.catalog.domain.exception.NotOwnerException;
import slash.navigation.gpx.binding11.*;
import slash.navigation.rest.*;
import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.rest.exception.UnAuthorizedException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Logger;

import static slash.common.io.Transfer.asUtf8;
import static slash.common.io.Transfer.decodeUri;
import static slash.navigation.gpx.GpxUtil.marshal11;
import static slash.navigation.gpx.GpxUtil.unmarshal11;

/**
 * Encapsulates REST access to the RemoteCatalog service of RouteConverter.
 *
 * @author Christian Pesch
 */

public class RemoteCatalog implements Catalog {
    private static final Logger log = Logger.getLogger(RemoteCatalog.class.getName());

    private static final String ROOT_CATEGORY_URI = "categories/.gpx";
    private static final String ROUTES_URI = "routes/";
    private static final String FILES_URI = "files/";

    private final String rootUrl;
    private final Credentials credentials;

    public RemoteCatalog(String rootUrl, Credentials credentials) {
        this.rootUrl = rootUrl;
        this.credentials = credentials;
    }

    public Category getRootCategory() {
        return new RemoteCategory(this, rootUrl + ROOT_CATEGORY_URI, "");
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

    private static final ObjectFactory gpxFactory = new ObjectFactory();

    private static GpxType createGpxType() {
        GpxType gpxType = gpxFactory.createGpxType();
        gpxType.setCreator("RouteCatalog Client");
        gpxType.setVersion("1.1");
        return gpxType;
    }

    private static String createCategoryXml(String parentUrl, String name) {
        MetadataType metadataType = gpxFactory.createMetadataType();
        metadataType.setName(asUtf8(name));
        if (parentUrl != null)
            metadataType.setKeywords(asUtf8(decodeUri(parentUrl)));

        GpxType gpxType = createGpxType();
        gpxType.setMetadata(metadataType);
        return toXml(gpxType);
    }

    private static String createRouteXml(String category, String description, String fileUrl) {
        MetadataType metadataType = gpxFactory.createMetadataType();
        if (description != null)
            metadataType.setDesc(asUtf8(description));
        if (category != null)
            metadataType.setKeywords(asUtf8(decodeUri(category)));

        GpxType gpxType = createGpxType();
        gpxType.setMetadata(metadataType);

        RteType rteType = gpxFactory.createRteType();
        LinkType linkType = gpxFactory.createLinkType();
        linkType.setHref(fileUrl);
        rteType.getLink().add(linkType);
        gpxType.getRte().add(rteType);

        return toXml(gpxType);
    }

    private static String toXml(GpxType gpxType) {
        StringWriter writer = new StringWriter();
        try {
            marshal11(gpxType, writer);
        } catch (JAXBException e) {
            throw new RuntimeException("Cannot marshall " + gpxType + ": " + e, e);
        }
        return writer.toString();
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
        log.fine("Deleting " + url);
        return new Delete(url, credentials);
    }

    private Post prepareAddCategory(String categoryUrl, String name) throws IOException {
        categoryUrl = ensureEndsWithSlash(removeDotGpx(categoryUrl));
        log.fine("Adding " + name + " to " + categoryUrl);
        String xml = createCategoryXml(null, name);
        Post request = new Post(categoryUrl, credentials);
        request.addFile("file", xml.getBytes());
        return request;
    }

    String addCategory(String categoryUrl, String name) throws IOException {
        Post request = prepareAddCategory(categoryUrl, name);
        String result = request.executeAsString();
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
        log.fine("Updating " + categoryUrl + " to " + parentUrl + " with name " + name);
        String xml = createCategoryXml(parentUrl, name);
        Put request = new Put(categoryUrl, credentials);
        request.addFile("file", xml.getBytes());
        return request;
    }

    String updateCategory(String categoryUrl, String parentUrl, String name) throws IOException {
        Put request = prepareUpdateCategory(categoryUrl, parentUrl, name);
        String result = request.executeAsString();
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
        String result = request.executeAsString();
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
        log.fine("Adding file " + file.getAbsolutePath());
        Post request = new Post(getFilesUrl(), credentials);
        request.addFile("file", file);
        return request;
    }

    public String addFile(File file) throws IOException {
        Post request = prepareAddFile(file);
        String result = request.executeAsString();
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
        String result = request.executeAsString();
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
        log.fine("Adding " + fileUrl + " to category " + categoryUrl + " with description " + description);
        String xml = createRouteXml(categoryUrl, description, fileUrl);
        Post request = new Post(getRoutesUrl(), credentials);
        request.addFile("file", xml.getBytes());
        return request;
    }

    private Put prepareUpdateRoute(String categoryUrl, String routeUrl, String description, String fileUrl) throws IOException {
        log.fine("Updating " + routeUrl + " to " + categoryUrl + "," + description + "," + fileUrl);
        String xml = createRouteXml(categoryUrl, description, fileUrl);
        Put request = new Put(routeUrl, credentials);
        request.addFile("file", xml.getBytes());
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
        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot add route " + description, getRoutesUrl());
        if (request.isForbidden())
            throw new NotOwnerException("Cannot add route " + description, getRoutesUrl());
        if (!request.isSuccessful())
            throw new IOException("POST on " + getRoutesUrl() + " with route " + description + "," + categoryUrl + "," + fileUrl + " not successful: " + result);
        return request.getLocation();
    }

    public void updateRoute(String categoryUrl, String routeUrl, String description, String fileUrl) throws IOException {
        Put request = prepareUpdateRoute(categoryUrl, routeUrl, description, fileUrl);
        String result = request.executeAsString();
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
        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Cannot delete route", routeUrl);
        if (request.isNotFound())
            throw new NotFoundException("Cannot delete route", routeUrl);
        if (request.isForbidden())
            throw new NotOwnerException("Cannot delete route", routeUrl);
        if (!request.isSuccessful())
            throw new IOException("DELETE on " + routeUrl + " not successful: " + result);
    }
}
