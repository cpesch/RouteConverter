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

import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.navigation.routes.impl.RoutesTableModel.DURATION_COLUMN;
import static slash.navigation.routes.impl.RoutesTableModel.LENGTH_COLUMN;

/**
 * Unit tests for the {@link Preferences} round-trip of {@link RoutesTableSortPreferences}.
 * Backed by an in-memory {@link Preferences} node so the test is deterministic and does not
 * depend on the platform backing store (which flushes asynchronously on macOS).
 *
 * @author Christian Pesch
 */
public class RoutesTableSortPreferencesTest {
    private Preferences node;
    private RoutesTableSortPreferences preferences;

    @Before
    public void setUp() {
        node = new InMemoryPreferences(null, "");
        preferences = new RoutesTableSortPreferences(node, "routes");
    }

    /**
     * A minimal synchronous, in-memory {@link Preferences} implementation for the test.
     */
    private static class InMemoryPreferences extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, InMemoryPreferences> children = new HashMap<>();

        InMemoryPreferences(AbstractPreferences parent, String name) {
            super(parent, name);
        }

        protected void putSpi(String key, String value) {
            values.put(key, value);
        }

        protected String getSpi(String key) {
            return values.get(key);
        }

        protected void removeSpi(String key) {
            values.remove(key);
        }

        protected void removeNodeSpi() {
            values.clear();
            children.clear();
        }

        protected String[] keysSpi() {
            return values.keySet().toArray(new String[0]);
        }

        protected String[] childrenNamesSpi() {
            return children.keySet().toArray(new String[0]);
        }

        protected AbstractPreferences childSpi(String name) {
            return children.computeIfAbsent(name, n -> new InMemoryPreferences(this, n));
        }

        protected void syncSpi() {
        }

        protected void flushSpi() {
        }
    }

    @Test
    public void noPersistedSortLoadsEmpty() {
        assertTrue(preferences.loadSortKeys().isEmpty());
    }

    @Test
    public void savesAndRestoresColumnAndDirection() {
        preferences.saveSortKeys(singletonList(new SortKey(LENGTH_COLUMN, SortOrder.DESCENDING)));

        List<SortKey> restored = preferences.loadSortKeys();
        assertEquals(1, restored.size());
        assertEquals(LENGTH_COLUMN, restored.get(0).getColumn());
        assertEquals(SortOrder.DESCENDING, restored.get(0).getSortOrder());
    }

    @Test
    public void restoreSurvivesANewInstanceLikeARestart() {
        preferences.saveSortKeys(singletonList(new SortKey(DURATION_COLUMN, SortOrder.ASCENDING)));

        // a fresh instance on the same node models the next application start
        RoutesTableSortPreferences afterRestart = new RoutesTableSortPreferences(node, "routes");
        List<SortKey> restored = afterRestart.loadSortKeys();
        assertEquals(1, restored.size());
        assertEquals(DURATION_COLUMN, restored.get(0).getColumn());
        assertEquals(SortOrder.ASCENDING, restored.get(0).getSortOrder());
    }

    @Test
    public void savingEmptyOrUnsortedClearsThePersistedSort() {
        preferences.saveSortKeys(singletonList(new SortKey(LENGTH_COLUMN, SortOrder.ASCENDING)));
        preferences.saveSortKeys(emptyList());
        assertTrue(preferences.loadSortKeys().isEmpty());

        preferences.saveSortKeys(singletonList(new SortKey(LENGTH_COLUMN, SortOrder.ASCENDING)));
        preferences.saveSortKeys(singletonList(new SortKey(LENGTH_COLUMN, SortOrder.UNSORTED)));
        assertTrue(preferences.loadSortKeys().isEmpty());
    }
}
