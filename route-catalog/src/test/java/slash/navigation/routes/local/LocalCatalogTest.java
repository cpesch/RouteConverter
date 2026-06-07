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
import slash.navigation.routes.Category;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link LocalCatalog}.
 *
 * @author Christian Pesch
 */
public class LocalCatalogTest {

    private File tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("local-catalog-test").toFile();
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

    @Test
    public void getRootCategoryIsNotNull() {
        LocalCatalog catalog = new LocalCatalog(tempDir.getAbsolutePath());
        assertNotNull(catalog.getRootCategory());
    }

    @Test
    public void getRootCategoryNameMatchesDirName() throws IOException {
        LocalCatalog catalog = new LocalCatalog(tempDir.getAbsolutePath());
        Category root = catalog.getRootCategory();
        assertEquals(tempDir.getName(), root.getName());
    }

    @Test
    public void getRootCategoryHrefContainsTempPath() throws IOException {
        LocalCatalog catalog = new LocalCatalog(tempDir.getAbsolutePath());
        Category root = catalog.getRootCategory();
        String href = root.getHref();
        assertNotNull(href);
        assertTrue("href should start with file:", href.startsWith("file:"));
        assertTrue("href should contain dir name", href.contains(tempDir.getName()));
    }

    @Test
    public void getRootCategorySubCategoriesEmptyInitially() throws IOException {
        LocalCatalog catalog = new LocalCatalog(tempDir.getAbsolutePath());
        Category root = catalog.getRootCategory();
        assertTrue("new temp dir should have no subcategories", root.getCategories().isEmpty());
    }

    @Test
    public void getRootCategoryReflectsCreatedSubDir() throws IOException {
        File subDir = new File(tempDir, "subcat");
        assertTrue(subDir.mkdir());

        LocalCatalog catalog = new LocalCatalog(tempDir.getAbsolutePath());
        Category root = catalog.getRootCategory();
        assertEquals(1, root.getCategories().size());
        assertEquals("subcat", root.getCategories().get(0).getName());
    }

    @Test
    public void getRootCategoryRoutesEmptyInitially() throws IOException {
        LocalCatalog catalog = new LocalCatalog(tempDir.getAbsolutePath());
        Category root = catalog.getRootCategory();
        assertTrue("new temp dir should have no routes", root.getRoutes().isEmpty());
    }

    @Test
    public void twoCallsToGetRootCategoryReturnEquivalentCategory() throws IOException {
        LocalCatalog catalog = new LocalCatalog(tempDir.getAbsolutePath());
        Category root1 = catalog.getRootCategory();
        Category root2 = catalog.getRootCategory();
        assertEquals(root1.getName(), root2.getName());
        assertEquals(root1.getHref(), root2.getHref());
    }
}

