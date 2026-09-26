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
package slash.navigation.converter.gui.helpers;

import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.MapPreferencesModel;
import slash.navigation.converter.gui.models.PositionListsModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.mapview.MapView;
import slash.navigation.mapview.MapViewCallback;

import javax.swing.*;
import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceRegistry}.
 *
 * @author Christian Pesch
 */

public class ServiceRegistryTest {
    private static final int THREAD_COUNT = 16;

    private ServiceRegistry registry() {
        return new ServiceRegistry(() -> mock(JTable.class), () -> mock(PositionsModel.class), () -> mock(JFrame.class),
                new ElevationServiceFacade(), new GeocodingServiceFacade());
    }

    @Test
    public void positionAugmenterInitializesExactlyOnceUnderContention() throws InterruptedException {
        ServiceRegistry registry = registry();
        Set<PositionAugmenter> seen = new CopyOnWriteArraySet<>();
        runConcurrently(() -> seen.add(registry.getPositionAugmenter()));
        assertEquals(1, seen.size());
        assertSame(registry.getPositionAugmenter(), seen.iterator().next());
    }

    @Test
    public void audioPlayerInitializesExactlyOnceUnderContention() throws InterruptedException {
        ServiceRegistry registry = registry();
        Set<AudioPlayer> seen = new CopyOnWriteArraySet<>();
        runConcurrently(() -> seen.add(registry.getAudioPlayer()));
        assertEquals(1, seen.size());
        assertSame(registry.getAudioPlayer(), seen.iterator().next());
    }

    @Test
    public void mapViewIsNotAvailableUntilSet() {
        ServiceRegistry registry = registry();
        assertFalse(registry.isMapViewAvailable());
        assertNull(registry.getMapView());
    }

    @Test
    public void setMapViewCreatesFromClassNameAndPassesNoPreviousTheFirstTime() {
        ServiceRegistry registry = registry();
        registry.setMapView(StubMapView.class.getName(), (previous, created) -> assertNull(previous));

        assertTrue(registry.isMapViewAvailable());
        assertTrue(registry.getMapView() instanceof StubMapView);
    }

    @Test
    public void setMapViewPassesThePreviousOneToTheCallbackForDisposal() {
        // ServiceRegistry only owns the field; disposing the outgoing view (and any
        // Swing wiring around it) is the caller's job, done inside the callback --
        // see BaseRouteConverter.setMapView().
        ServiceRegistry registry = registry();
        registry.setMapView(StubMapView.class.getName(), (previous, created) -> {
            if (previous != null) {
                previous.dispose();
            }
        });
        StubMapView first = (StubMapView) registry.getMapView();

        registry.setMapView(StubMapView.class.getName(), (previous, created) -> {
            assertSame(first, previous);
            previous.dispose();
        });

        assertTrue(first.isDisposed());
        assertNotSame(first, registry.getMapView());
    }

    @Test
    public void setMapViewFallsBackToNullOnUnknownClassName() {
        ServiceRegistry registry = registry();
        AtomicInteger seenNull = new AtomicInteger(0);
        registry.setMapView("does.not.Exist", (previous, created) -> {
            if (created == null) {
                seenNull.incrementAndGet();
            }
        });

        assertEquals(1, seenNull.get());
        assertFalse(registry.isMapViewAvailable());
        assertNull(registry.getMapView());
    }

    @Test
    public void disposeDisposesEachCreatedService() {
        ServiceRegistry registry = registry();
        registry.setMapView(StubMapView.class.getName(), (previous, created) -> { });
        StubMapView mapView = (StubMapView) registry.getMapView();
        PositionAugmenter positionAugmenter = registry.getPositionAugmenter();
        AudioPlayer audioPlayer = registry.getAudioPlayer();

        registry.dispose();

        assertTrue(mapView.isDisposed());
        // PositionAugmenter/AudioPlayer don't expose dispose-state; that dispose()
        // doesn't recreate them (checked below, and in accessorAfterDisposeReturnsSameNotRecreatedInstance)
        // is the behavioural check for those.
        assertSame(positionAugmenter, registry.getPositionAugmenter());
        assertSame(audioPlayer, registry.getAudioPlayer());
    }

    @Test
    public void disposeIsIdempotent() {
        ServiceRegistry registry = registry();
        registry.setMapView(StubMapView.class.getName(), (previous, created) -> { });
        StubMapView mapView = (StubMapView) registry.getMapView();

        registry.dispose();
        registry.dispose();

        assertEquals(1, mapView.getDisposeCount());
    }

    @Test
    public void accessorAfterDisposeReturnsSameNotRecreatedInstance() {
        ServiceRegistry registry = registry();
        PositionAugmenter before = registry.getPositionAugmenter();

        registry.dispose();

        assertSame(before, registry.getPositionAugmenter());
    }

    private void runConcurrently(Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);
        AtomicInteger failures = new AtomicInteger(0);
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        task.run();
                    } catch (Throwable t) {
                        failures.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        assertEquals(0, failures.get());
    }

    /**
     * A trivial, no-op {@link MapView} instantiable via
     * {@code Class.forName(...).getDeclaredConstructor()}, the same way
     * {@link ServiceRegistry} creates a real map view from its class name.
     */
    public static class StubMapView implements MapView {
        private int disposeCount;

        public boolean isDisposed() {
            return disposeCount > 0;
        }

        public int getDisposeCount() {
            return disposeCount;
        }

        public void initialize(PositionsModel positionsModel, PositionListsModel positionListsModel,
                                MapPreferencesModel preferencesModel, MapViewCallback mapViewCallback) {
        }

        public boolean isDownload() {
            return false;
        }

        public String getMapIdentifier() {
            return "stub";
        }

        public String getMapsPath() {
            return null;
        }

        public void setMapsPath(String path) {
        }

        public String getThemesPath() {
            return null;
        }

        public void setThemesPath(String path) {
        }

        public Throwable getInitializationCause() {
            return null;
        }

        public void dispose() {
            disposeCount++;
        }

        public Component getComponent() {
            return null;
        }

        public void resize() {
        }

        public void showAllPositions() {
        }

        public void showMapBorder(BoundingBox mapBoundingBox) {
        }

        public void showCoverageOverlay(BoundingBox mapBoundingBox, String category, Map<BoundingBox, Boolean> coverageTiles) {
        }

        public void showPositionMagnifier(List<NavigationPosition> positions) {
        }

        public NavigationPosition getCenter() {
            return null;
        }

        public void setCenter(NavigationPosition position) {
        }

        public BoundingBox getBoundingBox() {
            return null;
        }

        public void setSelectedPositions(List<NavigationPosition> selectedPositions) {
        }

        public void setSelectedPositions(int[] selectedPositions, boolean replaceSelection) {
        }

        public boolean isSupportsPrinting() {
            return false;
        }

        public void print(String title) {
        }
    }
}
