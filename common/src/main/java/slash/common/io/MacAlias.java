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

package slash.common.io;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static slash.common.system.Platform.isMac;

/**
 * Represents a macOS Finder alias (a regular file carrying bookmark data, the
 * counterpart to a Windows {@link WindowsShortcut}). Detection sniffs the
 * "book" bookmark-data magic; resolution to the target path is delegated to
 * {@code osascript}/Finder, since the bookmark format is not stably parseable.
 *
 * @author Christian Pesch
 */
public class MacAlias implements ResolvableLink {
    // modern Finder aliases are bookmark data starting with "book\0\0\0\0mark"
    private static final byte[] MAGIC = {'b', 'o', 'o', 'k'};
    private static final long RESOLVE_TIMEOUT_SECONDS = 10;

    private final String realFileName;
    private final boolean directory;

    /**
     * Provides a quick test to see if this could be a valid macOS alias.
     * Instantiating a {@link MacAlias} spawns an {@code osascript} process, so
     * any code looping over several files should first check this.
     *
     * @param file the potential alias
     * @return true if this is a macOS running a file that carries the alias magic, false otherwise
     * @throws IOException if an IOException is thrown while reading from the file
     */
    public static boolean isPotentialValidAlias(File file) throws IOException {
        if (!isMac() || !file.isFile())
            return false;

        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] head = new byte[MAGIC.length];
            return inputStream.read(head) == MAGIC.length && Arrays.equals(head, MAGIC);
        }
    }

    public MacAlias(File file) throws IOException {
        this.realFileName = resolveTarget(file);
        if (realFileName == null)
            throw new IOException(format("Could not resolve macOS alias %s", file));
        this.directory = new File(realFileName).isDirectory();
    }

    /**
     * @return the name of the filesystem object pointed to by this alias
     */
    public String getRealFilename() {
        return realFileName;
    }

    /**
     * @return true if the alias points to a directory, false otherwise
     */
    public boolean isDirectory() {
        return directory;
    }

    public boolean isFile() {
        return !isDirectory();
    }

    /**
     * Asks Finder for the original item of the alias and returns its POSIX path,
     * or null if the alias cannot be resolved (dangling, or Finder unavailable).
     */
    private static String resolveTarget(File file) throws IOException {
        String path = file.getAbsolutePath().replace("\\", "\\\\").replace("\"", "\\\"");
        String script = "tell application \"Finder\" to get POSIX path of " +
                "(original item of ((POSIX file \"" + path + "\") as alias) as text)";
        Process process = new ProcessBuilder("osascript", "-e", script)
                .redirectErrorStream(false)
                .start();
        try {
            String result;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))) {
                result = reader.readLine();
            }
            if (!process.waitFor(RESOLVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0 || result == null || result.trim().isEmpty())
                return null;
            return result.trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(format("Interrupted while resolving macOS alias %s", file), e);
        }
    }
}
