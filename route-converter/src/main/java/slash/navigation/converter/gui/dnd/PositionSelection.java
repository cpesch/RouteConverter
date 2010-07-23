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

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleRoute;
import slash.navigation.nmn.NavigatingPoiWarnerFormat;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Acts as a container for drag and drop operations with {@link BaseNavigationPosition}s.
 *
 * @author Christian Pesch
 */

public class PositionSelection implements Transferable {
    public static final DataFlavor positionFlavor = new DataFlavor(PositionSelection.class, "List of Positions");
    public static final DataFlavor stringFlavor = DataFlavor.stringFlavor;

    private final List<BaseNavigationPosition> positions;
    private final String string;

    public PositionSelection(List<BaseNavigationPosition> positions) {
        this.positions = positions;
        this.string = createStringFor(positions);
    }

    private String createStringFor(List<BaseNavigationPosition> positions) {
        NavigatingPoiWarnerFormat format = new NavigatingPoiWarnerFormat();
        SimpleRoute route = format.createRoute(RouteCharacteristics.Waypoints, null, positions);
        StringWriter writer = new StringWriter();
        format.write(route, new PrintWriter(writer), 0, positions.size());
        return writer.toString();
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{positionFlavor, stringFlavor};
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
        if (positionFlavor.equals(flavor))
            return positions;
        if (stringFlavor.equals(flavor))
            return string;
        throw new UnsupportedFlavorException(flavor);
    }
}