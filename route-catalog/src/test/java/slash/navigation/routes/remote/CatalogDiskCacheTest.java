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

package slash.navigation.routes.remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class CatalogDiskCacheTest {
    private static final String URL = "https://api.routeconverter.com/v1/categories/1/";
    private static final String BODY = "<?xml version=\"1.0\"?>\n<catalog>\n<category name=\"Root\"/>\n</catalog>";
    private static final String ETAG = "\"abc123\"";
    private static final long LAST_MODIFIED = 1_700_000_000_000L;

    private File directory;
    private CatalogDiskCache cache;

    @Before
    public void setUp() throws IOException {
        directory = Files.createTempDirectory("catalog-cache-test").toFile();
        cache = new CatalogDiskCache(directory);
    }

    @After
    public void tearDown() throws IOException {
        if (directory != null && directory.exists())
            Files.walk(directory.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)
                    .forEach(File::delete);
    }

    @Test
    public void testMissReturnsNull() {
        assertNull(cache.get(URL));
    }

    @Test
    public void testHit() {
        cache.put(URL, BODY, ETAG, LAST_MODIFIED);

        CatalogDiskCache.Entry entry = cache.get(URL);
        assertNotNull(entry);
        assertEquals(BODY, entry.getBody());
        assertEquals(ETAG, entry.getETag());
        assertEquals(Long.valueOf(LAST_MODIFIED), entry.getLastModified());
    }

    @Test
    public void testNullValidatorsRoundTrip() {
        cache.put(URL, BODY, null, null);

        CatalogDiskCache.Entry entry = cache.get(URL);
        assertNotNull(entry);
        assertEquals(BODY, entry.getBody());
        assertNull(entry.getETag());
        assertNull(entry.getLastModified());
    }

    @Test
    public void testReplace() {
        cache.put(URL, BODY, ETAG, LAST_MODIFIED);
        cache.put(URL, "<catalog><updated/></catalog>", "\"def456\"", LAST_MODIFIED + 1);

        CatalogDiskCache.Entry entry = cache.get(URL);
        assertEquals("<catalog><updated/></catalog>", entry.getBody());
        assertEquals("\"def456\"", entry.getETag());
        assertEquals(Long.valueOf(LAST_MODIFIED + 1), entry.getLastModified());
    }

    @Test
    public void testDifferentUrlsAreDistinct() {
        cache.put(URL, BODY, ETAG, LAST_MODIFIED);
        String otherUrl = "https://api.routeconverter.com/v1/categories/2/";
        cache.put(otherUrl, "<catalog><other/></catalog>", null, null);

        assertEquals(BODY, cache.get(URL).getBody());
        assertEquals("<catalog><other/></catalog>", cache.get(otherUrl).getBody());
    }

    @Test
    public void testRemove() {
        cache.put(URL, BODY, ETAG, LAST_MODIFIED);
        assertNotNull(cache.get(URL));

        cache.remove(URL);
        assertNull(cache.get(URL));
    }

    @Test
    public void testRemoveMissingIsHarmless() {
        cache.remove(URL);
        cache.remove(null);
        assertNull(cache.get(URL));
    }

    @Test
    public void testAtomicWriteLeavesNoTemporaryFiles() {
        cache.put(URL, BODY, ETAG, LAST_MODIFIED);

        File[] cacheFiles = directory.listFiles((dir, name) -> name.endsWith(".cache"));
        File[] tempFiles = directory.listFiles((dir, name) -> name.endsWith(".tmp"));
        assertNotNull(cacheFiles);
        assertEquals(1, cacheFiles.length);
        assertNotNull(tempFiles);
        assertEquals(0, tempFiles.length);
    }

    @Test
    public void testCorruptEntryTreatedAsMiss() throws IOException {
        cache.put(URL, BODY, ETAG, LAST_MODIFIED);
        File cacheFile = directory.listFiles((dir, name) -> name.endsWith(".cache"))[0];

        Files.writeString(cacheFile.toPath(), "this is not a valid cache entry", UTF_8);

        assertNull(cache.get(URL));
    }

    @Test
    public void testTruncatedEntryTreatedAsMiss() throws IOException {
        cache.put(URL, BODY, ETAG, LAST_MODIFIED);
        File cacheFile = directory.listFiles((dir, name) -> name.endsWith(".cache"))[0];

        // only the magic header, simulating a partially written body
        Files.writeString(cacheFile.toPath(), "RCCATALOG1\n", UTF_8);

        assertNull(cache.get(URL));
    }

    @Test
    public void testPutCreatesMissingDirectory() {
        File nested = new File(directory, "does/not/exist/yet");
        CatalogDiskCache nestedCache = new CatalogDiskCache(nested);

        nestedCache.put(URL, BODY, ETAG, LAST_MODIFIED);

        assertEquals(BODY, nestedCache.get(URL).getBody());
    }
}
