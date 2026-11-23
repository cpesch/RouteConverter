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
package slash.navigation.mapview.mapsforge.overlays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.model.DisplayModel;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.tileserver.TileServer;
import slash.navigation.mapview.mapsforge.tiles.TileLayerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class OverlayManagerTest {
    private TileLayerFactory tileLayerFactory;
    private ItemTableModel<TileServer> appliedOverlaysModel;
    private OverlayManager.Context context;
    private OverlayManager manager;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        tileLayerFactory = mock(TileLayerFactory.class);
        appliedOverlaysModel = mock(ItemTableModel.class);
        context = mock(OverlayManager.Context.class);
        when(context.getDisplayModel()).thenReturn(mock(DisplayModel.class));
        manager = new OverlayManager(tileLayerFactory, appliedOverlaysModel, context);
    }

    @Test
    public void insertAddsStartsAndRedrawsEachOverlay() {
        TileDownloadLayer first = mock(TileDownloadLayer.class);
        TileDownloadLayer second = mock(TileDownloadLayer.class);
        when(appliedOverlaysModel.getItem(0)).thenReturn(mock(TileServer.class));
        when(appliedOverlaysModel.getItem(1)).thenReturn(mock(TileServer.class));
        when(tileLayerFactory.createOverlayLayer(any())).thenReturn(first, second);

        manager.insert(0, 1);

        assertEquals(2, manager.getLayer().layers.size());
        verify(first).setDisplayModel(any(DisplayModel.class));
        verify(first).start();
        verify(second).start();
        verify(context, times(2)).redrawLayers();
        verify(context, times(1)).forceOverlayDisplay();
    }

    @Test
    public void deleteRemovesAndDestroysOverlay() {
        TileDownloadLayer overlay = mock(TileDownloadLayer.class);
        when(appliedOverlaysModel.getItem(0)).thenReturn(mock(TileServer.class));
        when(tileLayerFactory.createOverlayLayer(any())).thenReturn(overlay);
        manager.insert(0, 0);

        manager.delete(0, 0);

        assertEquals(0, manager.getLayer().layers.size());
        verify(overlay).onDestroy();
    }

    @Test
    public void deleteIgnoresRowsBeyondCurrentOverlays() {
        manager.delete(3, 5);

        assertEquals(0, manager.getLayer().layers.size());
        verifyNoInteractions(tileLayerFactory);
    }

    @Test
    public void insertWhileOverlaysAreDrawnDoesNotBreakTheDraw() throws Exception {
        TileDownloadLayer first = mock(TileDownloadLayer.class);
        TileDownloadLayer second = mock(TileDownloadLayer.class);
        when(appliedOverlaysModel.getItem(0)).thenReturn(mock(TileServer.class));
        when(appliedOverlaysModel.getItem(1)).thenReturn(mock(TileServer.class));
        when(tileLayerFactory.createOverlayLayer(any())).thenReturn(first, second);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            release.await();
            return null;
        }).when(first).draw(any(), anyByte(), any(), any(), any());
        manager.insert(0, 0);

        AtomicReference<Throwable> caught = new AtomicReference<>();
        Thread drawThread = new Thread(() -> {
            try {
                manager.getLayer().draw(null, (byte) 0, null, null, null);
            } catch (Throwable t) {
                caught.set(t);
            }
        });
        drawThread.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));

        Thread insertThread = new Thread(() -> manager.insert(1, 1));
        insertThread.start();
        awaitBlockedOrTerminated(insertThread);

        release.countDown();
        drawThread.join(5000);
        insertThread.join(5000);

        assertNull(caught.get());
        assertEquals(2, manager.getLayer().layers.size());
    }

    @Test
    public void deleteWhileOverlaysAreDrawnDoesNotBreakTheDraw() throws Exception {
        TileDownloadLayer overlay = mock(TileDownloadLayer.class);
        when(appliedOverlaysModel.getItem(0)).thenReturn(mock(TileServer.class));
        when(tileLayerFactory.createOverlayLayer(any())).thenReturn(overlay);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            release.await();
            return null;
        }).when(overlay).draw(any(), anyByte(), any(), any(), any());
        manager.insert(0, 0);

        AtomicReference<Throwable> caught = new AtomicReference<>();
        Thread drawThread = new Thread(() -> {
            try {
                manager.getLayer().draw(null, (byte) 0, null, null, null);
            } catch (Throwable t) {
                caught.set(t);
            }
        });
        drawThread.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));

        Thread deleteThread = new Thread(() -> manager.delete(0, 0));
        deleteThread.start();
        awaitBlockedOrTerminated(deleteThread);

        release.countDown();
        drawThread.join(5000);
        deleteThread.join(5000);

        assertNull(caught.get());
        assertEquals(0, manager.getLayer().layers.size());
        verify(overlay, times(1)).onDestroy();
    }

    @Test
    public void insertSetsDisplayModelBeforeAdding() {
        TileDownloadLayer overlay = mock(TileDownloadLayer.class);
        when(appliedOverlaysModel.getItem(0)).thenReturn(mock(TileServer.class));
        when(tileLayerFactory.createOverlayLayer(any())).thenReturn(overlay);
        doAnswer(invocation -> {
            assertFalse(manager.getLayer().layers.contains(overlay));
            return null;
        }).when(overlay).setDisplayModel(any(DisplayModel.class));

        manager.insert(0, 0);

        verify(overlay).setDisplayModel(any(DisplayModel.class));
        assertTrue(manager.getLayer().layers.contains(overlay));
    }

    private static void awaitBlockedOrTerminated(Thread thread) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            Thread.State state = thread.getState();
            if (state == Thread.State.BLOCKED || state == Thread.State.TERMINATED)
                return;
            Thread.sleep(10);
        }
    }
}
