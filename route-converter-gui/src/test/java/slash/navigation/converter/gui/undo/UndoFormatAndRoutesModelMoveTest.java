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

package slash.navigation.converter.gui.undo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.FormatAndRoutes;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.FormatAndRoutesModelImpl;
import slash.navigation.converter.gui.models.PositionsModelCallback;
import slash.navigation.converter.gui.models.PositionsModelImpl;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gui.undo.UndoManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;

/**
 * Moving position lists reorders them, keeps the selected list and can be undone (#400).
 */
public class UndoFormatAndRoutesModelMoveTest {
    private UndoManager undoManager;
    private UndoFormatAndRoutesModel model;
    private BaseRoute<?, ?> b;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setUp() {
        PositionsModelImpl positionsModel = new PositionsModelImpl(new PositionsModelCallback() {
            public String getStringAt(NavigationPosition position, int columnIndex) {
                return null;
            }

            public boolean setValueAt(NavigationPosition position, int columnIndex, Object value) {
                return true;
            }
        });
        undoManager = new UndoManager();
        model = new UndoFormatAndRoutesModel(undoManager,
                new FormatAndRoutesModelImpl(positionsModel, new CharacteristicsModel()));

        Gpx11Format format = new Gpx11Format();
        List routes = new ArrayList();
        routes.add(format.createRoute(Route, "A", new ArrayList<>()));
        routes.add(format.createRoute(Track, "B", new ArrayList<>()));
        routes.add(format.createRoute(Route, "C", new ArrayList<>()));
        routes.add(format.createRoute(Track, "D", new ArrayList<>()));
        model.setRoutes(new FormatAndRoutes(format, routes));
        b = model.getRoute(1);
        model.setSelectedRoute(b, false);
        model.setModified(false);
    }

    private String names() {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < model.getSize(); i++)
            buffer.append(model.getRoute(i).getName());
        return buffer.toString();
    }

    @Test
    public void testMoveKeepsTheSelectedListAndMarksModified() {
        model.movePositionLists(new int[]{1, 0, 2, 3});
        assertEquals("BACD", names());
        assertEquals(b, model.getSelectedRoute());
        assertTrue(model.isModified());
    }

    @Test
    public void testUndoAndRedo() {
        model.movePositionLists(new int[]{2, 3, 0, 1});
        assertEquals("CDAB", names());
        undoManager.undo();
        assertEquals("ABCD", names());
        undoManager.redo();
        assertEquals("CDAB", names());
        assertEquals(b, model.getSelectedRoute());
    }

    @Test
    public void testRejectsNoPermutation() {
        assertRejected(new int[]{0, 0, 1, 2});
        assertRejected(new int[]{0, 1, 2});
        assertRejected(new int[]{0, 1, 2, 4});
        assertEquals("ABCD", names());
        assertFalse(undoManager.canUndo());
    }

    private void assertRejected(int[] order) {
        try {
            model.movePositionLists(order);
            fail("Accepted " + java.util.Arrays.toString(order));
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
