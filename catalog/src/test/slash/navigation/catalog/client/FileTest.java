/*
  You may freely copy, distribute, modify and use this class as long
  as the original author attribution remains intact.  See message
  below.

  Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.catalog.client;

import slash.navigation.rest.*;

import java.io.File;
import java.io.IOException;

public class FileTest extends CatalogClientTest {

    private Get readFile(int key) throws IOException {
        return new Get(FILES_URL + key + "/");
    }

    private Put updateFile(int key, String fileName, String userName, String password) throws IOException {
        Put request = new Put(FILES_URL + key + "/");
        request.setAuthentication(userName, password);
        request.setParameter("file", new File(TEST_PATH + fileName));
        return request;
    }

    private Put updateFile(int key, String fileName) throws IOException {
        return updateFile(key, fileName, USERNAME, PASSWORD);
    }

    private Delete deleteFile(int key,  String userName, String password) throws IOException {
        Delete request = new Delete(FILES_URL + key + "/");
        request.setAuthentication(userName, password);
        return request;
    }

    private Delete deleteFile(int key) throws IOException {
        return deleteFile(key, USERNAME, PASSWORD);
    }


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

    public void testCreateWithNotExistingUser() throws Exception {
        HttpRequest request = createFile("filestest.gpx", "user-does-not-exist", PASSWORD);
        assertNull(request.execute());
        assertEquals(401, request.getResult());
        assertFalse(request.isSuccessful());
        assertTrue(request.isUnAuthorized());
    }

    public void testCreateWithWrongPassword() throws Exception {
        HttpRequest request = createFile("filestest.gpx", USERNAME, "password-is-wrong");
        assertNull(request.execute());
        assertEquals(401, request.getResult());
        assertFalse(request.isSuccessful());
        assertTrue(request.isUnAuthorized());
    }

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

    public void testRead() throws Exception {
        Post request1 = createFile("filestest.gpx");
        request1.execute();
        String result1 = request1.getLocation();
        int key = parseFileKey(result1);
        HttpRequest request2 = readFile(key);
        String result2 = request2.execute();
        assertEquals(200, request2.getResult());
        assertTrue(request2.isSuccessful());
        assertEquals(readFileToString("filestest.gpx"), result2);
    }

    public void testReadNotExisting() throws Exception {
        HttpRequest request = readFile(Integer.MAX_VALUE);
        assertNotNull(request.execute());
        assertEquals(404, request.getResult());
        assertFalse(request.isSuccessful());
    }

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
