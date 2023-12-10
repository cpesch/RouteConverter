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

package slash.navigation.converter.gui.models;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static slash.common.io.Files.toFile;

public class RecentUrlsModelTest {
    private static final int LIMIT = 10;
    private final RecentUrlsModel model = new RecentUrlsModel(Preferences.userRoot());

    private List<File> tempFiles;

    @Before
    public void setUp() {
        tempFiles = new ArrayList<>();
        model.removeAllUrls();
        assertEquals(0, model.getUrls().size());
    }

    @After
    public void tearDown() {
        for (File file : tempFiles)
            if (file.exists())
                assertTrue(file.delete());
        tempFiles.clear();
        model.removeAllUrls();
        assertEquals(0, model.getUrls().size());
    }

    private File createTempFile(String prefix, String suffix) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        tempFiles.add(file);
        return file;
    }

    @Test
    public void testAddUrl() throws IOException {
        File tempFile = createTempFile("recent1", ".url");
        URL url = tempFile.toURI().toURL();
        model.addUrl(url);
        assertEquals(singletonList(url), model.getUrls());
    }

    @Test
    public void testAddExistingUrl() throws IOException {
        File tempFile = createTempFile("recent2", ".url");
        URL url = tempFile.toURI().toURL();
        model.addUrl(url);
        model.addUrl(url);
        model.addUrl(url);
        assertEquals(singletonList(url), model.getUrls());
    }

    @Test
    public void testReadExistingUrlBug() throws IOException {
        File firstTempFile = createTempFile("first", ".url");
        URL first = firstTempFile.toURI().toURL();
        File secondTempFile = createTempFile("second", ".url");
        URL second = secondTempFile.toURI().toURL();
        File thirdTempFile = createTempFile("third", ".url");
        URL third = thirdTempFile.toURI().toURL();
        model.addUrl(first);
        model.addUrl(second);
        model.addUrl(first);
        model.addUrl(third);
        assertEquals(asList(third, first, second), model.getUrls());
    }

    @Test
    public void testLatestFirst() throws IOException {
        List<URL> expected = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            File tempFile = createTempFile("recent-" + i + "-", ".url");
            URL url = tempFile.toURI().toURL();
            model.addUrl(url);
            expected.add(0, url);
            assertEquals(expected, model.getUrls());
        }
    }

    @Ignore // sometimes fails on command line
    @Test
    public void testLimit() throws IOException {
        assertEquals(0, model.getUrls().size());
        List<URL> collected = new ArrayList<>();
        for (int i = 0; i < 2 * LIMIT; i++) {
            File tempFile = createTempFile("recent-" + i + "-", ".url");
            URL url = tempFile.toURI().toURL();
            model.addUrl(url);
            collected.add(0, url);
            List<URL> expected = collected.subList(0, min(i + 1, LIMIT));
            List<URL> actual = model.getUrls();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);
        }
    }

    @Ignore // sometimes fails on command line
    @Test
    public void testSkipNotExistentFiles() throws IOException {
        List<URL> collected = new ArrayList<>();
        for (int i = 0; i < LIMIT; i++) {
            File tempFile = createTempFile("recent-" + i + "-", ".url");
            URL url = tempFile.toURI().toURL();
            model.addUrl(url);
            collected.add(0, url);
        }

        for (int i = 0; i < LIMIT; i++) {
            File file = toFile(collected.get(i));
            assertNotNull(file);
            assertTrue(file.delete());
            List<URL> expected = collected.subList(i + 1, min(collected.size(), LIMIT));
            List<URL> actual = model.getUrls();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);
        }
    }
}
