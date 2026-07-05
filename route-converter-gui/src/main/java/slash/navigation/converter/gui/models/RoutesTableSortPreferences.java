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

import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import java.util.List;
import java.util.prefs.Preferences;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.swing.SortOrder.ASCENDING;
import static slash.navigation.converter.gui.models.LocalActionConstants.ROUTES;

/**
 * Persists the sort column and direction of the browse routes table across restarts,
 * using the same {@link Preferences} mechanism {@link AbstractTableColumnModel} uses for
 * its {@code -visible-}/{@code -order-} keys, with new {@code -sort-column-}/{@code -sort-order-}
 * keys.
 *
 * @author Christian Pesch
 */

public class RoutesTableSortPreferences {
    private static final String SORT_COLUMN_KEY = "-sort-column";
    private static final String SORT_ORDER_KEY = "-sort-order";

    private final Preferences preferences;
    private final String preferencesPrefix;

    public RoutesTableSortPreferences() {
        this(Preferences.userNodeForPackage(RoutesTableSortPreferences.class), ROUTES);
    }

    RoutesTableSortPreferences(Preferences preferences, String preferencesPrefix) {
        this.preferences = preferences;
        this.preferencesPrefix = preferencesPrefix;
    }

    private String createSortColumnKey() {
        return preferencesPrefix + SORT_COLUMN_KEY;
    }

    private String createSortOrderKey() {
        return preferencesPrefix + SORT_ORDER_KEY;
    }

    /**
     * Returns the persisted sort keys to restore on startup, or an empty list if none
     * were persisted (or the stored order is unreadable).
     */
    public List<SortKey> loadSortKeys() {
        int column = preferences.getInt(createSortColumnKey(), -1);
        if (column < 0)
            return emptyList();

        SortOrder sortOrder;
        try {
            sortOrder = SortOrder.valueOf(preferences.get(createSortOrderKey(), ASCENDING.name()));
        } catch (IllegalArgumentException e) {
            sortOrder = ASCENDING;
        }
        if (sortOrder == SortOrder.UNSORTED)
            return emptyList();
        return singletonList(new SortKey(column, sortOrder));
    }

    /**
     * Persists the primary sort key (column and direction), or clears the persisted sort
     * when there is none or it is unsorted.
     */
    public void saveSortKeys(List<? extends SortKey> sortKeys) {
        SortKey sortKey = sortKeys.isEmpty() ? null : sortKeys.get(0);
        if (sortKey == null || sortKey.getSortOrder() == SortOrder.UNSORTED) {
            preferences.remove(createSortColumnKey());
            preferences.remove(createSortOrderKey());
            return;
        }
        preferences.putInt(createSortColumnKey(), sortKey.getColumn());
        preferences.put(createSortOrderKey(), sortKey.getSortOrder().name());
    }
}
