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
package slash.navigation.datasources;

import org.junit.Before;
import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.helpers.DataSourceService;
import slash.navigation.download.Checksum;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static slash.common.TestCase.calendar;

public class DataSourceServiceTest {
    private DataSourceService service;

    @Before
    public void setUp() throws Exception {
        service = new DataSourceService();
        service.load(getClass().getResourceAsStream("testdatasources.xml"));
    }

    private void checkEdition(Edition edition, String id, String name, String href, int dataSourceCount) {
        assertEquals(id, edition.getId());
        assertEquals(name, edition.getName());
        assertEquals(href, edition.getHref());
        assertEquals(dataSourceCount, edition.getDataSources().size());
    }

    private void checkDatasource(DataSource dataSource, String id, String name, String baseUrl, String directory) {
        assertEquals(id, dataSource.getId());
        assertEquals(name, dataSource.getName());
        assertEquals(baseUrl, dataSource.getBaseUrl());
        assertEquals(directory, dataSource.getDirectory());
    }

    private Fragment<Downloadable> getFragment(List<Fragment<Downloadable>> fragments, String key) {
        for (Fragment<Downloadable> fragment : fragments) {
            if (fragment.getKey().equals(key))
                return fragment;
        }
        return null;
    }


    private void checkFragments(List<Fragment<Downloadable>> fragments, String... keyValues) {
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            Fragment<Downloadable> fragment = getFragment(fragments, key);
            assertNotNull(fragment);
            assertEquals(value, fragment.getKey());
            checkChecksum(fragment.getLatestChecksum());
        }
    }

    private void checkChecksum(Checksum checksum) {
        assertNotNull(checksum);
        assertEquals(1L, checksum.getContentLength().longValue());
        assertEquals( calendar(2011, 1, 1, 1, 1, 1), checksum.getLastModified());
        assertEquals("a", checksum.getSHA1());
    }

    private void checkBoundingBox(BoundingBox boundingBox) {
        assertNotNull(boundingBox);
        assertEquals(new SimpleNavigationPosition(1.0, 2.0), boundingBox.getNorthEast());
        assertEquals(new SimpleNavigationPosition(3.0, 4.0), boundingBox.getSouthWest());
    }

    private File getFile(List<File> files, String uri) {
        for (File file : files) {
            if (file.getUri().equals(uri))
                return file;
        }
        return null;
    }

    private void checkFiles(List<File> files, String... keyValues) {
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            File file = getFile(files, key);
            assertNotNull(file);
            checkChecksum(file.getLatestChecksum());
            checkBoundingBox(file.getBoundingBox());
            assertEquals(value, file.getLatestChecksum().getSHA1());
        }
    }

    @Test
    public void testEditions() {
        List<Edition> editions = service.getEditions();

        assertEquals(2, editions.size());
        checkEdition(editions.get(0), "ed1", "edition1", "http://edition1", 2);
        checkDatasource(editions.get(0).getDataSources().get(1), "id2", null, null, null);
        checkEdition(editions.get(1), "ed2", "edition2", "http://edition2", 1);
    }

    @Test
    public void testDataSource() {
        List<DataSource> dataSources = service.getDataSources();

        String baseUrl1 = "http://local1/1/";
        String baseUrl2 = "http://local2/2/";
        String baseUrl3 = "http://local3/3/";

        assertEquals(3, dataSources.size());
        checkDatasource(dataSources.get(0), "id1", "name1", baseUrl1, "dir1");
        checkDatasource(service.getDataSourceByUrlPrefix(baseUrl2), "id2", "name2", baseUrl2, "dir2/dir3");
        checkDatasource(dataSources.get(2), "id3", "name3", baseUrl3, "dir4");

        File file1 = getFile(service.getDataSourceByUrlPrefix(baseUrl1).getFiles(), "x/y/z.data");
        assertNotNull(file1);
        checkFragments(file1.getFragments(), "a", "a");
        File file2 = getFile(service.getDataSourceByUrlPrefix(baseUrl2).getFiles(), "x/y/z.data");
        assertNotNull(file2);
        checkFragments(file2.getFragments(), "b", "b");
        File file3 = getFile(service.getDataSourceByUrlPrefix(baseUrl2).getFiles(), "z/y/x.data");
        assertNotNull(file3);
        checkFragments(file3.getFragments(), "c", "c");

        checkFiles(service.getDataSourceByUrlPrefix(baseUrl1).getFiles(), "x/y/z.data", "a");
        checkFiles(service.getDataSourceByUrlPrefix(baseUrl3).getFiles());
        checkFiles(service.getDataSourceByUrlPrefix(baseUrl2).getFiles(), "x/y/z.data", "a", "z/y/x.data", "a");
    }
}
