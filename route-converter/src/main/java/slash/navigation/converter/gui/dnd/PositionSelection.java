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

package slash.navigation.converter.gui.dnd;

import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.SimpleRoute;
import slash.navigation.common.NavigationPosition;
import slash.navigation.simple.GlopusFormat;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static slash.navigation.base.NavigationFormatConverter.convertPositions;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Acts as a container for drag and drop operations with {@link BaseNavigationPosition}s.
 *
 * @author Christian Pesch
 */

public class PositionSelection implements Transferable {
    private static final Logger log = Logger.getLogger(PositionSelection.class.getName());
    public static final DataFlavor POSITION_FLAVOR = new DataFlavor(PositionSelection.class, "List of Positions");
    public static final DataFlavor STRING_FLAVOR = DataFlavor.stringFlavor;

    private final List<NavigationPosition> positions;
    private final BaseNavigationFormat format;
    private final String string;

    public PositionSelection(List<NavigationPosition> positions, BaseNavigationFormat format) {
        this.positions = positions;
        this.format = format;
        this.string = createStringFor(positions);
    }

    private String createStringFor(List<NavigationPosition> sourcePositions) {
        GlopusFormat targetFormat = new GlopusFormat();
        List<BaseNavigationPosition> targetPositions = new ArrayList<>();
        try {
            targetPositions = convertPositions(sourcePositions, targetFormat);
        } catch (IOException e) {
            log.severe("Cannot convert " + sourcePositions + " for selection: " + e);
        }
        SimpleRoute targetRoute = targetFormat.createRoute(Waypoints, null, targetPositions);

        StringWriter writer = new StringWriter();
        targetFormat.write(targetRoute, new PrintWriter(writer), 0, targetPositions.size());
        return writer.toString();
    }

    public List<NavigationPosition> getPositions() {
        return positions;
    }

    public BaseNavigationFormat getFormat() {
        return format;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{POSITION_FLAVOR, STRING_FLAVOR};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (DataFlavor f : getTransferDataFlavors()) {
            if (f.equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (POSITION_FLAVOR.equals(flavor))
            return this;
        if (STRING_FLAVOR.equals(flavor))
            return string;
        throw new UnsupportedFlavorException(flavor);
    }
}