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

import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import slash.navigation.base.BaseRoute;
import slash.navigation.converter.gui.models.ColorModel;
import slash.navigation.converter.gui.models.PositionListsModel;
import slash.navigation.gui.models.IntegerModel;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;

import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.awt.EventQueue.invokeAndWait;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static slash.navigation.mapview.mapsforge.renderer.NonSelectedPositionListsRenderer.lighten;

public class NonSelectedPositionListsRendererTest {

    /**
     * A {@link PositionListsModel} with no routes that counts how often the renderer reads it -
     * one read per actual rebuild. No routes means the rebuild touches no other collaborator.
     */
    private static class CountingPositionListsModel implements PositionListsModel {
        final AtomicInteger reads = new AtomicInteger();
        public List<BaseRoute> getRoutes() { reads.incrementAndGet(); return new ArrayList<>(); }
        public BaseRoute getSelectedRoute() { return null; }
        public void addListDataListener(ListDataListener l) { }
        public void removeListDataListener(ListDataListener l) { }
    }

    private NonSelectedPositionListsRenderer newRenderer(PositionListsModel model) {
        return new NonSelectedPositionListsRenderer(mock(MapsforgeMapView.class), model,
                mock(ColorModel.class), mock(ColorModel.class),
                mock(IntegerModel.class), mock(IntegerModel.class), mock(GraphicFactory.class));
    }

    @Test
    public void updateDefersTheRebuildInsteadOfRunningItOnTheEventThread() throws Exception {
        CountingPositionListsModel model = new CountingPositionListsModel();
        NonSelectedPositionListsRenderer renderer = newRenderer(model);

        // spec 00015 regression: a color/line-width change reaches update() on the EDT; the
        // rebuild must NOT run synchronously inside that event (it janks the Options dialog),
        // so no read has happened yet by the time the event returns
        invokeAndWait(() -> {
            renderer.update();
            assertEquals("rebuild must be deferred, not run inline during the change event",
                    0, model.reads.get());
        });

        invokeAndWait(() -> { });   // drain the EDT so the deferred rebuild runs
        assertEquals("deferred rebuild runs exactly once after the event returns",
                1, model.reads.get());
    }

    @Test
    public void rapidUpdatesCoalesceIntoASingleRebuild() throws Exception {
        CountingPositionListsModel model = new CountingPositionListsModel();
        NonSelectedPositionListsRenderer renderer = newRenderer(model);

        // simulate a JColorChooser drag: many update() calls within one EDT event
        invokeAndWait(() -> {
            for (int i = 0; i < 50; i++)
                renderer.update();
        });

        invokeAndWait(() -> { });   // drain the EDT
        assertEquals("50 rapid updates must coalesce into a single rebuild", 1, model.reads.get());
    }

    @Test
    public void factorZeroKeepsTheColor() {
        assertEquals(0xFF3379FF, lighten(0xFF3379FF, 0.0f));
    }

    @Test
    public void factorOneTurnsRgbWhiteButKeepsAlpha() {
        assertEquals(0xFFFFFFFF, lighten(0xFF3379FF, 1.0f));
        assertEquals(0x80FFFFFF, lighten(0x80000000, 1.0f));
    }

    @Test
    public void factorHalfBlendsHalfwayToWhite() {
        // each channel: c + (255 - c) * 0.5 -> 0 becomes 127
        assertEquals(0xFF7F7F7F, lighten(0xFF000000, 0.5f));
    }

    @Test
    public void alphaIsPreserved() {
        assertEquals(0x80, (lighten(0x803379FF, 0.55f) >>> 24) & 0xFF);
    }

    @Test
    public void lightenedIsBrighterPerChannel() {
        int original = 0xFF2050A0;
        int lightened = lighten(original, 0.55f);
        assertEquals(true, ((lightened >> 16) & 0xFF) > ((original >> 16) & 0xFF));
        assertEquals(true, ((lightened >> 8) & 0xFF) > ((original >> 8) & 0xFF));
        assertEquals(true, (lightened & 0xFF) > (original & 0xFF));
    }
}
