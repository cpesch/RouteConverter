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
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static slash.navigation.converter.gui.dnd.PositionSelection.POSITION_FLAVOR;
import static slash.navigation.converter.gui.dnd.PositionSelection.STRING_FLAVOR;

/**
 * Tests for {@link PositionSelection}.
 *
 * @author Christian Pesch
 */
public class PositionSelectionTest {

    private static PositionSelection selection() {
        List<NavigationPosition> positions = asList(
                new Wgs84Position(1.0, 2.0, null, null, null, "A"),
                new Wgs84Position(3.0, 4.0, null, null, null, "B"));
        return new PositionSelection(positions);
    }

    @Test
    public void copiesInputPositions() {
        NavigationPosition original = new Wgs84Position(1.0, 2.0, null, null, null, "A");
        PositionSelection sut = new PositionSelection(List.of(original));

        assertEquals(1, sut.getPositions().size());
        assertNotSame(original, sut.getPositions().get(0));
    }

    @Test
    public void offersPositionAndStringFlavors() {
        DataFlavor[] flavors = selection().getTransferDataFlavors();

        assertArrayEquals(new DataFlavor[]{POSITION_FLAVOR, STRING_FLAVOR}, flavors);
    }

    @Test
    public void supportsOnlyItsOwnFlavors() {
        PositionSelection sut = selection();

        assertTrue(sut.isDataFlavorSupported(POSITION_FLAVOR));
        assertTrue(sut.isDataFlavorSupported(STRING_FLAVOR));
        assertFalse(sut.isDataFlavorSupported(DataFlavor.imageFlavor));
    }

    @Test
    public void positionFlavorReturnsTheSelectionItself() throws Exception {
        PositionSelection sut = selection();

        assertSame(sut, sut.getTransferData(POSITION_FLAVOR));
    }

    @Test
    public void stringFlavorReturnsNonEmptyRenderedText() throws Exception {
        Object data = selection().getTransferData(STRING_FLAVOR);

        assertTrue(data instanceof String);
        assertFalse(((String) data).isEmpty());
    }

    @Test
    public void unsupportedFlavorThrows() {
        PositionSelection sut = selection();

        assertThrows(UnsupportedFlavorException.class, () -> sut.getTransferData(DataFlavor.imageFlavor));
    }
}
