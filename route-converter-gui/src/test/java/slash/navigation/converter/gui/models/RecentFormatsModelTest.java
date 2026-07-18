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
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gui.models.InMemoryPreferences;
import slash.navigation.kml.Kml22Format;
import slash.navigation.tcx.Tcx2Format;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class RecentFormatsModelTest {
    private static final int LIMIT = 5;
    private static final String MAXIMUM_RECENT_FORMAT_COUNT_PREFERENCE = "maximumRecentFormatCount";

    private final NavigationFormatRegistry registry = new NavigationFormatRegistry();
    private RecentFormatsModel recentFormatsModel;

    @Before
    public void setUp() {
        // In-memory preferences isolate the test from the shared user store: parallel
        // surefire forks all write java.util.prefs through the same OS backing store,
        // which can clobber unflushed changes (e.g. "expected 4 but was 3" in testLimit).
        Preferences preferences = new InMemoryPreferences();
        preferences.putInt(MAXIMUM_RECENT_FORMAT_COUNT_PREFERENCE, LIMIT);
        recentFormatsModel = new RecentFormatsModel(registry, preferences);
    }

    @Test
    public void testAddFormat() {
        NavigationFormat format = new Gpx11Format();
        recentFormatsModel.addFormat(format);
        assertEquals(singletonList(format), recentFormatsModel.getFormats());
    }

    @Test
    public void testAddExistingFormat() {
        NavigationFormat format = new Gpx11Format();
        recentFormatsModel.addFormat(format);
        recentFormatsModel.addFormat(format);
        recentFormatsModel.addFormat(format);
        assertEquals(singletonList(format), recentFormatsModel.getFormats());
    }

    @Test
    public void testReadExistingFormatBug() {
        NavigationFormat first = new Gpx11Format();
        NavigationFormat second = new Kml22Format();
        NavigationFormat third = new Tcx2Format();
        recentFormatsModel.addFormat(first);
        recentFormatsModel.addFormat(second);
        recentFormatsModel.addFormat(first);
        recentFormatsModel.addFormat(third);
        assertEquals(asList(third, first, second), recentFormatsModel.getFormats());
    }

    @Test
    public void testLatestFirst() {
        List<NavigationFormat> expected = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            NavigationFormat format = registry.getWriteFormats().get(i);
            recentFormatsModel.addFormat(format);
            expected.add(0, format);
            assertEquals(expected, recentFormatsModel.getFormats());
        }
    }

    @Test
    public void testLimit() {
        List<NavigationFormat> collected = new ArrayList<>();
        for (int i = 0; i < 2 * LIMIT; i++) {
            NavigationFormat format = registry.getWriteFormats().get(i);
            recentFormatsModel.addFormat(format);
            collected.add(0, format);
            List<NavigationFormat> expected = collected.subList(0, min(i + 1, LIMIT));
            List<NavigationFormat> actual = recentFormatsModel.getFormats();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);
        }
    }
}
