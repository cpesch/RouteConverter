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

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.common.io.Files.toFile;

public class RecentUrlsModelTest {
    private static final int LIMIT = 10;
    private RecentUrlsModel recentUrlsModel = new RecentUrlsModel();

    @Before
    public void setUp() {
        recentUrlsModel.removeAllUrls();
    }

    @Test
    public void testAddUrl() throws IOException {
        URL url = File.createTempFile("recent", ".url").toURI().toURL();
        recentUrlsModel.addUrl(url);
        assertEquals(Arrays.asList(url), recentUrlsModel.getUrls());
    }

    @Test
    public void testAddExistingUrl() throws IOException {
        URL url = File.createTempFile("recent", ".url").toURI().toURL();
        recentUrlsModel.addUrl(url);
        recentUrlsModel.addUrl(url);
        recentUrlsModel.addUrl(url);
        assertEquals(Arrays.asList(url), recentUrlsModel.getUrls());
    }

    @Test
    public void testReaddExistingUrlBug() throws IOException {
        URL first = File.createTempFile("first", ".url").toURI().toURL();
        URL second = File.createTempFile("second", ".url").toURI().toURL();
        URL third = File.createTempFile("third", ".url").toURI().toURL();
        recentUrlsModel.addUrl(first);
        recentUrlsModel.addUrl(second);
        recentUrlsModel.addUrl(first);
        recentUrlsModel.addUrl(third);
        assertEquals(Arrays.asList(third, first, second), recentUrlsModel.getUrls());
    }

    @Test
    public void testLatestFirst() throws IOException {
        List<URL> expected = new ArrayList<URL>();
        for (int i = 0; i < 5; i++) {
            URL url = File.createTempFile("recent-" + i + "-", ".url").toURI().toURL();
            recentUrlsModel.addUrl(url);
            expected.add(0, url);
            assertEquals(expected, recentUrlsModel.getUrls());
        }
    }

    @Test
    public void testLimit() throws IOException {
        List<URL> collected = new ArrayList<URL>();
        for (int i = 0; i < 2 * LIMIT; i++) {
            URL url = File.createTempFile("recent-" + i + "-", ".url").toURI().toURL();
            recentUrlsModel.addUrl(url);
            collected.add(0, url);
            List<URL> expected = collected.subList(0, Math.min(i + 1, LIMIT));
            List<URL> actual = recentUrlsModel.getUrls();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testSkipNotExistantFiles() throws IOException {
        List<URL> collected = new ArrayList<URL>();
        for (int i = 0; i < LIMIT; i++) {
            URL url = File.createTempFile("recent-" + i + "-", ".url").toURI().toURL();
            recentUrlsModel.addUrl(url);
            collected.add(0, url);
        }

        for (int i = 0; i < LIMIT; i++) {
            assertTrue(toFile(collected.get(i)).delete());
            List<URL> expected = collected.subList(i + 1, Math.min(collected.size(), LIMIT));
            List<URL> actual = recentUrlsModel.getUrls();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);
        }
    }
}
