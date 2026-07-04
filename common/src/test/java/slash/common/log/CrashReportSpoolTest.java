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

package slash.common.log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Comparator.comparing;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CrashReportSpoolTest {
    private File directory;
    private CrashReportSpool spool;

    @Before
    public void setUp() throws IOException {
        directory = createTempDirectory("crash-report-spool").toFile();
        spool = new CrashReportSpool(directory);
    }

    @After
    public void tearDown() {
        File[] files = directory.listFiles();
        if (files != null)
            for (File file : files)
                //noinspection ResultOfMethodCallIgnored
                file.delete();
        //noinspection ResultOfMethodCallIgnored
        directory.delete();
    }

    @Test
    public void testWriteAndRead() throws IOException {
        String json = "{\n  \"schema_version\": 1\n}";
        File file = spool.write(json);
        assertTrue(file.exists());
        assertEquals(json, spool.read(file));
        assertEquals(1, spool.list().size());
    }

    @Test
    public void testNewest() throws IOException {
        spool.write("{\"n\":0}");
        File second = spool.write("{\"n\":1}");
        assertEquals(second, spool.newest());
    }

    @Test
    public void testCapKeepsNewestAndDeletesOldest() throws IOException {
        List<File> written = new ArrayList<>();
        for (int i = 0; i < 15; i++)
            written.add(spool.write("{\"n\":" + i + "}"));

        // the spool caps at MAXIMUM_FILES; the oldest are deleted first
        assertEquals(CrashReportSpool.MAXIMUM_FILES, spool.list().size());

        written.sort(comparing(File::getName));
        int deleted = written.size() - CrashReportSpool.MAXIMUM_FILES;
        for (int i = 0; i < deleted; i++)
            assertFalse("oldest report should be deleted: " + written.get(i), written.get(i).exists());
        for (int i = deleted; i < written.size(); i++)
            assertTrue("newest report should survive: " + written.get(i), written.get(i).exists());
    }

    @Test
    public void testDelete() throws IOException {
        File file = spool.write("{\"n\":0}");
        assertTrue(spool.delete(file));
        assertFalse(file.exists());
        assertEquals(0, spool.list().size());
    }

    @Test
    public void testListEmptyWhenDirectoryMissing() {
        CrashReportSpool missing = new CrashReportSpool(new File(directory, "does-not-exist"));
        assertEquals(0, missing.list().size());
        assertEquals(null, missing.newest());
    }
}
