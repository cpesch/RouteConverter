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

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RouteFileFilter}, which lists local routes while hiding
 * dot-prefixed system files such as {@code .DS_Store}.
 *
 * @author Christian Pesch
 */
public class RouteFileFilterTest {
    private final RouteFileFilter filter = new RouteFileFilter();
    private File tempDir;

    @Before
    public void setUp() {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "rff-test-" + System.currentTimeMillis());
        assertTrue(tempDir.mkdir());
    }

    @After
    public void tearDown() {
        File[] children = tempDir.listFiles();
        if (children != null)
            for (File child : children)
                //noinspection ResultOfMethodCallIgnored
                child.delete();
        //noinspection ResultOfMethodCallIgnored
        tempDir.delete();
    }

    private File file(String name) throws IOException {
        File file = new File(tempDir, name);
        assertTrue(file.createNewFile());
        return file;
    }

    @Test
    public void acceptsRegularFile() throws IOException {
        assertTrue(filter.accept(file("route.gpx")));
    }

    @Test
    public void acceptsFileWithoutExtension() throws IOException {
        assertTrue(filter.accept(file("route")));
    }

    @Test
    public void rejectsDotFile() throws IOException {
        assertFalse(filter.accept(file(".DS_Store")));
    }

    @Test
    public void rejectsDirectory() {
        File dir = new File(tempDir, "subDirectory");
        assertTrue(dir.mkdir());
        assertFalse(filter.accept(dir));
    }
}
