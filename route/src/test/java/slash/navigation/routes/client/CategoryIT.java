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
package slash.navigation.routes.client;

import org.junit.Test;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.rest.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;
import static slash.common.io.Transfer.encodeUri;
import static slash.navigation.gpx.GpxUtil.unmarshal11;

/** @deprecated replace by new remote tests */
public class CategoryIT extends RouteCatalogClientBase {

    private File writeToTempFile(String string) throws IOException {
        File tempFile = File.createTempFile("categoryit", ".xml");
        tempFile.deleteOnExit();
        FileWriter fileWriter = new FileWriter(tempFile);
        fileWriter.write(string);
        fileWriter.close();
        return tempFile;
    }

    private String parseCategoryKey(String result) {
        return result.substring(result.lastIndexOf("categories/") + 11, result.length() - GPX_URL_POSTFIX.length());
    }

    private Post createCategoryFromFile(String fileName,
                                        String authenticationUserName, String authenticationPassword) throws IOException {
        Post request = new Post(CATEGORIES_URL, new SimpleCredentials(authenticationUserName, authenticationPassword));
        request.addFile("file", new File(TEST_PATH + fileName));
        return request;
    }

    private Post createCategoryFromFile(String fileName) throws IOException {
        return createCategoryFromFile(fileName, USERNAME, PASSWORD);
    }

    private Post createCategory(String parent, String name,
                                String authenticationUserName, String authenticationPassword) throws IOException, JAXBException {
        String xml = createCategoryXml(name);

        Post request = new Post(CATEGORIES_URL + encodeUri(parent) + "/", new SimpleCredentials(authenticationUserName, authenticationPassword));
        request.addFile("file", writeToTempFile(xml));
        return request;
    }

    private Post createCategory(String parent, String name) throws IOException, JAXBException {
        return createCategory(parent, name, USERNAME, PASSWORD);
    }

    private Get readCategory(String key) {
        return new Get(CATEGORIES_URL + encodeUri(key) + GPX_URL_POSTFIX);
    }

    private Put updateCategory(String key, String name,
                               String authenticationUserName, String authenticationPassword) throws IOException, JAXBException {
        String xml = createCategoryXml(name);

        Put request = new Put(CATEGORIES_URL + encodeUri(key) + GPX_URL_POSTFIX, new SimpleCredentials(authenticationUserName, authenticationPassword));
        request.addFile("file", writeToTempFile(xml));
        return request;
    }

    private Put updateCategory(String key, String name) throws IOException, JAXBException {
        return updateCategory(key, name, USERNAME, PASSWORD);
    }

    @Test
    public void testCreateFromFile() throws Exception {
        Post request2 = createCategoryFromFile("categoriestest.gpx");
        String result = request2.executeAsString();
        assertTrue(result.contains("category"));
        assertTrue(result.contains("created"));
        String location = request2.getLocation();
        assertTrue(location.contains("/catalog/categories/"));
        assertEquals(201, request2.getStatusCode());
        assertTrue(request2.isSuccessful());
    }

    @Test
    public void testCreateRootCategoryFromJAXB() throws Exception {
        String name = "Category " + System.currentTimeMillis();
        Post request2 = createCategory("", name);
        String result = request2.executeAsString();
        assertTrue(result.contains("category"));
        assertTrue(result.contains(name));
        assertTrue(result.contains("created"));
        String location = request2.getLocation();
        assertTrue(location.contains("/catalog/categories/"));
        assertEquals(201, request2.getStatusCode());
        assertTrue(request2.isSuccessful());
    }

    @Test
    public void testCreateUploadCategoryFromJAXB() throws Exception {
        String name = "Category " + System.currentTimeMillis();
        Post request2 = createCategory("Upload", name);
        String result = request2.executeAsString();
        assertTrue(result.contains("category"));
        assertTrue(result.contains(name));
        assertTrue(result.contains("created"));
        String location = request2.getLocation();
        assertTrue(location.contains("/catalog/categories/"));
        assertEquals(201, request2.getStatusCode());
        assertTrue(request2.isSuccessful());
    }

    @Test
    public void testRead() throws Exception {
        Post request1 = createCategoryFromFile("categoriestest.gpx");
        request1.executeAsString();
        String key = parseCategoryKey(request1.getLocation());

        HttpRequest request2 = readCategory(key);
        String result2 = request2.executeAsString();
        assertEquals(200, request2.getStatusCode());
        assertTrue(request2.isSuccessful());

        GpxType gpxType = unmarshal11(result2);
        assertNotNull(gpxType);
        assertEquals("Interesting", gpxType.getMetadata().getName());
        assertEquals(USERNAME, gpxType.getMetadata().getAuthor().getName());
        assertEquals(USERS_URL + USERNAME + GPX_URL_POSTFIX, gpxType.getMetadata().getAuthor().getLink().getHref());
        assertNull(gpxType.getMetadata().getDesc());
        assertEquals(0, gpxType.getMetadata().getLink().size());
    }

    @Test
    public void testReadRoot() throws Exception {
        HttpRequest request1 = readCategory("");
        String result1 = request1.executeAsString();
        assertEquals(200, request1.getStatusCode());
        assertTrue(request1.isSuccessful());

        GpxType gpxType = unmarshal11(result1);
        assertNotNull(gpxType);
        assertEquals("", gpxType.getMetadata().getName());
        assertEquals(USERNAME, gpxType.getMetadata().getAuthor().getName());
        assertEquals(USERS_URL + USERNAME + GPX_URL_POSTFIX, gpxType.getMetadata().getAuthor().getLink().getHref());
        assertNull(gpxType.getMetadata().getDesc());
        assertTrue(gpxType.getMetadata().getLink().size() > 0);
    }

    @Test
    public void testReadWithSpaces() throws Exception {
        String name = "Category " + System.currentTimeMillis();
        Post request1 = createCategory("Upload", name);
        request1.executeAsString();
        String key = parseCategoryKey(request1.getLocation());

        HttpRequest request2 = readCategory(key);
        String result2 = request2.executeAsString();
        assertEquals(200, request2.getStatusCode());
        assertTrue(request2.isSuccessful());

        GpxType gpxType = unmarshal11(result2);
        assertNotNull(gpxType);
        assertEquals(name, gpxType.getMetadata().getName());
        assertEquals(USERNAME, gpxType.getMetadata().getAuthor().getName());
        assertEquals(USERS_URL + USERNAME + GPX_URL_POSTFIX, gpxType.getMetadata().getAuthor().getLink().getHref());
        assertNull(gpxType.getMetadata().getDesc());
        assertEquals(0, gpxType.getMetadata().getLink().size());
    }

    @Test
    public void testUpdate() throws Exception {
        Post request1 = createCategory("Upload", "Interesting");
        request1.executeAsString();
        String key = parseCategoryKey(request1.getLocation());
        String newName = "Interesting" + System.currentTimeMillis();
        Put request2 = updateCategory(key, newName);
        String result2 = request2.executeAsString();
        assertEquals("category /Upload/" + newName + " updated", result2);
        assertEquals(201, request2.getStatusCode());
        assertTrue(request2.isSuccessful());
        String newKey = parseCategoryKey(request2.getLocation());
        HttpRequest request3 = readCategory(newKey);
        String result3 = request3.executeAsString();
        assertEquals(200, request3.getStatusCode());
        assertTrue(request3.isSuccessful());
        GpxType gpxType = unmarshal11(result3);
        assertNotNull(gpxType);
    }

    @Test
    public void testUpdateWithWrongPassword() throws Exception {
        Post request1 = createCategory("Upload", "Interesting");
        request1.executeAsString();
        String key = parseCategoryKey(request1.getLocation());
        HttpRequest request2 = updateCategory(key, "Interesting" + System.currentTimeMillis(), "userdoesnotexist", "passwordiswrong");
        assertNull(request2.executeAsString());
        assertEquals(401, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    @Test
    public void testUpdateNotMyUser() throws Exception {
        createUser("alif", "topr", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD).executeAsString();
        Post request1 = createCategory("Upload", "Interesting");
        request1.executeAsString();
        String key = parseCategoryKey(request1.getLocation());
        HttpRequest request2 = updateCategory(key, "Interesting" + System.currentTimeMillis(), "alif", "topr");
        request2.executeAsString();
        assertEquals(403, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }

    @Test
    public void testDelete() throws Exception {
        Post request1 = createCategory("Upload", "Interesting");
        request1.executeAsString();
        String key = parseCategoryKey(request1.getLocation());
        HttpRequest request2 = deleteCategory(key);
        String result2 = request2.executeAsString();
        assertEquals("category /" + key + " deleted", result2);
        assertEquals(200, request2.getStatusCode());
        assertTrue(request2.isSuccessful());
        HttpRequest request3 = readCategory(key);
        String result3 = request3.executeAsString();
        assertNotNull(result3);
        assertEquals(404, request3.getStatusCode());
        assertFalse(request3.isSuccessful());
    }

    @Test
    public void testDeleteWithWrongPassword() throws Exception {
        Post request1 = createCategory("Upload", "Interesting");
        request1.executeAsString();
        String key = parseCategoryKey(request1.getLocation());
        HttpRequest request2 = deleteCategory(key, "userdoesnotexist", "passwordiswrong");
        assertNull(request2.executeAsString());
        assertEquals(401, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    @Test
    public void testDeleteNotMyUser() throws Exception {
        createUser("alif", "toup", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD).executeAsString();
        Post request1 = createCategory("Upload", "Interesting");
        request1.executeAsString();
        String key = parseCategoryKey(request1.getLocation());
        HttpRequest request2 = deleteCategory(key, "alif", "toup");
        request2.executeAsString();
        assertEquals(403, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }
}