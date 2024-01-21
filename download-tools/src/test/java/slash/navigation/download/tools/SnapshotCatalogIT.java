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

import org.junit.Test;

import jakarta.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class SnapshotCatalogIT {
    private static final String API = System.getProperty("api", "https://api.routeconverter.com/");

    private File createEditionsXml(SnapshotCatalog snapshotCatalog) {
        return new File(snapshotCatalog.getRootDirectory(), "editions.xml");
    }

    private File createOfflineXml(SnapshotCatalog snapshotCatalog) {
        return new File(snapshotCatalog.getEditionsDirectory(), "offline.xml");
    }

    private File createDataSourceXml(SnapshotCatalog snapshotCatalog) {
        return new File(snapshotCatalog.getDataSourcesDirectory(), "androidmaps.xml");
    }

    private void snapshotEditions(boolean reset) throws IOException, JAXBException {
        SnapshotCatalog snapshot1 = new SnapshotCatalog();
        snapshot1.setDataSourcesServer(API);
        snapshot1.setReset(reset);

        File before = createEditionsXml(snapshot1);
        if (before.exists())
            assertTrue(before.delete());
        assertFalse(before.exists());

        snapshot1.snapshot();

        File edition1 = createEditionsXml(snapshot1);
        assertTrue(edition1.exists());
        File offline1 = createOfflineXml(snapshot1);
        assertTrue(offline1.exists());
        File datasource1 = createDataSourceXml(snapshot1);
        assertTrue(datasource1.exists());

        SnapshotCatalog snapshot2 = new SnapshotCatalog();
        snapshot2.setDataSourcesServer(API);
        snapshot2.snapshot();

        File edition2 = createEditionsXml(snapshot2);
        assertTrue(edition2.exists());
        File offline2 = createOfflineXml(snapshot2);
        assertTrue(offline2.exists());
        File datasource2 = createDataSourceXml(snapshot2);
        assertTrue(datasource2.exists());

        assertEquals(edition1.length(), edition2.length());

        assertEquals(offline1.length(), offline2.length());
        assertEquals(offline1.lastModified(), offline2.lastModified());

        assertEquals(datasource1.length(), datasource2.length());
        assertEquals(datasource1.lastModified(), datasource2.lastModified());
    }

    @Test
    public void snapshotEditionsWithoutReset() throws IOException, JAXBException {
        snapshotEditions(false);
    }

    @Test
    public void snapshotEditionsWithReset() throws IOException, JAXBException {
        snapshotEditions(true);
    }
}
