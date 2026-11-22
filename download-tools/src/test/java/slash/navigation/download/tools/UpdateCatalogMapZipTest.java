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

import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.datasources.binding.BoundingBoxType;
import slash.navigation.datasources.binding.FragmentType;
import slash.navigation.datasources.binding.MapType;
import slash.navigation.download.tools.helpers.RangeReader;
import slash.navigation.download.tools.helpers.RemoteMapReader;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.lang.Math.min;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createFragmentType;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createMapType;
import static slash.navigation.download.tools.UpdateCatalog.describeMapZip;
import static slash.navigation.download.tools.UpdateCatalog.extractRemoteMapBoundingBox;
import static slash.navigation.maps.mapsforge.helpers.MapUtil.extractMapBoundingBox;

/**
 * Tests that {@link UpdateCatalog#describeMapZip} finds the {@code .map} entries of a map zip by its central
 * directory wherever they are, and that it and {@link UpdateCatalog#extractRemoteMapBoundingBox} read
 * mapsforge headers larger than the first range of 16 KB.
 *
 * @author Christian Pesch
 */
public class UpdateCatalogMapZipTest {
    private static final int FIRST_RANGE = 16 * 1024;
    // odd seconds: a DOS time cannot hold them, only the extended timestamp
    private static final long EXTENDED_TIME = 1_700_000_001_000L;
    private static final long DOS_TIME = 1_700_000_002_000L;
    private static final double MIN_LATITUDE = 42.123456, MIN_LONGITUDE = -9.654321,
            MAX_LATITUDE = 43.5, MAX_LONGITUDE = -7.25;
    private static final int MAP_DATA_SIZE = 4096;

    /** A {@link RangeReader} on a byte array that records what was read. */
    private static class BytesRangeReader implements RangeReader {
        private final byte[] bytes;
        private long bytesRead;
        private int requests;

        BytesRangeReader(byte[] bytes) {
            this.bytes = bytes;
        }

        public byte[] read(long offset, int length) throws IOException {
            if (offset < 0 || offset >= bytes.length)
                throw new IOException("Range not satisfiable: " + offset);
            byte[] result = Arrays.copyOfRange(bytes, (int) offset, (int) min(bytes.length, offset + length));
            bytesRead += result.length;
            requests++;
            return result;
        }
    }

    private static void writeVbeString(DataOutputStream out, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        int length = bytes.length;
        while (length > 0x7F) {
            out.writeByte((length & 0x7F) | 0x80);
            length >>>= 7;
        }
        out.writeByte(length);
        out.write(bytes);
    }

    /**
     * @param tagCount the number of POI tags, which makes the header larger
     * @return a mapsforge map with a valid header followed by zeros
     */
    static byte[] mapsforgeMap(int tagCount) throws IOException {
        Random random = new Random(tagCount);
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        DataOutputStream header = new DataOutputStream(headerBytes);
        header.writeInt(3); // file version
        header.writeLong(0); // file size, patched below
        header.writeLong(1_600_000_000_000L); // map date
        header.writeInt((int) Math.round(MIN_LATITUDE * 1e6));
        header.writeInt((int) Math.round(MIN_LONGITUDE * 1e6));
        header.writeInt((int) Math.round(MAX_LATITUDE * 1e6));
        header.writeInt((int) Math.round(MAX_LONGITUDE * 1e6));
        header.writeShort(256); // tile size
        writeVbeString(header, "Mercator");
        header.writeByte(0); // no optional fields
        header.writeShort(tagCount); // POI tags
        for (int i = 0; i < tagCount; i++)
            // random hex makes the header hard to compress
            writeVbeString(header, "tag" + i + "=" + Long.toHexString(random.nextLong()) + Long.toHexString(random.nextLong()));
        header.writeShort(0); // way tags
        header.writeByte(1); // zoom intervals
        header.writeByte(14); // base zoom level
        header.writeByte(0); // minimal zoom level
        header.writeByte(21); // maximal zoom level
        int headerSize = headerBytes.size() + 16;
        long startAddress = 24 + headerSize;
        header.writeLong(startAddress);
        header.writeLong(MAP_DATA_SIZE);
        header.flush();

        byte[] fields = headerBytes.toByteArray();
        long fileSize = startAddress + MAP_DATA_SIZE;
        for (int i = 0; i < 8; i++)
            fields[4 + i] = (byte) (fileSize >>> (56 - 8 * i));

        ByteArrayOutputStream map = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(map);
        out.write("mapsforge binary OSM".getBytes(StandardCharsets.US_ASCII));
        out.writeInt(headerSize);
        out.write(fields);
        out.write(new byte[MAP_DATA_SIZE]);
        out.flush();
        assertEquals(fileSize, map.size());
        return map.toByteArray();
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new Random(size).nextBytes(bytes);
        return bytes;
    }

    private record Entry(String name, byte[] data, int method, boolean extendedTimestamp) {
    }

    private static byte[] zipOf(String comment, Entry... entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (Entry entry : entries) {
                ZipEntry zipEntry = new ZipEntry(entry.name());
                if (entry.extendedTimestamp())
                    zipEntry.setLastModifiedTime(FileTime.fromMillis(EXTENDED_TIME));
                else
                    zipEntry.setTime(DOS_TIME);
                zipEntry.setMethod(entry.method());
                if (entry.method() == ZipEntry.STORED) {
                    CRC32 crc = new CRC32();
                    crc.update(entry.data());
                    zipEntry.setSize(entry.data().length);
                    zipEntry.setCompressedSize(entry.data().length);
                    zipEntry.setCrc(crc.getValue());
                }
                out.putNextEntry(zipEntry);
                out.write(entry.data());
                out.closeEntry();
            }
            if (comment != null)
                out.setComment(comment);
        }
        return bytes.toByteArray();
    }

    private static Entry readme(int index) {
        return new Entry("README-" + index + ".txt", ("Read me " + index + "\n").repeat(200).getBytes(StandardCharsets.UTF_8),
                ZipEntry.DEFLATED, false);
    }

    /** What the first range of 16 KB finds when the zip is read as a stream from its start. */
    private static List<String> mapsInFirstRange(byte[] zip) {
        List<String> result = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new java.io.ByteArrayInputStream(Arrays.copyOf(zip, FIRST_RANGE)))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null)
                if (entry.getName().endsWith(".map"))
                    result.add(entry.getName());
        } catch (IOException e) {
            // the range ends within an entry
        }
        return result;
    }

    private static MapType mapType() {
        return createMapType("Galicia.zip", null, null);
    }

    private static void assertBoundingBox(BoundingBoxType boundingBox) {
        assertNotNull(boundingBox);
        assertEquals(MAX_LATITUDE, boundingBox.getNorthEast().getLatitude(), 1e-6);
        assertEquals(MAX_LONGITUDE, boundingBox.getNorthEast().getLongitude(), 1e-6);
        assertEquals(MIN_LATITUDE, boundingBox.getSouthWest().getLatitude(), 1e-6);
        assertEquals(MIN_LONGITUDE, boundingBox.getSouthWest().getLongitude(), 1e-6);
    }

    private static void assertBoundingBox(BoundingBox boundingBox) {
        assertNotNull(boundingBox);
        assertEquals(MAX_LATITUDE, boundingBox.northEast().getLatitude(), 1e-6);
        assertEquals(MAX_LONGITUDE, boundingBox.northEast().getLongitude(), 1e-6);
        assertEquals(MIN_LATITUDE, boundingBox.southWest().getLatitude(), 1e-6);
        assertEquals(MIN_LONGITUDE, boundingBox.southWest().getLongitude(), 1e-6);
    }

    private static void assertFragment(FragmentType fragment, String key, long lastModified, long contentLength) {
        assertEquals(key, fragment.getKey());
        assertEquals(1, fragment.getChecksum().size());
        assertEquals(Long.valueOf(contentLength), fragment.getChecksum().get(0).getContentLength());
        assertEquals(lastModified, fragment.getChecksum().get(0).getLastModified().toGregorianCalendar().getTimeInMillis());
        assertNull(fragment.getChecksum().get(0).getSha1());
    }

    @Test
    public void syntheticMapHeaderIsValid() throws IOException {
        byte[] map = mapsforgeMap(10);
        assertBoundingBox(extractMapBoundingBox(new java.io.ByteArrayInputStream(map), map.length));
    }

    @Test
    public void mapLastInZip() throws IOException {
        // the layout of alternativaslibres.org: nested theme zip, readme files, then the map
        byte[] map = mapsforgeMap(10);
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry("alternativaslibres.zip", randomBytes(100 * 1024), ZipEntry.STORED, true));
        for (int i = 0; i < 8; i++)
            entries.add(readme(i));
        entries.add(new Entry("Galicia.osm.map", map, ZipEntry.DEFLATED, true));
        byte[] zip = zipOf(null, entries.toArray(new Entry[0]));
        assertTrue(mapsInFirstRange(zip).isEmpty());

        MapType mapType = mapType();
        BytesRangeReader reader = new BytesRangeReader(zip);
        describeMapZip(mapType, "Galicia.zip", reader, (long) zip.length);

        assertEquals(1, mapType.getFragment().size());
        assertFragment(mapType.getFragment().get(0), "Galicia.osm.map", EXTENDED_TIME, map.length);
        assertBoundingBox(mapType.getBoundingBox());
        assertTrue("must not download the nested zip", reader.bytesRead < zip.length);
    }

    @Test
    public void largeHeaderInZipReplacesFragmentsOfFirstRange() throws IOException {
        // the layout of paws.cz: the map is the first entry, but its header exceeds the first range
        byte[] map = mapsforgeMap(600);
        int headerBytes = map.length - MAP_DATA_SIZE;
        assertTrue(headerBytes > FIRST_RANGE);
        byte[] zip = zipOf(null, new Entry("Czechia.map", map, ZipEntry.DEFLATED, false));
        assertEquals(singletonList("Czechia.map"), mapsInFirstRange(zip));

        MapType mapType = mapType();
        mapType.getFragment().add(createFragmentType("Czechia.map", DOS_TIME, null));
        describeMapZip(mapType, "Czechia.zip", new BytesRangeReader(zip), (long) zip.length);

        assertEquals(1, mapType.getFragment().size());
        assertFragment(mapType.getFragment().get(0), "Czechia.map", DOS_TIME, map.length);
        assertBoundingBox(mapType.getBoundingBox());
    }

    @Test
    public void headerLargerThanFirstChunkIsReadInLargerChunks() throws IOException {
        byte[] map = mapsforgeMap(5000);
        assertTrue(map.length - MAP_DATA_SIZE > 2 * 64 * 1024);
        byte[] zip = zipOf(null, readme(0), new Entry("Large.map", map, ZipEntry.STORED, true));

        MapType mapType = mapType();
        describeMapZip(mapType, "Large.zip", new BytesRangeReader(zip), (long) zip.length);

        assertEquals(1, mapType.getFragment().size());
        assertFragment(mapType.getFragment().get(0), "Large.map", EXTENDED_TIME, map.length);
        assertBoundingBox(mapType.getBoundingBox());
    }

    @Test
    public void severalMapsAndAComment() throws IOException {
        byte[] map1 = mapsforgeMap(10);
        byte[] map2 = mapsforgeMap(20);
        byte[] zip = zipOf("PK\u0005\u0006 looks like an end of central directory",
                new Entry("a/One.map", map1, ZipEntry.DEFLATED, true), readme(1),
                new Entry("b/Two.MAP", map2, ZipEntry.STORED, false), new Entry("c/", new byte[0], ZipEntry.STORED, false));

        MapType mapType = mapType();
        describeMapZip(mapType, "Two.zip", new BytesRangeReader(zip), (long) zip.length);

        assertEquals(2, mapType.getFragment().size());
        assertFragment(mapType.getFragment().get(0), "a/One.map", EXTENDED_TIME, map1.length);
        assertFragment(mapType.getFragment().get(1), "b/Two.MAP", DOS_TIME, map2.length);
        assertBoundingBox(mapType.getBoundingBox());
    }

    @Test
    public void centralDirectoryLargerThanTail() throws IOException {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 1500; i++)
            entries.add(new Entry(String.format("a-rather-long-directory-name/readme-%04d.txt", i),
                    new byte[]{(byte) i}, ZipEntry.STORED, true));
        byte[] map = mapsforgeMap(10);
        entries.add(new Entry("Last.map", map, ZipEntry.DEFLATED, true));
        byte[] zip = zipOf(null, entries.toArray(new Entry[0]));

        List<RemoteMapReader.MapEntry> mapEntries = RemoteMapReader.readMapEntries(new BytesRangeReader(zip), zip.length);

        assertEquals(1, mapEntries.size());
        assertEquals("Last.map", mapEntries.get(0).name());
        assertEquals(map.length, mapEntries.get(0).size());
    }

    @Test
    public void zipWithoutMapKeepsFragmentsOfFirstRange() throws IOException {
        byte[] zip = zipOf(null, readme(0), readme(1));
        MapType mapType = mapType();
        mapType.getFragment().add(createFragmentType("Kept.map", DOS_TIME, 1L));

        describeMapZip(mapType, "Readme.zip", new BytesRangeReader(zip), (long) zip.length);

        assertEquals(1, mapType.getFragment().size());
        assertEquals("Kept.map", mapType.getFragment().get(0).getKey());
        assertNull(mapType.getBoundingBox());
    }

    @Test
    public void unreadableZipKeepsFragmentsOfFirstRange() throws IOException {
        MapType mapType = mapType();
        mapType.getFragment().add(createFragmentType("Kept.map", DOS_TIME, 1L));

        describeMapZip(mapType, "Broken.zip", new BytesRangeReader(randomBytes(100_000)), 100_000L);
        describeMapZip(mapType, "NoRange.zip", (offset, length) -> {
            throw new IOException("no range support");
        }, 100_000L);
        describeMapZip(mapType, "NoSize.zip", (offset, length) -> {
            fail("must not read without a size");
            return null;
        }, null);

        assertEquals(1, mapType.getFragment().size());
        assertEquals("Kept.map", mapType.getFragment().get(0).getKey());
    }

    @Test
    public void bareMapWithLargeHeader() throws IOException {
        byte[] map = mapsforgeMap(600);
        int headerBytes = map.length - MAP_DATA_SIZE;
        assertTrue(headerBytes > FIRST_RANGE);
        // what the first range of 16 KB leaves: too short for the header
        assertNull(extractMapBoundingBox(new java.io.ByteArrayInputStream(Arrays.copyOf(map, FIRST_RANGE)), map.length));

        assertBoundingBox(extractRemoteMapBoundingBox("Czechia.map", new BytesRangeReader(map), (long) map.length));
        assertEquals(headerBytes, RemoteMapReader.readMapHeader(new BytesRangeReader(map), map.length).length);
    }

    @Test
    public void bareMapWithHeaderLargerThanFirstChunk() throws IOException {
        byte[] map = mapsforgeMap(5000);
        BytesRangeReader reader = new BytesRangeReader(map);

        assertBoundingBox(extractRemoteMapBoundingBox("Large.map", reader, (long) map.length));
        assertEquals(2, reader.requests);
    }

    @Test
    public void noMapsforgeMagic() {
        assertNull(extractRemoteMapBoundingBox("NoMap.map", new BytesRangeReader(randomBytes(30_000)), 30_000L));
    }
}
