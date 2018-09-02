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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import slash.navigation.rest.Get;
import slash.navigation.rest.SimpleCredentials;
import slash.navigation.routes.Category;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.io.File.createTempFile;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.io.IOUtils.copyLarge;
import static org.junit.Assert.assertTrue;

public abstract class BaseRemoteCatalogTest {
    protected static final String API = System.getProperty("api", "http://localhost:8000/");
    protected static final String USERNAME = "test";
    protected static final String ANOTHER_USERNAME = "another";
    protected static final String SUPER_USERNAME = "super";
    protected static final String PASSWORD = "test";
    protected static final String UMLAUTS = "\u00E4\u00F6\u00FC\u00DF\u00C4\u00D6\u00DC";
    protected static final String SPECIAL_CHARACTERS = "@!§$%&()=";
    protected static final String SAMPLE_FILE_NAME = "sample-file.itn";
    protected static final File SAMPLE_FILE = new File("route/src/test/resources/" + SAMPLE_FILE_NAME);

    protected static RemoteCatalog catalog;
    protected static Category test;

    @BeforeClass
    public static void setUp() throws Exception {
        catalog = new RemoteCatalog(API, new SimpleCredentials(USERNAME, PASSWORD));
        Category root = catalog.getRootCategory();
        String url = catalog.addCategory(root.getHref(), "Testrun " + currentTimeMillis());
        test = catalog.getCategory(url);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        catalog.deleteCategory(test.getHref());
        catalog = null;
    }

    protected static void assertNotFound(String url) throws IOException {
        Get get = new Get(url);
        get.executeAsString();
        assertTrue(get.isNotFound());
    }

    protected File getUrlAsFile(String url) throws IOException {
        Get get = new Get(url);
        File tempFile = createTempFile("url-as-file", ".bin");
        tempFile.deleteOnExit();
        try (InputStream inputStream = get.executeAsStream(); OutputStream outputStream = new FileOutputStream(tempFile)) {
            copyLarge(inputStream, outputStream);
        }
        return tempFile;
    }
}
