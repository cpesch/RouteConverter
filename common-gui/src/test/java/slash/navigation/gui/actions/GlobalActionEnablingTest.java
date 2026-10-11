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
package slash.navigation.gui.actions;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The map popup's Delete shares the global "delete" action with the position list. The list
 * disables it while no position is selected; that state must not leak into the map, where Delete
 * works on the position under the pointer -- #439.
 */
public class GlobalActionEnablingTest {
    private static final String POSITIONS = "positions";
    private static final String MAP = "map";

    private ActionManager createActionManager() {
        ActionManager actionManager = new ActionManager();
        actionManager.registerGlobal("delete");
        actionManager.registerLocal("delete", POSITIONS, "delete-position");
        actionManager.registerLocal("delete", MAP, "delete-position-map");
        return actionManager;
    }

    @Test
    public void testStateOfOneLocalNameDoesNotLeakIntoAnother() {
        ActionManager actionManager = createActionManager();
        actionManager.setLocalName(POSITIONS);
        actionManager.enableLocal("delete", POSITIONS, false);
        assertFalse(actionManager.get("delete").isEnabled());

        actionManager.setLocalName(MAP);
        assertTrue(actionManager.get("delete").isEnabled());
    }

    @Test
    public void testStateIsRestoredWhenSwitchingBack() {
        ActionManager actionManager = createActionManager();
        actionManager.setLocalName(POSITIONS);
        actionManager.enableLocal("delete", POSITIONS, false);
        actionManager.setLocalName(MAP);

        actionManager.setLocalName(POSITIONS);
        assertFalse(actionManager.get("delete").isEnabled());
    }

    @Test
    public void testStateChangedWhileAnotherLocalNameIsActiveIsApplied() {
        ActionManager actionManager = createActionManager();
        actionManager.setLocalName(MAP);
        // selecting a position on the map updates the position list's state in the background
        actionManager.enableLocal("delete", POSITIONS, true);
        actionManager.enableLocal("delete", POSITIONS, false);
        assertTrue(actionManager.get("delete").isEnabled());

        actionManager.setLocalName(POSITIONS);
        assertFalse(actionManager.get("delete").isEnabled());
    }
}
