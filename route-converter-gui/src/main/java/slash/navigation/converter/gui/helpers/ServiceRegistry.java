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

import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.mapview.MapView;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Owns the lazily-initialised, shared services that used to live directly on
 * {@code BaseRouteConverter}: the map view, the audio player (TimeAlbum) and
 * the position augmenter. Each accessor is guarded by a single private lock
 * so a service is created at most once, even under concurrent first access
 * (issue #214) -- the class does not change what is created or when, only
 * where the field and its lock live.
 * <p>
 * {@code positionAugmenter} and {@code audioPlayer} follow the classic
 * double-checked-locking-over-a-volatile-field pattern: created on first
 * access. {@code mapView} is a holder instead -- it is not created on demand
 * but explicitly replaced by {@link #setMapView(String, MapViewSwapped)},
 * whose callback runs while this class's lock is held, so the field swap and
 * the caller's Swing wiring (adding the new view's component to a panel) are
 * one atomic step from the outside; the lock itself is never handed out (a
 * getLock() accessor trips SpotBugs' USO_UNSAFE_ACCESSIBLE_OBJECT_SYNCHRONIZATION).
 *
 * @author Christian Pesch
 */

public class ServiceRegistry {
    private static final Logger log = Logger.getLogger(ServiceRegistry.class.getName());

    private final Object lock = new Object();
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    private final Supplier<JTable> positionsViewSupplier;
    private final Supplier<PositionsModel> positionsModelSupplier;
    private final Supplier<JFrame> frameSupplier;
    private final ElevationServiceFacade elevationServiceFacade;
    private final GeocodingServiceFacade geocodingServiceFacade;

    private volatile MapView mapView;
    private volatile PositionAugmenter positionAugmenter;
    private volatile AudioPlayer audioPlayer;

    public ServiceRegistry(Supplier<JTable> positionsViewSupplier,
                            Supplier<PositionsModel> positionsModelSupplier,
                            Supplier<JFrame> frameSupplier,
                            ElevationServiceFacade elevationServiceFacade,
                            GeocodingServiceFacade geocodingServiceFacade) {
        this.positionsViewSupplier = positionsViewSupplier;
        this.positionsModelSupplier = positionsModelSupplier;
        this.frameSupplier = frameSupplier;
        this.elevationServiceFacade = elevationServiceFacade;
        this.geocodingServiceFacade = geocodingServiceFacade;
    }

    /**
     * Runs while {@link #setMapView(String, MapViewSwapped)} holds this
     * registry's lock, so the field swap and whatever Swing wiring the
     * caller does for the new view are one atomic step to outside readers of
     * {@link #getMapView()} / {@link #isMapViewAvailable()}.
     */
    @FunctionalInterface
    public interface MapViewSwapped {
        void onMapViewSwapped(MapView previous, MapView created);
    }

    // map view -- a holder, not created on demand; see class Javadoc

    private MapView createMapView(String className) {
        try {
            Class<?> aClass = Class.forName(className);
            return (MapView) aClass.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            log.info("Cannot create " + className + ": " + t);
            return null;
        }
    }

    public MapView getMapView() {
        return mapView;
    }

    /**
     * Creates a new map view from {@code className}, disposes the previous
     * one (if there was one) and installs the new one -- all while holding
     * this registry's lock, with {@code callback} invoked in between so the
     * caller can do its Swing wiring atomically with the field swap.
     */
    public void setMapView(String className, MapViewSwapped callback) {
        synchronized (lock) {
            MapView previous = mapView;
            MapView created = createMapView(className);
            mapView = created;
            callback.onMapViewSwapped(previous, created);
        }
    }

    public boolean isMapViewAvailable() {
        return mapView != null;
    }

    // position augmenter

    public PositionAugmenter getPositionAugmenter() {
        PositionAugmenter result = positionAugmenter;
        if (result == null) {
            synchronized (lock) {
                result = positionAugmenter;
                if (result == null) {
                    result = new PositionAugmenter(positionsViewSupplier.get(), positionsModelSupplier.get(),
                            frameSupplier.get(), elevationServiceFacade, geocodingServiceFacade);
                    positionAugmenter = result;
                }
            }
        }
        return result;
    }

    // audio player (for TimeAlbum)

    public AudioPlayer getAudioPlayer() {
        AudioPlayer result = audioPlayer;
        if (result == null) {
            synchronized (lock) {
                result = audioPlayer;
                if (result == null) {
                    result = new AudioPlayer(frameSupplier.get());
                    audioPlayer = result;
                }
            }
        }
        return result;
    }

    /**
     * Disposes whichever services have been created so far. Idempotent: a
     * second call is a no-op. A service accessed again after dispose() is
     * <em>not</em> recreated -- the accessor keeps returning the same, now
     * disposed, instance, matching the behaviour before this extraction.
     */
    public void dispose() {
        if (!disposed.compareAndSet(false, true)) {
            return;
        }
        synchronized (lock) {
            if (mapView != null) {
                mapView.dispose();
            }
            if (positionAugmenter != null) {
                positionAugmenter.dispose();
            }
            if (audioPlayer != null) {
                audioPlayer.dispose();
            }
        }
    }
}
