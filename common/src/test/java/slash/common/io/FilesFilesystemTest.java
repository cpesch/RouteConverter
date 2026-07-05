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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static slash.common.io.Files.absolutize;
import static slash.common.io.Files.asDialogString;
import static slash.common.io.Files.checkDirectory;
import static slash.common.io.Files.checkFile;
import static slash.common.io.Files.collectFiles;
import static slash.common.io.Files.createReadablePath;
import static slash.common.io.Files.createTargetFiles;
import static slash.common.io.Files.findExistingPath;
import static slash.common.io.Files.generateChecksum;
import static slash.common.io.Files.lastPathFragment;
import static slash.common.io.Files.recursiveDelete;
import static slash.common.io.Files.setLastModified;
import static slash.common.io.Files.shortenPath;
import static slash.common.io.Files.toFile;
import static slash.common.io.Files.toUrls;

/**
 * Tests the file-system and path helpers of {@link Files} that operate on real temporary
 * files/directories - the branches not exercised by {@code FilesTest}/{@code FilesUrlTest}.
 *
 * @author Christian Pesch
 */
public class FilesFilesystemTest {
    private File directory;

    @Before
    public void setUp() throws IOException {
        directory = File.createTempFile("filesfilesystem", "");
        assertTrue(directory.delete());
        assertTrue(directory.mkdirs());
    }

    @After
    public void tearDown() throws IOException {
        if (directory.exists())
            recursiveDelete(directory);
    }

    private File writeFile(File file, byte[] content) throws IOException {
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content);
        }
        return file;
    }

    @Test
    public void createReadablePathReturnsCanonicalPathForFile() throws IOException {
        File file = writeFile(new File(directory, "a.txt"), new byte[]{1});
        assertEquals(file.getCanonicalPath(), createReadablePath(file));
    }

    @Test
    public void createReadablePathHandlesFileAndNonFileUrls() throws Exception {
        File file = writeFile(new File(directory, "b.txt"), new byte[]{1});
        assertEquals(file.getCanonicalPath(), createReadablePath(file.toURI().toURL()));

        URL http = new URL("http://www.routeconverter.com/index.html");
        assertEquals(http.toExternalForm(), createReadablePath(http));
    }

    @Test
    public void toFileReturnsFileForFileUrlAndNullOtherwise() throws Exception {
        File file = new File(directory, "c.txt");
        assertEquals(file.getAbsoluteFile(), toFile(file.toURI().toURL()).getAbsoluteFile());
        assertNull(toFile(new URL("http://www.routeconverter.com/")));
    }

    @Test
    public void toUrlsFallsBackFromMalformedUrlToFile() {
        List<URL> urls = toUrls("relative/path/to/file.gpx");

        assertEquals(1, urls.size());
        assertEquals("file", urls.get(0).getProtocol());
    }

    @Test
    public void shortenPathReturnsTheLastFragmentWhenItFillsTheLimit() {
        assertEquals("...cdefghij", shortenPath("/parent/abcdefghij", 11));
    }

    @Test
    public void lastPathFragmentTruncatesLongFragmentsWithEllipsis() {
        assertEquals("...bbbbbbb", lastPathFragment("/a/bbbbbbbbbbbb", 10));
    }

    @Test
    public void createTargetFilesForSingleAndMultipleOutputs() {
        File pattern = new File(directory, "route.gpx");

        File[] single = createTargetFiles(pattern, 1, ".kml", 64);
        assertEquals(1, single.length);
        assertTrue(single[0].getName().endsWith(".kml"));

        File[] many = createTargetFiles(pattern, 3, ".kml", 64);
        assertEquals(3, many.length);
        assertTrue(many[0].getName().endsWith("1.kml"));
        assertTrue(many[2].getName().endsWith("3.kml"));
    }

    @Test
    public void checkFileAcceptsAReadableFileAndRejectsMissingOrDirectory() throws IOException {
        File file = writeFile(new File(directory, "readable.txt"), new byte[]{1});
        checkFile(file);  // no exception

        assertCheckFails(() -> checkFile(new File(directory, "missing.txt")));
        assertCheckFails(() -> checkFile(directory));   // a directory is not a file
    }

    @Test
    public void checkDirectoryAcceptsADirectoryAndRejectsMissingOrFile() throws IOException {
        checkDirectory(directory);  // no exception

        File file = writeFile(new File(directory, "file.txt"), new byte[]{1});
        assertCheckFails(() -> checkDirectory(new File(directory, "missing")));
        assertCheckFails(() -> checkDirectory(file));   // a file is not a directory
    }

    private interface Check {
        void run() throws IOException;
    }

    private void assertCheckFails(Check check) {
        try {
            check.run();
            fail("expected FileNotFoundException");
        } catch (FileNotFoundException expected) {
            // expected
        } catch (IOException e) {
            fail("unexpected: " + e);
        }
    }

    @Test
    public void writePartialFileTruncatesToTheGivenSize() throws IOException {
        File file = new File(directory, "partial.bin");
        Files.writePartialFile(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}), 3, file);
        assertEquals(3, file.length());
    }

    @Test
    public void writePartialFileExtendsToTheGivenSize() throws IOException {
        File file = new File(directory, "partial2.bin");
        Files.writePartialFile(new ByteArrayInputStream(new byte[]{1, 2, 3}), 5, file);
        assertEquals(5, file.length());
    }

    @Test
    public void generateChecksumOfFileMatchesStreamChecksum() throws IOException {
        byte[] content = {10, 20, 30, 40};
        File file = writeFile(new File(directory, "sum.bin"), content);
        assertEquals(generateChecksum(new ByteArrayInputStream(content)), generateChecksum(file));
    }

    @Test
    public void setLastModifiedIgnoresNullAndAppliesValues() throws IOException {
        File file = writeFile(new File(directory, "touched.txt"), new byte[]{1});

        setLastModified(file, (Long) null);        // no-op
        setLastModified(file, (slash.common.type.CompactCalendar) null);  // no-op

        setLastModified(file, 100000L);
        assertTrue(file.lastModified() > 0);

        setLastModified(file, slash.common.type.CompactCalendar.fromMillis(200000L));
        assertTrue(file.lastModified() > 0);
    }

    @Test
    public void collectFilesDescendsTheTreeAndFiltersByExtension() throws IOException {
        writeFile(new File(directory, "a.gpx"), new byte[]{1});
        writeFile(new File(directory, "notes.txt"), new byte[]{1});
        writeFile(new File(directory, "sub/b.gpx"), new byte[]{1});

        assertEquals(2, collectFiles(directory, ".gpx").size());
        assertEquals(3, collectFiles(directory).size());  // no extension filter
    }

    @Test
    public void findExistingPathWalksUpToTheFirstExistingAncestor() {
        File missing = new File(directory, "no/such/child");
        assertEquals(directory, findExistingPath(missing));
    }

    @Test
    public void absolutizeMakesRelativeFilesAbsolute() {
        assertTrue(absolutize(new File("relative.txt")).isAbsolute());
    }

    @Test
    public void recursiveDeleteRemovesTheWholeTree() throws IOException {
        writeFile(new File(directory, "sub/deep/file.txt"), new byte[]{1});

        recursiveDelete(directory);

        assertFalse(directory.exists());
    }

    @Test
    public void asDialogStringHandlesNullEmptyAndMultipleEntries() {
        assertEquals("null", asDialogString(null, false));
        assertEquals("none", asDialogString(asList(), false));

        String joined = asDialogString(asList("one", "two", "three"), false);
        assertTrue(joined.contains(",\n"));
        assertTrue(joined.contains(" and\n"));
        assertTrue(joined.startsWith("'one'"));
    }
}
