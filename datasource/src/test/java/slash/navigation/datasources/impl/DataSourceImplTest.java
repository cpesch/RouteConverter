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

package slash.navigation.datasources.impl;

import org.junit.Test;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.Fragment;
import slash.navigation.datasources.binding.*;
import slash.navigation.download.Checksum;

import static org.junit.Assert.*;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createChecksumType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createFragmentType;

/**
 * Unit tests for {@link DataSourceImpl}, {@link DownloadableImpl}, and {@link FragmentImpl}.
 *
 * @author Christian Pesch
 */
public class DataSourceImplTest {

    private static final ObjectFactory FACTORY = new ObjectFactory();

    // --- builder helpers ---

    private static DatasourceType datasourceType(String id, String baseUrl) {
        DatasourceType dt = FACTORY.createDatasourceType();
        dt.setId(id);
        dt.setName("Name of " + id);
        dt.setBaseUrl(baseUrl);
        dt.setDirectory("dir/" + id);
        dt.setAction(ActionType.COPY);
        return dt;
    }

    private static ChecksumType checksumType(long contentLength, String sha1) {
        return createChecksumType(fromMillis(1_000_000_000_000L), contentLength, sha1);
    }

    private static FileType fileType(String uri, String sha1) {
        FileType ft = FACTORY.createFileType();
        ft.setUri(uri);
        ft.getChecksum().add(checksumType(100L, sha1));
        return ft;
    }

    private static MapType mapType(String uri, String sha1) {
        MapType mt = FACTORY.createMapType();
        mt.setUri(uri);
        mt.getChecksum().add(checksumType(200L, sha1));
        return mt;
    }

    private static ThemeType themeType(String uri, String sha1) {
        ThemeType tt = FACTORY.createThemeType();
        tt.setUri(uri);
        tt.getChecksum().add(checksumType(300L, sha1));
        return tt;
    }

    private static FragmentType fragmentType(String key, String sha1) {
        FragmentType frag = FACTORY.createFragmentType();
        frag.setKey(key);
        frag.getChecksum().add(checksumType(50L, sha1));
        return frag;
    }

    // --- DataSourceImpl basic getters ---

    @Test
    public void getIdReturnsId() {
        DataSource ds = new DataSourceImpl(datasourceType("myId", "http://base/"));
        assertEquals("myId", ds.getId());
    }

    @Test
    public void getNameReturnsName() {
        DataSource ds = new DataSourceImpl(datasourceType("id1", "http://base/"));
        assertEquals("Name of id1", ds.getName());
    }

    @Test
    public void getBaseUrlReturnsBaseUrl() {
        DataSource ds = new DataSourceImpl(datasourceType("id1", "http://example.com/"));
        assertEquals("http://example.com/", ds.getBaseUrl());
    }

    @Test
    public void getDirectoryReturnsDirectory() {
        DataSource ds = new DataSourceImpl(datasourceType("id1", "http://base/"));
        assertEquals("dir/id1", ds.getDirectory());
    }

    @Test
    public void getActionReturnsCopy() {
        DataSource ds = new DataSourceImpl(datasourceType("id1", "http://base/"));
        assertEquals("Copy", ds.getAction());
    }

    @Test
    public void equalsAndHashCodeBasedOnId() {
        DataSource ds1 = new DataSourceImpl(datasourceType("sameId", "http://url1/"));
        DataSource ds2 = new DataSourceImpl(datasourceType("sameId", "http://url2/"));
        DataSource ds3 = new DataSourceImpl(datasourceType("otherId", "http://url1/"));

        assertEquals(ds1, ds2);
        assertEquals(ds1.hashCode(), ds2.hashCode());
        assertNotEquals(ds1, ds3);
    }

    @Test
    public void toStringContainsId() {
        DataSource ds = new DataSourceImpl(datasourceType("myId", "http://base/"));
        assertTrue(ds.toString().contains("myId"));
    }

    // --- getFiles / getMaps / getThemes ---

    @Test
    public void getFilesReturnsAllFiles() {
        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(fileType("a/b.map", "sha-a"));
        dt.getFile().add(fileType("c/d.map", "sha-c"));

        DataSource ds = new DataSourceImpl(dt);
        assertEquals(2, ds.getFiles().size());
        assertEquals("a/b.map", ds.getFiles().get(0).getUri());
    }

    @Test
    public void getMapsReturnsAllMaps() {
        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getMap().add(mapType("maps/m1.map", "sha-m1"));

        DataSource ds = new DataSourceImpl(dt);
        assertEquals(1, ds.getMaps().size());
        assertEquals("maps/m1.map", ds.getMaps().get(0).getUri());
    }

    @Test
    public void getThemesReturnsAllThemes() {
        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getTheme().add(themeType("themes/t1.xml", "sha-t1"));

        DataSource ds = new DataSourceImpl(dt);
        assertEquals(1, ds.getThemes().size());
        assertEquals("themes/t1.xml", ds.getThemes().get(0).getUri());
    }

    // --- getDownloadable ---

    @Test
    public void getDownloadableFindsFileByUri() {
        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(fileType("path/to/file.zip", "sha-file"));

        DataSource ds = new DataSourceImpl(dt);
        Downloadable found = ds.getDownloadable("path/to/file.zip");
        assertNotNull(found);
        assertEquals("path/to/file.zip", found.getUri());
    }

    @Test
    public void getDownloadableFindsMapByUri() {
        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getMap().add(mapType("path/to/map.map", "sha-map"));

        DataSource ds = new DataSourceImpl(dt);
        Downloadable found = ds.getDownloadable("path/to/map.map");
        assertNotNull(found);
        assertEquals("path/to/map.map", found.getUri());
    }

    @Test
    public void getDownloadableReturnsNullForUnknownUri() {
        DataSource ds = new DataSourceImpl(datasourceType("id1", "http://base/"));
        assertNull(ds.getDownloadable("no/such/file.zip"));
    }

    // --- getDownloadableBySHA1 ---

    @Test
    public void getDownloadableBySHA1FindsFile() {
        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(fileType("path/file.zip", "target-sha"));
        dt.getFile().add(fileType("other/file.zip", "other-sha"));

        DataSource ds = new DataSourceImpl(dt);
        Downloadable found = ds.getDownloadableBySHA1("target-sha");
        assertNotNull(found);
        assertEquals("path/file.zip", found.getUri());
    }

    @Test
    public void getDownloadableBySHA1ReturnsNullWhenNotFound() {
        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(fileType("path/file.zip", "sha1"));

        DataSource ds = new DataSourceImpl(dt);
        assertNull(ds.getDownloadableBySHA1("no-such-sha"));
    }

    // --- getFragment ---

    @Test
    public void getFragmentFindsFragmentByKey() {
        FileType ft = fileType("path/file.zip", "sha-file");
        ft.getFragment().add(fragmentType("frag-key-1", "sha-frag"));

        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(ft);

        DataSource ds = new DataSourceImpl(dt);
        Fragment<Downloadable> frag = ds.getFragment("frag-key-1");
        assertNotNull(frag);
        assertEquals("frag-key-1", frag.getKey());
    }

    @Test
    public void getFragmentReturnsNullForUnknownKey() {
        DataSource ds = new DataSourceImpl(datasourceType("id1", "http://base/"));
        assertNull(ds.getFragment("unknown-key"));
    }

    // --- getFragmentBySHA1 ---

    @Test
    public void getFragmentBySHA1FindsFragment() {
        FileType ft = fileType("path/file.zip", "sha-file");
        ft.getFragment().add(fragmentType("frag-key-1", "target-frag-sha"));

        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(ft);

        DataSource ds = new DataSourceImpl(dt);
        Fragment<Downloadable> frag = ds.getFragmentBySHA1("target-frag-sha");
        assertNotNull(frag);
        assertEquals("frag-key-1", frag.getKey());
    }

    @Test
    public void getFragmentBySHA1ReturnsNullWhenNotFound() {
        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(fileType("path/file.zip", "sha-file"));

        DataSource ds = new DataSourceImpl(dt);
        assertNull(ds.getFragmentBySHA1("no-such-frag-sha"));
    }

    // --- DownloadableImpl checksums and fragments ---

    @Test
    public void downloadableGetChecksumsReturnsAllChecksums() {
        FileType ft = FACTORY.createFileType();
        ft.setUri("file.zip");
        ft.getChecksum().add(checksumType(100L, "sha-1"));
        ft.getChecksum().add(checksumType(200L, "sha-2"));

        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(ft);

        DataSource ds = new DataSourceImpl(dt);
        Downloadable d = ds.getDownloadable("file.zip");
        assertNotNull(d);
        assertEquals(2, d.getChecksums().size());
    }

    @Test
    public void downloadableGetLatestChecksumReturnsLatest() {
        FileType ft = FACTORY.createFileType();
        ft.setUri("file.zip");
        ft.getChecksum().add(createChecksumType(fromMillis(1_000_000_000_000L), 100L, "sha-old"));
        ft.getChecksum().add(createChecksumType(fromMillis(1_100_000_000_000L), 200L, "sha-new"));

        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(ft);

        DataSource ds = new DataSourceImpl(dt);
        Downloadable d = ds.getDownloadable("file.zip");
        assertNotNull(d);
        Checksum latest = d.getLatestChecksum();
        assertNotNull(latest);
        assertEquals("sha-new", latest.getSHA1());
    }

    @Test
    public void downloadableGetFragmentsReturnsAll() {
        FileType ft = fileType("file.zip", "sha-file");
        ft.getFragment().add(fragmentType("frag1", "sha-frag1"));
        ft.getFragment().add(fragmentType("frag2", "sha-frag2"));

        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(ft);

        DataSource ds = new DataSourceImpl(dt);
        Downloadable d = ds.getDownloadable("file.zip");
        assertNotNull(d);
        assertEquals(2, d.getFragments().size());
    }

    // --- FragmentImpl ---

    @Test
    public void fragmentGetLatestChecksumReturnsLatest() {
        FileType ft = fileType("file.zip", "sha-file");
        FragmentType frag = FACTORY.createFragmentType();
        frag.setKey("frag-key");
        frag.getChecksum().add(createChecksumType(fromMillis(1_000_000_000_000L), 50L, "sha-frag-old"));
        frag.getChecksum().add(createChecksumType(fromMillis(1_100_000_000_000L), 60L, "sha-frag-new"));
        ft.getFragment().add(frag);

        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(ft);

        DataSource ds = new DataSourceImpl(dt);
        Fragment<Downloadable> found = ds.getFragment("frag-key");
        assertNotNull(found);
        Checksum latest = found.getLatestChecksum();
        assertNotNull(latest);
        assertEquals("sha-frag-new", latest.getSHA1());
    }

    @Test
    public void fragmentGetDownloadableReturnsParent() {
        FileType ft = fileType("file.zip", "sha-file");
        ft.getFragment().add(fragmentType("frag-key", "sha-frag"));

        DatasourceType dt = datasourceType("id1", "http://base/");
        dt.getFile().add(ft);

        DataSource ds = new DataSourceImpl(dt);
        Fragment<Downloadable> found = ds.getFragment("frag-key");
        assertNotNull(found);
        assertNotNull(found.getDownloadable());
        assertEquals("file.zip", found.getDownloadable().getUri());
    }
}

