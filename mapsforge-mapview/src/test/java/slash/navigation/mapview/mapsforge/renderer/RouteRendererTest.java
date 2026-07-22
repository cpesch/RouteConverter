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
import slash.navigation.converter.gui.models.ColorModel;
import slash.navigation.gui.models.IntegerModel;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;
import slash.navigation.mapview.mapsforge.MapsforgeMapViewCallback;
import slash.navigation.mapview.mapsforge.models.RouteQuality;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the quality-to-paint mapping in {@link RouteRenderer}.
 *
 * @author Christian Pesch
 */
public class RouteRendererTest {
    private GraphicFactory graphicFactory;
    private RouteRenderer renderer;

    @Before
    public void setUp() {
        graphicFactory = mock(GraphicFactory.class);
        IntegerModel routeLineWidthModel = mock(IntegerModel.class);
        when(routeLineWidthModel.getInteger()).thenReturn(4);
        renderer = new RouteRenderer(mock(MapsforgeMapView.class), mock(MapsforgeMapViewCallback.class),
                mock(ColorModel.class), routeLineWidthModel, graphicFactory);
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
}
