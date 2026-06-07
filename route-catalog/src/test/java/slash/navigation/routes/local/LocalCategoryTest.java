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
package slash.navigation.routes.local;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.rest.exception.ForbiddenException;
import slash.navigation.routes.Category;
import slash.navigation.routes.Route;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link LocalCategory} ? create, delete, update, createRoute, equals/hashCode/toString.
 *
 * @author Christian Pesch
 */
public class LocalCategoryTest {

    private File tempDir;
    private LocalCatalog catalog;
    private LocalCategory rootCategory;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("local-category-test").toFile();
        catalog = new LocalCatalog(tempDir.getAbsolutePath());
        rootCategory = (LocalCategory) catalog.getRootCategory();
    }

    @After
    public void tearDown() {
        deleteRecursive(tempDir);
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null)
                for (File child : children)
                    deleteRecursive(child);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    // ---- getName and getHref ----

    @Test
    public void getNameMatchesDirName() throws IOException {
        assertEquals(tempDir.getName(), rootCategory.getName());
    }

    @Test
    public void getHrefStartsWithFile() throws IOException {
        assertTrue(rootCategory.getHref().startsWith("file:"));
    }

    // ---- create ----

    @Test
    public void createReturnsCategoryWithCorrectName() throws IOException {
        Category child = rootCategory.create("MyRoute");
        assertEquals("MyRoute", child.getName());
    }

    @Test
    public void createAppearsInGetCategories() throws IOException {
        rootCategory.create("Alpha");
        List<Category> categories = rootCategory.getCategories();
        assertEquals(1, categories.size());
        assertEquals("Alpha", categories.get(0).getName());
    }

    @Test(expected = ForbiddenException.class)
    public void createWithForwardSlashThrowsForbiddenException() throws IOException {
        rootCategory.create("A/B");
    }

    @Test(expected = DuplicateNameException.class)
    public void createDuplicateThrowsDuplicateNameException() throws IOException {
        rootCategory.create("Dup");
        rootCategory.create("Dup");
    }

    // ---- delete ----

    @Test
    public void deleteRemovesCategoryFromGetCategories() throws IOException {
        Category child = rootCategory.create("ToDelete");
        assertEquals(1, rootCategory.getCategories().size());
        child.delete();
        assertTrue(rootCategory.getCategories().isEmpty());
    }

    // ---- update (rename) ----

    @Test
    public void updateRenamesCategoryAndChangesName() throws IOException {
        LocalCategory child = (LocalCategory) rootCategory.create("OldName");
        child.update(null, "NewName");
        assertEquals("NewName", child.getName());
    }

    @Test
    public void updateRenamedCategoryAppearsInParent() throws IOException {
        LocalCategory child = (LocalCategory) rootCategory.create("Before");
        child.update(null, "After");
        List<Category> categories = rootCategory.getCategories();
        assertEquals(1, categories.size());
        assertEquals("After", categories.get(0).getName());
    }

    // ---- createRoute(String, String) ? URL shortcut ----

    @Test
    public void createRouteFromUrlReturnsRoute() throws IOException {
        Route route = rootCategory.createRoute("my-route.url", "https://example.com/route.gpx");
        assertNotNull(route);
    }

    @Test
    public void createRouteFromUrlAppearsInGetRoutes() throws IOException {
        rootCategory.createRoute("my-route.url", "https://example.com/route.gpx");
        List<Route> routes = rootCategory.getRoutes();
        assertEquals(1, routes.size());
    }

    @Test
    public void createRouteFromUrlFileContainsInternetShortcut() throws IOException {
        rootCategory.createRoute("shortcut.url", "https://static.routeconverter.com/route.gpx");
        List<Route> routes = rootCategory.getRoutes();
        assertFalse(routes.isEmpty());
        Route route = routes.get(0);
        assertNotNull(route.getHref());
        assertTrue(route.getHref().startsWith("file:"));
    }

    // ---- createRoute(String, File) ? file copy ----

    @Test
    public void createRouteFromFileCopiesFile() throws IOException {
        File src = File.createTempFile("src-route", ".gpx");
        try {
            try (FileWriter w = new FileWriter(src)) {
                w.write("<gpx/>");
            }
            Route route = rootCategory.createRoute("copied.gpx", src);
            assertNotNull(route);
            List<Route> routes = rootCategory.getRoutes();
            assertFalse(routes.isEmpty());
        } finally {
            //noinspection ResultOfMethodCallIgnored
            src.delete();
        }
    }

    // ---- getRoutes reflects both URL and file routes ----

    @Test
    public void getRoutesCountReflectsBothRouteTypes() throws IOException {
        File src = File.createTempFile("multi-route", ".gpx");
        try {
            try (FileWriter w = new FileWriter(src)) {
                w.write("<gpx/>");
            }
            rootCategory.createRoute("file-route.gpx", src);
            rootCategory.createRoute("url-route.url", "https://example.com/r.gpx");
            assertEquals(2, rootCategory.getRoutes().size());
        } finally {
            //noinspection ResultOfMethodCallIgnored
            src.delete();
        }
    }

    // ---- equals / hashCode / toString ----

    @Test
    public void equalsSameDirectoryIsTrue() throws IOException {
        LocalCategory a = (LocalCategory) catalog.getRootCategory();
        LocalCategory b = (LocalCategory) catalog.getRootCategory();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalsDifferentDirectoryIsFalse() throws IOException {
        Category child = rootCategory.create("Sub");
        assertNotEquals(rootCategory, child);
    }

    @Test
    public void equalsNullIsFalse() throws IOException {
        assertNotEquals(rootCategory, null);
    }

    @Test
    public void equalsWrongTypeIsFalse() throws IOException {
        assertNotEquals(rootCategory, "not-a-category");
    }

    @Test
    public void toStringContainsLocalCategoryAndDirName() throws IOException {
        String s = rootCategory.toString();
        assertTrue(s.contains("LocalCategory"));
        assertTrue(s.contains(tempDir.getName()));
    }
}

