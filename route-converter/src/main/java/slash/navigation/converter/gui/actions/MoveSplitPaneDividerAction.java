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

package slash.navigation.converter.gui.actions;

import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;

/**
 * {@link Action} that moves the divider location of a given split pane.
 *
 * @author Christian Pesch
 */

public class MoveSplitPaneDividerAction extends FrameAction {
    private JSplitPane splitPane;
    private int dividerLocation;

    public MoveSplitPaneDividerAction(JSplitPane splitPane, int dividerLocation) {
        this.splitPane = splitPane;
        this.dividerLocation = dividerLocation;
    }

    public void run() {
        splitPane.setDividerLocation(dividerLocation);
    }
}
