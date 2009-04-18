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
package slash.navigation.catalog.client;

import slash.navigation.TestCase;
import slash.navigation.rest.Delete;
import slash.navigation.rest.Post;
import slash.navigation.gpx.GpxUtil;
import slash.navigation.gpx.binding11.*;
import slash.navigation.gpx.routecatalog10.UserextensionType;
import slash.navigation.util.InputOutput;

import javax.xml.bind.JAXBException;
import java.io.*;

public abstract class CatalogClientTest extends TestCase {
    protected static final String TEST_PATH = "..\\catalog\\src\\resources\\";
    // protected static final String HOST = "www.routeconverter.de";
    // protected static final String HOST = "localhost:8080";
    protected static final String HOST = "localhost:8000";
    protected static final String ROOT = "http://" + HOST + "/catalog/";
    protected static final String USERNAME = "routeconverter";
    protected static final String PASSWORD = "sts-cp";

    protected static final String CATEGORIES_URL = ROOT + "categories/";
    protected static final String ROUTES_URL = ROOT + "routes/";
    protected static final String FILES_URL = ROOT + "files/";
    protected static final String USERS_URL = ROOT + "users/";
    protected static final String GPX_URL_POSTFIX = ".gpx";
    protected static final String FILE_URL_POSTFIX = "/";

    private File tempFile;
    private ObjectFactory gpxFactory = new ObjectFactory();
    private slash.navigation.gpx.routecatalog10.ObjectFactory rcFactory = new slash.navigation.gpx.routecatalog10.ObjectFactory();


    protected void setUp() throws Exception {
        super.setUp();
        forceDeleteUser("ivan");
        forceDeleteUser("alif");
        forceDeleteCategory("Interesting");
        forceDeleteCategory("Upload/Interesting");
        tempFile = File.createTempFile("catalogclienttest", ".xml");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (tempFile != null)
            assertTrue(tempFile.delete());
    }


    protected String readFileToString(String fileName) throws IOException {
        FileInputStream fis = new FileInputStream(TEST_PATH + fileName);
        try {
            return new String(InputOutput.readBytes(fis), "ISO8859-1");
        }
        finally {
            fis.close();
        }
    }

    protected File writeToTempFile(String string) throws IOException {
        FileWriter fileWriter = new FileWriter(tempFile);
        fileWriter.write(string);
        fileWriter.close();
        return tempFile;
    }

    protected String parseCategoryKey(String result) {
        return result.substring(result.lastIndexOf("categories/") + 11, result.length() - GPX_URL_POSTFIX.length());
    }

    protected int parseRouteKey(String result) {
        return Integer.parseInt(result.substring(result.lastIndexOf('/') + 1, result.length() - GPX_URL_POSTFIX.length()));
    }

    protected int parseFileKey(String result) {
        if (result.endsWith(FILE_URL_POSTFIX))
            result = result.substring(0, result.length() - FILE_URL_POSTFIX.length());
        return Integer.parseInt(result.substring(result.lastIndexOf('/') + 1));
    }

    protected String parseUserKey(String result) {
        return result.substring(result.lastIndexOf('/') + 1, result.length() - GPX_URL_POSTFIX.length());
    }


    private GpxType createGpxType() {
        GpxType gpxType = gpxFactory.createGpxType();
        gpxType.setCreator("RouteCatalog Client");
        gpxType.setVersion("1.1");
        return gpxType;
    }

    private String toXml(GpxType gpxType) throws JAXBException {
        StringWriter writer = new StringWriter();
        GpxUtil.marshal11(gpxType, writer);
        return writer.toString();
    }

    protected String createCategoryXml(String name) throws JAXBException {
        MetadataType metadataType = gpxFactory.createMetadataType();

        metadataType.setName(name);

        GpxType gpxType = createGpxType();
        gpxType.setMetadata(metadataType);

        return toXml(gpxType);
    }

    protected String createRouteXml(String category, Integer fileKey, String description) throws JAXBException {
        MetadataType metadataType = gpxFactory.createMetadataType();

        metadataType.setDesc(description);
        metadataType.setKeywords(category);

        GpxType gpxType = createGpxType();
        gpxType.setMetadata(metadataType);

        if (fileKey != null) {
            RteType rteType = gpxFactory.createRteType();
            LinkType linkType = gpxFactory.createLinkType();
            linkType.setHref(FILES_URL + fileKey);
            rteType.getLink().add(linkType);
            gpxType.getRte().add(rteType);
        }

        return toXml(gpxType);
    }

    protected String createUserXml(String userName, String password, String firstName, String lastName, String email) throws JAXBException {
        MetadataType metadataType = gpxFactory.createMetadataType();
        metadataType.setName(userName);

        UserextensionType userextensionType = rcFactory.createUserextensionType();
        userextensionType.setEmail(email);
        userextensionType.setFirstname(firstName);
        userextensionType.setLastname(lastName);
        userextensionType.setPassword(password);

        ExtensionsType extensionsType = gpxFactory.createExtensionsType();
        extensionsType.getAny().add(userextensionType);
        metadataType.setExtensions(extensionsType);

        GpxType gpxType = createGpxType();
        gpxType.setMetadata(metadataType);
        return toXml(gpxType);
    }


    protected Delete deleteCategory(String key, String authenticationUserName, String authenticationPassword) {
        Delete request = new Delete(CATEGORIES_URL + key + GPX_URL_POSTFIX);
        request.setAuthentication(authenticationUserName, authenticationPassword);
        return request;
    }

    protected Delete deleteCategory(String key) {
        return deleteCategory(key, USERNAME, PASSWORD);
    }

    private void forceDeleteCategory(String categoryName) {
        try {
            deleteCategory(categoryName).execute(false);
        } catch (IOException e) {
            // intentionally left empty
        }
    }


    protected Post createRoute(String category, Integer fileKey, String description,
                               String authenticationUserName, String authenticationPassword) throws IOException, JAXBException {
        String xml = createRouteXml(category, fileKey, description);

        Post request = new Post(ROUTES_URL);
        request.setAuthentication(authenticationUserName, authenticationPassword);
        request.setParameter("file", writeToTempFile(xml));
        return request;
    }

    protected Post createRoute(String category, Integer fileKey, String description) throws IOException, JAXBException {
        return createRoute(category, fileKey, description, USERNAME, PASSWORD);
    }

    protected Post createUser(String fileName,
                              String authenticationUserName, String authenticationPassword) throws IOException {
        Post request = new Post(USERS_URL);
        request.setAuthentication(authenticationUserName, authenticationPassword);
        request.setParameter("file", new File(TEST_PATH + fileName));
        return request;
    }

    protected Post createUser(String fileName) throws IOException {
        return createUser(fileName, USERNAME, PASSWORD);
    }

    protected Post createUser(String userName, String password,
                              String firstName, String lastName, String email,
                              String authenticationUserName, String authenticationPassword) throws IOException, JAXBException {
        String xml = createUserXml(userName, password, firstName, lastName, email);

        Post request = new Post(USERS_URL);
        request.setAuthentication(authenticationUserName, authenticationPassword);
        request.setParameter("file", writeToTempFile(xml));
        return request;
    }

    protected Delete deleteUser(String userName,
                                String authenticationUserName, String authenticationPassword) throws IOException {
        Delete request = new Delete(USERS_URL + userName + GPX_URL_POSTFIX);
        request.setAuthentication(authenticationUserName, authenticationPassword);
        return request;
    }

    protected Delete deleteUser(String userName) throws IOException {
        return deleteUser(userName, USERNAME, PASSWORD);
    }

    private void forceDeleteUser(String userName) {
        try {
            deleteUser(userName).execute(false);
        } catch (IOException e) {
            // intentionally left empty
        }
    }


    protected Post createFile(String fileName,
                              String authenticationUserName, String authenticationPassword) throws IOException {
        Post request = new Post(FILES_URL);
        request.setAuthentication(authenticationUserName, authenticationPassword);
        request.setParameter("file", new File(TEST_PATH + fileName));
        return request;
    }

    protected Post createFile(String fileName) throws IOException {
        return createFile(fileName, USERNAME, PASSWORD);
    }
}
