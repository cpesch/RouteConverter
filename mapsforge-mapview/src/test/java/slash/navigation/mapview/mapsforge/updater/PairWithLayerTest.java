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
package slash.navigation.mapview.mapsforge.updater;

import org.junit.Test;
import org.mapsforge.map.layer.Layer;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PairWithLayer}.
 *
 * @author Christian Pesch
 */
public class PairWithLayerTest {
    private final NavigationPosition first = new SimpleNavigationPosition(1.0, 2.0);
    private final NavigationPosition second = new SimpleNavigationPosition(3.0, 4.0);
    private final NavigationPosition noCoordinates = new SimpleNavigationPosition(null, null);

    @Test
    public void exposesConstructorArgumentsAndRowIsMutable() {
        PairWithLayer sut = new PairWithLayer(first, second, 7);

        assertSame(first, sut.getFirst());
        assertSame(second, sut.getSecond());
        assertEquals(7, sut.getRow());

        sut.setRow(9);
        assertEquals(9, sut.getRow());
    }

    @Test
    public void hasCoordinatesOnlyWhenBothEndsDo() {
        assertTrue(new PairWithLayer(first, second, 0).hasCoordinates());
        assertFalse(new PairWithLayer(first, noCoordinates, 0).hasCoordinates());
        assertFalse(new PairWithLayer(noCoordinates, second, 0).hasCoordinates());
    }

    @Test
    public void layerAndDistanceAndTimeAreMutableAndInitiallyNull() {
        PairWithLayer sut = new PairWithLayer(first, second, 0);
        assertNull(sut.getLayer());
        assertNull(sut.getDistanceAndTime());

        Layer layer = mock(Layer.class);
        sut.setLayer(layer);
        sut.setDistanceAndTime(DistanceAndTime.ZERO);

        assertSame(layer, sut.getLayer());
        assertEquals(DistanceAndTime.ZERO, sut.getDistanceAndTime());
    }

    @Test
    public void equalityIsByFirstAndSecondOnly() {
        PairWithLayer one = new PairWithLayer(first, second, 0);
        PairWithLayer sameEndsDifferentRowAndLayer = new PairWithLayer(first, second, 99);
        sameEndsDifferentRowAndLayer.setLayer(mock(Layer.class));

        assertEquals(one, sameEndsDifferentRowAndLayer);
        assertEquals(one.hashCode(), sameEndsDifferentRowAndLayer.hashCode());

        assertNotEquals(one, new PairWithLayer(first, noCoordinates, 0));
        assertNotEquals(one, new PairWithLayer(noCoordinates, second, 0));
    }

    @Test
    public void notEqualToNullOrOtherType() {
        PairWithLayer sut = new PairWithLayer(first, second, 0);

        assertNotEquals(sut, null);
        assertNotEquals(sut, "not a pair");
    }

    @Test
    public void toStringMentionsEndsAndRow() {
        String text = new PairWithLayer(first, second, 5).toString();

        assertTrue(text.contains("PairWithLayer"));
        assertTrue(text.contains("row=5"));
    }
}
