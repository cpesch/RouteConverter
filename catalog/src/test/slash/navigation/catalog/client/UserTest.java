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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.catalog.client;

import slash.navigation.gpx.GpxUtil;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.routecatalog10.UserextensionType;
import slash.navigation.rest.Get;
import slash.navigation.rest.HttpRequest;
import slash.navigation.rest.Post;
import slash.navigation.rest.Put;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class UserTest extends CatalogClientTest {

    private Get readUser(String user) throws IOException {
        return new Get(USERS_URL + user + GPX_URL_POSTFIX);
    }

    private Put updateUser(String key, String fileName,
                           String authenticationUserName, String authenticationPassword) throws IOException {
        Put request = new Put(USERS_URL + key + GPX_URL_POSTFIX);
        request.setAuthentication(authenticationUserName, authenticationPassword);
        request.setParameter("file", new File(TEST_PATH + fileName));
        return request;
    }

    protected Put updateUser(String key,
                             String userName, String password,
                             String firstName, String lastName, String email,
                             String authenticationUserName, String authenticationPassword) throws IOException, JAXBException {
        String xml = createUserXml(userName, password, firstName, lastName, email);

        Put request = new Put(USERS_URL + key + GPX_URL_POSTFIX);
        request.setAuthentication(authenticationUserName, authenticationPassword);
        request.setParameter("file", writeToTempFile(xml));
        return request;
    }


    public void testCreateFromFile() throws Exception {
        Post request = createUser("userstest.gpx");
        String result = request.execute();
        assertTrue(result.contains("user"));
        assertTrue(result.contains("created"));
        String location = request.getLocation();
        assertTrue(location.contains("/catalog/users/"));
        assertEquals(201, request.getResult());
        assertTrue(request.isSuccessful());
    }

    public void testCreateFromJAXB() throws Exception {
        Post request = createUser("ivan", "secret", "Ivan", "Secret", "ivan@secret.org", USERNAME, PASSWORD);
        String result = request.execute();
        assertTrue(result.contains("user"));
        assertTrue(result.contains("created"));
        String location = request.getLocation();
        assertTrue(location.contains("/catalog/users/"));
        assertEquals(201, request.getResult());
        assertTrue(request.isSuccessful());
    }

    public void testCreateWithSameNameNotAllowed() throws Exception {
        HttpRequest request1 = createUser("userstest.gpx");
        request1.execute();
        assertTrue(request1.isSuccessful());
        HttpRequest request2 = createUser("userstest.gpx");
        request2.execute();
        assertFalse(request2.isSuccessful());
    }

    public void testRead() throws Exception {
        Post request1 = createUser("userstest.gpx");
        request1.execute();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = readUser(key);
        String result2 = request2.execute();
        assertEquals(200, request2.getResult());
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

    public void testUpdate() throws Exception {
        Post request1 = createUser("userstest.gpx");
        request1.execute();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = updateUser(key, "alif", "topf", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD);
        String result2 = request2.execute();
        assertEquals("user alif updated", result2);
        assertEquals(200, request2.getResult());
        assertTrue(request2.isSuccessful());
        HttpRequest request3 = readUser("alif");
        String result3 = request3.execute();
        assertEquals(200, request3.getResult());
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

    public void testUpdateWithWrongPassword() throws Exception {
        Post request1 = createUser("userstest.gpx");
        request1.execute();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = updateUser(key, "alif", "topf", "Ali", "Top", "ali@top.org", "user-does-not-exist", "password-is-wrong");
        assertNull(request2.execute());
        assertEquals(401, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    public void testUpdateNotMyUser() throws Exception {
        createUser("alif", "topr", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD).execute();
        Post request1 = createUser("userstest.gpx");
        request1.execute();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = updateUser(key, "userstest.gpx", "alif", "topr");
        request2.execute();
        assertEquals(403, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }

    public void testDelete() throws Exception {
        Post request1 = createUser("userstest.gpx");
        request1.execute();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = deleteUser(key);
        String result2 = request2.execute();
        assertEquals("user ivan deleted", result2);
        assertEquals(200, request2.getResult());
        assertTrue(request2.isSuccessful());
        HttpRequest request3 = readUser(key);
        String result3 = request3.execute();
        assertNotNull(result3);
        assertEquals(404, request3.getResult());
        assertFalse(request3.isSuccessful());
    }

    public void testDeleteWithWrongPassword() throws Exception {
        Post request1 = createUser("userstest.gpx");
        request1.execute();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = deleteUser(key, "user-does-not-exist", "password-is-wrong");
        assertNull(request2.execute());
        assertEquals(401, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    public void testDeleteNotMyUser() throws Exception {
        createUser("alif", "stop", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD).execute();
        Post request1 = createUser("userstest.gpx");
        request1.execute();
        String key = parseUserKey(request1.getLocation());
        HttpRequest request2 = deleteUser(key, "alif", "stop");
        request2.execute();
        assertEquals(403, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }

}
