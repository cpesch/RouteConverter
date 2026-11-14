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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.datasources.binding.ChecksumType;
import slash.navigation.datasources.binding.FragmentType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.io.File.createTempFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.download.tools.UpdateCatalog.extractHgtFragments;

/**
 * Tests that {@link UpdateCatalog#extractHgtFragments} describes the {@code .hgt} entries of a
 * partially downloaded zip by their local headers and never hashes the zero padding (rc/RouteConverter#362).
 *
 * @author Christian Pesch
 */
public class UpdateCatalogFragmentsTest {
    private static final int RANGE = 16 * 1024;
    // larger than the downloaded range and incompressible, so even the deflated data exceeds it
    private static final byte[] HGT = new byte[64 * 1024];
    private static final long TIME = 1_700_000_000_000L;

    static {
        new Random(362).nextBytes(HGT);
    }

    private File zip;

    @Before
    public void setUp() throws IOException {
        zip = createTempFile("tile", ".zip");
    }

    @After
    public void tearDown() {
        if (zip.exists())
            zip.delete();
    }

    private static ZipEntry entry(String name, int method) {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(TIME);
        entry.setMethod(method);
        if (method == ZipEntry.STORED) {
            CRC32 crc = new CRC32();
            crc.update(HGT);
            entry.setSize(HGT.length);
            entry.setCompressedSize(HGT.length);
            entry.setCrc(crc.getValue());
        }
        return entry;
    }

    private static byte[] zipOf(int method, String... names) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (String name : names) {
                out.putNextEntry(entry(name, method));
                out.write(HGT);
                out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    /** What {@code Files.writePartialFile} leaves: the first 16 KB, then zeros up to the full length. */
    private void writePartial(byte[] zipBytes) throws IOException {
        assertTrue(zipBytes.length > RANGE);
        try (RandomAccessFile file = new RandomAccessFile(zip, "rw")) {
            file.write(zipBytes, 0, RANGE);
            file.setLength(zipBytes.length);
        }
    }

    private void writeFull(byte[] zipBytes) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(zip, "rw")) {
            file.write(zipBytes);
        }
    }

    private static void assertHeaderOnly(FragmentType fragment, String key, Long contentLength) {
        assertEquals(key, fragment.getKey());
        assertEquals(1, fragment.getChecksum().size());
        ChecksumType checksum = fragment.getChecksum().get(0);
        assertEquals(contentLength, checksum.getContentLength());
        assertNotNull(checksum.getLastModified());
        assertNull("partial data must never be hashed", checksum.getSha1());
    }

    @Test
    public void storedEntryInPartialZip() throws IOException {
        writePartial(zipOf(ZipEntry.STORED, "N46E008.hgt"));

        List<FragmentType> fragments = extractHgtFragments(zip);

        assertEquals(1, fragments.size());
        assertHeaderOnly(fragments.get(0), "N46E008.hgt", (long) HGT.length);
    }

    @Test
    public void deflatedEntryInPartialZip() throws IOException {
        writePartial(zipOf(ZipEntry.DEFLATED, "N46E008.hgt"));

        List<FragmentType> fragments = extractHgtFragments(zip);

        // ZipOutputStream writes deflated entries with a data descriptor: the local header has no size
        assertEquals(1, fragments.size());
        assertHeaderOnly(fragments.get(0), "N46E008.hgt", null);
    }

    @Test
    public void fragmentsBeforeTheUnreadableEntryAreKept() throws IOException {
        // the second local header lies in the zero padding: reading stops there without an exception
        writePartial(zipOf(ZipEntry.STORED, "N46E008.hgt", "N46E009.hgt"));

        List<FragmentType> fragments = extractHgtFragments(zip);

        assertEquals(1, fragments.size());
        assertHeaderOnly(fragments.get(0), "N46E008.hgt", (long) HGT.length);
    }

    @Test
    public void completeZipListsAllEntriesWithoutSha1() throws IOException {
        writeFull(zipOf(ZipEntry.STORED, "N46E008.hgt", "N46E009.hgt", "readme.txt"));

        List<FragmentType> fragments = extractHgtFragments(zip);

        assertEquals(2, fragments.size());
        assertHeaderOnly(fragments.get(0), "N46E008.hgt", (long) HGT.length);
        assertHeaderOnly(fragments.get(1), "N46E009.hgt", (long) HGT.length);
    }

    @Test
    public void missingFileYieldsNoFragments() {
        assertTrue(zip.delete());

        assertTrue(extractHgtFragments(zip).isEmpty());
    }
}
