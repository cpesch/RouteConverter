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
import slash.common.type.CompactCalendar;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.binding.*;
import slash.navigation.datasources.impl.DataSourceImpl;
import slash.navigation.download.Checksum;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.rest.Get;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.calendar;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.datasources.DataSourcesUtil.asBoundingBoxType;
import static slash.navigation.datasources.DataSourcesUtil.createChecksumType;

public class ChecksumsIT extends RouteFeedbackServiceBase {
    private static final CompactCalendar CALENDARF2 = calendar(2014, 1, 1, 1, 2, 0);
    private static final CompactCalendar CALENDARF3 = calendar(2014, 1, 1, 1, 3, 0);
    private static final CompactCalendar CALENDARF3F1 = calendar(2014, 1, 1, 1, 3, 1);
    private static final CompactCalendar CALENDARF3F2 = calendar(2014, 1, 1, 1, 3, 2);
    private static final CompactCalendar CALENDARM = calendar(2014, 1, 2, 1, 1, 0);
    private static final CompactCalendar CALENDARMF = calendar(2014, 1, 2, 1, 1, 0);
    private static final CompactCalendar CALENDART = calendar(2014, 1, 3, 1, 1, 0);
    private static final CompactCalendar CALENDARTF = calendar(2014, 1, 3, 1, 1, 0);

    private DataSource createDataSource(long id) {
        DatasourceType datasourceType = new DatasourceType();
        datasourceType.setId("id" + id);
        datasourceType.setName("name" + id);
        datasourceType.setBaseUrl("baseUrl" + id);
        datasourceType.setDirectory("directory" + id);

        FileType file2 = new FileType();
        file2.setUri("file2uri" + id);
        FragmentType file2fragment = new FragmentType();
        file2fragment.setKey("fragmentkey" + id);
        file2.getFragment().add(file2fragment);
        datasourceType.getFile().add(file2);

        FileType file3 = new FileType();
        file3.setUri("file3uri" + id);
        file3.setBoundingBox(asBoundingBoxType(new BoundingBox(new SimpleNavigationPosition(1.0, 2.0), new SimpleNavigationPosition(3.0, 4.0))));
        file3.getChecksum().add(createChecksumType(3L, CALENDARF3, "file3"));
        FragmentType fileType3fragment1 = new FragmentType();
        fileType3fragment1.setKey("fragment1key");
        fileType3fragment1.getChecksum().add(createChecksumType(31L, CALENDARF3F1, "file3 fragment1"));
        file3.getFragment().add(fileType3fragment1);
        FragmentType fileType3fragment2 = new FragmentType();
        fileType3fragment2.setKey("fragment2key");
        fileType3fragment2.getChecksum().add(createChecksumType(32L, CALENDARF3F2, "file3 fragment2"));
        file3.getFragment().add(fileType3fragment2);
        datasourceType.getFile().add(file3);

        MapType map = new MapType();
        map.setUri("mapuri" + id);
        map.setBoundingBox(asBoundingBoxType(new BoundingBox(new SimpleNavigationPosition(1.0, 2.0), new SimpleNavigationPosition(3.0, 4.0))));
        map.getChecksum().add(createChecksumType(100L, CALENDARM, "map"));
        FragmentType mapfragment = new FragmentType();
        mapfragment.setKey("fragmentkey" + id);
        mapfragment.getChecksum().add(createChecksumType(101L, CALENDARMF, "map fragment"));
        map.getFragment().add(mapfragment);
        datasourceType.getMap().add(map);

        ThemeType theme = new ThemeType();
        theme.setUri("themeuri" + id);
        theme.setImageUrl("imageurl");
        theme.getChecksum().add(createChecksumType(200L, CALENDART, "theme"));
        FragmentType themefragment = new FragmentType();
        themefragment.setKey("fragmentkey" + id);
        themefragment.getChecksum().add(createChecksumType(201L, CALENDARTF, "theme fragment"));
        theme.getFragment().add(themefragment);
        datasourceType.getTheme().add(theme);

        return new DataSourceImpl(datasourceType);
    }

    private Map<FileAndChecksum, List<FileAndChecksum>> createFileAndChecksums(long id) {
        Map<FileAndChecksum, List<FileAndChecksum>> result = new HashMap<>();
        result.put(createFileAndChecksum("file2uri" + id, CALENDARF2, 2L, "file2"), null);
        result.put(createFileAndChecksum("file3uri" + id, CALENDARF3, 3L, "file3"),
                asList(createFileAndChecksum("fragment1key", CALENDARF3F1, 31L, "file3 fragment1"),
                        createFileAndChecksum("fragment2key", CALENDARF3F2, 32L, "file3 fragment2")));
        result.put(createFileAndChecksum("mapuri" + id, CALENDARM, 100L, "map"),
                asList(createFileAndChecksum("fragmentkey" + id, CALENDARMF, 101L, "map fragment")));
        result.put(createFileAndChecksum("themeuri" + id, CALENDART, 200L, "theme"),
                asList(createFileAndChecksum("fragmentkey" + id, CALENDARTF, 201L, "theme fragment")));
        return result;
    }

    private FileAndChecksum createFileAndChecksum(String fileUri, CompactCalendar lastModified, long contentLength, String sha1) {
        FileAndChecksum result = new FileAndChecksum(new File(fileUri), new Checksum(lastModified, contentLength, sha1));
        result.setActualChecksum(new Checksum(fromMillis(lastModified.getTimeInMillis() + 1000), contentLength + 1, sha1 + "-actual"));
        return result;
    }

    private String[] createFilterUris(long id) {
        String baseUrl = "baseUrl" + id;
        return new String[]{baseUrl + "file1uri" + id, baseUrl + "file2uri" + id, baseUrl + "file3uri" + id,
                baseUrl + "mapuri" + id, baseUrl + "themeuri" + id};
    }

    @Test
    public void testSendChecksum() throws Exception {
        long id = currentTimeMillis();
        assertEquals("created datasource id" + id + "\n" +
                        "created File file2uri" + id + "\n" +
                        "created FileFragment file2uri" + id + " -> fragmentkey" + id + "\n" +
                        "created File file3uri" + id + "\n" +
                        "created FileChecksum 2014-01-01 01:03:01, 4, file3-actual for file3uri" + id + "\n" +
                        "created FileFragment file3uri" + id + " -> fragment1key\n" +
                        "created FileFragmentChecksum 2014-01-01 01:03:02, 32, file3 fragment1-actual for file3uri" + id + " -> fragment1key\n" +
                        "created FileFragment file3uri" + id + " -> fragment2key\n" +
                        "created FileFragmentChecksum 2014-01-01 01:03:03, 33, file3 fragment2-actual for file3uri" + id + " -> fragment2key\n" +
                        "created Map mapuri" + id + "\n" +
                        "created MapChecksum 2014-01-02 01:01:01, 101, map-actual for mapuri" + id + "\n" +
                        "created MapFragment mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "created MapFragmentChecksum 2014-01-02 01:01:01, 102, map fragment-actual for mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "created Theme themeuri" + id + "\n" +
                        "created ThemeChecksum 2014-01-03 01:01:01, 201, theme-actual for themeuri" + id + "\n" +
                        "created ThemeFragment themeuri" + id + " -> fragmentkey" + id + "\n" +
                        "created ThemeFragmentChecksum 2014-01-03 01:01:01, 202, theme fragment-actual for themeuri" + id + " -> fragmentkey" + id,
                routeFeedback.sendChecksums(createDataSource(id), createFileAndChecksums(id), createFilterUris(id)));
    }

    @Test
    public void testSendChecksumTwice() throws Exception {
        long id = currentTimeMillis();
        assertEquals("created datasource id" + id + "\n" +
                        "created File file2uri" + id + "\n" +
                        "created FileFragment file2uri" + id + " -> fragmentkey" + id + "\n" +
                        "created File file3uri" + id + "\n" +
                        "created FileChecksum 2014-01-01 01:03:01, 4, file3-actual for file3uri" + id + "\n" +
                        "created FileFragment file3uri" + id + " -> fragment1key\n" +
                        "created FileFragmentChecksum 2014-01-01 01:03:02, 32, file3 fragment1-actual for file3uri" + id + " -> fragment1key\n" +
                        "created FileFragment file3uri" + id + " -> fragment2key\n" +
                        "created FileFragmentChecksum 2014-01-01 01:03:03, 33, file3 fragment2-actual for file3uri" + id + " -> fragment2key\n" +
                        "created Map mapuri" + id + "\n" +
                        "created MapChecksum 2014-01-02 01:01:01, 101, map-actual for mapuri" + id + "\n" +
                        "created MapFragment mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "created MapFragmentChecksum 2014-01-02 01:01:01, 102, map fragment-actual for mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "created Theme themeuri" + id + "\n" +
                        "created ThemeChecksum 2014-01-03 01:01:01, 201, theme-actual for themeuri" + id + "\n" +
                        "created ThemeFragment themeuri" + id + " -> fragmentkey" + id + "\n" +
                        "created ThemeFragmentChecksum 2014-01-03 01:01:01, 202, theme fragment-actual for themeuri" + id + " -> fragmentkey" + id,
                routeFeedback.sendChecksums(createDataSource(id), createFileAndChecksums(id), createFilterUris(id)));

        Get get = new Get(DATASOURCES + "datasource/id" + id + ".xml");
        assertTrue(get.executeAsString().contains(Long.toString(id)));

        assertEquals("existing datasource id" + id + "\n" +
                        "existing File file2uri" + id + "\n" +
                        "existing FileFragment file2uri" + id + " -> fragmentkey" + id + "\n" +
                        "existing File file3uri" + id + "\n" +
                        "existing FileChecksum 2014-01-01 01:03:01, 4, file3-actual for file3uri" + id + "\n" +
                        "existing FileFragment file3uri" + id + " -> fragment1key\n" +
                        "existing FileFragmentChecksum 2014-01-01 01:03:02, 32, file3 fragment1-actual for file3uri" + id + " -> fragment1key\n" +
                        "existing FileFragment file3uri" + id + " -> fragment2key\n" +
                        "existing FileFragmentChecksum 2014-01-01 01:03:03, 33, file3 fragment2-actual for file3uri" + id + " -> fragment2key\n" +
                        "existing Map mapuri" + id + "\n" +
                        "existing MapChecksum 2014-01-02 01:01:01, 101, map-actual for mapuri" + id + "\n" +
                        "existing MapFragment mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "existing MapFragmentChecksum 2014-01-02 01:01:01, 102, map fragment-actual for mapuri" + id + " -> fragmentkey" + id + "\n" +
                        "existing Theme themeuri" + id + "\n" +
                        "existing ThemeChecksum 2014-01-03 01:01:01, 201, theme-actual for themeuri" + id + "\n" +
                        "existing ThemeFragment themeuri" + id + " -> fragmentkey" + id + "\n" +
                        "existing ThemeFragmentChecksum 2014-01-03 01:01:01, 202, theme fragment-actual for themeuri" + id + " -> fragmentkey" + id,
                routeFeedback.sendChecksums(createDataSource(id), createFileAndChecksums(id), createFilterUris(id)));
    }
}