package slash.navigation.catalog.client;

import slash.navigation.gpx.GpxUtil;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.rest.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

public class CategoryIT extends CatalogClientBase {

    private Post createCategoryFromFile(String fileName,
                                        String authenticationUserName, String authenticationPassword) throws IOException {
        Post request = new Post(CATEGORIES_URL);
        request.setAuthentication(authenticationUserName, authenticationPassword);
        request.setParameter("file", new File(TEST_PATH + fileName));
        return request;
    }

    private Post createCategoryFromFile(String fileName) throws IOException {
        return createCategoryFromFile(fileName, USERNAME, PASSWORD);
    }

    private Post createCategory(String parent, String name,
                                String authenticationUserName, String authenticationPassword) throws IOException, JAXBException {
        String xml = createCategoryXml(name);

        Post request = new Post(CATEGORIES_URL + Helper.encodeUri(parent) + "/");
        request.setAuthentication(authenticationUserName, authenticationPassword);
        request.setParameter("file", writeToTempFile(xml));
        return request;
    }

    private Post createCategory(String parent, String name) throws IOException, JAXBException {
        return createCategory(parent, name, USERNAME, PASSWORD);
    }

    private Get readCategory(String key) throws IOException {
        return new Get(CATEGORIES_URL + Helper.encodeUri(key) + GPX_URL_POSTFIX);
    }

    private Put updateCategory(String key, String name,
                               String authenticationUserName, String authenticationPassword) throws IOException, JAXBException {
        String xml = createCategoryXml(name);

        Put request = new Put(CATEGORIES_URL + Helper.encodeUri(key) + GPX_URL_POSTFIX);
        request.setAuthentication(authenticationUserName, authenticationPassword);
        request.setParameter("file", writeToTempFile(xml));
        return request;
    }

    private Put updateCategory(String key, String name) throws IOException, JAXBException {
        return updateCategory(key, name, USERNAME, PASSWORD);
    }


    public void testCreateFromFile() throws Exception {
        Post request2 = createCategoryFromFile("categoriestest.gpx");
        String result = request2.execute();
        assertTrue(result.contains("category"));
        assertTrue(result.contains("created"));
        String location = request2.getLocation();
        assertTrue(location.contains("/catalog/categories/"));
        assertEquals(201, request2.getResult());
        assertTrue(request2.isSuccessful());
    }

    public void testCreateRootCategoryFromJAXB() throws Exception {
        String name = "Category " + System.currentTimeMillis();
        Post request2 = createCategory("", name);
        String result = request2.execute();
        assertTrue(result.contains("category"));
        assertTrue(result.contains(name));
        assertTrue(result.contains("created"));
        String location = request2.getLocation();
        assertTrue(location.contains("/catalog/categories/"));
        assertEquals(201, request2.getResult());
        assertTrue(request2.isSuccessful());
    }

    public void testCreateUploadCategoryFromJAXB() throws Exception {
        String name = "Category " + System.currentTimeMillis();
        Post request2 = createCategory("Upload", name);
        String result = request2.execute();
        assertTrue(result.contains("category"));
        assertTrue(result.contains(name));
        assertTrue(result.contains("created"));
        String location = request2.getLocation();
        assertTrue(location.contains("/catalog/categories/"));
        assertEquals(201, request2.getResult());
        assertTrue(request2.isSuccessful());
    }

    public void testRead() throws Exception {
        Post request1 = createCategoryFromFile("categoriestest.gpx");
        request1.execute();
        String key = parseCategoryKey(request1.getLocation());

        HttpRequest request2 = readCategory(key);
        String result2 = request2.execute();
        assertEquals(200, request2.getResult());
        assertTrue(request2.isSuccessful());

        GpxType gpxType = GpxUtil.unmarshal11(result2);
        assertNotNull(gpxType);
        assertEquals("Interesting", gpxType.getMetadata().getName());
        assertEquals(USERNAME, gpxType.getMetadata().getAuthor().getName());
        assertEquals(USERS_URL + USERNAME + GPX_URL_POSTFIX, gpxType.getMetadata().getAuthor().getLink().getHref());
        assertNull(gpxType.getMetadata().getDesc());
        assertEquals(0, gpxType.getMetadata().getLink().size());
    }

    public void testReadRoot() throws Exception {
        HttpRequest request1 = readCategory("");
        String result1 = request1.execute();
        assertEquals(200, request1.getResult());
        assertTrue(request1.isSuccessful());

        GpxType gpxType = GpxUtil.unmarshal11(result1);
        assertNotNull(gpxType);
        assertEquals("", gpxType.getMetadata().getName());
        assertEquals("unknown", gpxType.getMetadata().getAuthor().getName());
        assertEquals(USERS_URL + "unknown" + GPX_URL_POSTFIX, gpxType.getMetadata().getAuthor().getLink().getHref());
        assertNull(gpxType.getMetadata().getDesc());
        assertTrue(gpxType.getMetadata().getLink().size() > 0);
    }

    public void testReadWithSpaces() throws Exception {
        String name = "Category " + System.currentTimeMillis();
        Post request1 = createCategory("Upload", name);
        request1.execute();
        String key = parseCategoryKey(request1.getLocation());

        HttpRequest request2 = readCategory(key);
        String result2 = request2.execute();
        assertEquals(200, request2.getResult());
        assertTrue(request2.isSuccessful());

        GpxType gpxType = GpxUtil.unmarshal11(result2);
        assertNotNull(gpxType);
        assertEquals(name, gpxType.getMetadata().getName());
        assertEquals(USERNAME, gpxType.getMetadata().getAuthor().getName());
        assertEquals(USERS_URL + USERNAME + GPX_URL_POSTFIX, gpxType.getMetadata().getAuthor().getLink().getHref());
        assertNull(gpxType.getMetadata().getDesc());
        assertEquals(0, gpxType.getMetadata().getLink().size());
    }

    public void testUpdate() throws Exception {
        Post request1 = createCategory("Upload", "Interesting");
        request1.execute();
        String key = parseCategoryKey(request1.getLocation());
        String newName = "Interesting" + System.currentTimeMillis();
        Put request2 = updateCategory(key, newName);
        String result2 = request2.execute();
        assertEquals("category /Upload/" + newName + " updated", result2);
        assertEquals(201, request2.getResult());
        assertTrue(request2.isSuccessful());
        String newKey = parseCategoryKey(request2.getLocation());
        HttpRequest request3 = readCategory(newKey);
        String result3 = request3.execute();
        assertEquals(200, request3.getResult());
        assertTrue(request3.isSuccessful());
        GpxType gpxType = GpxUtil.unmarshal11(result3);
        assertNotNull(gpxType);
        // TODO test more
    }

    public void testUpdateWithWrongPassword() throws Exception {
        Post request1 = createCategory("Upload", "Interesting");
        request1.execute();
        String key = parseCategoryKey(request1.getLocation());
        HttpRequest request2 = updateCategory(key, "Interesting" + System.currentTimeMillis(), "user-does-not-exist", "password-is-wrong");
        assertNull(request2.execute());
        assertEquals(401, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    public void testUpdateNotMyUser() throws Exception {
        createUser("alif", "topr", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD).execute();
        Post request1 = createCategory("Upload", "Interesting");
        request1.execute();
        String key = parseCategoryKey(request1.getLocation());
        HttpRequest request2 = updateCategory(key, "Interesting" + System.currentTimeMillis(), "alif", "topr");
        request2.execute();
        assertEquals(403, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }

    public void testDelete() throws Exception {
        Post request1 = createCategory("Upload", "Interesting");
        request1.execute();
        String key = parseCategoryKey(request1.getLocation());
        HttpRequest request2 = deleteCategory(key);
        String result2 = request2.execute();
        assertEquals("category /" + key + " deleted", result2);
        assertEquals(200, request2.getResult());
        assertTrue(request2.isSuccessful());
        HttpRequest request3 = readCategory(key);
        String result3 = request3.execute();
        assertNotNull(result3);
        assertEquals(404, request3.getResult());
        assertFalse(request3.isSuccessful());
    }

    public void testDeleteWithWrongPassword() throws Exception {
        Post request1 = createCategory("Upload", "Interesting");
        request1.execute();
        String key = parseCategoryKey(request1.getLocation());
        HttpRequest request2 = deleteCategory(key, "user-does-not-exist", "password-is-wrong");
        assertNull(request2.execute());
        assertEquals(401, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isUnAuthorized());
    }

    public void testDeleteNotMyUser() throws Exception {
        createUser("alif", "toup", "Ali", "Top", "ali@top.org", USERNAME, PASSWORD).execute();
        Post request1 = createCategory("Upload", "Interesting");
        request1.execute();
        String key = parseCategoryKey(request1.getLocation());
        HttpRequest request2 = deleteCategory(key, "alif", "toup");
        request2.execute();
        assertEquals(403, request2.getResult());
        assertFalse(request2.isSuccessful());
        assertTrue(request2.isForbidden());
    }
}
