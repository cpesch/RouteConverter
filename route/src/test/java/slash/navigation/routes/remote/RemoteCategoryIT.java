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

import org.junit.Test;
import slash.navigation.rest.SimpleCredentials;
import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.rest.exception.ForbiddenException;
import slash.navigation.rest.exception.UnAuthorizedException;
import slash.navigation.routes.Category;
import slash.navigation.routes.NotFoundException;
import slash.navigation.routes.NotOwnerException;
import slash.navigation.routes.remote.binding.FileType;

import java.io.IOException;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.*;
import static slash.navigation.routes.remote.RemoteCatalog.CATEGORY_URI;

public class RemoteCategoryIT extends BaseRemoteCatalogTest {
    @Test
    public void testGetRoot() throws IOException {
        Category root = catalog.getRootCategory();
        assertNotNull(root);
        assertEquals("", root.getName());

        String url = root.getHref();
        assertTrue(url.startsWith(API + CATEGORY_URI));
        assertTrue(url.endsWith("/"));

        assertNotNull(root.getCategories());
        assertNotNull(root.getRoutes());
    }

    private void createAndDeleteCategory(String name) throws IOException {
        Category category1 = test.create(name);
        assertTrue(category1.getHref().startsWith(API + CATEGORY_URI));
        assertTrue(category1.getHref().endsWith("/"));

        Category category2 = catalog.getCategory(category1.getHref());
        assertNotNull(category2);
        assertEquals(category1.getHref(), category2.getHref());
        assertEquals(name, category2.getName());
        assertNotNull(category2.getCategories());
        assertNotNull(category2.getRoutes());
        assertNotNull(test.getRoutes());
        assertNotNull(test.getCategories());
        assertTrue(test.getCategories().contains(category2));

        category2.delete();

        assertNull(catalog.getCategory(category2.getHref()));
        // assertFalse(test.getCategories().contains(category2));
    }

    @Test
    public void testCreateAndDeleteCategory() throws IOException {
        createAndDeleteCategory("New Category " + currentTimeMillis());
    }

    @Test
    public void testCreateCategoryWithUmlauts() throws IOException {
        createAndDeleteCategory("Umlauts Category " + currentTimeMillis() + " " + UMLAUTS);
    }

    @Test
    public void testCreateCategoryWithSpecialCharacters() throws IOException {
        createAndDeleteCategory("Special Category " + currentTimeMillis() + " " + SPECIAL_CHARACTERS);
    }

    @Test
    public void testCreateCategoryWithPluses() throws IOException {
        createAndDeleteCategory("Plus Category " + currentTimeMillis() + " A + B + C");
    }

    @Test(expected = ForbiddenException.class)
    public void testCannotCreateCategoryWithSlashes() throws IOException {
        createAndDeleteCategory("/Slashes/Category/" + currentTimeMillis() + "/");
    }

    @Test(expected = ForbiddenException.class)
    public void testCannotCreateCategoryWithBackslashes() throws IOException {
        createAndDeleteCategory("\\Slashes\\Category\\" + currentTimeMillis() + "/");
    }

    @Test(expected = UnAuthorizedException.class)
    public void testCreateCategoryForbidden() throws IOException {
        RemoteCatalog wrong = new RemoteCatalog(API, new SimpleCredentials(USERNAME, "wrong-password"));
        wrong.addCategory(API, "egal");
    }

    @Test(expected = DuplicateNameException.class)
    public void testCannotCreateCategoryWithSameName() throws IOException {
        String name = "NewCategory" + currentTimeMillis();
        catalog.addCategory(test.getHref(), name);
        catalog.addCategory(test.getHref(), name);
    }

    @Test(expected = NotFoundException.class)
    public void testCannotCreateCategoryWithNotExistingParent() throws IOException {
        catalog.addCategory(API + CATEGORY_URI + currentTimeMillis() + "/", "NewCategory" + currentTimeMillis());
    }

    @Test(expected = ForbiddenException.class)
    public void testDeleteCategoryForbidden() throws IOException {
        RemoteCatalog wrong = new RemoteCatalog(API, new SimpleCredentials(USERNAME, "wrong-password"));
        wrong.deleteCategory(API);
    }

    @Test(expected = NotOwnerException.class)
    public void testCannotDeleteRootCategory() throws IOException {
        Category root = catalog.getRootCategory();
        catalog.deleteCategory(root.getHref());
    }

    @Test(expected = NotFoundException.class)
    public void testCannotDeleteNotExistingCategory() throws IOException {
        catalog.deleteCategory(API + CATEGORY_URI + currentTimeMillis() + "/");
    }

    @Test(expected = NotOwnerException.class)
    public void testCannotDeleteCategoryFromOtherUser() throws IOException {
        RemoteCatalog another = new RemoteCatalog(API, new SimpleCredentials(ANOTHER_USERNAME, PASSWORD));
        String url = another.addCategory(test.getHref(), "MyCategory" + currentTimeMillis());

        catalog.deleteCategory(url);
    }

    @Test
    public void testSuperuserCanDeleteCategoryFromOtherUser() throws IOException {
        String name = "MyCategory" + currentTimeMillis();
        String url = catalog.addCategory(test.getHref(), name);

        RemoteCatalog superUser = new RemoteCatalog(API, new SimpleCredentials(SUPER_USERNAME, PASSWORD));
        superUser.deleteCategory(url);

        for (Category category : catalog.getRootCategory().getCategories()) {
            if (category.getHref().equals(url)) {
                assertTrue("Category " + name + " still exists", false);
            }
        }
    }

    @Test
    public void testUpdateCategory() throws IOException {
        String firstUrl = catalog.addCategory(test.getHref(), "First Category" + currentTimeMillis());
        String secondUrl = catalog.addCategory(test.getHref(), "Second Category" + currentTimeMillis());

        String name = "New Category" + currentTimeMillis();
        String url = catalog.addCategory(firstUrl, name);
        Category category1 = catalog.getCategory(url);
        assertTrue(catalog.getCategory(firstUrl).getCategories().contains(category1));
        assertFalse(catalog.getCategory(secondUrl).getCategories().contains(category1));

        catalog.updateCategory(url, secondUrl, name);
        Category category2 = catalog.getCategory(url);
        assertNotNull(category2);
        assertEquals(name, category2.getName());
        assertEquals(secondUrl, ((RemoteCategory) category2).getParent().getHref());
        assertFalse(catalog.getCategory(firstUrl).getCategories().contains(category2));
        assertTrue(catalog.getCategory(secondUrl).getCategories().contains(category2));

        String name2 = "UpdatedCategory" + currentTimeMillis();
        catalog.updateCategory(url, secondUrl, name2);
        Category category3 = catalog.getCategory(url);
        assertNotNull(category3);
        assertEquals(name2, category3.getName());
        assertEquals(secondUrl, ((RemoteCategory) category3).getParent().getHref());

        String name3 = "UpdatedAndMovedCategory" + currentTimeMillis();
        catalog.updateCategory(url, firstUrl, name3);
        Category category4 = catalog.getCategory(url);
        assertNotNull(category4);
        assertEquals(name3, category4.getName());
        assertEquals(firstUrl, ((RemoteCategory) category4).getParent().getHref());
    }

    @Test(expected = NotOwnerException.class)
    public void testCannotUpdateRootCategory() throws IOException {
        Category root = catalog.getRootCategory();
        catalog.updateCategory(root.getHref(), API, "egal");
    }

    @Test(expected = NotFoundException.class)
    public void testCannotUpdateNotExistingCategory() throws IOException {
        catalog.updateCategory(API + CATEGORY_URI + currentTimeMillis() + "/", API, "egal");
    }

    @Test(expected = DuplicateNameException.class)
    public void testCannotUpdateCategoryWithSameName() throws IOException {
        String name = "FirstCategory" + currentTimeMillis();
        catalog.addCategory(test.getHref(), name);
        String url = catalog.addCategory(test.getHref(), "SecondCategory" + currentTimeMillis());

        catalog.updateCategory(url, test.getHref(), name);
    }

    @Test(expected = ForbiddenException.class)
    public void testCannotUpdateCategoryWithSlashes() throws IOException {
        String url = catalog.addCategory(test.getHref(), "A Category " + currentTimeMillis());

        catalog.updateCategory(url, test.getHref(), "/Slashes/Category/" + currentTimeMillis() + "/");
    }

    @Test(expected = DuplicateNameException.class)
    public void testCannotMoveToSelfAsParent() throws Exception {
        String parentName = "Parent " + currentTimeMillis();
        Category parent = test.create(parentName);
        String moveName = "Move " + currentTimeMillis();
        Category move = parent.create(moveName);
        move.update(move, move.getName());
    }

    @Test(expected = DuplicateNameException.class)
    public void testCannotMoveToOwnChild() throws Exception {
        String parentName = "Parent " + currentTimeMillis();
        Category parent = test.create(parentName);
        String moveName = "Move " + currentTimeMillis();
        Category move = parent.create(moveName);
        String childName = "Child " + currentTimeMillis();
        Category child = move.create(childName);
        move.update(child, move.getName());
    }

    @Test
    public void testDeletingCategoryDeletesRouteAndFile() throws IOException {
        String categoryUrl = catalog.addCategory(test.getHref(), "To Be Deleted Category" + currentTimeMillis());
        String fileUrl = catalog.addFile(SAMPLE_FILE);
        FileType fileType = catalog.getFile(fileUrl);
        String routeUrl = catalog.addRoute(categoryUrl, "To Be Deleted Route " + currentTimeMillis(), fileUrl, null);

        catalog.deleteCategory(categoryUrl);

        assertNull(catalog.getCategory(categoryUrl));
        assertNull(catalog.getRoute(routeUrl));
        assertNull(catalog.getFile(fileUrl));
        assertNotFound(fileType.getUrl());
    }
}
