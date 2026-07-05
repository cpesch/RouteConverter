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
 * Unit tests for {@link DirectoryFileFilter}.
 *
 * @author Christian Pesch
 */
public class DirectoryFileFilterTest {

    private final DirectoryFileFilter filter = new DirectoryFileFilter();

    private File tempDir;
    private File dotDir;
    private File regularFile;
    private File lnkFile;

    @Before
    public void setUp() throws IOException {
        File base = new File(System.getProperty("java.io.tmpdir"), "dff-test-" + System.currentTimeMillis());
        assertTrue(base.mkdir());
        tempDir = base;

        dotDir = new File(base, ".hidden");
        assertTrue(dotDir.mkdir());

        regularFile = File.createTempFile("route", ".gpx", base);

        lnkFile = new File(base, "shortcut.lnk");
        // a .lnk is accepted only when it carries the shortcut magic (little-endian
        // DWORD 0x0000004C at offset 0) and is at least 0x64 bytes long
        byte[] header = new byte[0x64];
        header[0] = 0x4C;
        java.nio.file.Files.write(lnkFile.toPath(), header);
    }

    @After
    public void tearDown() {
        deleteRecursively(tempDir);
    }

    private static void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null)
                for (File child : children)
                    deleteRecursively(child);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    @Test
    public void acceptsNonDotDirectory() {
        File dir = new File(tempDir, "normalDir");
        assertTrue(dir.mkdir());
        assertTrue(filter.accept(dir));
    }

    @Test
    public void rejectsDotDirectory() {
        assertFalse(filter.accept(dotDir));
    }

    @Test
    public void rejectsRegularFileWithNonLnkExtension() {
        assertFalse(filter.accept(regularFile));
    }

    @Test
    public void acceptsLnkFile() {
        assertTrue(filter.accept(lnkFile));
    }

    @Test
    public void rejectsLnkFileWithoutMagic() throws IOException {
        File emptyLnk = new File(tempDir, "empty.lnk");
        assertTrue(emptyLnk.createNewFile());
        assertFalse(filter.accept(emptyLnk));
    }

    @Test
    public void rejectsFileWithoutExtension() throws IOException {
        File noExt = new File(tempDir, "noextension");
        assertTrue(noExt.createNewFile());
        assertFalse(filter.accept(noExt));
    }
}

