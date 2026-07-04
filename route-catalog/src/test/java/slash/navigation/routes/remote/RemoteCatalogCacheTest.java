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
import slash.navigation.rest.Get;
import slash.navigation.routes.remote.binding.CatalogType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;

import static org.junit.Assert.*;

/**
 * Exercises the conditional GET / disk cache logic in {@link RemoteCatalog#fetch} without a
 * live server by scripting the HTTP responses returned by {@link RemoteCatalog#createGet}.
 */
public class RemoteCatalogCacheTest {
    private static final String URL = "https://api.routeconverter.com/v1/categories/1/";
    private static final String ETAG = "\"v1\"";
    private static final long LAST_MODIFIED = 1_700_000_000_000L;

    private File directory;
    private CatalogDiskCache cache;
    private ScriptedCatalog catalog;

    private static String catalogXml(String name) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<catalog xmlns=\"http://api.routeconverter.com/v1/schemas/route-catalog\">\n" +
                "  <category parent=\"\" name=\"" + name + "\" href=\"" + URL + "\"/>\n" +
                "</catalog>";
    }

    @Before
    public void setUp() throws IOException {
        directory = Files.createTempDirectory("catalog-fetch-test").toFile();
        cache = new CatalogDiskCache(directory);
        catalog = new ScriptedCatalog(cache);
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
    public void testColdFetchSendsNoValidatorsAndStoresToDisk() throws IOException {
        catalog.enqueue(new FakeGet(200, catalogXml("Root"), ETAG, LAST_MODIFIED));

        CatalogType result = catalog.fetch(URL);

        assertEquals("Root", result.getCategory().getName());
        assertNull(catalog.lastGet.sentIfNoneMatch);
        assertNull(catalog.lastGet.sentIfModifiedSince);
        assertNotNull(cache.get(URL));
        assertEquals(ETAG, cache.get(URL).getETag());
    }

    @Test
    public void testWarmFetchSendsValidatorsAndServesDiskCopyOn304() throws IOException {
        catalog.enqueue(new FakeGet(200, catalogXml("Root"), ETAG, LAST_MODIFIED));
        catalog.fetch(URL);

        catalog.enqueue(new FakeGet(304, null, null, null));
        CatalogType result = catalog.fetch(URL);

        assertEquals(ETAG, catalog.lastGet.sentIfNoneMatch);
        assertEquals(Long.valueOf(LAST_MODIFIED), catalog.lastGet.sentIfModifiedSince);
        // body came from disk, not from the (empty) 304 response
        assertEquals("Root", result.getCategory().getName());
    }

    @Test
    public void testModifiedFetch200ReplacesDiskEntry() throws IOException {
        catalog.enqueue(new FakeGet(200, catalogXml("Root"), ETAG, LAST_MODIFIED));
        catalog.fetch(URL);

        catalog.enqueue(new FakeGet(200, catalogXml("Renamed"), "\"v2\"", LAST_MODIFIED + 1));
        CatalogType result = catalog.fetch(URL);

        assertEquals("Renamed", result.getCategory().getName());
        assertEquals("\"v2\"", cache.get(URL).getETag());
        assertTrue(cache.get(URL).getBody().contains("Renamed"));
    }

    @Test
    public void testMutationDropsDiskEntrySoNextFetchRendersFresh() throws IOException {
        catalog.enqueue(new FakeGet(200, catalogXml("Root"), ETAG, LAST_MODIFIED));
        catalog.fetch(URL);
        assertNotNull(cache.get(URL));

        // simulates the drop performed by every own mutation (addCategory, updateRoute, ...)
        catalog.dropFromDiskCache(URL);
        assertNull(cache.get(URL));

        // next fetch is a cache miss: no conditional headers, fresh body served
        catalog.enqueue(new FakeGet(200, catalogXml("Fresh"), "\"v3\"", LAST_MODIFIED + 2));
        CatalogType result = catalog.fetch(URL);

        assertNull(catalog.lastGet.sentIfNoneMatch);
        assertEquals("Fresh", result.getCategory().getName());
    }

    @Test
    public void testUnsuccessfulFetchReturnsNullAndLeavesCacheUntouched() throws IOException {
        catalog.enqueue(new FakeGet(200, catalogXml("Root"), ETAG, LAST_MODIFIED));
        catalog.fetch(URL);

        catalog.enqueue(new FakeGet(500, null, null, null));
        assertNull(catalog.fetch(URL));

        // previous good entry is still there
        assertNotNull(cache.get(URL));
        assertTrue(cache.get(URL).getBody().contains("Root"));
    }

    private static class ScriptedCatalog extends RemoteCatalog {
        private final Deque<FakeGet> responses = new ArrayDeque<>();
        private FakeGet lastGet;

        ScriptedCatalog(CatalogDiskCache diskCache) {
            super("http://localhost/", null, diskCache);
        }

        void enqueue(FakeGet get) {
            responses.add(get);
        }

        @Override
        Get createGet(String url) {
            lastGet = responses.poll();
            if (lastGet == null)
                throw new IllegalStateException("No scripted response for " + url);
            return lastGet;
        }
    }

    private static class FakeGet extends Get {
        private final int statusCode;
        private final String body;
        private final String eTag;
        private final Long lastModified;
        private String sentIfNoneMatch;
        private Long sentIfModifiedSince;

        FakeGet(int statusCode, String body, String eTag, Long lastModified) {
            super("http://localhost/");
            this.statusCode = statusCode;
            this.body = body;
            this.eTag = eTag;
            this.lastModified = lastModified;
        }

        @Override
        public void setIfNoneMatch(String eTag) {
            this.sentIfNoneMatch = eTag;
        }

        @Override
        public void setIfModifiedSince(long modifiedSince) {
            this.sentIfModifiedSince = modifiedSince;
        }

        @Override
        public String executeAsString() {
            return body;
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public String getETag() {
            return eTag;
        }

        @Override
        public Long getLastModified() {
            return lastModified;
        }
    }
}
