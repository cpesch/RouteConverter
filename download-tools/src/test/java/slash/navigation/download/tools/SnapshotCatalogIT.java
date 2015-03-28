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
package slash.navigation.download.tools;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;
import static slash.navigation.download.tools.base.BaseDownloadTool.getRootDirectory;

public class SnapshotCatalogIT {
    private static final String API = System.getProperty("api", "http://localhost:8000/");

    @Before
    public void setUp() throws IOException {
        File editionsXml = createEditionsXml();
        if (editionsXml.exists())
            assertTrue(editionsXml.delete());
    }

    private File createEditionsXml() {
        return new File(getRootDirectory(), "editions.xml");
    }

    @Test
    public void snapshotEditions() throws IOException, JAXBException {
        File before = createEditionsXml();
        assertFalse(before.exists());

        SnapshotCatalog snapshot1 = new SnapshotCatalog();
        snapshot1.setUrl(API);
        snapshot1.setReset(true);
        snapshot1.snapshot();

        File after = createEditionsXml();
        assertTrue(after.exists());
        long length = after.length();
        long lastModified = after.lastModified();

        SnapshotCatalog snapshot2 = new SnapshotCatalog();
        snapshot2.setUrl(API);
        snapshot2.snapshot();

        File second = createEditionsXml();
        assertTrue(second.exists());
        assertEquals(length, after.length());
        assertEquals(lastModified, second.lastModified());
    }
}
