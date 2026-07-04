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

import org.junit.Test;
import slash.navigation.routes.impl.RouteModel;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static slash.navigation.converter.gui.dnd.RouteSelection.routeFlavor;

/**
 * Tests for {@link RouteSelection}.
 *
 * @author Christian Pesch
 */
public class RouteSelectionTest {
    private final List<RouteModel> routes = List.of(mock(RouteModel.class));
    private final RouteSelection sut = new RouteSelection(routes);

    @Test
    public void offersOnlyTheRouteFlavor() {
        assertArrayEquals(new DataFlavor[]{routeFlavor}, sut.getTransferDataFlavors());
    }

    @Test
    public void supportsOnlyTheRouteFlavor() {
        assertTrue(sut.isDataFlavorSupported(routeFlavor));
        assertFalse(sut.isDataFlavorSupported(DataFlavor.stringFlavor));
    }

    @Test
    public void returnsTheRouteListForItsFlavor() throws Exception {
        assertSame(routes, sut.getTransferData(routeFlavor));
    }

    @Test
    public void unsupportedFlavorThrows() {
        assertThrows(UnsupportedFlavorException.class, () -> sut.getTransferData(DataFlavor.stringFlavor));
    }
}
