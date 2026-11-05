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

package slash.navigation.maps.mapsforge;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.maps.mapsforge.MapsforgeMapManager.isMapsforgeMapFile;

public class MapsforgeMapManagerTest {

    private File writeFile(String content) throws IOException {
        File file = File.createTempFile("MapsforgeMapManagerTest", ".map");
        file.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.US_ASCII));
        }
        return file;
    }

    @Test
    public void testMapsforgeHeaderIsRecognized() throws IOException {
        File file = writeFile("mapsforge binary OSM" + "some more padding bytes to simulate a real map file");
        assertTrue(isMapsforgeMapFile(file));
    }

    @Test
    public void testHtmlPageIsNotRecognized() throws IOException {
        File file = writeFile("<!doctype html><html><head></head><body>error</body></html>");
        assertFalse(isMapsforgeMapFile(file));
    }

    @Test
    public void testEmptyFileIsNotRecognized() throws IOException {
        File file = writeFile("");
        assertFalse(isMapsforgeMapFile(file));
    }

    @Test
    public void testNonExistentFileIsNotRecognized() {
        File file = new File("does/not/exist.map");
        assertFalse(isMapsforgeMapFile(file));
    }
}
