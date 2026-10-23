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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import slash.common.prefs.InMemoryPreferences;

import java.io.File;
import java.nio.file.Files;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Directories}.
 *
 * @author Christian Pesch
 */

public class DirectoriesTest {
    private final Preferences preferences = new InMemoryPreferences();
    private File applicationDirectory;
    private File temporaryDirectory;

    @BeforeEach
    public void setUp() throws Exception {
        applicationDirectory = Files.createTempDirectory("directories-test-application").toFile();
        temporaryDirectory = Files.createTempDirectory("directories-test-temporary").toFile();
        preferences.put("applicationDirectory", applicationDirectory.getAbsolutePath());
        preferences.put("temporaryDirectory", temporaryDirectory.getAbsolutePath());
        Directories.setPreferences(preferences);
    }

    @AfterEach
    public void tearDown() {
        Directories.setPreferences(Preferences.userNodeForPackage(Files.class));
    }

    @Test
    public void testApplicationDirectoryComesFromPreferences() {
        assertEquals(applicationDirectory, Directories.getApplicationDirectory());
        assertTrue(Directories.getApplicationDirectory().isDirectory());
    }

    @Test
    public void testApplicationSubDirectoryComesFromPreferences() {
        assertEquals(new File(applicationDirectory, "routes"), Directories.getApplicationDirectory("routes"));
        assertTrue(Directories.getApplicationDirectory("routes").isDirectory());
    }

    @Test
    public void testTemporaryDirectoryComesFromPreferences() {
        assertEquals(temporaryDirectory, Directories.getTemporaryDirectory());
        assertTrue(Directories.getTemporaryDirectory().isDirectory());
    }

    @Test
    public void testEnsureDirectoryToleratesConcurrentCreation() {
        File directory = new ConcurrentlyCreatedDirectory();
        assertEquals(directory, Directories.ensureDirectory(directory));
    }

    @Test
    public void testEnsureDirectoryThrowsIfCreationFails() {
        File directory = new NeverCreatableDirectory();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Directories.ensureDirectory(directory));
        assertTrue(exception.getMessage().startsWith("Cannot create directory"));
    }

    @Test
    public void testEnsureDirectoryReturnsExistingDirectoryUnchanged() throws Exception {
        File directory = Files.createTempDirectory("directories-test-existing").toFile();
        assertEquals(directory, Directories.ensureDirectory(directory));
    }

    @Test
    public void testEnsureDirectoryThrowsForRegularFile() throws Exception {
        File file = Files.createTempFile("directories-test-file", ".tmp").toFile();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Directories.ensureDirectory(file));
        assertTrue(exception.getMessage().startsWith("Cannot create directory"));
    }

    // simulates another process creating the directory between the exists() and mkdirs() calls
    private static class ConcurrentlyCreatedDirectory extends File {
        private boolean isDirectoryCalled = false;

        private ConcurrentlyCreatedDirectory() {
            super("concurrently-created-directory");
        }

        @Override
        public boolean isDirectory() {
            boolean result = isDirectoryCalled;
            isDirectoryCalled = true;
            return result;
        }

        @Override
        public boolean mkdirs() {
            return false;
        }
    }

    private static class NeverCreatableDirectory extends File {
        private NeverCreatableDirectory() {
            super("never-creatable-directory");
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean mkdirs() {
            return false;
        }
    }
}
