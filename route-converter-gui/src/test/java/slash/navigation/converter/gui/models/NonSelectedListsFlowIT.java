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

import org.junit.Test;
import slash.navigation.base.*;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Guards the {@link PositionListsModel} contract that MapsforgeMapView's gray
 * non-selected-list rendering is driven by: {@link PositionListsModel#getRoutes()} /
 * {@link PositionListsModel#getSelectedRoute()} stay consistent, and the ListDataEvents
 * that trigger a gray-set rebuild fire on load and on selection switch. It does not
 * exercise the mapsforge rendering itself (no map view is instantiated here).
 */
public class NonSelectedListsFlowIT {

    private static List<BaseRoute> graySet(PositionListsModel model) {
        List<BaseRoute> result = new ArrayList<>();
        BaseRoute selected = model.getSelectedRoute();
        for (BaseRoute route : model.getRoutes())
            if (route != selected)
                result.add(route);
        return result;
    }

    @Test
    public void grayListsAreAllRoutesExceptSelectedAndFollowSwitches() throws Exception {
        PositionsModel positionsModel = new PositionsModelImpl(new PositionsModelCallback() {
            public String getStringAt(slash.navigation.common.NavigationPosition p, int c) { return null; }
            public void setValueAt(slash.navigation.common.NavigationPosition p, int c, Object v) { }
        });
        CharacteristicsModel characteristicsModel = new CharacteristicsModel();
        FormatAndRoutesModel model = new FormatAndRoutesModelImpl(positionsModel, characteristicsModel);

        final int[] contentsChanged = {0}, intervalAdded = {0};
        model.addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e) { intervalAdded[0]++; }
            public void intervalRemoved(ListDataEvent e) { }
            public void contentsChanged(ListDataEvent e) { contentsChanged[0]++; }
        });

        NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());
        ParserResult result = parser.read(new File("../navigation-formats-samples/src/test/from.gpx"));
        assertTrue("parse failed", result.isSuccessful());

        List<BaseRoute> all = result.getAllRoutes();
        assertTrue("expected a multi-list file with at least 3 lists, got " + all.size(), all.size() >= 3);

        model.setRoutes(new FormatAndRoutes(result.getFormat(), all));

        // load fired an interval-added event (map rebuilds the gray set from it)
        assertTrue("load did not fire a list event", intervalAdded[0] >= 1);

        // first list is selected; every other list is the gray set
        assertEquals(all.get(0), model.getSelectedRoute());
        List<BaseRoute> gray = graySet(model);
        assertEquals(all.size() - 1, gray.size());
        assertFalse(gray.contains(model.getSelectedRoute()));

        // switch selection -> previously selected joins gray, newly selected leaves it
        contentsChanged[0] = 0;
        model.setSelectedRoute(all.get(2));
        assertTrue("switch did not fire contentsChanged", contentsChanged[0] >= 1);
        assertEquals(all.get(2), model.getSelectedRoute());
        gray = graySet(model);
        assertEquals(all.size() - 1, gray.size());
        assertTrue("previously selected list must now be gray", gray.contains(all.get(0)));
        assertFalse("newly selected list must not be gray", gray.contains(all.get(2)));
    }
}
