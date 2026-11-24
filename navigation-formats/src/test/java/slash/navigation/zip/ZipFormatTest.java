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


package slash.navigation.zip;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserResult;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.kml.Kml22Format;
import slash.navigation.loc.GeoCachingFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZipFormatTest {
    private static final String GPX10 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<gpx version=\"1.0\" creator=\"test\" xmlns=\"http://www.topografix.com/GPX/1/0\">\n" +
            "<wpt lat=\"50.1\" lon=\"10.2\"><name>A</name></wpt>\n" +
            "<wpt lat=\"50.2\" lon=\"10.3\"><name>B</name></wpt>\n" +
            "</gpx>\n";
    private static final String KML22 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><name>D</name>\n" +
            "<Placemark><name>C</name><Point><coordinates>11.1,51.1,0</coordinates></Point></Placemark>\n" +
            "<Placemark><name>E</name><Point><coordinates>11.2,51.2,0</coordinates></Point></Placemark>\n" +
            "</Document></kml>\n";
    private static final String LOC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<loc version=\"1.0\" src=\"EasyGPS\">\n" +
            "<waypoint><name id=\"GC1\"><![CDATA[First]]></name><coord lat=\"52.1\" lon=\"12.1\"/></waypoint>\n" +
            "<waypoint><name id=\"GC2\"><![CDATA[Second]]></name><coord lat=\"52.2\" lon=\"12.2\"/></waypoint>\n" +
            "</loc>\n";
    // no XML declaration and no BOM, like the garbled KML that GarbleKml22Format exists for:
    // read as raw windows-1252 bytes, the umlauts are not valid UTF-8, so Kml22Format declines
    // and GarbleKml22Format reads it instead, extending Kml22Format
    private static final String GARBLE_KML = "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><Placemark>" +
            "<name>Grün Ö Ä ß Straße</name>" +
            "<Point><coordinates>12.3,51.3,0</coordinates></Point>" +
            "</Placemark></Document></kml>";

    private final NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());
    private final List<File> files = new ArrayList<>();

    @AfterEach
    public void tearDown() {
        for (File file : files)
            assertTrue(file.delete());
    }

    private File zip(String... namesAndContents) throws IOException {
        File file = File.createTempFile("zipformattest", ".zip");
        files.add(file);
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            for (int i = 0; i < namesAndContents.length; i += 2) {
                zip.putNextEntry(new ZipEntry(namesAndContents[i]));
                zip.write(namesAndContents[i + 1].getBytes(UTF_8));
                zip.closeEntry();
            }
        }
        return file;
    }

    private File zipBytes(String name1, byte[] content1, String name2, byte[] content2) throws IOException {
        File file = File.createTempFile("zipformattest", ".zip");
        files.add(file);
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            zip.putNextEntry(new ZipEntry(name1));
            zip.write(content1);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(name2));
            zip.write(content2);
            zip.closeEntry();
        }
        return file;
    }

    @Test
    public void reportsTheFormatOfASingleEntry() throws IOException {
        ParserResult result = parser.read(zip("a.gpx", GPX10));
        assertTrue(result.isSuccessful());
        assertEquals(Gpx10Format.class, result.getFormat().getClass());
        assertEquals(2, result.getTheRoute().getPositionCount());
    }

    @Test
    public void reportsTheFormatOfEntriesReadByTheSameFormat() throws IOException {
        ParserResult result = parser.read(zip("a.kml", KML22, "b.kml", KML22));
        assertTrue(result.isSuccessful());
        assertEquals(Kml22Format.class, result.getFormat().getClass());
        assertEquals(2, result.getAllRoutes().size());
    }

    @Test
    public void reportsAGpxBackedEntryFormat() throws IOException {
        ParserResult result = parser.read(zip("a.loc", LOC));
        assertTrue(result.isSuccessful());
        assertEquals(GeoCachingFormat.class, result.getFormat().getClass());
        assertEquals(2, result.getTheRoute().getPositionCount());
    }

    @Test
    public void keepsGpx11ForMixedEntries() throws IOException {
        ParserResult result = parser.read(zip("a.gpx", GPX10, "b.kml", KML22));
        assertTrue(result.isSuccessful());
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
        assertEquals(2, result.getAllRoutes().size());
    }

    @Test
    public void reportsTheBaseFormatForAGarbledAndARegularEntryOfTheSameFormat() throws IOException {
        byte[] garbled = GARBLE_KML.getBytes(Charset.forName("windows-1252"));
        ParserResult result = parser.read(zipBytes("a.kml", garbled, "b.kml", KML22.getBytes(UTF_8)));
        assertTrue(result.isSuccessful());
        assertEquals(Kml22Format.class, result.getFormat().getClass());
        assertEquals(2, result.getAllRoutes().size());
    }

    @Test
    public void keepsTheRoutesOfEarlierEntries() throws IOException {
        ParserResult result = parser.read(zip("a.kml", GARBLE_KML, "b.kml", KML22));
        assertTrue(result.isSuccessful());
        assertEquals(Kml22Format.class, result.getFormat().getClass());
        assertEquals(2, result.getAllRoutes().size());
        // one route per entry: the earlier entry's single position is kept, not replaced by the later entry
        assertEquals(asList(1, 2), result.getAllRoutes().stream().map(BaseRoute::getPositionCount).sorted().collect(toList()));
    }

    private static String largeGpx(int waypoints) {
        StringBuilder buffer = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.0\" creator=\"test\" xmlns=\"http://www.topografix.com/GPX/1/0\">\n");
        for (int i = 0; i < waypoints; i++)
            buffer.append("<wpt lat=\"50.").append(1000 + i).append("\" lon=\"10.").append(1000 + i)
                    .append("\"><name>Waypoint ").append(i).append("</name></wpt>\n");
        return buffer.append("</gpx>\n").toString();
    }

    @Test
    public void readsStreamedEntriesLargerThanTheReadBuffer() throws IOException {
        // ZipOutputStream writes data descriptors, so ZipInputStream knows no entry size up front,
        // like Google Takeout archives; the first entry is far larger than BufferedInputStream's 8 KB
        String large = largeGpx(1000);
        assertTrue(large.length() > 4 * 8192);
        ParserResult result = parser.read(zip("Takeout/a.gpx", large, "Takeout/b.kml", KML22, "Takeout/c.gpx", GPX10));
        assertTrue(result.isSuccessful());
        assertEquals(asList(2, 2, 1000), result.getAllRoutes().stream().map(BaseRoute::getPositionCount).sorted().collect(toList()));
    }

    private File corruptSecondEntry(File file) throws IOException {
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        int count = 0;
        for (int i = 0; i < bytes.length - 4; i++) {
            if (bytes[i] == 'P' && bytes[i + 1] == 'K' && bytes[i + 2] == 3 && bytes[i + 3] == 4 && ++count == 2) {
                // garble the deflated data behind the second local file header (30 bytes + name)
                for (int j = i + 60; j < i + 120; j++)
                    bytes[j] = (byte) 0xff;
                java.nio.file.Files.write(file.toPath(), bytes);
                return file;
            }
        }
        throw new IllegalStateException("no second entry in " + file);
    }

    @Test
    public void readsLargeStreamedEntriesOfAnArchiveInAnArchive() throws IOException {
        // a nested archive is read as a stream, where ZipInputStream knows no entry size up front
        File inner = zip("a.gpx", largeGpx(1000), "b.kml", KML22);
        byte[] innerBytes = java.nio.file.Files.readAllBytes(inner.toPath());
        File outer = File.createTempFile("zipformattest", ".zip");
        files.add(outer);
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(outer))) {
            zip.putNextEntry(new ZipEntry("inner.zip"));
            zip.write(innerBytes);
            zip.closeEntry();
        }
        ParserResult result = parser.read(outer);
        assertTrue(result.isSuccessful());
        assertEquals(asList(2, 1000), result.getAllRoutes().stream().map(BaseRoute::getPositionCount).sorted().collect(toList()));
    }

    private File withoutCentralDirectory(File file) throws IOException {
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        for (int i = 0; i < bytes.length - 4; i++) {
            if (bytes[i] == 'P' && bytes[i + 1] == 'K' && bytes[i + 2] == 1 && bytes[i + 3] == 2) {
                // cut at the first central directory header, like an interrupted download
                java.nio.file.Files.write(file.toPath(), java.util.Arrays.copyOf(bytes, i));
                return file;
            }
        }
        throw new IllegalStateException("no central directory in " + file);
    }

    @Test
    public void readsTheEntriesOfAnArchiveWithoutCentralDirectory() throws IOException {
        ParserResult result = parser.read(withoutCentralDirectory(zip("a.gpx", GPX10, "b.gpx", largeGpx(200), "c.kml", KML22)));
        assertTrue(result.isSuccessful());
        assertEquals(asList(2, 2, 200), result.getAllRoutes().stream().map(BaseRoute::getPositionCount).sorted().collect(toList()));
    }

    @Test
    public void skipsEntriesLargerThanTheCap() throws IOException {
        int before = ZipFormat.maximumEntrySize;
        ZipFormat.maximumEntrySize = 4096;
        try {
            String large = largeGpx(200);
            assertTrue(large.length() > 4096);
            ParserResult result = parser.read(zip("a.gpx", large, "b.kml", KML22));
            assertTrue(result.isSuccessful());
            assertEquals(asList(2), result.getAllRoutes().stream().map(BaseRoute::getPositionCount).sorted().collect(toList()));
        } finally {
            ZipFormat.maximumEntrySize = before;
        }
    }

    @Test
    public void aCorruptEntryEndsReadingANestedArchive() throws IOException {
        // documents a limit: a stream cannot skip a damaged deflated entry, so the entries after it are lost
        File inner = corruptSecondEntry(zip("a.gpx", GPX10, "b.gpx", largeGpx(200), "c.kml", KML22));
        byte[] innerBytes = java.nio.file.Files.readAllBytes(inner.toPath());
        File outer = File.createTempFile("zipformattest", ".zip");
        files.add(outer);
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(outer))) {
            zip.putNextEntry(new ZipEntry("inner.zip"));
            zip.write(innerBytes);
            zip.closeEntry();
        }
        ParserResult result = parser.read(outer);
        assertTrue(result.isSuccessful());
        assertEquals(asList(2), result.getAllRoutes().stream().map(BaseRoute::getPositionCount).sorted().collect(toList()));
    }

    @Test
    public void readsTheEntriesAroundACorruptEntry() throws IOException {
        ParserResult result = parser.read(corruptSecondEntry(zip("a.gpx", GPX10, "b.gpx", largeGpx(200), "c.kml", KML22)));
        assertTrue(result.isSuccessful());
        assertEquals(asList(2, 2), result.getAllRoutes().stream().map(BaseRoute::getPositionCount).sorted().collect(toList()));
    }

    @Test
    public void doesNotThrowForATopLevelFileWithoutAnEndOfCentralDirectoryRecord() throws IOException {
        // an incomplete or oddly-produced download: ZipFile's constructor throws ZipException,
        // which must not propagate out of read() and fail format auto-detection; probe with
        // ZipFormat alone, not the full registry, since several garble-tolerant text formats
        // (e.g. NmnFormat subclasses) also read a single bogus line without throwing and would
        // otherwise claim this content themselves, which is a pre-existing, unrelated quirk of
        // the registry-wide fallback and not what this test is about
        File file = File.createTempFile("zipformattest", ".zip");
        files.add(file);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("not a zip archive".getBytes(UTF_8));
        }
        ParserResult result = parser.read(file, List.of(new ZipFormat()));
        assertFalse(result.isSuccessful());
    }

    @Test
    public void reportsTheEntryFormatOfAnArchiveInAnArchive() throws IOException {
        File inner = zip("a.gpx", GPX10);
        byte[] innerBytes = java.nio.file.Files.readAllBytes(inner.toPath());
        File outer = File.createTempFile("zipformattest", ".zip");
        files.add(outer);
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(outer))) {
            zip.putNextEntry(new ZipEntry("inner.zip"));
            zip.write(innerBytes);
            zip.closeEntry();
        }
        ParserResult result = parser.read(outer);
        assertTrue(result.isSuccessful());
        assertEquals(Gpx10Format.class, result.getFormat().getClass());
    }
}
