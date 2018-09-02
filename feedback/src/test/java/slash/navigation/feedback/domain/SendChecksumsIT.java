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
import slash.navigation.datasources.binding.ActionType;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.FileType;
import slash.navigation.datasources.binding.FragmentType;
import slash.navigation.datasources.binding.MapType;
import slash.navigation.datasources.binding.ThemeType;
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
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.calendar;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.datasources.DataSourceManager.DATASOURCES_URI;
import static slash.navigation.datasources.helpers.DataSourcesUtil.asBoundingBoxType;

public class SendChecksumsIT extends RouteFeedbackServiceBase {
    private static final CompactCalendar CALENDARF1 = calendar(2014, 1, 1, 1, 1, 1);
    private static final CompactCalendar CALENDARF1F1 = calendar(2014, 1, 1, 1, 3, 1);
    private static final CompactCalendar CALENDARF2 = calendar(2014, 2, 2, 2, 2, 2);
    private static final CompactCalendar CALENDARF2F2 = calendar(2014, 2, 2, 2, 6, 2);

    private DatasourceType createDataSourceType(long id) {
        DatasourceType datasourceType = new DatasourceType();
        datasourceType.setId("id" + id);
        datasourceType.setName("name" + id);
        datasourceType.setBaseUrl("baseUrl" + id);
        datasourceType.setDirectory("directory" + id);
        datasourceType.setAction(ActionType.fromValue("Copy"));
        return datasourceType;
    }

    private FileType createFileType(long id) {
        FileType file = new FileType();
        file.setUri("fileuri" + id);
        FragmentType filefragment = new FragmentType();
        filefragment.setKey("fragmentkey" + id);
        file.getFragment().add(filefragment);
        return file;
    }

    private String createFilterUriForFile(long id) {
        return "baseUrl" + id + "fileuri" + id;
    }

    private FileAndChecksum createFileAndChecksum(String fileUri, CompactCalendar lastModified, Long contentLength, String sha1) {
        FileAndChecksum result = new FileAndChecksum(new File(fileUri), new Checksum(lastModified, contentLength, sha1));
        if (lastModified != null || contentLength != null || sha1 != null)
            result.setActualChecksum(new Checksum(lastModified != null ? fromMillis(lastModified.getTimeInMillis() + 60 * 1000) : null,
                    contentLength != null ? contentLength + 1000 : null, sha1 != null ? sha1 + "-actual" : null));
        return result;
    }

    private Map<FileAndChecksum, List<FileAndChecksum>> createFileAndNoChecksumsForFile(long id) {
        Map<FileAndChecksum, List<FileAndChecksum>> result = new HashMap<>();
        result.put(createFileAndChecksum("fileuri" + id, null, null, null),
                singletonList(createFileAndChecksum("fragmentkey", null, null, null)));
        return result;
    }

    private Map<FileAndChecksum, List<FileAndChecksum>> createFileAndChecksumForFile(long id) {
        Map<FileAndChecksum, List<FileAndChecksum>> result = new HashMap<>();
        result.put(createFileAndChecksum("fileuri" + id, CALENDARF1, 1L, "file-sha"),
                singletonList(createFileAndChecksum("fragmentkey" + id, CALENDARF1F1, 11L, "file-fragment-sha")));
        return result;
    }

    private Map<FileAndChecksum, List<FileAndChecksum>> createFileAndChecksumForFileNoSHA(long id) {
        Map<FileAndChecksum, List<FileAndChecksum>> result = new HashMap<>();
        result.put(createFileAndChecksum("fileuri" + id, CALENDARF1, 1L, null),
                singletonList(createFileAndChecksum("fragmentkey" + id, CALENDARF1F1, 11L, null)));
        return result;
    }

    private Map<FileAndChecksum, List<FileAndChecksum>> createFileAndChecksumForFileNoLastModified(long id) {
        Map<FileAndChecksum, List<FileAndChecksum>> result = new HashMap<>();
        result.put(createFileAndChecksum("fileuri" + id, null, 1L, "file-sha"),
                singletonList(createFileAndChecksum("fragmentkey" + id, null, 11L, "file-fragment-sha")));
        return result;
    }

    private Map<FileAndChecksum, List<FileAndChecksum>> createFileAndChecksumForFileOtherLastModified(long id) {
        Map<FileAndChecksum, List<FileAndChecksum>> result = new HashMap<>();
        result.put(createFileAndChecksum("fileuri" + id, CALENDARF2, 3L, "file-sha"),
                singletonList(createFileAndChecksum("fragmentkey" + id, CALENDARF2F2, 31L, "file-fragment-sha")));
        return result;
    }

    private Map<FileAndChecksum, List<FileAndChecksum>> createFileAndChecksumForFileOtherSHA(long id) {
        Map<FileAndChecksum, List<FileAndChecksum>> result = new HashMap<>();
        result.put(createFileAndChecksum("fileuri" + id, CALENDARF2, 2L, "file-sha-2"),
                singletonList(createFileAndChecksum("fragmentkey" + id, CALENDARF2F2, 21L, "file-fragment-sha-2")));
        return result;
    }

    private MapType createMapType(long id) {
        MapType map = new MapType();
        map.setUri("mapuri" + id);
        map.setBoundingBox(asBoundingBoxType(new BoundingBox(new SimpleNavigationPosition(1.0, 2.0), new SimpleNavigationPosition(3.0, 4.0))));
        FragmentType mapfragment = new FragmentType();
        mapfragment.setKey("fragmentkey" + id);
        map.getFragment().add(mapfragment);
        return map;
    }

    private Map<FileAndChecksum, List<FileAndChecksum>> createFileAndChecksumForMap(long id) {
        Map<FileAndChecksum, List<FileAndChecksum>> result = new HashMap<>();
        result.put(createFileAndChecksum("mapuri" + id, CALENDARF1, 100L, "map-sha"),
                singletonList(createFileAndChecksum("fragmentkey" + id, CALENDARF1F1, 110L, "map-fragment-sha")));
        return result;
    }

    private ThemeType createThemeType(long id) {
        ThemeType theme = new ThemeType();
        theme.setUri("themeuri" + id);
        theme.setImageUrl("imageurl");
        FragmentType themefragment1 = new FragmentType();
        themefragment1.setKey("fragment1key" + id);
        theme.getFragment().add(themefragment1);
        FragmentType themefragment2 = new FragmentType();
        themefragment2.setKey("fragment2key" + id);
        theme.getFragment().add(themefragment2);
        return theme;
    }

    private Map<FileAndChecksum, List<FileAndChecksum>> createFileAndChecksumForTheme(long id) {
        Map<FileAndChecksum, List<FileAndChecksum>> result = new HashMap<>();
        result.put(createFileAndChecksum("themeuri" + id, CALENDARF1, 5L, "theme-sha"),
                asList(createFileAndChecksum("fragment1key" + id, CALENDARF1F1, 6L, "theme-fragment1-sha"),
                        createFileAndChecksum("fragment2key" + id, CALENDARF2F2, 7L, "theme-fragment2-sha")));
        return result;
    }


    @Test
    public void testFileWithFragmentNoChecksums() throws Exception {
        long id = currentTimeMillis();
        DatasourceType dataSourceType = createDataSourceType(id);
        FileType fileType = createFileType(id);
        dataSourceType.getFile().add(fileType);
        assertEquals("\"create DataSource id" + id + ", " +
                        "create File fileuri" + id + ", " +
                        "create FileFragment fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceType), createFileAndNoChecksumsForFile(id), createFilterUriForFile(id)));

        Get get = new Get(API + DATASOURCES_URI + "id" + id + ".xml");
        assertTrue(get.executeAsString().contains(Long.toString(id)));

        fileType.setBoundingBox(asBoundingBoxType(new BoundingBox(new SimpleNavigationPosition(1.0, 2.0), new SimpleNavigationPosition(3.0, 4.0))));
        assertEquals("\"read DataSource id" + id + ", " +
                        "create File fileuri" + id + ", " +
                        "read FileFragment fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceType), createFileAndNoChecksumsForFile(id), createFilterUriForFile(id)));
    }

    @Test
    public void testFileWithFragment() throws Exception {
        long id = currentTimeMillis();
        DatasourceType dataSourceType = createDataSourceType(id);
        dataSourceType.getFile().add(createFileType(id));
        assertEquals("\"create DataSource id" + id + ", " +
                        "create File fileuri" + id + ", " +
                        "create FileChecksum 2014-01-01 01:02:01, 1001, file-sha-actual for fileuri" + id + ", " +
                        "create FileFragment fileuri" + id + " -> fragmentkey" + id + ", " +
                        "create FileFragmentChecksum 2014-01-01 01:04:01, 1011, file-fragment-sha-actual for fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceType), createFileAndChecksumForFile(id), createFilterUriForFile(id)));
    }

    @Test
    public void testFileWithFragmentAddChecksumsLater() throws Exception {
        long id = currentTimeMillis();
        DatasourceType dataSourceTypeFirst = createDataSourceType(id);
        dataSourceTypeFirst.getFile().add(createFileType(id));
        assertEquals("\"create DataSource id" + id + ", " +
                        "create File fileuri" + id + ", " +
                        "create FileFragment fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceTypeFirst), createFileAndNoChecksumsForFile(id), createFilterUriForFile(id)));

        DatasourceType dataSourceTypeLater = createDataSourceType(id);
        dataSourceTypeLater.getFile().add(createFileType(id));
        assertEquals("\"read DataSource id" + id + ", " +
                        "read File fileuri" + id + ", " +
                        "create FileChecksum 2014-01-01 01:02:01, 1001, file-sha-actual for fileuri" + id + ", " +
                        "read FileFragment fileuri" + id + " -> fragmentkey" + id + ", " +
                        "create FileFragmentChecksum 2014-01-01 01:04:01, 1011, file-fragment-sha-actual for fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceTypeLater), createFileAndChecksumForFile(id), createFilterUriForFile(id)));

        DatasourceType dataSourceTypeRead = createDataSourceType(id);
        dataSourceTypeRead.getFile().add(createFileType(id));
        assertEquals("\"read DataSource id" + id + ", " +
                        "read File fileuri" + id + ", " +
                        "read FileChecksum 2014-01-01 01:02:01, 1001, file-sha-actual for fileuri" + id + ", " +
                        "read FileFragment fileuri" + id + " -> fragmentkey" + id + ", " +
                        "read FileFragmentChecksum 2014-01-01 01:04:01, 1011, file-fragment-sha-actual for fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceTypeRead), createFileAndChecksumForFile(id), createFilterUriForFile(id)));
    }

    @Test
    public void testFileWithFragmentNewChecksumOtherSHA() throws Exception {
        long id = currentTimeMillis();
        DatasourceType dataSourceTypeFirst = createDataSourceType(id);
        dataSourceTypeFirst.getFile().add(createFileType(id));
        assertEquals("\"create DataSource id" + id + ", " +
                        "create File fileuri" + id + ", " +
                        "create FileChecksum 2014-01-01 01:02:01, 1001, file-sha-actual for fileuri" + id + ", " +
                        "create FileFragment fileuri" + id + " -> fragmentkey" + id + ", " +
                        "create FileFragmentChecksum 2014-01-01 01:04:01, 1011, file-fragment-sha-actual for fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceTypeFirst), createFileAndChecksumForFile(id), createFilterUriForFile(id)));

        DatasourceType dataSourceTypeSecond = createDataSourceType(id);
        dataSourceTypeSecond.getFile().add(createFileType(id));
        assertEquals("\"read DataSource id" + id + ", " +
                        "read File fileuri" + id + ", " +
                        "create FileChecksum 2014-02-02 02:03:02, 1002, file-sha-2-actual for fileuri" + id + ", " +
                        "read FileFragment fileuri" + id + " -> fragmentkey" + id + ", " +
                        "create FileFragmentChecksum 2014-02-02 02:07:02, 1021, file-fragment-sha-2-actual for fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceTypeSecond), createFileAndChecksumForFileOtherSHA(id), createFilterUriForFile(id)));
    }

    @Test
    public void testFileWithFragmentNewChecksumOtherLastModified() throws Exception {
        long id = currentTimeMillis();
        DatasourceType dataSourceTypeFirst = createDataSourceType(id);
        dataSourceTypeFirst.getFile().add(createFileType(id));
        assertEquals("\"create DataSource id" + id + ", " +
                        "create File fileuri" + id + ", " +
                        "create FileChecksum 2014-01-01 01:02:01, 1001, file-sha-actual for fileuri" + id + ", " +
                        "create FileFragment fileuri" + id + " -> fragmentkey" + id + ", " +
                        "create FileFragmentChecksum 2014-01-01 01:04:01, 1011, file-fragment-sha-actual for fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceTypeFirst), createFileAndChecksumForFile(id), createFilterUriForFile(id)));

        DatasourceType dataSourceTypeSecond = createDataSourceType(id);
        dataSourceTypeSecond.getFile().add(createFileType(id));
        assertEquals("\"read DataSource id" + id + ", " +
                        "read File fileuri" + id + ", " +
                        "read FileChecksum 2014-01-01 01:02:01, 1001, file-sha-actual for fileuri" + id + ", " +
                        "read FileFragment fileuri" + id + " -> fragmentkey" + id + ", " +
                        "read FileFragmentChecksum 2014-01-01 01:04:01, 1011, file-fragment-sha-actual for fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceTypeSecond), createFileAndChecksumForFileOtherLastModified(id), createFilterUriForFile(id)));
    }

    @Test
    public void testFileWithFragmentAddSHALater() throws Exception {
        long id = currentTimeMillis();
        DatasourceType dataSourceTypeFirst = createDataSourceType(id);
        dataSourceTypeFirst.getFile().add(createFileType(id));
        assertEquals("\"create DataSource id" + id + ", " +
                        "create File fileuri" + id + ", " +
                        "create FileChecksum 2014-01-01 01:02:01, 1001, None for fileuri" + id + ", " +
                        "create FileFragment fileuri" + id + " -> fragmentkey" + id + ", " +
                        "create FileFragmentChecksum 2014-01-01 01:04:01, 1011, None for fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceTypeFirst), createFileAndChecksumForFileNoSHA(id), createFilterUriForFile(id)));

        DatasourceType dataSourceTypeSecond = createDataSourceType(id);
        dataSourceTypeSecond.getFile().add(createFileType(id));
        assertEquals("\"read DataSource id" + id + ", " +
                        "read File fileuri" + id + ", " +
                        "create FileChecksum 2014-01-01 01:02:01, 1001, file-sha-actual for fileuri" + id + ", " +
                        "read FileFragment fileuri" + id + " -> fragmentkey" + id + ", " +
                        "create FileFragmentChecksum 2014-01-01 01:04:01, 1011, file-fragment-sha-actual for fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceTypeSecond), createFileAndChecksumForFile(id), createFilterUriForFile(id)));
    }

    @Test
    public void testFileWithFragmentAddLastModifiedLater() throws Exception {
        long id = currentTimeMillis();
        DatasourceType dataSourceTypeFirst = createDataSourceType(id);
        dataSourceTypeFirst.getFile().add(createFileType(id));
        assertEquals("\"create DataSource id" + id + ", " +
                        "create File fileuri" + id + ", " +
                        "create FileChecksum None, 1001, file-sha-actual for fileuri" + id + ", " +
                        "create FileFragment fileuri" + id + " -> fragmentkey" + id + ", " +
                        "create FileFragmentChecksum None, 1011, file-fragment-sha-actual for fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceTypeFirst), createFileAndChecksumForFileNoLastModified(id), createFilterUriForFile(id)));

        DatasourceType dataSourceTypeSecond = createDataSourceType(id);
        dataSourceTypeSecond.getFile().add(createFileType(id));
        assertEquals("\"read DataSource id" + id + ", " +
                        "read File fileuri" + id + ", " +
                        "create FileChecksum 2014-01-01 01:02:01, 1001, file-sha-actual for fileuri" + id + ", " +
                        "read FileFragment fileuri" + id + " -> fragmentkey" + id + ", " +
                        "create FileFragmentChecksum 2014-01-01 01:04:01, 1011, file-fragment-sha-actual for fileuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceTypeSecond), createFileAndChecksumForFile(id), createFilterUriForFile(id)));
    }

    @Test
    public void testMapWithFragment() throws Exception {
        long id = currentTimeMillis();
        DatasourceType dataSourceType = createDataSourceType(id);
        dataSourceType.getMap().add(createMapType(id));
        assertEquals("\"create DataSource id" + id + ", " +
                        "create Map mapuri" + id + ", " +
                        "create MapChecksum 2014-01-01 01:02:01, 1100, map-sha-actual for mapuri" + id + ", " +
                        "create MapFragment mapuri" + id + " -> fragmentkey" + id + ", " +
                        "create MapFragmentChecksum 2014-01-01 01:04:01, 1110, map-fragment-sha-actual for mapuri" + id + " -> fragmentkey" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceType), createFileAndChecksumForMap(id), "baseUrl" + id + "mapuri" + id));
    }

    @Test
    public void testThemeWithFragments() throws Exception {
        long id = currentTimeMillis();
        DatasourceType dataSourceType = createDataSourceType(id);
        dataSourceType.getTheme().add(createThemeType(id));
        assertEquals("\"create DataSource id" + id + ", " +
                        "create Theme themeuri" + id + ", " +
                        "create ThemeChecksum 2014-01-01 01:02:01, 1005, theme-sha-actual for themeuri" + id + ", " +
                        "create ThemeFragment themeuri" + id + " -> fragment1key" + id + ", " +
                        "create ThemeFragmentChecksum 2014-01-01 01:04:01, 1006, theme-fragment1-sha-actual for themeuri" + id + " -> fragment1key" + id + ", " +
                        "create ThemeFragment themeuri" + id + " -> fragment2key" + id + ", " +
                        "create ThemeFragmentChecksum 2014-02-02 02:07:02, 1007, theme-fragment2-sha-actual for themeuri" + id + " -> fragment2key" + id + ", \"",
                routeFeedback.sendChecksums(new DataSourceImpl(dataSourceType), createFileAndChecksumForTheme(id), "baseUrl" + id + "themeuri" + id));
    }
}