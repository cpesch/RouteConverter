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
package slash.navigation.datasources.helpers;

import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Fragment;
import slash.navigation.datasources.binding.BoundingBoxType;
import slash.navigation.datasources.binding.CatalogType;
import slash.navigation.datasources.binding.ChecksumType;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.FileType;
import slash.navigation.datasources.binding.FragmentType;
import slash.navigation.datasources.binding.MapType;
import slash.navigation.datasources.binding.SourceType;
import slash.navigation.datasources.binding.ThemeType;
import slash.navigation.download.Checksum;
import slash.navigation.download.FileAndChecksum;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.zip.ZipEntry;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.datasources.binding.ActionType.COPY;
import static slash.navigation.datasources.helpers.DataSourcesUtil.asBoundingBox;
import static slash.navigation.datasources.helpers.DataSourcesUtil.asBoundingBoxType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.asChecksum;
import static slash.navigation.datasources.helpers.DataSourcesUtil.asChecksums;
import static slash.navigation.datasources.helpers.DataSourcesUtil.asDatasourceType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.asMetaDataComparablePath;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createChecksumType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createFileType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createFragmentType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createMapType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createThemeType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.toXml;
import static slash.navigation.datasources.helpers.DataSourcesUtil.unmarshal;

/**
 * Tests JAXB round-tripping for datasource catalog bindings.
 *
 * @author Christian Pesch
 */
public class DataSourcesUtilTest {
    @Test
    public void testRoundTripSourceConfiguration() throws Exception {
        CatalogType catalogType;
        try (InputStream in = getClass().getResourceAsStream("/slash/navigation/datasources/testdatasources.xml")) {
            catalogType = unmarshal(in);
        }

        String xml = toXml(catalogType);
        assertTrue(xml.contains("http://api.routeconverter.com/v1/schemas/datasource-catalog"));

        CatalogType roundTrip;
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            roundTrip = unmarshal(in);
        }

        checkSource(getDataSource(roundTrip, "id1"), "http://local1/index.html", Integer.valueOf(2), asList("*.data", "*.zip"), asList("*/skip.data"));
        checkSource(getDataSource(roundTrip, "id2"), null, null, asList(), asList());
        assertNull(getDataSource(roundTrip, "id3").getSource());
    }

    private DatasourceType getDataSource(CatalogType catalogType, String id) {
        for (DatasourceType datasourceType : catalogType.getDatasource()) {
            if (id.equals(datasourceType.getId()))
                return datasourceType;
        }
        throw new AssertionError("Datasource not found: " + id);
    }

    private void checkSource(DatasourceType datasourceType, String url, Integer level,
                             java.util.List<String> includes, java.util.List<String> excludes) {
        assertNotNull(datasourceType);

        SourceType sourceType = datasourceType.getSource();
        assertNotNull(sourceType);
        assertEquals(url, sourceType.getUrl());
        assertEquals(level, sourceType.getLevel());
        assertEquals(includes, sourceType.getInclude());
        assertEquals(excludes, sourceType.getExclude());
    }

    @Test
    public void checksumTypeRoundTripsThroughDomainChecksum() {
        ChecksumType checksumType = createChecksumType(fromMillis(1000), 42L, "abc");

        assertEquals(Long.valueOf(42), checksumType.getContentLength());
        assertEquals("abc", checksumType.getSha1());
        assertNotNull(checksumType.getLastModified());

        Checksum checksum = asChecksum(checksumType);
        assertEquals(Long.valueOf(42), checksum.getContentLength());
        assertEquals("abc", checksum.getSHA1());
        assertEquals(fromMillis(1000), checksum.getLastModified());
    }

    @Test
    public void asChecksumsCollectsTheActualChecksums() {
        FileAndChecksum fileAndChecksum = new FileAndChecksum(new File("ignored"), null);
        fileAndChecksum.setActualChecksum(new Checksum(fromMillis(0), 5L, "sha"));

        assertEquals(asList(new Checksum(fromMillis(0), 5L, "sha")), asChecksums(Set.of(fileAndChecksum)));
    }

    @Test
    public void boundingBoxRoundTripsThroughBinding() {
        BoundingBox roundTrip = asBoundingBox(asBoundingBoxType(new BoundingBox(10.0, 50.0, 8.0, 48.0)));

        assertNotNull(roundTrip);
        assertEquals(10.0, roundTrip.northEast().getLongitude(), 0.0);
        assertEquals(50.0, roundTrip.northEast().getLatitude(), 0.0);
        assertEquals(8.0, roundTrip.southWest().getLongitude(), 0.0);
        assertEquals(48.0, roundTrip.southWest().getLatitude(), 0.0);
    }

    @Test
    public void boundingBoxConversionsReturnNullOnMissingCorners() {
        assertNull(asBoundingBoxType(null));
        assertNull(asBoundingBox(null));
        assertNull(asBoundingBox(new BoundingBoxType()));   // neither north-east nor south-west set
    }

    @Test
    public void datasourceTypeCopiesTheIdentifyingFields() {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getId()).thenReturn("id");
        when(dataSource.getName()).thenReturn("name");
        when(dataSource.getBaseUrl()).thenReturn("http://base/");
        when(dataSource.getDirectory()).thenReturn("dir");
        when(dataSource.getAction()).thenReturn("Copy");

        DatasourceType datasourceType = asDatasourceType(dataSource);

        assertEquals("id", datasourceType.getId());
        assertEquals("name", datasourceType.getName());
        assertEquals("http://base/", datasourceType.getBaseUrl());
        assertEquals("dir", datasourceType.getDirectory());
        assertEquals(COPY, datasourceType.getAction());
    }

    @Test
    public void createFileTypeCarriesUriBoundingBoxAndChecksums() {
        FileType fileType = createFileType("file.zip", asList(new Checksum(fromMillis(0), 1L, "s")),
                new BoundingBox(10.0, 50.0, 8.0, 48.0));

        assertEquals("file.zip", fileType.getUri());
        assertNotNull(fileType.getBoundingBox());
        assertEquals(1, fileType.getChecksum().size());
    }

    @Test
    public void createFileTypeToleratesNullChecksumsAndBoundingBox() {
        FileType fileType = createFileType("file.zip", null, null);

        assertEquals("file.zip", fileType.getUri());
        assertNull(fileType.getBoundingBox());
        assertTrue(fileType.getChecksum().isEmpty());
    }

    @Test
    public void createMapTypeCarriesUriAndChecksums() {
        MapType mapType = createMapType("map.map", asList(new Checksum(fromMillis(0), 2L, "s")), null);

        assertEquals("map.map", mapType.getUri());
        assertEquals(1, mapType.getChecksum().size());
    }

    @Test
    public void createThemeTypeCarriesUriImageUrlAndChecksums() {
        ThemeType themeType = createThemeType("theme.zip", asList(new Checksum(fromMillis(0), 3L, "s")), "preview.png");

        assertEquals("theme.zip", themeType.getUri());
        assertEquals("preview.png", themeType.getImageUrl());
        assertEquals(1, themeType.getChecksum().size());
    }

    @Test
    public void createFragmentTypeFromKeyAndSizes() throws Exception {
        FragmentType fragmentType = createFragmentType("key", 1000L, 99L);

        assertEquals("key", fragmentType.getKey());
        assertEquals(1, fragmentType.getChecksum().size());
        assertEquals(Long.valueOf(99), fragmentType.getChecksum().get(0).getContentLength());
    }

    @Test
    public void createFragmentTypeFromZipEntryComputesTheChecksum() throws Exception {
        ZipEntry entry = new ZipEntry("fragment");
        entry.setTime(1000L);
        entry.setSize(3);

        FragmentType fragmentType = createFragmentType("key", entry,
                new ByteArrayInputStream(new byte[]{1, 2, 3}));

        ChecksumType checksumType = fragmentType.getChecksum().get(0);
        assertEquals(Long.valueOf(3), checksumType.getContentLength());
        assertNotNull(checksumType.getSha1());
    }

    @Test
    public void createFragmentTypeFromFragmentAndFileChecksums() {
        @SuppressWarnings("unchecked")
        Fragment<?> fragment = mock(Fragment.class);
        when(fragment.getKey()).thenReturn("key");
        FileAndChecksum fileAndChecksum = new FileAndChecksum(new File("ignored"), null);
        fileAndChecksum.setActualChecksum(new Checksum(fromMillis(0), 7L, "s"));

        FragmentType fragmentType = createFragmentType(fragment, Set.of(fileAndChecksum));

        assertEquals("key", fragmentType.getKey());
        assertEquals(1, fragmentType.getChecksum().size());
    }

    @Test
    public void metaDataComparablePathUsesForwardSlashes() throws Exception {
        File file = File.createTempFile("datasources", ".tmp");
        file.deleteOnExit();

        String path = asMetaDataComparablePath(file);

        assertFalse(path.contains("\\"));
        assertTrue(path.endsWith(file.getName()));
    }
}

