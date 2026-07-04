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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A small on-disk cache for catalog responses keyed by URL. Each entry stores the
 * response body together with its {@code ETag} and {@code Last-Modified} validators
 * so that {@link RemoteCatalog} can issue conditional GET requests and serve the
 * body from disk on a {@code 304 Not Modified}.
 * <p>
 * Entries are written atomically (temp file plus rename) so a partial write can
 * never be served as a valid cache hit. A corrupt or unreadable entry is treated
 * as a miss.
 *
 * @author Christian Pesch
 */

class CatalogDiskCache {
    private static final Logger log = Logger.getLogger(CatalogDiskCache.class.getName());
    private static final String MAGIC = "RCCATALOG1";
    private static final String SUFFIX = ".cache";

    private final File directory;

    CatalogDiskCache(File directory) {
        this.directory = directory;
    }

    private static String hash(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(url.getBytes(UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes)
                builder.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private File fileFor(String url) {
        return new File(directory, hash(url) + SUFFIX);
    }

    /**
     * Returns the cached entry for the given url or {@code null} on a miss or a
     * corrupt/unreadable entry.
     */
    synchronized Entry get(String url) {
        File file = fileFor(url);
        if (!file.isFile())
            return null;

        try {
            String content = Files.readString(file.toPath(), UTF_8);
            int p1 = content.indexOf('\n');
            int p2 = p1 < 0 ? -1 : content.indexOf('\n', p1 + 1);
            int p3 = p2 < 0 ? -1 : content.indexOf('\n', p2 + 1);
            if (p3 < 0 || !MAGIC.equals(content.substring(0, p1))) {
                log.warning("Ignoring corrupt catalog cache entry " + file);
                return null;
            }
            String eTag = content.substring(p1 + 1, p2);
            String lastModified = content.substring(p2 + 1, p3);
            String body = content.substring(p3 + 1);
            return new Entry(body,
                    eTag.isEmpty() ? null : eTag,
                    lastModified.isEmpty() ? null : Long.valueOf(lastModified));
        } catch (RuntimeException | IOException e) {
            log.warning("Cannot read catalog cache entry " + file + ": " + e);
            return null;
        }
    }

    /**
     * Atomically stores the body and its validators for the given url. A failure
     * to write leaves any previous entry untouched.
     */
    synchronized void put(String url, String body, String eTag, Long lastModified) {
        if (body == null)
            return;

        String content = MAGIC + '\n' +
                (eTag != null ? eTag : "") + '\n' +
                (lastModified != null ? lastModified.toString() : "") + '\n' +
                body;

        File file = fileFor(url);
        Path temp = null;
        try {
            if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory())
                throw new IOException("Cannot create cache directory " + directory);

            temp = Files.createTempFile(directory.toPath(), "catalog", ".tmp");
            Files.writeString(temp, content, UTF_8);
            try {
                Files.move(temp, file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING);
            } catch (IOException e) {
                // some file systems don't support atomic move
                Files.move(temp, file.toPath(), REPLACE_EXISTING);
            }
            temp = null;
        } catch (RuntimeException | IOException e) {
            log.warning("Cannot write catalog cache entry " + file + ": " + e);
        } finally {
            if (temp != null)
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    log.warning("Cannot delete temporary cache file " + temp + ": " + e);
                }
        }
    }

    /**
     * Drops the cached entry for the given url, if any.
     */
    synchronized void remove(String url) {
        if (url == null)
            return;
        File file = fileFor(url);
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            log.warning("Cannot delete catalog cache entry " + file + ": " + e);
        }
    }

    static class Entry {
        private final String body;
        private final String eTag;
        private final Long lastModified;

        Entry(String body, String eTag, Long lastModified) {
            this.body = body;
            this.eTag = eTag;
            this.lastModified = lastModified;
        }

        String getBody() {
            return body;
        }

        String getETag() {
            return eTag;
        }

        Long getLastModified() {
            return lastModified;
        }
    }
}
