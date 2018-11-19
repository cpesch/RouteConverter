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
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static java.io.File.createTempFile;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static slash.common.io.Files.toFile;

public class RecentUrlsModelTest {
    private static final int LIMIT = 10;
    private RecentUrlsModel model = new RecentUrlsModel(Preferences.userRoot());

    @Before
    public void setUp() {
        model.removeAllUrls();
    }

    @After
    public void tearDown() {
        model.removeAllUrls();
    }

    @Test
    public void testAddUrl() throws IOException {
        File tempFile = createTempFile("recent", ".url");
        tempFile.deleteOnExit();
        URL url = tempFile.toURI().toURL();
        model.addUrl(url);
        assertEquals(singletonList(url), model.getUrls());
    }

    @Test
    public void testAddExistingUrl() throws IOException {
        File tempFile = createTempFile("recent", ".url");
        tempFile.deleteOnExit();
        URL url = tempFile.toURI().toURL();
        model.addUrl(url);
        model.addUrl(url);
        model.addUrl(url);
        assertEquals(singletonList(url), model.getUrls());
    }

    @Test
    public void testReadExistingUrlBug() throws IOException {
        File firstTempFile = createTempFile("first", ".url");
        firstTempFile.deleteOnExit();
        URL first = firstTempFile.toURI().toURL();
        File secondTempFile = createTempFile("second", ".url");
        secondTempFile.deleteOnExit();
        URL second = secondTempFile.toURI().toURL();
        File thirdTempFile = createTempFile("third", ".url");
        thirdTempFile.deleteOnExit();
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
            tempFile.deleteOnExit();
            URL url = tempFile.toURI().toURL();
            model.addUrl(url);
            expected.add(0, url);
            assertEquals(expected, model.getUrls());
        }
    }

    @Test
    public void testLimit() throws IOException {
        List<URL> collected = new ArrayList<>();
        for (int i = 0; i < 2 * LIMIT; i++) {
            File tempFile = createTempFile("recent-" + i + "-", ".url");
            tempFile.deleteOnExit();
            URL url = tempFile.toURI().toURL();
            model.addUrl(url);
            collected.add(0, url);
            List<URL> expected = collected.subList(0, min(i + 1, LIMIT));
            List<URL> actual = model.getUrls();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testSkipNotExistentFiles() throws IOException {
        List<URL> collected = new ArrayList<>();
        for (int i = 0; i < LIMIT; i++) {
            File tempFile = createTempFile("recent-" + i + "-", ".url");
            tempFile.deleteOnExit();
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
