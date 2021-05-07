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

package slash.navigation.routes.remote;

import slash.navigation.rest.*;
import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.rest.exception.ForbiddenException;
import slash.navigation.rest.exception.ServiceUnavailableException;
import slash.navigation.rest.exception.UnAuthorizedException;
import slash.navigation.routes.*;
import slash.navigation.routes.remote.binding.CatalogType;
import slash.navigation.routes.remote.binding.CategoryType;
import slash.navigation.routes.remote.binding.FileType;
import slash.navigation.routes.remote.binding.RouteType;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static java.lang.String.format;
import static slash.navigation.rest.HttpRequest.APPLICATION_JSON;
import static slash.navigation.routes.remote.helpers.RoutesUtil.unmarshal;

/**
 * Encapsulates REST access to the RemoteCatalog service of RouteConverter.
 *
 * @author Christian Pesch
 */

public class RemoteCatalog implements Catalog {
    private static final Logger log = Logger.getLogger(RemoteCatalog.class.getName());

    private static final String FORMAT_XML = "?format=xml";
    private static final String V1 = "v1/";
    public static final String CATEGORY_URI = V1 + "categories/";
    private static final String ROOT_CATEGORY_URI = CATEGORY_URI + "1/";
    public static final String ROUTE_URI = V1 + "routes/";
    public static final String FILE_URI = V1 + "files/";

    private final String rootUrl;
    private final Credentials credentials;

    public RemoteCatalog(String rootUrl, Credentials credentials) {
        this.rootUrl = rootUrl;
        this.credentials = credentials;
    }

    public Category getRootCategory() {
        return new RemoteCategory(this, rootUrl + ROOT_CATEGORY_URI, "");
    }

    CatalogType fetch(String url) throws IOException {
        long start = System.currentTimeMillis();
        String urlWithXml = url + FORMAT_XML;
        try {
            Get get = new Get(urlWithXml);
            String result = get.executeAsString();
            if (get.isSuccessful())
                try {
                    return unmarshal(result);
                } catch (JAXBException e) {
                    throw new IOException("Cannot unmarshall " + result + ": " + e, e);
                }
        }
        finally {
            long end = System.currentTimeMillis();
            log.info("Fetching from " + urlWithXml + " took " + (end - start) + " milliseconds");
        }
        return null;
    }

    /*for test only*/Category getCategory(String url) throws IOException {
        CatalogType catalogType = fetch(url);
        if (catalogType == null)
            return null;
        CategoryType categoryType = catalogType.getCategory();
        return new RemoteCategory(this, url, categoryType.getName());
    }

    /*for test only*/Route getRoute(String url) throws IOException {
        CatalogType catalogType = fetch(url);
        if (catalogType == null)
            return null;
        RouteType routeType = catalogType.getRoute();
        return new RemoteRoute(new RemoteCategory(this, routeType.getCategory()), url, routeType.getDescription(), routeType.getCreator(), routeType.getUrl());
    }

    /*for test only*/FileType getFile(String url) throws IOException {
        CatalogType catalogType = fetch(url);
        if (catalogType == null)
            return null;
        return catalogType.getFile();
    }

    String addCategory(String categoryUrl, String name) throws IOException {
        log.info(format("Adding category %s to %s", name, categoryUrl));
        Post request = new Post(rootUrl + CATEGORY_URI, credentials);
        request.setAccept(APPLICATION_JSON);
        request.addString("parent", categoryUrl);
        request.addString("name", name);
        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Not authorized to add category " + name, categoryUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Forbidden to add category " + name, categoryUrl);
        if (request.isBadRequest())
            throw new NotFoundException("Category not found", categoryUrl);
        if (request.isPreconditionFailed())
            throw new DuplicateNameException("Category " + name + " already exists", categoryUrl);
        if (!request.isSuccessful())
            throw new IOException("POST on " + (rootUrl + CATEGORY_URI) + " with payload " + name + " not successful: " + result);
        return request.getLocation();
    }

    void updateCategory(String categoryUrl, String parentUrl, String name) throws IOException {
        log.info(format("Updating category %s to parent %s and name %s", categoryUrl, parentUrl, name));
        Put request = new Put(categoryUrl, credentials);
        request.setAccept(APPLICATION_JSON);
        request.addString("parent", parentUrl);
        request.addString("name", name);
        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Not authorized to update category " + name, categoryUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Forbidden to update category " + name, categoryUrl);
        if (request.isNotFound())
            throw new NotFoundException("Category not found", categoryUrl);
        if (request.isBadRequest())
            throw new NotOwnerException("Not owner of category to update", categoryUrl);
        if (request.isPreconditionFailed())
            throw new DuplicateNameException("Category " + name + " already exists", categoryUrl);
        if (!request.isSuccessful())
            throw new IOException("PUT on " + categoryUrl + " with payload " + parentUrl + "/" + name + " not successful: " + result);
    }

    void deleteCategory(String categoryUrl) throws IOException {
        log.info(format("Deleting category %s", categoryUrl));
        Delete request = new Delete(categoryUrl, credentials);
        request.setAccept(APPLICATION_JSON);
        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Not authorized to delete category", categoryUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Forbidden to delete category", categoryUrl);
        if (request.isNotFound())
            throw new NotFoundException("Category not found", categoryUrl);
        if (request.isBadRequest())
            throw new NotOwnerException("Not owner of category to delete", categoryUrl);
        if (!request.isSuccessful())
            throw new IOException("DELETE on " + categoryUrl + " not successful: " + result);
    }

    String addRoute(String categoryUrl, String description, String localFile, String remoteUrl) throws IOException {
        log.info(format("Adding route %s to category %s with remote url %s", description, categoryUrl, remoteUrl));
        Post request = new Post(rootUrl + ROUTE_URI, credentials);
        request.setAccept(APPLICATION_JSON);
        request.addString("category", categoryUrl);
        request.addString("description", description);
        if (localFile != null)
            request.addString("localFile", localFile);
        if (remoteUrl != null)
            request.addString("remoteUrl", remoteUrl);
        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Not authorized to add route " + description, categoryUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Forbidden to add route " + description, categoryUrl);
        if (request.isBadRequest())
            throw new NotFoundException("Category not found", categoryUrl);
        if (request.isPreconditionFailed())
            throw new DuplicateNameException("Route " + description + " already exists", categoryUrl);
        if (!request.isSuccessful())
            throw new IOException("POST on " + (rootUrl + ROUTE_URI) + " with route " + description + "," + categoryUrl + "," + remoteUrl + " not successful: " + result);
        return request.getLocation();
    }

    void updateRoute(String routeUrl, String categoryUrl, String description, String localFile, String remoteUrl) throws IOException {
        log.info(format("Updating route %s to category %s and description %s with remote url %s", routeUrl, categoryUrl, description, remoteUrl));
        Put request = new Put(routeUrl, credentials);
        request.setAccept(APPLICATION_JSON);
        request.addString("category", categoryUrl);
        request.addString("description", description);
        if (localFile != null)
            request.addString("localFile", localFile);
        if (remoteUrl != null)
            request.addString("remoteUrl", remoteUrl);
        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Not authorized to update route", routeUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Forbidden to update route", routeUrl);
        if (request.isNotFound())
            throw new NotFoundException("Route not found", routeUrl);
        if (request.isBadRequest())
            throw new NotOwnerException("Not owner of route to update", routeUrl);
        if (request.isPreconditionFailed())
            throw new DuplicateNameException("Route " + description + " already exists", description);
        if (!request.isSuccessful())
            throw new IOException("PUT on " + routeUrl + " with route " + description + "," + categoryUrl + "," + remoteUrl + " not successful: " + result);
    }

    void deleteRoute(String routeUrl) throws IOException {
        log.info(format("Deleting route %s", routeUrl));
        Delete request = new Delete(routeUrl, credentials);
        request.setAccept(APPLICATION_JSON);
        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Not authorized to delete route", routeUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Forbidden to delete route", routeUrl);
        if (request.isNotFound())
            throw new NotFoundException("Route not found", routeUrl);
        if (request.isBadRequest())
            throw new NotOwnerException("Not owner of route to delete", routeUrl);
        if (!request.isSuccessful())
            throw new IOException("DELETE on " + routeUrl + " not successful: " + result);
    }

    String addFile(File file) throws IOException {
        log.info(format("Adding file %s", file));
        String fileUrl = rootUrl + FILE_URI;
        Post request = new Post(fileUrl, credentials);
        request.setAccept(APPLICATION_JSON);
        request.addString("name", file.getName());
        request.addFile("file", file);
        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Not authorized to add file " + file, fileUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Forbidden to add file " + file, fileUrl);
        if (request.isPreconditionFailed())
            throw new ServiceUnavailableException("File " + file + " is too large", fileUrl, result);
        if (!request.isSuccessful())
            throw new IOException("POST on " + fileUrl + " with file " + file + " not successful: " + result);
        return request.getLocation();
    }

    void deleteFile(String fileUrl) throws IOException {
        log.info(format("Adding file %s", fileUrl));
        Delete request = new Delete(fileUrl, credentials);
        String result = request.executeAsString();
        if (request.isUnAuthorized())
            throw new UnAuthorizedException("Not authorized to delete file", fileUrl);
        if (request.isForbidden())
            throw new ForbiddenException("Forbidden to delete file", fileUrl);
        if (request.isNotFound())
            throw new NotFoundException("File not found", fileUrl);
        if (request.isBadRequest())
            throw new NotOwnerException("Not owner of file to delete", fileUrl);
        if (!request.isSuccessful())
            throw new IOException("DELETE on " + fileUrl + " not successful: " + result);
    }
}
