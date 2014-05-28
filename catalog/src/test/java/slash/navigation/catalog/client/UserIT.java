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

import org.junit.Test;
import slash.navigation.gpx.GpxUtil;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.routecatalog10.UserextensionType;
import slash.navigation.rest.Get;
import slash.navigation.rest.HttpRequest;
import slash.navigation.rest.Post;
import slash.navigation.rest.Put;
import slash.navigation.rest.SimpleCredentials;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UserIT extends RouteCatalogClientBase {

    private Get readUser(String user) {
        return new Get(USERS_URL + user + GPX_URL_POSTFIX);
    }

    private Put updateUser(String key, String fileName,
                           String authenticationUserName, String authenticationPassword) throws IOException {
        Put request = new Put(USERS_URL + key + GPX_URL_POSTFIX, new SimpleCredentials(authenticationUserName, authenticationPassword));
        request.addFile("file", new File(TEST_PATH + fileName));
        return request;
    }

    protected Put updateUser(String key,
                             String userName, String password,
                             String firstName, String lastName, String email,
                             String authenticationUserName, String authenticationPassword) throws IOException, JAXBException {
        String xml = createUserXml(userName, password, firstName, lastName, email);
        Put request = new Put(USERS_URL + key + GPX_URL_POSTFIX, new SimpleCredentials(authenticationUserName, authenticationPassword));
        request.addFile("file", xml.getBytes());
        return request;
    }


    @Test
    public void testCreateFromFile() throws Exception {
        Post request = createUser("userstest.gpx");
        String result = request.executeAsString();
        assertTrue(result.contains("user"));
        assertTrue(result.contains("created"));
        String location = request.getLocation();
        assertTrue(location.contains("/catalog/users/"));
        assertEquals(201, request.getStatusCode());
        assertTrue(request.isSuccessful());
    }

    @Test
    public void testCreateFromJAXB() throws Exception {
        Post request = createUser("ivan", "secret", "Ivan", "Secret", "ivan@secret.org", USERNAME, PASSWORD);
        String result = request.executeAsString();
        assertTrue(result.contains("user"));
        assertTrue(result.contains("created"));
        String location = request.getLocation();
        assertTrue(location.contains("/catalog/users/"));
        assertEquals(201, request.getStatusCode());
        assertTrue(request.isSuccessful());
    }

    @Test
    public void testCreateWithSameNameNotAllowed() throws Exception {
        HttpRequest request1 = createUser("userstest.gpx");
        request1.executeAsString();
        assertTrue(request1.isSuccessful());
        HttpRequest request2 = createUser("userstest.gpx");
        request2.executeAsString();
        assertFalse(request2.isSuccessful());
    }

    @Test
    public void testRead() throws Exception {
        Post request1 = createUser("userstest.gpx");
        request1.executeAsString();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = readUser(key);
        String result2 = request2.executeAsString();
        assertEquals(200, request2.getStatusCode());
        assertTrue(request2.isSuccessful());

        GpxType gpxType = GpxUtil.unmarshal11(result2);
        assertNotNull(gpxType);
        assertEquals(key, gpxType.getMetadata().getName());
        assertNull(gpxType.getMetadata().getDesc());
        assertNull(gpxType.getMetadata().getKeywords());
        List<Object> anys = gpxType.getMetadata().getExtensions().getAny();
        assertEquals(1, anys.size());
        JAXBElement any = (JAXBElement) anys.get(0);
        UserextensionType extension = (UserextensionType) any.getValue();
        assertEquals("ivan@secret.org", extension.getEmail());
        assertEquals("Ivan", extension.getFirstname());
        assertNotNull(extension.getLastlogin());
        assertEquals("Secret", extension.getLastname());
        assertNull(extension.getPassword());
    }

    @Test
    public void testUpdate() throws Exception {
        Post request1 = createUser("userstest.gpx");
        request1.executeAsString();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = updateUser(key, "alif", "topf", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD);
        String result2 = request2.executeAsString();
        assertEquals("user alif updated", result2);
        assertEquals(200, request2.getStatusCode());
        assertTrue(request2.isSuccessful());
        HttpRequest request3 = readUser("alif");
        String result3 = request3.executeAsString();
        assertEquals(200, request3.getStatusCode());
        assertTrue(request3.isSuccessful());

        GpxType gpxType = GpxUtil.unmarshal11(result3);
        assertNotNull(gpxType);
        assertEquals("alif", gpxType.getMetadata().getName());
        assertNull(gpxType.getMetadata().getDesc());
        assertNull(gpxType.getMetadata().getKeywords());
        List<Object> anys = gpxType.getMetadata().getExtensions().getAny();
        assertEquals(1, anys.size());
        JAXBElement any = (JAXBElement) anys.get(0);
        UserextensionType extension = (UserextensionType) any.getValue();
        assertEquals("ali@top.org", extension.getEmail());
        assertEquals("Ali", extension.getFirstname());
        assertNotNull(extension.getLastlogin());
        assertEquals("Top", extension.getLastname());
        assertNull(extension.getPassword());    }

    @Test
    public void testUpdateWithWrongPassword() throws Exception {
        Post request1 = createUser("userstest.gpx");
        request1.executeAsString();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = updateUser(key, "alif", "topf", "Ali", "Top", "ali@top.org", "user-does-not-exist", "password-is-wrong");
        assertNull(request2.executeAsString());
        assertEquals(401, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    @Test
    public void testUpdateNotMyUser() throws Exception {
        createUser("alif", "topr", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD).executeAsString();
        Post request1 = createUser("userstest.gpx");
        request1.executeAsString();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = updateUser(key, "userstest.gpx", "alif", "topr");
        request2.executeAsString();
        assertEquals(403, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }

    @Test
    public void testDelete() throws Exception {
        Post request1 = createUser("userstest.gpx");
        request1.executeAsString();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = deleteUser(key);
        String result2 = request2.executeAsString();
        assertEquals("user ivan deleted", result2);
        assertEquals(200, request2.getStatusCode());
        assertTrue(request2.isSuccessful());
        HttpRequest request3 = readUser(key);
        String result3 = request3.executeAsString();
        assertNotNull(result3);
        assertEquals(404, request3.getStatusCode());
        assertFalse(request3.isSuccessful());
    }

    @Test
    public void testDeleteWithWrongPassword() throws Exception {
        Post request1 = createUser("userstest.gpx");
        request1.executeAsString();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = deleteUser(key, "user-does-not-exist", "password-is-wrong");
        assertNull(request2.executeAsString());
        assertEquals(401, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    @Test
    public void testDeleteNotMyUser() throws Exception {
        createUser("alif", "stop", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD).executeAsString();
        Post request1 = createUser("userstest.gpx");
        request1.executeAsString();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = deleteUser(key, "alif", "stop");
        request2.executeAsString();
        assertEquals(403, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }

}
