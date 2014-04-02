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
package slash.navigation.download.datasources;

import org.junit.Before;
import org.junit.Test;
import slash.navigation.download.datasources.binding.DatasourceType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DataSourceServiceIT {
    private DataSourceService service;

    @Before
    public void setUp() throws Exception {
        service = new DataSourceService();
        service.load(getClass().getResourceAsStream("testdatasources.xml"));
    }

    private void checkDatasourceType(DatasourceType datasourceType, String name, String baseUrl, String directory) {
        assertEquals(name, datasourceType.getName());
        assertEquals(baseUrl, datasourceType.getBaseUrl());
        assertEquals(directory, datasourceType.getDirectory());
    }

    private void checkArchives(Map<String, Fragment> archiveMap, String... keyValues) {
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            assertEquals(value, archiveMap.get(key).getUri());
        }
    }

    private void checkFiles(Map<String, File> fileMap, String... keyValues) {
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            assertEquals(value, fileMap.get(key).getChecksum());
        }
    }

    @Test
    public void testBaseUrl() throws IOException {
        List<DatasourceType> datasourceTypes = service.getDatasourceTypes();

        assertEquals(3, datasourceTypes.size());
        checkDatasourceType(datasourceTypes.get(0), "1", "http://local1/1/", "dir1");
        checkDatasourceType(service.getDataSource("2"), "2", "http://local2/2/", "dir2/dir3");
        checkDatasourceType(datasourceTypes.get(2), "3", "http://local3/3/", "dir4");

        checkArchives(service.getFragments("1"), "a", "x/y/z.data");
        checkArchives(service.getFragments("3"));
        checkArchives(service.getFragments("2"), "b", "x/y/z.data", "c", "z/y/x.data");

        checkFiles(service.getFiles("1"), "x/y/z.data", "x");
        checkFiles(service.getFiles("3"));
        checkFiles(service.getFiles("2"), "x/y/z.data", "x", "z/y/x.data", "z");
    }
}
