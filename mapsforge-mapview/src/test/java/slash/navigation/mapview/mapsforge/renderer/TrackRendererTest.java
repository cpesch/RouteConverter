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
package slash.navigation.mapview.mapsforge.renderer;

import org.junit.Before;
import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.converter.gui.models.ColorModel;
import slash.navigation.gui.models.IntegerModel;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;
import slash.navigation.mapview.mapsforge.lines.Line;
import slash.navigation.mapview.mapsforge.updater.PairWithLayer;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TrackRenderer}.
 *
 * @author Christian Pesch
 */
public class TrackRendererTest {
    private GraphicFactory graphicFactory;
    private MapsforgeMapView mapView;
    private GroupLayer trackLayer;
    private TrackRenderer renderer;

    @Before
    public void setUp() {
        graphicFactory = mock(GraphicFactory.class);
        when(graphicFactory.createPaint()).thenReturn(mock(Paint.class));
        mapView = mock(MapsforgeMapView.class);
        when(mapView.getTileSize()).thenReturn(256);

        trackLayer = new GroupLayer();
        when(mapView.getTrackLayer()).thenReturn(trackLayer);

        when(mapView.asLatLong(any(NavigationPosition.class))).thenAnswer(invocation -> {
            NavigationPosition pos = invocation.getArgument(0);
            return new LatLong(pos.getLatitude(), pos.getLongitude());
        });

        IntegerModel trackLineWidthModel = mock(IntegerModel.class);
        when(trackLineWidthModel.getInteger()).thenReturn(4);
        ColorModel trackColorModel = mock(ColorModel.class);
        when(trackColorModel.getColor()).thenReturn(java.awt.Color.BLUE);

        renderer = new TrackRenderer(mapView, trackColorModel, trackLineWidthModel, graphicFactory);
    }

    private NavigationPosition createPosition(double longitude, double latitude) {
        NavigationPosition position = new SimpleNavigationPosition(longitude, latitude);
        return position;
    }

    private PairWithLayer createPair(double lon1, double lat1, double lon2, double lat2) {
        return new PairWithLayer(createPosition(lon1, lat1), createPosition(lon2, lat2), 0);
    }

    @Test
    public void renderTrackAddsLinesToGroupLayer() {
        List<PairWithLayer> pairWithLayers = asList(
                createPair(0.0, 0.0, 1.0, 1.0),
                createPair(1.0, 1.0, 2.0, 2.0),
                createPair(2.0, 2.0, 3.0, 3.0)
        );

        renderer.renderTrack(pairWithLayers, () -> {});

        assertEquals("Should add 3 lines to track layer", 3, trackLayer.layers.size());
        for (Layer layer : trackLayer.layers) {
            assertTrue("Each layer should be a Line", layer instanceof Line);
        }
    }

    @Test
    public void renderTrackSkipsPairsWithoutCoordinates() {
        List<PairWithLayer> pairWithLayers = new ArrayList<>();
        pairWithLayers.add(createPair(0.0, 0.0, 1.0, 1.0));

        NavigationPosition noCoordPosition = mock(NavigationPosition.class);
        when(noCoordPosition.hasCoordinates()).thenReturn(false);
        pairWithLayers.add(new PairWithLayer(noCoordPosition, createPosition(2.0, 2.0), 1));

        pairWithLayers.add(createPair(1.0, 1.0, 2.0, 2.0));

        renderer.renderTrack(pairWithLayers, () -> {});

        assertEquals("Should skip pair without coordinates", 2, trackLayer.layers.size());
    }

    @Test
    public void renderTrackSetsLayerOnPairWithLayer() {
        PairWithLayer pairWithLayer = createPair(0.0, 0.0, 1.0, 1.0);

        renderer.renderTrack(asList(pairWithLayer), () -> {});

        assertNotNull("Layer should be set on pair", pairWithLayer.getLayer());
        assertTrue("Layer should be a Line", pairWithLayer.getLayer() instanceof Line);
    }

    @Test
    public void renderTrackHandlesEmptyList() {
        List<PairWithLayer> pairWithLayers = new ArrayList<>();

        renderer.renderTrack(pairWithLayers, () -> {});

        assertEquals("Should handle empty list gracefully", 0, trackLayer.layers.size());
    }

    @Test
    public void renderTrackWithAllPairsWithoutCoordinates() {
        List<PairWithLayer> pairWithLayers = new ArrayList<>();
        NavigationPosition noCoordPosition1 = mock(NavigationPosition.class);
        when(noCoordPosition1.hasCoordinates()).thenReturn(false);
        NavigationPosition noCoordPosition2 = mock(NavigationPosition.class);
        when(noCoordPosition2.hasCoordinates()).thenReturn(false);
        pairWithLayers.add(new PairWithLayer(noCoordPosition1, noCoordPosition2, 0));

        renderer.renderTrack(pairWithLayers, () -> {});

        assertEquals("Should not add any lines if all pairs lack coordinates", 0, trackLayer.layers.size());
    }

    @Test
    public void renderTrackCalculatesDistanceAndTime() {
        PairWithLayer pairWithLayer = createPair(0.0, 0.0, 1.0, 1.0);

        renderer.renderTrack(asList(pairWithLayer), () -> {});

        DistanceAndTime distanceAndTime = pairWithLayer.getDistanceAndTime();
        assertNotNull("Should calculate distance and time", distanceAndTime);
        assertNotNull("Distance should be calculated", distanceAndTime.getDistance());
        assertNotNull("Time should be calculated", distanceAndTime.getTime());
    }
}
