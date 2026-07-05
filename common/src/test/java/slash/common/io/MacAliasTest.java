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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static slash.common.io.Files.recursiveDelete;
import static slash.common.system.Platform.isMac;

/**
 * Unit tests for {@link MacAlias}. Alias creation and resolution require macOS
 * with a running Finder, so the resolving tests are skipped elsewhere.
 *
 * @author Christian Pesch
 */
public class MacAliasTest {
    private File base;
    private File targetFile;
    private File targetDirectory;

    @Before
    public void setUp() throws IOException {
        base = new File(System.getProperty("java.io.tmpdir"), "mac-alias-test-" + System.currentTimeMillis());
        assertTrue(base.mkdir());
        targetFile = new File(base, "target.gpx");
        assertTrue(targetFile.createNewFile());
        targetDirectory = new File(base, "targetDirectory");
        assertTrue(targetDirectory.mkdir());
    }

    @After
    public void tearDown() throws IOException {
        recursiveDelete(base);
    }

    @Test
    public void testRejectsNonAliasFile() throws IOException {
        assertFalse(MacAlias.isPotentialValidAlias(targetFile));
    }

    @Test
    public void testRejectsDirectory() throws IOException {
        assertFalse(MacAlias.isPotentialValidAlias(targetDirectory));
    }

    @Test
    public void testDetectsAndResolvesFileAlias() throws IOException {
        assumeTrue(isMac());
        File alias = createAlias(targetFile);
        assumeTrue(alias != null && alias.isFile());

        assertTrue(MacAlias.isPotentialValidAlias(alias));
        MacAlias macAlias = new MacAlias(alias);
        assertTrue(macAlias.isFile());
        assertFalse(macAlias.isDirectory());
        assertEquals(targetFile.getCanonicalPath(), new File(macAlias.getRealFilename()).getCanonicalPath());
    }

    @Test
    public void testDetectsAndResolvesDirectoryAlias() throws IOException {
        assumeTrue(isMac());
        File alias = createAlias(targetDirectory);
        assumeTrue(alias != null && alias.isFile());

        assertTrue(MacAlias.isPotentialValidAlias(alias));
        MacAlias macAlias = new MacAlias(alias);
        assertTrue(macAlias.isDirectory());
        assertFalse(macAlias.isFile());
        assertEquals(targetDirectory.getCanonicalPath(), new File(macAlias.getRealFilename()).getCanonicalPath());
    }

    /**
     * Creates a Finder alias to the given target inside {@link #base} and returns
     * the alias file, or null if Finder was not available (e.g. headless CI).
     */
    private File createAlias(File target) {
        String script = format("tell application \"Finder\"%n" +
                "  set src to POSIX file \"%s\" as alias%n" +
                "  set aliasFile to make new alias file at (POSIX file \"%s\" as alias) to src%n" +
                "  get POSIX path of (aliasFile as text)%n" +
                "end tell", target.getAbsolutePath(), base.getAbsolutePath());
        try {
            Process process = new ProcessBuilder("osascript", "-e", script).start();
            String path;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                path = reader.readLine();
            }
            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0 || path == null)
                return null;
            return new File(path.trim());
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }
}
