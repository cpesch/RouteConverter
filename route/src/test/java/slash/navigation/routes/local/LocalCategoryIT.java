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
import slash.navigation.rest.exception.ForbiddenException;
import slash.navigation.routes.Category;

import java.io.File;
import java.io.IOException;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getTemporaryDirectory;

public class LocalCategoryIT {
    protected static final String UMLAUTS = "\u00E4\u00F6\u00FC\u00DF\u00C4\u00D6\u00DC";

    private File path;
    private LocalCatalog catalog;

    @Before
    public void setUp() {
        path = ensureDirectory(new File(getTemporaryDirectory(), "local-catalog-" + currentTimeMillis()));
        catalog = new LocalCatalog(path.getAbsolutePath());
    }

    @After
    public void tearDown() throws IOException {
        catalog.getRootCategory().delete();
    }

    @Test
    public void testGetRoot() throws IOException {
        Category root = catalog.getRootCategory();
        assertNotNull(root);
        assertEquals(path.getName(), root.getName());

        String url = root.getHref();
        assertEquals(url, path.toURI().toURL().toString());

        assertNotNull(root.getCategories());
        assertNotNull(root.getRoutes());
    }

    private LocalCategory getCategory(Category parent, String name) throws IOException {
        for (Category category : parent.getCategories()) {
            if(name.equals(category.getName()))
                return (LocalCategory) category;
        }
        return null;
    }

    private void createAndDeleteCategory(String name) throws IOException {
        Category category1 = catalog.getRootCategory().create(name);
        assertTrue(category1.getHref().startsWith(path.toURI().toURL().toString()));

        Category category2 = getCategory(catalog.getRootCategory(), name);
        assertNotNull(category2);
        assertEquals(category1.getHref(), category2.getHref());
        assertEquals(name, category2.getName());
        assertNotNull(category2.getCategories());
        assertNotNull(category2.getRoutes());
        assertTrue(catalog.getRootCategory().getCategories().contains(category2));

        category2.delete();

        assertNull(getCategory(catalog.getRootCategory(), name));
        assertFalse(catalog.getRootCategory().getCategories().contains(category2));
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

    @Test
    public void testUpdateCategory() throws IOException {
        Category firstParent = catalog.getRootCategory().create("First Category" + currentTimeMillis());
        Category secondParent = catalog.getRootCategory().create("Second Category" + currentTimeMillis());

        String name = "New Category" + currentTimeMillis();
        Category category = firstParent.create(name);
        assertTrue(firstParent.getCategories().contains(category));
        assertFalse(secondParent.getCategories().contains(category));

        category.update(secondParent, name);
        assertEquals(name, category.getName());
        assertFalse(firstParent.getCategories().contains(category));
        assertTrue(secondParent.getCategories().contains(category));

        String name2 = "UpdatedCategory" + currentTimeMillis();
        category.update(secondParent, name2);
        assertEquals(name2, category.getName());

        String name3 = "UpdatedAndMovedCategory" + currentTimeMillis();
        category.update(firstParent, name3);
        assertEquals(name3, category.getName());
        assertTrue(firstParent.getCategories().contains(category));
        assertFalse(secondParent.getCategories().contains(category));
    }
}
