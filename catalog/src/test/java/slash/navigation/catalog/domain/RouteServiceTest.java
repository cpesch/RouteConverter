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
package slash.navigation.catalog.domain;

import slash.common.io.Files;
import slash.navigation.rest.Helper;

import java.io.File;
import java.io.IOException;

public class RouteServiceTest extends BaseRouteServiceTest {
    private static final String DEFAULT_PREFIX = "route";
    private static final String DEFAULT_SUFFIX = ".file";
    private static final String A_PREFIX = "nice route";
    private static final String A_SUFFIX = ".kml";

    private File createTempFile(String fileName) throws IOException {
        File tmp = File.createTempFile(DEFAULT_PREFIX, DEFAULT_SUFFIX);
        File file = new File(tmp.getParentFile(), fileName);
        if (file.exists())
            assertTrue(file.delete());
        assertTrue(tmp.renameTo(file));
        return file;
    }

    private File routeCatalogCreateTempFile(String fileName) throws IOException {
        if (fileName == null)
            fileName = "route.file";
        String decodedName = Helper.decodeUri(fileName);
        String prefix = Files.removeExtension(decodedName);
        if (prefix.length() < 3)
            prefix = "rcc" + prefix;
        File file = File.createTempFile(prefix, Files.getExtension(decodedName));
        File tmp = new File(file.getParentFile(), decodedName);
        if (!tmp.exists()) {
            if (file.renameTo(tmp))
                file = tmp;
        }
        return file;
    }

    public void testCreateDefaultTempFile() throws IOException {
        File expected = createTempFile(DEFAULT_PREFIX + DEFAULT_SUFFIX);
        assertTrue(expected.exists());
        assertTrue(expected.delete());
        File file = routeCatalogCreateTempFile(null);
        assertEquals(expected, file);
    }

    public void testCreateDefaultTempFileIfItExists() throws IOException {
        File expected = createTempFile(DEFAULT_PREFIX + DEFAULT_SUFFIX);
        File file = routeCatalogCreateTempFile(null);
        assertNotEquals(expected, file);
        assertTrue(file.getName().startsWith(DEFAULT_PREFIX));
        assertTrue(file.getName().endsWith(DEFAULT_SUFFIX));
        assertTrue(file.getName().length() > DEFAULT_PREFIX.length() + DEFAULT_SUFFIX.length());
    }

    public void testCreateTempFile() throws IOException {
        File expected = createTempFile(A_PREFIX + A_SUFFIX);
        assertTrue(expected.exists());
        assertTrue(expected.delete());
        File file = routeCatalogCreateTempFile(A_PREFIX + A_SUFFIX);
        assertEquals(expected, file);
    }

    public void testCreateTempFileIfItExists() throws IOException {
        File expected = createTempFile(A_PREFIX + A_SUFFIX);
        File file = routeCatalogCreateTempFile(A_PREFIX + A_SUFFIX);
        assertNotEquals(expected, file);
        assertTrue(file.getName().startsWith(A_PREFIX));
        assertTrue(file.getName().endsWith(A_SUFFIX));
        assertTrue(file.getName().length() > A_PREFIX.length() + A_SUFFIX.length());
    }
}
