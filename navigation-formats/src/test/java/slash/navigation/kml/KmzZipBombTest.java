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

    Copyright (C) 2026 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.kml;

import org.junit.After;
import org.junit.Test;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.ParserResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static slash.navigation.kml.KmzFormat.MAX_UNCOMPRESSED_BYTES_PROPERTY;

/**
 * A KMZ is a zip; the reader must cap what it inflates (zip bomb) and what it iterates.
 */
public class KmzZipBombTest {
    private static final String KML_START = "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><Placemark><name>p</name>" +
            "<Point><coordinates>2,1,0</coordinates></Point></Placemark>";
    private static final String KML_END = "</Document></kml>";

    @After
    public void clearProperty() {
        System.clearProperty(MAX_UNCOMPRESSED_BYTES_PROPERTY);
    }

    private static byte[] kmz(String entryName, String padding) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write((KML_START + padding + KML_END).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return baos.toByteArray();
    }

    private static String padding(int megabytes) {
        char[] chars = new char[megabytes * 1024 * 1024];
        Arrays.fill(chars, ' ');
        return new String(chars);
    }

    @Test
    public void normalKmzIsRead() throws IOException {
        File file = File.createTempFile("normal", ".kmz");
        try {
            Files.write(file.toPath(), kmz("doc.kml", ""));
            NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());
            ParserResult result = parser.read(file, singletonList(new Kmz22Format()));
            assertTrue(result.isSuccessful());
            assertEquals(1, result.getAllRoutes().size());
        } finally {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    @Test
    public void inflatedSizeAboveTheCapIsRefused() throws IOException {
        // 8 MB of spaces deflates to a few KB
        byte[] bomb = kmz("doc.kml", padding(8));
        assertTrue("fixture is not compressed enough: " + bomb.length, bomb.length < 100_000);
        System.setProperty(MAX_UNCOMPRESSED_BYTES_PROPERTY, String.valueOf(1024 * 1024));
        try {
            new Kmz22Format().read(new ByteArrayInputStream(bomb), new ParserContextImpl<>());
            fail("expected the zip bomb to be refused");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("refusing"));
        }
    }

    @Test
    public void unreadEntryIsMeteredToo() throws IOException {
        // the delegate never reads this entry's tail (a non-KML entry fails fast), closeEntry() must not skip it for free
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            zip.putNextEntry(new ZipEntry("junk.bin"));
            byte[] chunk = new byte[1024 * 1024];
            for (int i = 0; i < 8; i++)
                zip.write(chunk);
            zip.closeEntry();
        }
        System.setProperty(MAX_UNCOMPRESSED_BYTES_PROPERTY, String.valueOf(1024 * 1024));
        try {
            new Kmz22Format().read(new ByteArrayInputStream(baos.toByteArray()), new ParserContextImpl<>());
            fail("expected the zip bomb to be refused");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("refusing"));
        }
    }

    @Test
    public void tooManyEntriesAreRefused() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            for (int i = 0; i <= KmzFormat.MAX_ENTRIES; i++) {
                zip.putNextEntry(new ZipEntry("f" + i + ".png"));
                zip.closeEntry();
            }
        }
        try {
            new Kmz22Format().read(new ByteArrayInputStream(baos.toByteArray()), new ParserContextImpl<>());
            fail("expected too many entries to be refused");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("entries"));
        }
    }
}
