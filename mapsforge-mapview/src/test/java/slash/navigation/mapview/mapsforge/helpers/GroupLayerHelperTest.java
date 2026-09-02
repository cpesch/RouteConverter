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
package slash.navigation.mapview.mapsforge.helpers;

import org.junit.Test;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.model.DisplayModel;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static slash.navigation.mapview.mapsforge.helpers.GroupLayerHelper.addToGroupLayer;
import static slash.navigation.mapview.mapsforge.helpers.GroupLayerHelper.removeFromGroupLayer;

/**
 * Tests for {@link GroupLayerHelper}.
 *
 * @author Christian Pesch
 */
public class GroupLayerHelperTest {
    private static class FakeLayer extends Layer {
        public void draw(BoundingBox boundingBox, byte zoomLevel, org.mapsforge.core.graphics.Canvas canvas,
                         Point topLeftPoint, Rotation rotation) {
        }

        public LatLong getPosition() {
            return null;
        }
    }

    private static List<Layer> createLayers(int count) {
        List<Layer> layers = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            layers.add(new FakeLayer());
        return layers;
    }

    @Test
    public void addAssignsTheDisplayModelToEveryChild() {
        GroupLayer groupLayer = new GroupLayer();
        DisplayModel displayModel = new DisplayModel();
        List<Layer> layers = createLayers(3);

        addToGroupLayer(groupLayer, displayModel, layers);

        assertEquals(3, groupLayer.layers.size());
        for (Layer layer : groupLayer.layers)
            assertSame("every child needs a display model or Layer#draw throws", displayModel, layer.getDisplayModel());
    }

    /**
     * {@link GroupLayer#setDisplayModel} only reaches the children present when it is called, which
     * is what {@link Layers#add} does once for the group. A child added afterwards is on its own.
     */
    @Test
    public void addAssignsTheDisplayModelToChildrenAddedAfterTheGroupWasRegistered() {
        GroupLayer groupLayer = new GroupLayer();
        DisplayModel displayModel = new DisplayModel();
        groupLayer.setDisplayModel(displayModel);

        Layer added = new FakeLayer();
        assertNull(added.getDisplayModel());

        addToGroupLayer(groupLayer, displayModel, added);

        assertSame(displayModel, added.getDisplayModel());
    }

    @Test
    public void addAppendsToTheExistingChildren() {
        GroupLayer groupLayer = new GroupLayer();
        DisplayModel displayModel = new DisplayModel();
        List<Layer> first = createLayers(2);
        List<Layer> second = createLayers(3);

        addToGroupLayer(groupLayer, displayModel, first);
        addToGroupLayer(groupLayer, displayModel, second);

        assertEquals(5, groupLayer.layers.size());
        assertTrue(groupLayer.layers.containsAll(first));
        assertTrue(groupLayer.layers.containsAll(second));
    }

    @Test
    public void addHandlesAnEmptyCollection() {
        GroupLayer groupLayer = new GroupLayer();

        addToGroupLayer(groupLayer, new DisplayModel(), emptyList());

        assertEquals(0, groupLayer.layers.size());
    }

    @Test
    public void removeTakesOutOnlyTheGivenLayers() {
        GroupLayer groupLayer = new GroupLayer();
        DisplayModel displayModel = new DisplayModel();
        List<Layer> layers = createLayers(5);
        addToGroupLayer(groupLayer, displayModel, layers);

        removeFromGroupLayer(groupLayer, asList(layers.get(1), layers.get(3)));

        assertEquals("removing a delta must not drop the rest of the group", 3, groupLayer.layers.size());
        assertTrue(groupLayer.layers.contains(layers.get(0)));
        assertTrue(groupLayer.layers.contains(layers.get(2)));
        assertTrue(groupLayer.layers.contains(layers.get(4)));
    }

    @Test
    public void removeSkipsLayersThatWereNeverRendered() {
        GroupLayer groupLayer = new GroupLayer();
        DisplayModel displayModel = new DisplayModel();
        List<Layer> layers = createLayers(2);
        addToGroupLayer(groupLayer, displayModel, layers);

        List<Layer> withNull = new ArrayList<>();
        withNull.add(layers.get(0));
        withNull.add(null);

        removeFromGroupLayer(groupLayer, withNull);

        assertEquals(1, groupLayer.layers.size());
        assertSame(layers.get(1), groupLayer.layers.get(0));
    }

    @Test
    public void removeHandlesLayersThatAreNotInTheGroup() {
        GroupLayer groupLayer = new GroupLayer();
        DisplayModel displayModel = new DisplayModel();
        List<Layer> layers = createLayers(2);
        addToGroupLayer(groupLayer, displayModel, layers);

        removeFromGroupLayer(groupLayer, createLayers(2));

        assertEquals(2, groupLayer.layers.size());
    }
}
