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
package slash.navigation.feedback.domain;

import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.binding.*;
import slash.navigation.datasources.impl.DataSourceImpl;
import slash.navigation.rest.Get;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.calendar;
import static slash.navigation.datasources.DataSourcesUtil.asBoundingBoxType;
import static slash.navigation.datasources.DataSourcesUtil.createChecksumType;

public class ChecksumsIT extends RouteFeedbackServiceBase {
    private DataSource createDataSource(long id) {
        DatasourceType datasourceType = new DatasourceType();
        datasourceType.setId("id" + id);
        datasourceType.setName("name" + id);
        datasourceType.setBaseUrl("baseUrl" + id);
        datasourceType.setDirectory("directory" + id);

        FileType file1 = new FileType();
        file1.setUri("file1uri" + id);
        FragmentType file1fragment = new FragmentType();
        file1fragment.setKey("fragmentkey" + id);
        file1.getFragment().add(file1fragment);
        datasourceType.getFile().add(file1);

        FileType file2 = new FileType();
        file2.setUri("file2uri" + id);
        file2.getChecksum().add(createChecksumType(2L, calendar(2014, 1, 1, 1, 2, 0), "file"));
        FragmentType file2fragment = new FragmentType();
        file2fragment.setKey("fragmentkey" + id);
        file2.getFragment().add(file2fragment);
        datasourceType.getFile().add(file2);

        FileType file3 = new FileType();
        file3.setUri("file3uri" + id);
        file3.setBoundingBox(asBoundingBoxType(new BoundingBox(new SimpleNavigationPosition(1.0, 2.0), new SimpleNavigationPosition(3.0, 4.0))));
        file3.getChecksum().add(createChecksumType(3L, calendar(2014, 1, 1, 1, 3, 0), "file"));
        FragmentType fileType3fragment1 = new FragmentType();
        fileType3fragment1.setKey("fragment1key" + id);
        fileType3fragment1.getChecksum().add(createChecksumType(31L, calendar(2014, 1, 1, 1, 3, 1), "file3 fragment1"));
        file3.getFragment().add(fileType3fragment1);
        FragmentType fileType3fragment2 = new FragmentType();
        fileType3fragment2.setKey("fragment2key" + id);
        fileType3fragment2.getChecksum().add(createChecksumType(32L, calendar(2014, 1, 1, 1, 3, 2), "file3 fragment2"));
        file3.getFragment().add(fileType3fragment2);
        datasourceType.getFile().add(file3);

        MapType map = new MapType();
        map.setUri("mapuri" + id);
        map.setBoundingBox(asBoundingBoxType(new BoundingBox(new SimpleNavigationPosition(1.0, 2.0), new SimpleNavigationPosition(3.0, 4.0))));
        map.getChecksum().add(createChecksumType(100L, calendar(2014, 1, 2, 1, 1, 0), "map"));
        FragmentType mapfragment = new FragmentType();
        mapfragment.setKey("fragmentkey" + id);
        mapfragment.getChecksum().add(createChecksumType(101L, calendar(2014, 1, 2, 1, 1, 0), "map fragment"));
        map.getFragment().add(mapfragment);
        datasourceType.getMap().add(map);

        ThemeType theme = new ThemeType();
        theme.setUri("themeuri" + id);
        theme.setImageUrl("imageurl");
        theme.getChecksum().add(createChecksumType(200L, calendar(2014, 1, 3, 1, 1, 0), "theme"));
        FragmentType themefragment = new FragmentType();
        themefragment.setKey("fragmentkey" + id);
        themefragment.getChecksum().add(createChecksumType(201L, calendar(2014, 1, 3, 1, 1, 0), "theme fragment"));
        theme.getFragment().add(themefragment);
        datasourceType.getTheme().add(theme);

        return new DataSourceImpl(datasourceType);
    }

    @Test
    public void testSendChecksum() throws Exception {
        long id = currentTimeMillis();
        assertEquals("created datasource id" + id + "\n" +
                        "created file file1uri" + id + "\n" +
                        "created fragment file1uri" + id + " -> fragmentkey" + id + "\n" +
                        "created file file2uri" + id + "\n" +
                        "created checksum 2014-01-01 01:02:00, 2, file\n" +
                        "added checksum 2014-01-01 01:02:00, 2, file to file2uri" + id + "\n" +
                        "created fragment file2uri" + id + " -> fragmentkey" + id + "\n" +
                        "created file file3uri" + id + "\n" +
                        "created checksum 2014-01-01 01:03:00, 3, file\n" +
                        "added checksum 2014-01-01 01:03:00, 3, file to file3uri" + id + "\n" +
                        "created fragment file3uri" + id + " -> fragment1key" + id + "\n" +
                        "created checksum 2014-01-01 01:03:01, 31, file3 fragment1\n" +
                        "added checksum 2014-01-01 01:03:01, 31, file3 fragment1 to file3uri" + id + " -> fragment1key" + id + "\n" +
                        "created fragment file3uri" + id + " -> fragment2key" + id + "\n" +
                        "created checksum 2014-01-01 01:03:02, 32, file3 fragment2\n" +
                        "added checksum 2014-01-01 01:03:02, 32, file3 fragment2 to file3uri" + id + " -> fragment2key" + id + "\n" +
                        "created map mapuri" + id + "\n" +
                        "created checksum 2014-01-02 01:01:00, 100, map\n" +
                        "added checksum 2014-01-02 01:01:00, 100, map to mapuri" + id + "\n" +
                        "created fragment mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "created checksum 2014-01-02 01:01:00, 101, map fragment\n" +
                        "added checksum 2014-01-02 01:01:00, 101, map fragment to mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "created theme themeuri" + id + "\n" +
                        "created checksum 2014-01-03 01:01:00, 200, theme\n" +
                        "added checksum 2014-01-03 01:01:00, 200, theme to themeuri" + id + "\n" +
                        "created fragment themeuri" + id + " -> fragmentkey" + id + "\n" +
                        "created checksum 2014-01-03 01:01:00, 201, theme fragment\n" +
                        "added checksum 2014-01-03 01:01:00, 201, theme fragment to themeuri" + id + " -> fragmentkey" + id,
                routeFeedback.sendChecksums(createDataSource(id)));
    }

    @Test
    public void testSendChecksumTwice() throws Exception {
        long id = currentTimeMillis();
        DataSource datasource = createDataSource(id);
        assertEquals("created datasource id" + id + "\n" +
                        "created file file1uri" + id + "\n" +
                        "created fragment file1uri" + id + " -> fragmentkey" + id + "\n" +
                        "created file file2uri" + id + "\n" +
                        "created checksum 2014-01-01 01:02:00, 2, file\n" +
                        "added checksum 2014-01-01 01:02:00, 2, file to file2uri" + id + "\n" +
                        "created fragment file2uri" + id + " -> fragmentkey" + id + "\n" +
                        "created file file3uri" + id + "\n" +
                        "created checksum 2014-01-01 01:03:00, 3, file\n" +
                        "added checksum 2014-01-01 01:03:00, 3, file to file3uri" + id + "\n" +
                        "created fragment file3uri" + id + " -> fragment1key" + id + "\n" +
                        "created checksum 2014-01-01 01:03:01, 31, file3 fragment1\n" +
                        "added checksum 2014-01-01 01:03:01, 31, file3 fragment1 to file3uri" + id + " -> fragment1key" + id + "\n" +
                        "created fragment file3uri" + id + " -> fragment2key" + id + "\n" +
                        "created checksum 2014-01-01 01:03:02, 32, file3 fragment2\n" +
                        "added checksum 2014-01-01 01:03:02, 32, file3 fragment2 to file3uri" + id + " -> fragment2key" + id + "\n" +
                        "created map mapuri" + id + "\n" +
                        "created checksum 2014-01-02 01:01:00, 100, map\n" +
                        "added checksum 2014-01-02 01:01:00, 100, map to mapuri" + id + "\n" +
                        "created fragment mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "created checksum 2014-01-02 01:01:00, 101, map fragment\n" +
                        "added checksum 2014-01-02 01:01:00, 101, map fragment to mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "created theme themeuri" + id + "\n" +
                        "created checksum 2014-01-03 01:01:00, 200, theme\n" +
                        "added checksum 2014-01-03 01:01:00, 200, theme to themeuri" + id + "\n" +
                        "created fragment themeuri" + id + " -> fragmentkey" + id + "\n" +
                        "created checksum 2014-01-03 01:01:00, 201, theme fragment\n" +
                        "added checksum 2014-01-03 01:01:00, 201, theme fragment to themeuri" + id + " -> fragmentkey" + id,
                routeFeedback.sendChecksums(datasource));

        Get get = new Get("http://" + HOST + "/datasources/datasource/id" + id + ".xml");
        assertTrue(get.executeAsString().contains(Long.toString(id)));

        assertEquals("existing datasource id" + id + "\n" +
                        "existing file file1uri" + id + "\n" +
                        "existing fragment file1uri" + id + " -> fragmentkey" + id + "\n" +
                        "existing file file2uri" + id + "\n" +
                        "existing checksum 2014-01-01 01:02:00, 2, file for file2uri" + id + "\n" +
                        "existing fragment file2uri" + id + " -> fragmentkey" + id + "\n" +
                        "existing file file3uri" + id + "\n" +
                        "existing checksum 2014-01-01 01:03:00, 3, file for file3uri" + id + "\n" +
                        "existing fragment file3uri" + id + " -> fragment1key" + id + "\n" +
                        "existing checksum 2014-01-01 01:03:01, 31, file3 fragment1 for file3uri" + id + " -> fragment1key" + id + "\n" +
                        "existing fragment file3uri" + id + " -> fragment2key" + id + "\n" +
                        "existing checksum 2014-01-01 01:03:02, 32, file3 fragment2 for file3uri" + id + " -> fragment2key" + id + "\n" +
                        "existing map mapuri" + id + "\n" +
                        "existing checksum 2014-01-02 01:01:00, 100, map for mapuri" + id + "\n" +
                        "existing fragment mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "existing checksum 2014-01-02 01:01:00, 101, map fragment for mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "existing theme themeuri" + id + "\n" +
                        "existing checksum 2014-01-03 01:01:00, 200, theme for themeuri" + id + "\n" +
                        "existing fragment themeuri" + id + " -> fragmentkey" + id + "\n" +
                        "existing checksum 2014-01-03 01:01:00, 201, theme fragment for themeuri" + id + " -> fragmentkey" + id,
                routeFeedback.sendChecksums(datasource));
    }
}