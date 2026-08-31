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
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.ColorModel;
import slash.navigation.gui.models.IntegerModel;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;
import slash.navigation.mapview.mapsforge.MapsforgeMapViewCallback;
import slash.navigation.mapview.mapsforge.models.RouteQuality;
import slash.navigation.mapview.mapsforge.updater.PairWithLayer;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static slash.navigation.routing.RoutingResult.Validity.Valid;

/**
 * Tests for the quality-to-paint mapping in {@link RouteRenderer}.
 *
 * @author Christian Pesch
 */
public class RouteRendererTest {
    private GraphicFactory graphicFactory;
    private MapsforgeMapView mapView;
    private MapsforgeMapViewCallback mapViewCallback;
    private RouteRenderer renderer;
    private GroupLayer trackLayer;

    @Before
    public void setUp() {
        graphicFactory = mock(GraphicFactory.class);
        when(graphicFactory.createPaint()).thenReturn(mock(Paint.class));
        mapView = mock(MapsforgeMapView.class);
        when(mapView.asLatLong(any(NavigationPosition.class))).thenReturn(new LatLong(0.0, 0.0));
        when(mapView.asLatLong(anyList())).thenReturn(emptyList());
        trackLayer = new GroupLayer();
        when(mapView.getTrackLayer()).thenReturn(trackLayer);
        when(mapView.getTileSize()).thenReturn(256);
        mapViewCallback = mock(MapsforgeMapViewCallback.class);
        doAnswer(invocation -> {
            throw new AssertionError("rendering failed", invocation.getArgument(0));
        }).when(mapViewCallback).handleRoutingException(any());
        IntegerModel routeLineWidthModel = mock(IntegerModel.class);
        when(routeLineWidthModel.getInteger()).thenReturn(4);
        ColorModel routeColorModel = mock(ColorModel.class);
        when(routeColorModel.getColor()).thenReturn(java.awt.Color.BLUE);
        renderer = new RouteRenderer(mapView, mapViewCallback,
                routeColorModel, routeLineWidthModel, graphicFactory);
    }

    @Test
    public void validQualityReusesGivenPaintWithoutCreatingANewOne() {
        Paint validPaint = mock(Paint.class);

        Paint result = renderer.choosePaint(RouteQuality.Valid, validPaint);

        assertSame(validPaint, result);
        verifyNoInteractions(graphicFactory);
    }

    @Test
    public void detourQualityCreatesOrangePaintWithRouteLineWidth() {
        Paint validPaint = mock(Paint.class);
        Paint detourPaint = mock(Paint.class);
        when(graphicFactory.createPaint()).thenReturn(detourPaint);

        Paint result = renderer.choosePaint(RouteQuality.Detour, validPaint);

        assertSame(detourPaint, result);
        verify(detourPaint).setColor(0xFFFFA500);
        verify(detourPaint).setStrokeWidth(4);
    }

    @Test
    public void invalidQualityCreatesRedPaintWithRouteLineWidth() {
        Paint validPaint = mock(Paint.class);
        Paint invalidPaint = mock(Paint.class);
        when(graphicFactory.createPaint()).thenReturn(invalidPaint);

        Paint result = renderer.choosePaint(RouteQuality.Invalid, validPaint);

        assertSame(invalidPaint, result);
        verify(invalidPaint).setColor(0xFFFF0000);
        verify(invalidPaint).setStrokeWidth(4);
    }

    private NavigationPosition createPosition() {
        NavigationPosition position = mock(NavigationPosition.class);
        when(position.hasCoordinates()).thenReturn(true);
        when(position.calculateDistance(any())).thenReturn(1.0);
        when(position.calculateTime(any())).thenReturn(1L);
        return position;
    }

    private PairWithLayer createPair(int row) {
        return new PairWithLayer(createPosition(), createPosition(), row);
    }

    @Test(timeout = 5000)
    public void cancelRenderingStopsRoutingTheRemainingLegs() {
        RoutingService routingService = mock(RoutingService.class);
        when(routingService.isInitialized()).thenReturn(true);
        when(routingService.isDownload()).thenReturn(false);
        when(mapViewCallback.getRoutingService()).thenReturn(routingService);
        // the first routed leg cancels the rendering - as replacing the route does
        when(routingService.getRouteBetween(any(), any(), any(), any())).thenAnswer(invocation -> {
            renderer.cancelRendering();
            return new RoutingResult(emptyList(), new DistanceAndTime(1.0, 1L), Valid);
        });
        List<PairWithLayer> pairWithLayers = asList(createPair(0), createPair(1), createPair(2));

        renderer.renderRoute("map", pairWithLayers, () -> {});

        // legs 2 and 3 are not routed anymore after the cancellation
        verify(routingService, times(1)).getRouteBetween(any(), any(), any(), any());
        // the straight lines went to the trackLayer as a single batch, not one AWT event per line
        assertEquals(3, trackLayer.layers.size());
    }

    @Test(timeout = 5000)
    public void cancelRenderingAbortsWaitingForRoutingServiceInitialization() throws InterruptedException {
        RoutingService routingService = mock(RoutingService.class);
        when(routingService.isInitialized()).thenReturn(false);
        when(mapViewCallback.getRoutingService()).thenReturn(routingService);
        List<PairWithLayer> pairWithLayers = asList(createPair(0));

        Thread canceller = new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
            renderer.cancelRendering();
        });
        canceller.start();

        // returns instead of spinning forever on the never-initialized service
        renderer.renderRoute("map", pairWithLayers, () -> {});

        canceller.join();
        verify(routingService, times(0)).getRouteBetween(any(), any(), any(), any());
    }

    @Test(timeout = 5000)
    public void routedLegSwapsStraightLineWithPolyline() {
        RoutingService routingService = mock(RoutingService.class);
        when(routingService.isInitialized()).thenReturn(true);
        when(routingService.isDownload()).thenReturn(false);
        when(mapViewCallback.getRoutingService()).thenReturn(routingService);
        when(routingService.getRouteBetween(any(), any(), any(), any())).thenReturn(
            new RoutingResult(emptyList(), new DistanceAndTime(1.0, 1L), Valid));
        List<PairWithLayer> pairWithLayers = asList(createPair(0));

        renderer.renderRoute("map", pairWithLayers, () -> {});

        // after routing, the straight Line should be replaced with a Polyline
        verify(routingService, times(1)).getRouteBetween(any(), any(), any(), any());
        assertEquals(1, trackLayer.layers.size());
    }
}
