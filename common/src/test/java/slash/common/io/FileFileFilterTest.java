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

package slash.common.io;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileFileFilterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final FileFileFilter filter = new FileFileFilter();

    @Test
    public void testAcceptsRegularFile() throws IOException {
        File file = tmp.newFile("test.txt");
        assertTrue(filter.accept(file));
    }

    @Test
    public void testRejectsDirectory() throws IOException {
        File dir = tmp.newFolder("subdir");
        assertFalse(filter.accept(dir));
    }

    @Test
    public void testRejectsNonExistentPath() throws IOException {
        File nonExistent = new File(tmp.getRoot(), "no-such-file.txt");
        assertFalse(filter.accept(nonExistent));
    }
}

