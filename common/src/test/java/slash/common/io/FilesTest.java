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
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import slash.common.type.CompactCalendar;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.io.File.createTempFile;
import static org.junit.Assert.*;
import static slash.common.TestCase.calendar;
import static slash.common.io.Files.*;
import static slash.common.type.CompactCalendar.fromMillis;

public class FilesTest {
    private File file;

    @Before
    public void setUp() throws Exception {
        file = createTempFile("convert", ".tmp");
        File renamed = new File(file.getParentFile(), "convert.tmp");
        assertTrue(file.renameTo(renamed));
        file = renamed;
    }

    @After
    public void tearDown() {
        assertTrue(file.delete());
    }

    @Test
    public void testNumberToString() {
        assertEquals("5", numberToString(5, 9));
        assertEquals("05", numberToString(5, 10));
        assertEquals("005", numberToString(5, 100));
        assertEquals("0005", numberToString(5, 1000));
    }

    @Test
    public void testCalculateConvertFileName() {
        int FILE_NAME_LENGTH = 18;

        assertEquals(new File(file.getParentFile(), "convert.itn").getAbsolutePath(),
                calculateConvertFileName(file, 0, 0, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert1.itn").getAbsolutePath(),
                calculateConvertFileName(file, 1, 3, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert9.itn").getAbsolutePath(),
                calculateConvertFileName(file, 9, 9, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert5.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 5, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert05.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 50, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert005.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 500, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert10.bcr").getAbsolutePath(),
                calculateConvertFileName(file, 10, 10, ".bcr", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert99.bcr").getAbsolutePath(),
                calculateConvertFileName(file, 99, 99, ".bcr", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert50.itn").getAbsolutePath(),
                calculateConvertFileName(file, 50, 50, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert050.itn").getAbsolutePath(),
                calculateConvertFileName(file, 50, 500, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert100.kml").getAbsolutePath(),
                calculateConvertFileName(file, 100, 100, ".kml", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert999.long").getAbsolutePath(),
                calculateConvertFileName(file, 999, 999, ".long", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert500.itn").getAbsolutePath(),
                calculateConvertFileName(file, 500, 500, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert500.itn").getAbsolutePath(),
                calculateConvertFileName(file, 500, 999, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert5000.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5000, 5000, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert5000.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5000, 9999, ".itn", FILE_NAME_LENGTH));
    }

    @Test
    public void testCalculateConvertFileNameLimitedLength() {
        int FILE_NAME_LENGTH = 4;

        assertEquals(new File(file.getParentFile(), "conv.itn").getAbsolutePath(),
                calculateConvertFileName(file, 0, 0, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "con9.itn").getAbsolutePath(),
                calculateConvertFileName(file, 9, 9, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "con5.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 5, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "co05.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 50, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "c005.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 500, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "5000.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5000, 5000, ".itn", FILE_NAME_LENGTH));
    }

    @Test
    public void testCalculateConvertFileNameMoreThanOneDot() throws IOException {
        File tempPath = createTempFile("test", "egal").getParentFile();
        assertEquals(new File(tempPath, "a.b.c.d.gpx").getAbsolutePath(),
                calculateConvertFileName(new File(tempPath, "a.b.c.d.e"), ".gpx", 255));
    }

    @Test
    public void testCalculateConvertFileNameThrowsException() {
        try {
            calculateConvertFileName(file, 10000, 10000, "gpx", 64);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // intentionally left empty
        }

        try {
            calculateConvertFileName(file, 5000, 10000, "gpx", 64);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // intentionally left empty
        }

        try {
            calculateConvertFileName(file, 1001, 999, "gpx", 64);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // intentionally left empty
        }
    }

   @Test
    public void testShortenPath() {
        assertEquals("C:\\Documents and Settings\\RouteConverter...\\ShortenPath.java",
                shortenPath("C:\\Documents and Settings\\RouteConverter\\My Documents\\RouteConverter\\Sources\\Tests\\ShortenPath.java", 60));
        assertEquals("C:\\Documents and Settings\\RouteConverter\\...\\ShortenPath.java",
                shortenPath("C:\\Documents and Settings\\RouteConverter\\My Documents\\RouteConverter\\Sources\\Tests\\ShortenPath.java", 61));
        assertEquals("\\\\RouteConverter\\RouteConverter\\My Docum...\\ShortenPath.java",
                shortenPath("\\\\RouteConverter\\RouteConverter\\My Documents\\RouteConverter\\Sources\\Tests\\ShortenPath.java", 60));
        assertEquals("/home/routeconverter/RouteConverter/sour.../ShortenPath.java",
                shortenPath("/home/routeconverter/RouteConverter/sources/tests/ShortenPath.java", 60));
        assertEquals(60, shortenPath("/home/routeconverter/RouteConverter/sources/tests/ShortenPath.java", 60).length());
        assertEquals("...l=54.105307,13.490181&sspn=0.132448,0.318604&ie=UTF8&z=12",
                shortenPath("http://maps.google.de/maps?f=d&hl=de&geocode=14250095960720490931,54.083160,13.475246%3B13832872253745319564,54.096925,13.383573%3B4731465831403354564,54.114440,13.528310&saddr=54.096925,+13.383573&daddr=54.08316,13.475246+to:54.114440,+13.528310&mra=ps&mrcr=0,1&sll=54.105307,13.490181&sspn=0.132448,0.318604&ie=UTF8&z=12", 60));
    }

    @Test
    public void testLastPathFragment() {
        assertEquals("file.gpx", lastPathFragment("file.gpx", 60));
        assertEquals("file.gpx", lastPathFragment("../file.gpx", 60));
        assertEquals("file.gpx", lastPathFragment("c:\\bla\\bla\\file.gpx", 60));
        assertEquals("file.gpx", lastPathFragment("c:/bla/bla/file.gpx", 60));
        assertEquals("file.gpx", lastPathFragment("file:///c:/bla/bla/file.gpx", 60));
        assertEquals("file.gpx", lastPathFragment("http://www.blabla.com/bla/bla/file.gpx", 60));
        assertEquals("...file.gpx", lastPathFragment("superlongfilenameforfile.gpx", 11));
        assertEquals(11, lastPathFragment("superlongfilenameforfile.gpx", 11).length());
        assertEquals("...l=54.105307,13.490181&sspn=0.132448,0.318604&ie=UTF8&z=12",
                lastPathFragment("http://maps.google.de/maps?f=d&hl=de&geocode=14250095960720490931,54.0831601,13.475246%3B13832872253745319564,54.096925,13.383573%3B4731465831403354564,54.114440,13.528310&saddr=54.096925,+13.383573&daddr=54.08316,13.475246+to:54.114440,+13.528310&mra=ps&mrcr=0,1&sll=54.105307,13.490181&sspn=0.132448,0.318604&ie=UTF8&z=12", 60));
    }

    @Test
    public void testLastModified() throws IOException {
        CompactCalendar actual = getLastModified(file);
        assertEquals("UTC", actual.getTimeZoneId());
        assertEquals(fromMillis(file.lastModified()), actual);

        setLastModified(file, calendar(2010, 4, 12, 14, 41, 15, 0, "GMT+1"));
        CompactCalendar expected = calendar(2010, 4, 12, 13, 41, 15, 0, "UTC");
        assertEquals(expected, getLastModified(file));
        assertEquals(expected.getTimeInMillis(), file.lastModified());
    }

    @Test
    public void testCollectFilesTerminatesOnDirectorySymlinkLoop() throws IOException {
        Path root = java.nio.file.Files.createTempDirectory("collect-files-loop");
        try {
            Path route = java.nio.file.Files.createFile(root.resolve("route.gpx"));
            Path subDirectory = java.nio.file.Files.createDirectory(root.resolve("sub"));
            Path nestedRoute = java.nio.file.Files.createFile(subDirectory.resolve("nested.gpx"));
            createSymbolicLinkOrSkip(subDirectory.resolve("back"), root);

            List<File> collected = collectFiles(root.toFile(), ".gpx");
            Set<String> collectedPaths = collected.stream().map(FilesTest::canonicalPath).collect(Collectors.toSet());

            assertEquals(2, collected.size());
            assertEquals(2, collectedPaths.size());
            assertTrue(collectedPaths.contains(route.toRealPath().toString()));
            assertTrue(collectedPaths.contains(nestedRoute.toRealPath().toString()));
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void testCollectFilesFollowsSymlinkedRootDirectoryOnce() throws IOException {
        Path root = java.nio.file.Files.createTempDirectory("collect-files-symlink-root");
        try {
            Path route = java.nio.file.Files.createFile(root.resolve("route.gpx"));
            Path link = root.getParent().resolve(root.getFileName() + "-link");
            createSymbolicLinkOrSkip(link, root);

            List<File> collected = collectFiles(link.toFile(), ".gpx");
            Set<String> collectedPaths = collected.stream().map(FilesTest::canonicalPath).collect(Collectors.toSet());

            assertEquals(1, collected.size());
            assertEquals(1, collectedPaths.size());
            assertTrue(collectedPaths.contains(route.toRealPath().toString()));

            java.nio.file.Files.deleteIfExists(link);
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void testCollectFilesWithoutExtensionsCollectsAllFiles() throws IOException {
        Path root = java.nio.file.Files.createTempDirectory("collect-files-all");
        try {
            Path route = java.nio.file.Files.createFile(root.resolve("route.gpx"));
            Path notes = java.nio.file.Files.createFile(root.resolve("notes.txt"));

            List<File> collected = collectFiles(root.toFile());
            Set<String> collectedPaths = collected.stream().map(FilesTest::canonicalPath).collect(Collectors.toSet());

            assertEquals(2, collected.size());
            assertEquals(2, collectedPaths.size());
            assertTrue(collectedPaths.contains(route.toRealPath().toString()));
            assertTrue(collectedPaths.contains(notes.toRealPath().toString()));
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void testRecursiveDeleteDoesNotDeleteExternalDirectoryThroughSymlink() throws IOException {
        Path root = java.nio.file.Files.createTempDirectory("recursive-delete-root");
        Path external = java.nio.file.Files.createTempDirectory("recursive-delete-external");
        try {
            Path inside = java.nio.file.Files.createFile(root.resolve("inside.gpx"));
            Path outside = java.nio.file.Files.createFile(external.resolve("outside.gpx"));
            createSymbolicLinkOrSkip(root.resolve("external"), external);

            recursiveDelete(root.toFile());

            assertFalse(java.nio.file.Files.exists(root));
            assertFalse(java.nio.file.Files.exists(inside));
            assertTrue(java.nio.file.Files.exists(external));
            assertTrue(java.nio.file.Files.exists(outside));
        } finally {
            deleteRecursively(root);
            deleteRecursively(external);
        }
    }

    @Test
    public void testRecursiveDeleteDeletesOnlySymlinkedRoot() throws IOException {
        Path target = java.nio.file.Files.createTempDirectory("recursive-delete-target");
        Path link = target.getParent().resolve(target.getFileName() + "-link");
        try {
            Path route = java.nio.file.Files.createFile(target.resolve("route.gpx"));
            createSymbolicLinkOrSkip(link, target);

            recursiveDelete(link.toFile());

            assertFalse(java.nio.file.Files.exists(link));
            assertTrue(java.nio.file.Files.exists(target));
            assertTrue(java.nio.file.Files.exists(route));
        } finally {
            java.nio.file.Files.deleteIfExists(link);
            deleteRecursively(target);
        }
    }

    @Test
    public void testRecursiveDeleteTerminatesOnDirectorySymlinkLoop() throws IOException {
        Path root = java.nio.file.Files.createTempDirectory("recursive-delete-loop");
        try {
            Path subDirectory = java.nio.file.Files.createDirectory(root.resolve("sub"));
            java.nio.file.Files.createFile(subDirectory.resolve("nested.gpx"));
            createSymbolicLinkOrSkip(subDirectory.resolve("back"), root);

            recursiveDelete(root.toFile());

            assertFalse(java.nio.file.Files.exists(root));
        } finally {
            deleteRecursively(root);
        }
    }

    private static String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new AssertionError("Could not resolve canonical path for " + file, e);
        }
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) throws IOException {
        try {
            java.nio.file.Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            Assume.assumeNoException(e);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !java.nio.file.Files.exists(root))
            return;

        java.nio.file.Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                java.nio.file.Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null)
                    throw exc;
                java.nio.file.Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
