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
import slash.navigation.datasources.helpers.DataSourceService;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DataSourceServiceTest {
    private DataSourceService service;

    @Before
    public void setUp() throws Exception {
        service = new DataSourceService();
        service.load(getClass().getResourceAsStream("testdatasources.xml"));
    }

    private void checkDatasourceType(DataSource dataSource, String id, String name, String baseUrl, String directory) {
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
        }
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
            assertEquals(value, file.getLatestChecksum().getSHA1());
        }
    }

    @Test
    public void testBaseUrl() throws IOException {
        List<DataSource> dataSources = service.getDataSources();

        String baseUrl1 = "http://local1/1/";
        String baseUrl2 = "http://local2/2/";
        String baseUrl3 = "http://local3/3/";

        assertEquals(3, dataSources.size());
        checkDatasourceType(dataSources.get(0), "id1", "name1", baseUrl1, "dir1");
        checkDatasourceType(service.getDataSourceByUrlPrefix(baseUrl2), "id2", "name2", baseUrl2, "dir2/dir3");
        checkDatasourceType(dataSources.get(2), "id3", "name3", baseUrl3, "dir4");

        File file1 = getFile(service.getDataSourceByUrlPrefix(baseUrl1).getFiles(), "x/y/z.data");
        assertNotNull(file1);
        checkFragments(file1.getFragments(), "a", "a");
        File file2 = getFile(service.getDataSourceByUrlPrefix(baseUrl2).getFiles(), "x/y/z.data");
        assertNotNull(file2);
        checkFragments(file2.getFragments(), "b", "b");
        File file3 = getFile(service.getDataSourceByUrlPrefix(baseUrl2).getFiles(), "z/y/x.data");
        assertNotNull(file3);
        checkFragments(file3.getFragments(), "c", "c");

        checkFiles(service.getDataSourceByUrlPrefix(baseUrl1).getFiles(), "x/y/z.data", "x");
        checkFiles(service.getDataSourceByUrlPrefix(baseUrl3).getFiles());
        checkFiles(service.getDataSourceByUrlPrefix(baseUrl2).getFiles(), "x/y/z.data", "x", "z/y/x.data", "z");
    }
}
