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

package slash.navigation.download;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class FileAndChecksumTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testGetFile() throws IOException {
        File file = tmp.newFile("test.zip");
        FileAndChecksum fc = new FileAndChecksum(file, null);
        assertEquals(file, fc.getFile());
    }

    @Test
    public void testGetExpectedChecksum() throws IOException {
        File file = tmp.newFile("test.zip");
        Checksum checksum = new Checksum(null, null, "abc123");
        FileAndChecksum fc = new FileAndChecksum(file, checksum);
        assertEquals(checksum, fc.getExpectedChecksum());
    }

    @Test
    public void testInitialActualChecksumIsNull() throws IOException {
        File file = tmp.newFile("test.zip");
        FileAndChecksum fc = new FileAndChecksum(file, null);
        assertNull(fc.getActualChecksum());
    }

    @Test
    public void testSetAndGetActualChecksum() throws IOException {
        File file = tmp.newFile("test.zip");
        FileAndChecksum fc = new FileAndChecksum(file, null);
        Checksum actual = new Checksum(null, null, "def456");
        fc.setActualChecksum(actual);
        assertEquals(actual, fc.getActualChecksum());
    }

    @Test
    public void testSetExpectedChecksum() throws IOException {
        File file = tmp.newFile("test.zip");
        Checksum original = new Checksum(null, null, "original");
        FileAndChecksum fc = new FileAndChecksum(file, original);
        Checksum updated = new Checksum(null, null, "updated");
        fc.setExpectedChecksum(updated);
        assertEquals(updated, fc.getExpectedChecksum());
    }

    @Test
    public void testToStringContainsFileName() throws IOException {
        File file = tmp.newFile("mymap.zip");
        FileAndChecksum fc = new FileAndChecksum(file, null);
        String s = fc.toString();
        assertTrue(s.contains("mymap.zip"));
    }

    @Test
    public void testNullExpectedChecksum() throws IOException {
        File file = tmp.newFile("test.zip");
        FileAndChecksum fc = new FileAndChecksum(file, null);
        assertNull(fc.getExpectedChecksum());
    }
}

