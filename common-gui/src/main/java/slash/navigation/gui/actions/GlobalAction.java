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

import slash.navigation.gui.Application;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static slash.navigation.gui.actions.ActionManager.perform;

/**
 * A global {@link Action} that maintains a map of
 * {@link ActionManager#getLocalName() local names} to local {@link Action}s.
 *
 * @author Christian Pesch
 */
class GlobalAction extends FrameAction {
    private final String globalName;
    private final Map<String, String> actionMap = new HashMap<>();

    public GlobalAction(String globalName) {
        this.globalName = globalName;
    }

    public void run() {
        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        String localName = actionManager.getLocalName();
        if (localName == null)
            throw new IllegalArgumentException("No local name set");

        String mappedName = actionMap.get(localName);
        if (mappedName == null)
            throw new IllegalArgumentException("No local action '" + localName + "' for '" + globalName + "' found");

        Action action = actionManager.get(mappedName);
        if (action == null)
            throw new IllegalArgumentException("No action '" + mappedName + "' found");

        perform(action, getEvent());
    }

    void registerLocal(String localName, String actionName) {
        String mappedName = actionMap.get(localName);
        if (mappedName != null)
            throw new IllegalArgumentException("Action '" + mappedName + "' for global '" + this.globalName +
                    "' and local '" + localName + "'already registered");

        actionMap.put(localName, actionName);
    }
}
