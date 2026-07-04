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
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PositionWithLayer}.
 *
 * @author Christian Pesch
 */
public class PositionWithLayerTest {
    private final NavigationPosition position = new SimpleNavigationPosition(1.0, 2.0);
    private final NavigationPosition noCoordinates = new SimpleNavigationPosition(null, null);

    @Test
    public void exposesPositionAndLayerIsInitiallyNull() {
        PositionWithLayer sut = new PositionWithLayer(position);

        assertSame(position, sut.getPosition());
        assertNull(sut.getLayer());
    }

    @Test
    public void hasCoordinatesMirrorsThePosition() {
        assertTrue(new PositionWithLayer(position).hasCoordinates());
        assertFalse(new PositionWithLayer(noCoordinates).hasCoordinates());
    }

    @Test
    public void layerIsMutable() {
        PositionWithLayer sut = new PositionWithLayer(position);
        Layer layer = mock(Layer.class);

        sut.setLayer(layer);

        assertSame(layer, sut.getLayer());
    }

    @Test
    public void equalityIsByPositionAndLayer() {
        Layer layer = mock(Layer.class);

        PositionWithLayer one = new PositionWithLayer(position);
        one.setLayer(layer);
        PositionWithLayer same = new PositionWithLayer(position);
        same.setLayer(layer);

        assertEquals(one, same);
        assertEquals(one.hashCode(), same.hashCode());

        PositionWithLayer differentLayer = new PositionWithLayer(position);
        differentLayer.setLayer(mock(Layer.class));
        assertNotEquals(one, differentLayer);

        PositionWithLayer differentPosition = new PositionWithLayer(noCoordinates);
        differentPosition.setLayer(layer);
        assertNotEquals(one, differentPosition);
    }

    @Test
    public void bothLayersNullStillEqualWhenSamePosition() {
        assertEquals(new PositionWithLayer(position), new PositionWithLayer(position));
    }

    @Test
    public void notEqualToNullOrOtherType() {
        PositionWithLayer sut = new PositionWithLayer(position);

        assertNotEquals(sut, null);
        assertNotEquals(sut, "not a position with layer");
    }

    @Test
    public void toStringMentionsPosition() {
        assertTrue(new PositionWithLayer(position).toString().contains("PositionWithLayer"));
    }
}
