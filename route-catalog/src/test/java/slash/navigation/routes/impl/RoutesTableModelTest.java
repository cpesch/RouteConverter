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
package slash.navigation.routes.impl;

import org.junit.Test;
import slash.navigation.routes.Category;
import slash.navigation.routes.Route;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static slash.navigation.routes.impl.RoutesTableModel.COLUMN_COUNT;
import static slash.navigation.routes.impl.RoutesTableModel.CREATOR_COLUMN;
import static slash.navigation.routes.impl.RoutesTableModel.DURATION_COLUMN;
import static slash.navigation.routes.impl.RoutesTableModel.LENGTH_COLUMN;
import static slash.navigation.routes.impl.RoutesTableModel.NAME_COLUMN;

/**
 * Unit tests for {@link RoutesTableModel}.
 *
 * @author Christian Pesch
 */
public class RoutesTableModelTest {

    private static Route route(String name) {
        return new Route() {
            public String getHref() { return "http://example.com/" + name; }
            public String getName() { return name; }
            public String getDescription() { return name; }
            public String getCreator() { return "tester"; }
            public String getUrl() { return getHref(); }
            public void update(Category parent, String description) {}
            public void delete() {}
        };
    }

    private static RouteModel routeModel(String name) {
        return new RouteModel(null, route(name));
    }

    @Test
    public void hasFourColumns() {
        RoutesTableModel model = new RoutesTableModel();
        assertEquals(4, COLUMN_COUNT);
        assertEquals(COLUMN_COUNT, model.getColumnCount());
        assertEquals(0, NAME_COLUMN);
        assertEquals(1, CREATOR_COLUMN);
        assertEquals(2, LENGTH_COLUMN);
        assertEquals(3, DURATION_COLUMN);
    }

    @Test
    public void valueAtReturnsRouteModelForAllColumns() {
        RoutesTableModel model = new RoutesTableModel();
        RouteModel route = routeModel("a route");
        model.setRoutes(new ArrayList<>(List.of(route)));

        for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++)
            assertSame(route, model.getValueAt(0, columnIndex));
    }

    @Test
    public void updateRouteFiresSingleRowUpdate() {
        RoutesTableModel model = new RoutesTableModel();
        RouteModel first = routeModel("first");
        RouteModel second = routeModel("second");
        RouteModel third = routeModel("third");
        model.setRoutes(new ArrayList<>(asList(first, second, third)));

        List<TableModelEvent> events = new ArrayList<>();
        TableModelListener listener = events::add;
        model.addTableModelListener(listener);

        model.updateRoute(second);

        assertEquals(1, events.size());
        TableModelEvent event = events.get(0);
        assertEquals(TableModelEvent.UPDATE, event.getType());
        assertEquals(1, event.getFirstRow());
        assertEquals(1, event.getLastRow());
    }

    @Test
    public void updateUnknownRouteThrows() {
        RoutesTableModel model = new RoutesTableModel();
        model.setRoutes(new ArrayList<>(List.of(routeModel("known"))));
        try {
            model.updateRoute(routeModel("unknown"));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
