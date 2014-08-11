/*
  You may freely copy, distribute, modify and use this class as long
  as the original author attribution remains intact.  See message
  below.

  Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.catalog.client;

import org.junit.Test;
import slash.navigation.gpx.GpxUtil;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.binding11.RteType;
import slash.navigation.rest.Delete;
import slash.navigation.rest.Get;
import slash.navigation.rest.HttpRequest;
import slash.navigation.rest.Post;
import slash.navigation.rest.Put;
import slash.navigation.rest.SimpleCredentials;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RouteIT extends RouteCatalogClientBase {

    private Get readRoute(int key) {
        return new Get(ROUTES_URL + key + GPX_URL_POSTFIX);
    }

    private Put updateRoute(int routeKey, String category, Integer fileKey, String description,
                            String authenticationUserName, String authenticationPassword) throws IOException, JAXBException {
        String xml = createRouteXml(category, fileKey, description);
        Put request = new Put(ROUTES_URL + routeKey + GPX_URL_POSTFIX, new SimpleCredentials(authenticationUserName, authenticationPassword));
        request.addFile("file", xml.getBytes());
        return request;
    }

    private Put updateRoute(int routeKey, String category, Integer fileKey, String description) throws IOException, JAXBException {
        return updateRoute(routeKey, category, fileKey, description, USERNAME, PASSWORD);
    }

    private Delete deleteRoute(int key, String authenticationUserName, String authenticationPassword) {
        return new Delete(ROUTES_URL + key + GPX_URL_POSTFIX, new SimpleCredentials(authenticationUserName, authenticationPassword));
    }

    private Delete deleteRoute(int key) {
        return deleteRoute(key, USERNAME, PASSWORD);
    }

    @Test
    public void testCreateFromJAXB() throws Exception {
        Post request1 = createFile("filestest.gpx");
        request1.executeAsString();
        assertTrue(request1.isSuccessful());
        int key = parseFileKey(request1.getLocation());

        Post request2 = createRoute("Upload", key, "Description/" + key);
        String result = request2.executeAsString();
        assertTrue(result.contains("route"));
        assertTrue(result.contains("created"));
        String location = request2.getLocation();
        assertTrue(location.contains("/catalog/routes/"));
        assertEquals(201, request2.getStatusCode());
        assertTrue(request2.isSuccessful());
    }

    @Test
    public void testCreateWithInvalidCategory() throws Exception {
        Post request1 = createRoute("Invalid category " + System.currentTimeMillis(), -1, "Description");
        String result = request1.executeAsString();
        assertTrue(result.contains("no"));
        assertTrue(result.contains("valid"));
        assertTrue(result.contains("category"));
        assertEquals(412, request1.getStatusCode());
        assertFalse(request1.isSuccessful());
    }

    @Test
    public void testRead() throws Exception {
        Post request1 = createFile("filestest.gpx");
        request1.executeAsString();
        int fileKey = parseFileKey(request1.getLocation());

        Post request2 = createRoute("Upload", fileKey, "Description" + fileKey);
        request2.executeAsString();
        int routeKey = parseRouteKey(request2.getLocation());

        HttpRequest request3 = readRoute(routeKey);
        String result2 = request3.executeAsString();
        assertEquals(200, request3.getStatusCode());
        assertTrue(request3.isSuccessful());

        GpxType gpxType = GpxUtil.unmarshal11(result2);
        assertNotNull(gpxType);
        assertEquals(Integer.toString(routeKey), gpxType.getMetadata().getName());
        assertEquals(USERNAME, gpxType.getMetadata().getAuthor().getName());
        assertEquals(USERS_URL + USERNAME + GPX_URL_POSTFIX, gpxType.getMetadata().getAuthor().getLink().getHref());
        assertNull(gpxType.getMetadata().getDesc());
        assertEquals(0, gpxType.getMetadata().getLink().size());
        assertEquals("/Upload", gpxType.getMetadata().getKeywords());

        Calendar expectedCal = Calendar.getInstance();
        GregorianCalendar actualCal = gpxType.getMetadata().getTime().toGregorianCalendar();
        actualCal.setTimeZone(expectedCal.getTimeZone());
        assertTrue(actualCal.before(expectedCal));

        assertEquals(1, gpxType.getRte().size());
        RteType rteType = gpxType.getRte().get(0);
        assertEquals(Integer.toString(routeKey), rteType.getName());
        assertEquals("Description" + fileKey, rteType.getDesc());
        assertEquals(FILES_URL + fileKey, rteType.getLink().get(0).getHref());
    }

    @Test
    public void testUpdate() throws Exception {
        Post request0 = createFile("filestest.gpx");
        request0.executeAsString();
        int fileKey = parseFileKey(request0.getLocation());

        Post request1 = createRoute("Upload", fileKey, "Description");
        request1.executeAsString();
        int routeKey = parseRouteKey(request1.getLocation());
        HttpRequest request2 = updateRoute(routeKey, "Upload", -2, "Updated description", USERNAME, PASSWORD);
        String result2 = request2.executeAsString();
        assertEquals("route " + routeKey + " updated", result2);
        assertEquals(200, request2.getStatusCode());
        assertTrue(request2.isSuccessful());
        HttpRequest request3 = readRoute(routeKey);
        String result3 = request3.executeAsString();
        assertEquals(200, request3.getStatusCode());
        assertTrue(request3.isSuccessful());
        GpxType gpxType = GpxUtil.unmarshal11(result3);
        assertNotNull(gpxType);
    }

    @Test
    public void testUpdateWithWrongPassword() throws Exception {
        Post request0 = createFile("filestest.gpx");
        request0.executeAsString();
        int fileKey = parseFileKey(request0.getLocation());

        Post request1 = createRoute("Upload", fileKey, "Description");
        request1.executeAsString();
        int routeKey = parseRouteKey(request1.getLocation());
        HttpRequest request2 = updateRoute(routeKey, "Upload", -2, "Updated description", "user-does-not-exist", "password-is-wrong");
        assertNull(request2.executeAsString());
        assertEquals(401, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    @Test
    public void testUpdateNotMyUser() throws Exception {
        createUser("alif", "toop", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD).executeAsString();

        Post request0 = createFile("filestest.gpx");
        request0.executeAsString();
        int fileKey = parseFileKey(request0.getLocation());

        Post request1 = createRoute("Upload", fileKey, "Description");
        request1.executeAsString();
        int routeKey = parseRouteKey(request1.getLocation());
        HttpRequest request2 = updateRoute(routeKey, "Upload", -2, "Updated description", "alif", "toop");
        request2.executeAsString();
        assertEquals(403, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }

    @Test
    public void testUpdateWithoutFile() throws Exception {
        Post request0 = createFile("filestest.gpx");
        request0.executeAsString();
        int fileKey = parseFileKey(request0.getLocation());

        Post request1 = createRoute("Upload", fileKey, "Description");
        String result1 = request1.executeAsString();
        assertTrue(result1.contains("route"));
        assertTrue(result1.contains("created"));
        int routeKey = parseRouteKey(request1.getLocation());
        HttpRequest request2 = updateRoute(routeKey, "Upload", null, "Updated description");
        String result2 = request2.executeAsString();
        assertTrue(result2.contains("no"));
        assertTrue(result2.contains("url"));
        assertTrue(result2.contains("given"));
        assertEquals(412, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
    }

    @Test
    public void testUpdateWithoutCategory() throws Exception {
        Post request0 = createFile("filestest.gpx");
        request0.executeAsString();
        int fileKey = parseFileKey(request0.getLocation());

        Post request1 = createRoute("Upload", fileKey, "Description");
        String result1 = request1.executeAsString();
        assertTrue(result1.contains("route"));
        assertTrue(result1.contains("created"));
        int routeKey = parseRouteKey(request1.getLocation());
        HttpRequest request2 = updateRoute(routeKey, null, fileKey, "Updated description");
        String result2 = request2.executeAsString();
        assertTrue(result2.contains("no"));
        assertTrue(result2.contains("category"));
        assertTrue(result2.contains("given"));
        assertEquals(412, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
    }

    @Test
    public void testUpdateWithInvalidCategory() throws Exception {
        Post request0 = createFile("filestest.gpx");
        request0.executeAsString();
        int fileKey = parseFileKey(request0.getLocation());

        Post request1 = createRoute("Upload", fileKey, "Description");
        String result1 = request1.executeAsString();
        assertTrue(result1.contains("route"));
        assertTrue(result1.contains("created"));
        int routeKey = parseRouteKey(request1.getLocation());
        HttpRequest request2 = updateRoute(routeKey, "Invalid category " + System.currentTimeMillis(), fileKey, "Updated description");
        String result2 = request2.executeAsString();
        assertTrue(result2.contains("no"));
        assertTrue(result2.contains("valid"));
        assertTrue(result2.contains("category"));
        assertEquals(412, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
    }

    @Test
    public void testDelete() throws Exception {
        Post request0 = createFile("filestest.gpx");
        request0.executeAsString();
        int fileKey = parseFileKey(request0.getLocation());

        Post request1 = createRoute("Upload", fileKey, "Description");
        request1.executeAsString();
        int key = parseRouteKey(request1.getLocation());
        HttpRequest request2 = deleteRoute(key);
        String result2 = request2.executeAsString();
        assertEquals("route " + key + " deleted", result2);
        assertEquals(200, request2.getStatusCode());
        assertTrue(request2.isSuccessful());
        HttpRequest request3 = readRoute(key);
        String result3 = request3.executeAsString();
        assertNotNull(result3);
        assertEquals(404, request3.getStatusCode());
        assertFalse(request3.isSuccessful());
    }

    @Test
    public void testDeleteWithWrongPassword() throws Exception {
        Post request0 = createFile("filestest.gpx");
        request0.executeAsString();
        int fileKey = parseFileKey(request0.getLocation());

        Post request1 = createRoute("Upload", fileKey, "Description");
        request1.executeAsString();
        int key = parseRouteKey(request1.getLocation());
        HttpRequest request2 = deleteRoute(key, "user-does-not-exist", "password-is-wrong");
        assertNull(request2.executeAsString());
        assertEquals(401, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    @Test
    public void testDeleteNotMyUser() throws Exception {
        createUser("alif", "topg", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD).executeAsString();

        Post request0 = createFile("filestest.gpx");
        request0.executeAsString();
        int fileKey = parseFileKey(request0.getLocation());

        Post request1 = createRoute("Upload", fileKey, "Description");
        request1.executeAsString();
        int key = parseRouteKey(request1.getLocation());
        HttpRequest request2 = deleteRoute(key, "alif", "topg");
        request2.executeAsString();
        assertEquals(403, request2.getStatusCode());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }
}
