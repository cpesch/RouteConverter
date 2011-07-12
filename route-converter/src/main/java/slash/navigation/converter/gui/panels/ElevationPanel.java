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

package slash.navigation.converter.gui.panels;

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.dnd.PanelDropHandler;
import slash.navigation.converter.gui.elevationview.ElevationView;

import javax.swing.*;
import java.awt.*;

/**
 * The elevation panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class ElevationPanel {
    protected JPanel elevationPanel;
    private ElevationView elevationView;

    public ElevationPanel() {
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        elevationView = new ElevationView(r.getPositionsModel(), r.getPositionsSelectionModel());
        elevationPanel = new JPanel(new BorderLayout());
        elevationPanel.add(elevationView.getComponent(), BorderLayout.CENTER);
        elevationPanel.setTransferHandler(new PanelDropHandler());
    }

    public Component getRootComponent() {
        return elevationPanel;
    }

    public void setSelectedPositions(int[] selectedPositions, boolean replaceSelection) {
        elevationView.setSelectedPositions(selectedPositions, replaceSelection);
    }

    public void print() {
        elevationView.print();
    }
}
