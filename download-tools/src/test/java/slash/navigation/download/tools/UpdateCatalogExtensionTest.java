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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import slash.navigation.datasources.binding.FragmentType;
import slash.navigation.datasources.binding.ThemeType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.io.File.createTempFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.navigation.datasources.DataSourceManager.DOT_MAP;
import static slash.navigation.datasources.DataSourceManager.DOT_ZIP;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createThemeType;
import static slash.navigation.download.tools.UpdateCatalog.describeTheme;
import static slash.navigation.download.tools.UpdateCatalog.hasExtension;
import static slash.navigation.graphhopper.PbfUtil.DOT_PBF;

/**
 * Tests that {@link UpdateCatalog} recognizes file extensions of upstream URIs regardless of
 * their case, e.g. a theme released as {@code TOTM-2022-03-18.ZIP}.
 *
 * @author Christian Pesch
 */
public class UpdateCatalogExtensionTest {
    private static final long TIME = 1_700_000_000_000L;

    private File zip;

    @BeforeEach
    public void setUp() throws IOException {
        zip = createTempFile("theme", ".zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(entry("TOTM/"));
            out.closeEntry();
            out.putNextEntry(entry("TOTM/totm.xml"));
            out.write("<rendertheme/>".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(entry("TOTM/patterns/forest.svg"));
            out.write("<svg/>".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

    @AfterEach
    public void tearDown() {
        if (zip.exists())
            zip.delete();
    }

    private static ZipEntry entry(String name) {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(TIME);
        return entry;
    }

    private List<FragmentType> describe(String uri) {
        ThemeType themeType = createThemeType(uri, null, null);
        describeTheme(themeType, uri, zip);
        return themeType.getFragment();
    }

    private static void assertThemeFragments(List<FragmentType> fragments) {
        assertEquals(2, fragments.size());
        assertEquals("TOTM/totm.xml", fragments.get(0).getKey());
        assertEquals("TOTM/patterns/forest.svg", fragments.get(1).getKey());
        for (FragmentType fragment : fragments)
            assertNotNull(fragment.getChecksum().get(0).getSha1(), "theme files are hashed");
    }

    @Test
    public void lowerCaseZipThemeGetsFragments() {
        assertThemeFragments(describe("TOTM-2022-03-18.zip"));
    }

    @Test
    public void upperCaseZipThemeGetsFragments() {
        assertThemeFragments(describe("TOTM-2022-03-18.ZIP"));
    }

    @Test
    public void mixedCaseZipThemeGetsFragments() {
        assertThemeFragments(describe("releases/download/v1/Theme.Zip"));
    }

    @Test
    public void nonZipThemeGetsNoFragments() {
        assertTrue(describe("theme.xml").isEmpty());
    }

    @Test
    public void hasExtensionIgnoresCase() {
        assertTrue(hasExtension("a/b.zip", DOT_ZIP));
        assertTrue(hasExtension("a/b.ZIP", DOT_ZIP));
        assertTrue(hasExtension("germany.MAP", DOT_MAP));
        assertTrue(hasExtension("europe-latest.osm.PBF", DOT_PBF));
    }

    @Test
    public void hasExtensionRejectsOtherExtensions() {
        assertFalse(hasExtension("a/b.zip.md5", DOT_ZIP));
        assertFalse(hasExtension("a/bzip", DOT_ZIP));
        assertFalse(hasExtension("a/b.rd5", DOT_ZIP));
        assertFalse(hasExtension(null, DOT_ZIP));
    }
}
