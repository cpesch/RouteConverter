/*
  You may freely copy, distribute, modify and use this class as long
  as the original author attribution remains intact.  See message
  below.

  Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.catalog.client;

import org.junit.Test;
import slash.navigation.rest.Delete;
import slash.navigation.rest.Get;
import slash.navigation.rest.HttpRequest;
import slash.navigation.rest.Post;
import slash.navigation.rest.Put;
import slash.navigation.rest.SimpleCredentials;

import java.io.File;
import java.io.IOException;

import static java.lang.Integer.MAX_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FileIT extends RouteCatalogClientBase {

    private Get readFile(int key) {
        return new Get(FILES_URL + key + "/");
    }

    private Put updateFile(int key, String fileName, String authenticationUserName, String authenticationPassword) throws IOException {
        Put request = new Put(FILES_URL + key + "/", new SimpleCredentials(authenticationUserName, authenticationPassword));
        request.addFile("file", new File(TEST_PATH + fileName));
        return request;
    }

    private Put updateFile(int key, String fileName) throws IOException {
        return updateFile(key, fileName, USERNAME, PASSWORD);
    }

    private Delete deleteFile(int key,  String authenticationUserName, String authenticationPassword) {
        return new Delete(FILES_URL + key + "/", new SimpleCredentials(authenticationUserName, authenticationPassword));
    }

    private Delete deleteFile(int key) throws IOException {
        return deleteFile(key, USERNAME, PASSWORD);
    }


    @Test
    public void testCreate() throws Exception {
        Post request = createFile("filestest.gpx");
        String result = request.execute();
        assertTrue(result.contains("file"));
        assertTrue(result.contains("created"));
        String location = request.getLocation();
        assertTrue(location.contains("/catalog/files/"));
        assertEquals(201, request.getResult());
        assertTrue(request.isSuccessful());
    }

    @Test
    public void testCreateWithNotExistingUser() throws Exception {
        HttpRequest request = createFile("filestest.gpx", "user-does-not-exist", PASSWORD);
        assertNull(request.execute());
        assertEquals(401, request.getResult());
        assertFalse(request.isSuccessful());
        assertTrue(request.isUnAuthorized());
    }

    @Test
    public void testCreateWithWrongPassword() throws Exception {
        HttpRequest request = createFile("filestest.gpx", USERNAME, "password-is-wrong");
        assertNull(request.execute());
        assertEquals(401, request.getResult());
        assertFalse(request.isSuccessful());
        assertTrue(request.isUnAuthorized());
    }

    @Test
    public void testCreateCheckIncreasingIds() throws Exception {
        Post request1 = createFile("filestest.gpx");
        request1.execute();
        String result1 = request1.getLocation();
        Post request2 = createFile("filestest.gpx");
        request2.execute();
        String result2 = request2.getLocation();
        int key1 = parseFileKey(result1);
        int key2 = parseFileKey(result2);
        assertEquals(key1 + 1, key2);
    }

    @Test
    public void testRead() throws Exception {
        Post request1 = createFile("filestest.gpx");
        request1.execute();
        String location1 = request1.getLocation();
        assertNotNull(location1);
        int key = parseFileKey(location1);
        HttpRequest request2 = readFile(key);
        String location2 = request2.execute();
        assertEquals(200, request2.getResult());
        assertTrue(request2.isSuccessful());
        assertEquals(readFileToString("filestest.gpx"), location2);
    }

    @Test
    public void testReadNotExisting() throws Exception {
        HttpRequest request = readFile(MAX_VALUE);
        assertNotNull(request.execute());
        assertEquals(404, request.getResult());
        assertFalse(request.isSuccessful());
    }

    @Test
    public void testUpdate() throws Exception {
        Post request1 = createFile("filestest.gpx");
        request1.execute();
        String result1 = request1.getLocation();
        int key = parseFileKey(result1);
        HttpRequest request2 = updateFile(key, "filestest.bcr");
        String result2 = request2.execute();
        assertEquals("file " + key + " updated", result2);
        assertEquals(200, request2.getResult());
        assertTrue(request2.isSuccessful());
        HttpRequest request3 = readFile(key);
        String result3 = request3.execute();
        assertEquals(200, request3.getResult());
        assertTrue(request3.isSuccessful());
        String expected3 = readFileToString("filestest.bcr");
        assertEquals(expected3.length(), result3.length());
        assertEquals(expected3, result3);
    }

    @Test
    public void testUpdateWithWrongPassword() throws Exception {
        Post request1 = createFile("filestest.gpx");
        request1.execute();
        String result1 = request1.getLocation();
        int key = parseFileKey(result1);
        HttpRequest request2 = updateFile(key, "filestest.bcr", "user-does-not-exist", "password-is-wrong");
        assertNull(request2.execute());
        assertEquals(401, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    @Test
    public void testUpdateNotMyRoute() throws Exception {
        createUser("userstest.gpx").execute();
        Post request1 = createFile("filestest.gpx");
        request1.execute();
        String result1 = request1.getLocation();
        int key = parseFileKey(result1);
        HttpRequest request2 = updateFile(key, "filestest.bcr", "ivan", "secret");
        assertNotNull(request2.execute());
        assertEquals(403, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }

    @Test
    public void testDelete() throws Exception {
        Post request1 = createFile("filestest.gpx");
        request1.execute();
        String result1 = request1.getLocation();
        int key = parseFileKey(result1);
        HttpRequest request2 = deleteFile(key);
        String result2 = request2.execute();
        assertEquals("file " + key + " deleted", result2);
        assertEquals(200, request2.getResult());
        assertTrue(request2.isSuccessful());
        HttpRequest request3 = readFile(key);
        String result3 = request3.execute();
        assertNotNull(result3);
        assertEquals(404, request3.getResult());
        assertFalse(request3.isSuccessful());
    }

    @Test
    public void testDeleteWithWrongPassword() throws Exception {
        Post request1 = createFile("filestest.gpx");
        request1.execute();
        String result1 = request1.getLocation();
        int key = parseFileKey(result1);
        HttpRequest request2 = deleteFile(key, "user-does-not-exist", "password-is-wrong");
        assertNull(request2.execute());
        assertEquals(401, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    @Test
    public void testDeleteNotMyRoute() throws Exception {
        createUser("userstest.gpx").execute();
        Post request1 = createFile("filestest.gpx");
        request1.execute();
        String result1 = request1.getLocation();
        int key = parseFileKey(result1);
        HttpRequest request2 = deleteFile(key, "ivan", "secret");
        assertNotNull(request2.execute());
        assertEquals(403, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }
}
