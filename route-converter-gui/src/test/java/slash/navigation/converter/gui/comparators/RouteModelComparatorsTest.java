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

package slash.navigation.converter.gui.comparators;

import org.junit.Test;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.converter.gui.models.RouteMetadataSource;
import slash.navigation.routes.Category;
import slash.navigation.routes.Route;
import slash.navigation.routes.impl.RouteModel;
import slash.navigation.routes.impl.RoutesTableModel;

import javax.swing.JTable;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.navigation.converter.gui.comparators.RouteModelComparators.byCreator;
import static slash.navigation.converter.gui.comparators.RouteModelComparators.byDistance;
import static slash.navigation.converter.gui.comparators.RouteModelComparators.byDuration;
import static slash.navigation.converter.gui.comparators.RouteModelComparators.byName;
import static slash.navigation.routes.impl.RoutesTableModel.CREATOR_COLUMN;
import static slash.navigation.routes.impl.RoutesTableModel.DURATION_COLUMN;
import static slash.navigation.routes.impl.RoutesTableModel.LENGTH_COLUMN;
import static slash.navigation.routes.impl.RoutesTableModel.NAME_COLUMN;

/**
 * Unit tests for {@link RouteModelComparators} and their use in a {@link TableRowSorter}.
 *
 * @author Christian Pesch
 */
public class RouteModelComparatorsTest {

    private static Route route(String url, String description, String creator) {
        return new Route() {
            public String getHref() { return url; }
            public String getName() { return description; }
            public String getDescription() { return description; }
            public String getCreator() { return creator; }
            public String getUrl() { return url; }
            public void update(Category parent, String description) {}
            public void delete() {}
        };
    }

    private static RouteModel model(String url, String description, String creator) {
        return new RouteModel(null, route(url, description, creator));
    }

    /**
     * A {@link RouteMetadataSource} backed by a map keyed by URL, like the caches wired in the panel.
     */
    private static RouteMetadataSource source(Map<String, DistanceAndTime> values) {
        return values::get;
    }

    // ---- byName / byCreator ----

    @Test
    public void nameSortsCaseInsensitively() {
        assertTrue(byName().compare(model("1", "apple", "x"), model("2", "Banana", "x")) < 0);
        assertTrue(byName().compare(model("1", "Banana", "x"), model("2", "apple", "x")) > 0);
        assertEquals(0, byName().compare(model("1", "Apple", "x"), model("2", "apple", "y")));
    }

    @Test
    public void creatorSortsCaseInsensitively() {
        assertTrue(byCreator().compare(model("1", "n", "alice"), model("2", "n", "Bob")) < 0);
        assertEquals(0, byCreator().compare(model("1", "n", "Bob"), model("2", "n", "bob")));
    }

    @Test
    public void nullNameSortsLast() {
        assertTrue(byName().compare(model("1", "anything", "x"), model("2", null, "x")) < 0);
        assertTrue(byName().compare(model("1", null, "x"), model("2", "anything", "x")) > 0);
    }

    // ---- byDistance ----

    @Test
    public void distanceSortsNumerically() {
        Map<String, DistanceAndTime> values = new HashMap<>();
        values.put("short", new DistanceAndTime(1000.0, null));
        values.put("long", new DistanceAndTime(50000.0, null));
        RouteMetadataSource source = source(values);
        assertTrue(byDistance(source).compare(model("short", "a", "x"), model("long", "b", "x")) < 0);
        assertTrue(byDistance(source).compare(model("long", "a", "x"), model("short", "b", "x")) > 0);
    }

    @Test
    public void missingDistanceSortsLast() {
        Map<String, DistanceAndTime> values = new HashMap<>();
        values.put("known", new DistanceAndTime(1000.0, null));
        RouteMetadataSource source = source(values);
        // unknown url -> null metadata; zero distance -> also treated as missing
        assertTrue(byDistance(source).compare(model("known", "a", "x"), model("unknown", "b", "x")) < 0);
        assertTrue(byDistance(source).compare(model("unknown", "a", "x"), model("known", "b", "x")) > 0);
        assertTrue(byDistance(source).compare(model("known", "a", "x"),
                model("zero", "b", "x")) < 0); // zero also missing (source returns null)
    }

    // ---- byDuration ----

    @Test
    public void durationSortsNumerically() {
        Map<String, DistanceAndTime> values = new HashMap<>();
        values.put("brief", new DistanceAndTime(null, 60_000L));
        values.put("epic", new DistanceAndTime(null, 3_600_000L));
        RouteMetadataSource source = source(values);
        assertTrue(byDuration(source).compare(model("brief", "a", "x"), model("epic", "b", "x")) < 0);
        assertTrue(byDuration(source).compare(model("epic", "a", "x"), model("brief", "b", "x")) > 0);
    }

    @Test
    public void missingDurationSortsLast() {
        Map<String, DistanceAndTime> values = new HashMap<>();
        values.put("known", new DistanceAndTime(null, 60_000L));
        RouteMetadataSource source = source(values);
        assertTrue(byDuration(source).compare(model("known", "a", "x"), model("unknown", "b", "x")) < 0);
        assertTrue(byDuration(source).compare(model("unknown", "a", "x"), model("known", "b", "x")) > 0);
    }

    // ---- TableRowSorter integration: header-click sorting + row-update path survival ----

    private static TableRowSorter<RoutesTableModel> sorter(RoutesTableModel model, RouteMetadataSource source) {
        TableRowSorter<RoutesTableModel> rowSorter = new TableRowSorter<>(model);
        rowSorter.setSortsOnUpdates(true);
        rowSorter.setComparator(NAME_COLUMN, byName());
        rowSorter.setComparator(CREATOR_COLUMN, byCreator());
        rowSorter.setComparator(LENGTH_COLUMN, byDistance(source));
        rowSorter.setComparator(DURATION_COLUMN, byDuration(source));
        return rowSorter;
    }

    private static List<String> viewDescriptions(JTable table) {
        RoutesTableModel model = (RoutesTableModel) table.getModel();
        List<String> result = new ArrayList<>();
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++)
            result.add(model.getRoute(table.convertRowIndexToModel(viewRow)).getDescription());
        return result;
    }

    @Test
    public void sortByNameProducesAlphabeticalViewOrder() {
        RoutesTableModel model = new RoutesTableModel();
        model.setRoutes(new ArrayList<>(List.of(
                model("1", "Zebra", "x"), model("2", "apple", "x"), model("3", "Mango", "x"))));
        JTable table = new JTable(model);
        table.setRowSorter(sorter(model, source(new HashMap<>())));

        table.getRowSorter().setSortKeys(singletonList(new SortKey(NAME_COLUMN, SortOrder.ASCENDING)));

        assertEquals(List.of("apple", "Mango", "Zebra"), viewDescriptions(table));
    }

    @Test
    public void sortByLengthPutsMissingLastAndSurvivesMetadataFill() {
        Map<String, DistanceAndTime> values = new HashMap<>();
        values.put("a", new DistanceAndTime(5000.0, null));
        // "b" and "c" have no metadata yet -> sort last
        RouteMetadataSource source = source(values);

        RoutesTableModel model = new RoutesTableModel();
        RouteModel a = model("a", "A", "x");
        RouteModel b = model("b", "B", "x");
        RouteModel c = model("c", "C", "x");
        model.setRoutes(new ArrayList<>(List.of(a, b, c)));
        JTable table = new JTable(model);
        table.setRowSorter(sorter(model, source));
        table.getRowSorter().setSortKeys(singletonList(new SortKey(LENGTH_COLUMN, SortOrder.ASCENDING)));

        // only "a" has a length -> it is first, the two missing follow
        assertEquals("A", model.getRoute(table.convertRowIndexToModel(0)).getDescription());

        // metadata fills in for "c" (shorter than "a"); the JTable forwards the
        // fireTableRowsUpdated to the sorter (setSortsOnUpdates) which must re-sort
        values.put("c", new DistanceAndTime(1000.0, null));
        model.updateRoute(c);

        assertEquals(List.of("C", "A", "B"), viewDescriptions(table));
    }

    @Test
    public void descendingSortRanksMissingLengthFirst() {
        Map<String, DistanceAndTime> values = new HashMap<>();
        values.put("a", new DistanceAndTime(5000.0, null));
        values.put("b", new DistanceAndTime(1000.0, null));
        // "c" has no metadata -> ranks as the largest value
        RouteMetadataSource source = source(values);

        RoutesTableModel model = new RoutesTableModel();
        model.setRoutes(new ArrayList<>(List.of(
                model("a", "A", "x"), model("b", "B", "x"), model("c", "C", "x"))));
        JTable table = new JTable(model);
        table.setRowSorter(sorter(model, source));

        // TableRowSorter negates the comparator for DESCENDING, so the missing value
        // (ranked as the maximum) sorts first - the documented, intended semantic
        table.getRowSorter().setSortKeys(singletonList(new SortKey(LENGTH_COLUMN, SortOrder.DESCENDING)));

        assertEquals(List.of("C", "A", "B"), viewDescriptions(table));
    }
}
