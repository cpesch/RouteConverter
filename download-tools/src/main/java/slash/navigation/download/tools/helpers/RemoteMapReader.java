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
package slash.navigation.download.tools.helpers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Locale.ROOT;

/**
 * Reads the mapsforge headers of remote maps with range requests, without downloading the maps.
 * <p>
 * A bare {@code .map} is read from its start for as many bytes as its header needs, which may be more
 * than the 16 KB of the first range request. For a map {@code .zip}, the end of central directory and
 * the central directory are read from the end of the zip, so the {@code .map} entries are found
 * wherever they are in the zip; then the local header and as much data of every {@code .map} entry as
 * its mapsforge header needs are read and inflated.
 * <p>
 * ZIP64 archives are not supported.
 *
 * @author Christian Pesch
 */
public class RemoteMapReader {
    static final byte[] MAPSFORGE_MAGIC = "mapsforge binary OSM".getBytes(StandardCharsets.US_ASCII);
    // the magic and the header size field precede the header size bytes
    static final int MAPSFORGE_PREFIX_SIZE = MAPSFORGE_MAGIC.length + 4;
    // MapFileHeader rejects larger headers
    static final int MAPSFORGE_MAXIMUM_HEADER_SIZE = 1_000_000;

    static final int FIRST_CHUNK_SIZE = 64 * 1024;
    private static final int MAXIMUM_CHUNK_SIZE = 8 * 1024 * 1024;
    private static final int MAXIMUM_CENTRAL_DIRECTORY_SIZE = 16 * 1024 * 1024;

    private static final int END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50;
    private static final int END_OF_CENTRAL_DIRECTORY_SIZE = 22;
    private static final int MAXIMUM_COMMENT_SIZE = 0xFFFF;
    private static final int CENTRAL_DIRECTORY_SIGNATURE = 0x02014b50;
    private static final int CENTRAL_DIRECTORY_HEADER_SIZE = 46;
    private static final int LOCAL_HEADER_SIGNATURE = 0x04034b50;
    private static final int LOCAL_HEADER_SIZE = 30;
    private static final long ZIP64_MARKER = 0xFFFFFFFFL;

    private static final String DOT_MAP = ".map";

    /**
     * A {@code .map} entry of a zip with the bytes of its mapsforge header.
     *
     * @param name the name of the entry
     * @param time the last modification time in milliseconds, from the extended timestamp if there is one
     * @param size the uncompressed size
     * @param header the magic, header size and header bytes of the map
     */
    public record MapEntry(String name, long time, long size, byte[] header) {
    }

    record CentralEntry(String name, int method, int dosTime, int dosDate, long compressedSize, long size,
                        long localHeaderOffset) {
    }

    private RemoteMapReader() {
    }

    /**
     * @return the magic, header size and header bytes of the bare mapsforge map
     */
    public static byte[] readMapHeader(RangeReader reader, long fileSize) throws IOException {
        int length = (int) min(FIRST_CHUNK_SIZE, fileSize > 0 ? fileSize : FIRST_CHUNK_SIZE);
        byte[] bytes = reader.read(0, length);
        int needed = neededHeaderBytes(bytes);
        if (bytes.length < needed) {
            bytes = reader.read(0, needed);
            if (bytes.length < needed)
                throw new IOException(format("Map header needs %d bytes, got %d", needed, bytes.length));
        }
        return copyOf(bytes, needed);
    }

    /**
     * @return the {@code .map} entries of the zip with their mapsforge headers
     */
    public static List<MapEntry> readMapEntries(RangeReader reader, long fileSize) throws IOException {
        List<MapEntry> result = new ArrayList<>();
        for (CentralEntry entry : readCentralDirectory(reader, fileSize)) {
            if (entry.name().endsWith("/") || !entry.name().toLowerCase(ROOT).endsWith(DOT_MAP))
                continue;
            result.add(readMapEntry(reader, fileSize, entry));
        }
        return result;
    }

    static List<CentralEntry> readCentralDirectory(RangeReader reader, long fileSize) throws IOException {
        if (fileSize < END_OF_CENTRAL_DIRECTORY_SIZE)
            throw new IOException(format("Zip of %d bytes is too small", fileSize));

        int tailLength = (int) min(fileSize, END_OF_CENTRAL_DIRECTORY_SIZE + MAXIMUM_COMMENT_SIZE);
        long tailOffset = fileSize - tailLength;
        byte[] tail = reader.read(tailOffset, tailLength);
        if (tail.length < tailLength)
            throw new IOException(format("Zip tail needs %d bytes, got %d", tailLength, tail.length));

        int end = findEndOfCentralDirectory(tail);
        if (end < 0)
            throw new IOException("Found no zip end of central directory");

        int entryCount = getShort(tail, end + 10);
        long centralDirectorySize = getInt(tail, end + 12);
        long centralDirectoryOffset = getInt(tail, end + 16);
        if (entryCount == 0xFFFF || centralDirectorySize == ZIP64_MARKER || centralDirectoryOffset == ZIP64_MARKER)
            throw new IOException("ZIP64 is not supported");
        if (centralDirectorySize > MAXIMUM_CENTRAL_DIRECTORY_SIZE)
            throw new IOException(format("Zip central directory of %d bytes is too large", centralDirectorySize));

        byte[] centralDirectory;
        int index;
        if (centralDirectoryOffset >= tailOffset) {
            centralDirectory = tail;
            index = (int) (centralDirectoryOffset - tailOffset);
        } else {
            centralDirectory = reader.read(centralDirectoryOffset, (int) centralDirectorySize);
            index = 0;
        }

        List<CentralEntry> result = new ArrayList<>();
        for (int i = 0; i < entryCount; i++) {
            if (index + CENTRAL_DIRECTORY_HEADER_SIZE > centralDirectory.length ||
                    getInt(centralDirectory, index) != CENTRAL_DIRECTORY_SIGNATURE)
                throw new IOException(format("Found no zip central directory header for entry %d", i));
            int method = getShort(centralDirectory, index + 10);
            int dosTime = getShort(centralDirectory, index + 12);
            int dosDate = getShort(centralDirectory, index + 14);
            long compressedSize = getInt(centralDirectory, index + 20);
            long size = getInt(centralDirectory, index + 24);
            int nameLength = getShort(centralDirectory, index + 28);
            int extraLength = getShort(centralDirectory, index + 30);
            int commentLength = getShort(centralDirectory, index + 32);
            long localHeaderOffset = getInt(centralDirectory, index + 42);
            if (index + CENTRAL_DIRECTORY_HEADER_SIZE + nameLength > centralDirectory.length)
                throw new IOException(format("Zip central directory ends in the name of entry %d", i));
            String name = new String(centralDirectory, index + CENTRAL_DIRECTORY_HEADER_SIZE, nameLength, StandardCharsets.UTF_8);
            result.add(new CentralEntry(name, method, dosTime, dosDate, compressedSize, size, localHeaderOffset));
            index += CENTRAL_DIRECTORY_HEADER_SIZE + nameLength + extraLength + commentLength;
        }
        return result;
    }

    private static int findEndOfCentralDirectory(byte[] tail) {
        int fallback = -1;
        for (int i = tail.length - END_OF_CENTRAL_DIRECTORY_SIZE; i >= 0; i--) {
            if (getInt(tail, i) != END_OF_CENTRAL_DIRECTORY_SIGNATURE)
                continue;
            // the comment ends the zip; a signature inside the comment does not match its length
            if (i + END_OF_CENTRAL_DIRECTORY_SIZE + getShort(tail, i + 20) == tail.length)
                return i;
            if (fallback < 0)
                fallback = i;
        }
        return fallback;
    }

    static MapEntry readMapEntry(RangeReader reader, long fileSize, CentralEntry entry) throws IOException {
        if (entry.size() == ZIP64_MARKER || entry.compressedSize() == ZIP64_MARKER || entry.localHeaderOffset() == ZIP64_MARKER)
            throw new IOException(format("ZIP64 entry %s is not supported", entry.name()));
        if (entry.method() != ZipEntry.STORED && entry.method() != ZipEntry.DEFLATED)
            throw new IOException(format("Compression method %d of entry %s is not supported", entry.method(), entry.name()));

        long available = fileSize - entry.localHeaderOffset();
        int chunkSize = (int) min(FIRST_CHUNK_SIZE, available);
        while (true) {
            byte[] chunk = reader.read(entry.localHeaderOffset(), chunkSize);
            if (chunk.length < LOCAL_HEADER_SIZE || getInt(chunk, 0) != LOCAL_HEADER_SIGNATURE)
                throw new IOException(format("Found no zip local header for entry %s", entry.name()));
            int nameLength = getShort(chunk, 26);
            int extraLength = getShort(chunk, 28);
            int dataOffset = LOCAL_HEADER_SIZE + nameLength + extraLength;
            long maximumChunkSize = min(min(available, dataOffset + entry.compressedSize()), MAXIMUM_CHUNK_SIZE);

            if (chunk.length >= dataOffset) {
                byte[] extra = new byte[extraLength];
                System.arraycopy(chunk, LOCAL_HEADER_SIZE + nameLength, extra, 0, extraLength);

                byte[] header = uncompress(chunk, dataOffset, entry.method(), MAPSFORGE_PREFIX_SIZE);
                if (header.length >= MAPSFORGE_PREFIX_SIZE) {
                    int needed = neededHeaderBytes(header);
                    header = uncompress(chunk, dataOffset, entry.method(), needed);
                    if (header.length >= needed)
                        return new MapEntry(entry.name(), toTime(entry, extra), entry.size(), header);
                }
            }

            if (chunkSize >= maximumChunkSize)
                throw new IOException(format("Could not read the map header of entry %s from %d bytes", entry.name(), chunkSize));
            chunkSize = (int) min(max((long) chunkSize * 4, dataOffset), maximumChunkSize);
        }
    }

    /**
     * @return the number of bytes the mapsforge header in the given first bytes of a map needs
     */
    static int neededHeaderBytes(byte[] bytes) throws IOException {
        if (bytes.length < MAPSFORGE_PREFIX_SIZE)
            throw new IOException(format("Map header needs at least %d bytes, got %d", MAPSFORGE_PREFIX_SIZE, bytes.length));
        for (int i = 0; i < MAPSFORGE_MAGIC.length; i++)
            if (bytes[i] != MAPSFORGE_MAGIC[i])
                throw new IOException("Found no mapsforge magic");
        int headerSize = ((bytes[20] & 0xFF) << 24) | ((bytes[21] & 0xFF) << 16) | ((bytes[22] & 0xFF) << 8) | (bytes[23] & 0xFF);
        if (headerSize <= 0 || headerSize > MAPSFORGE_MAXIMUM_HEADER_SIZE)
            throw new IOException(format("Invalid mapsforge header size %d", headerSize));
        return MAPSFORGE_PREFIX_SIZE + headerSize;
    }

    /**
     * @return the first {@code needed} uncompressed bytes of the entry data, fewer if the chunk ends before
     */
    static byte[] uncompress(byte[] chunk, int dataOffset, int method, int needed) throws IOException {
        int available = chunk.length - dataOffset;
        if (method == ZipEntry.STORED)
            return copyOfRange(chunk, dataOffset, dataOffset + max(0, min(needed, available)));

        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(chunk, dataOffset, available);
            byte[] result = new byte[needed];
            int length = 0;
            while (length < needed && !inflater.finished() && !inflater.needsInput() && !inflater.needsDictionary())
                length += inflater.inflate(result, length, needed - length);
            return copyOf(result, length);
        } catch (DataFormatException e) {
            throw new IOException("Cannot inflate map header: " + e.getMessage(), e);
        } finally {
            inflater.end();
        }
    }

    /**
     * Prefers the extended timestamp of the local header to the DOS time in local time, like {@link ZipEntry#getTime()}.
     */
    static long toTime(CentralEntry entry, byte[] localExtra) {
        ZipEntry zipEntry = new ZipEntry(entry.name());
        long dosTime = dosToJavaTime(entry.dosDate(), entry.dosTime());
        if (dosTime != -1)
            zipEntry.setTime(dosTime);
        if (localExtra.length > 0) {
            try {
                zipEntry.setExtra(localExtra);
            } catch (RuntimeException e) {
                // keep the DOS time
            }
        }
        return zipEntry.getTime();
    }

    static long dosToJavaTime(int dosDate, int dosTime) {
        try {
            LocalDateTime time = LocalDateTime.of(1980 + ((dosDate >> 9) & 0x7F), (dosDate >> 5) & 0x0F, dosDate & 0x1F,
                    (dosTime >> 11) & 0x1F, (dosTime >> 5) & 0x3F, (dosTime & 0x1F) * 2);
            return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeException e) {
            return -1;
        }
    }

    private static int getShort(byte[] bytes, int index) {
        return (bytes[index] & 0xFF) | ((bytes[index + 1] & 0xFF) << 8);
    }

    private static long getInt(byte[] bytes, int index) {
        return (getShort(bytes, index) | ((long) getShort(bytes, index + 2) << 16)) & 0xFFFFFFFFL;
    }

    private static byte[] copyOf(byte[] bytes, int length) {
        return length == bytes.length ? bytes : copyOfRange(bytes, 0, length);
    }

    private static byte[] copyOfRange(byte[] bytes, int from, int to) {
        return java.util.Arrays.copyOfRange(bytes, from, to);
    }
}
