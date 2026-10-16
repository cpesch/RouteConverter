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

import slash.navigation.common.BoundingBox;
import slash.navigation.geocoding.CategorizedNavigationPosition;
import slash.navigation.mapview.MapView;
import slash.navigation.mapview.PoiMarker;
import slash.navigation.pois.mapsforge.CategoryPoiResult;
import slash.navigation.pois.mapsforge.MapsforgePoiOverlayService;
import slash.navigation.pois.mapsforge.PoiCatalog;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.prefs.Preferences.userNodeForPackage;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.navigation.common.Bearing.calculateBearing;

/**
 * Shows the POIs of the selected categories on the map, driven by the "POIs" entry in the View
 * menu. The overlay stays while the route is edited and follows the map: the viewport is polled
 * like {@link CoverageOverlayController} does, since panning and zooming fire no event this
 * class observes, and the POIs are searched off the event dispatch thread.
 *
 * @author Christian Pesch
 */

public class PoiOverlayController {
    private static final Logger log = Logger.getLogger(PoiOverlayController.class.getName());
    private static final String ENABLED_PREFERENCE = "poiOverlayEnabled";
    private static final String CATEGORIES_PREFERENCE = "poiOverlayCategories";
    private static final String CATEGORY_SEPARATOR = "\n";

    // about zoom level 12 on a large screen: beyond that a city has more POIs than help planning
    static final double MAXIMUM_VIEWPORT_DIAGONAL_METERS = 50000.0;
    static final int LIMIT_PER_GROUP = 200;
    // one color per top-level category, saturated enough to stand out on a rendered map
    private static final Color[] GROUP_COLORS = {
            new Color(0xd62728), new Color(0x1f77b4), new Color(0xff7f0e), new Color(0x9467bd),
            new Color(0x8c564b), new Color(0xe377c2), new Color(0x17becf), new Color(0x2ca02c),
            new Color(0xbcbd22), new Color(0x393b79), new Color(0xad494a), new Color(0x7f7f7f)
    };

    public enum State {OFF, ZOOM_IN, NO_DATA, SHOWN, TRUNCATED}

    private final MapsforgePoiOverlayService service;
    private final Supplier<MapView> mapViewSupplier;
    private final Executor executor;
    private final Preferences preferences;
    private final List<ChangeListener> changeListeners = new CopyOnWriteArrayList<>();
    // incremented for every new search, so the result of an outdated one is dropped
    private final AtomicInteger generation = new AtomicInteger();

    // changed and read on the event dispatch thread; volatile as the dialog reads them, too
    private volatile boolean enabled;
    private volatile Set<String> selectedKeys;
    private BoundingBox lastViewport;
    private Set<String> lastKeys;
    private boolean overlayShown;
    private volatile State state = State.OFF;
    private volatile int count;

    public PoiOverlayController(MapsforgePoiOverlayService service, Supplier<MapView> mapViewSupplier) {
        this(service, mapViewSupplier, newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PoiOverlay");
            thread.setDaemon(true);
            return thread;
        }), userNodeForPackage(PoiOverlayController.class));
    }

    PoiOverlayController(MapsforgePoiOverlayService service, Supplier<MapView> mapViewSupplier,
                         Executor executor, Preferences preferences) {
        this.service = service;
        this.mapViewSupplier = mapViewSupplier;
        this.executor = executor;
        this.preferences = preferences;
        this.enabled = preferences.getBoolean(ENABLED_PREFERENCE, false);
        this.selectedKeys = Arrays.stream(preferences.get(CATEGORIES_PREFERENCE, "").split(CATEGORY_SEPARATOR))
                .filter(key -> !key.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void start() {
        new Timer(500, e -> refresh()).start();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        preferences.putBoolean(ENABLED_PREFERENCE, enabled);
        refresh();
    }

    public Set<String> getSelectedKeys() {
        return selectedKeys;
    }

    public void setSelectedKeys(Set<String> selectedKeys) {
        this.selectedKeys = Set.copyOf(selectedKeys);
        try {
            preferences.put(CATEGORIES_PREFERENCE, String.join(CATEGORY_SEPARATOR, this.selectedKeys));
        } catch (IllegalArgumentException e) {
            log.warning("Cannot store POI overlay categories: " + e);
        }
        refresh();
    }

    public State getState() {
        return state;
    }

    public int getCount() {
        return count;
    }

    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    public static Color getGroupColor(int group) {
        return GROUP_COLORS[group % GROUP_COLORS.length];
    }

    /**
     * Loads the categories for the viewport in the background and passes them, or {@code null}
     * without POI data, to the callback on the event dispatch thread. Downloads the POI data for
     * the viewport if it is missing, but only when zoomed in far enough to not fetch a country
     * for a glance at a continent.
     */
    public void loadCatalog(Consumer<PoiCatalog> callback) {
        MapView mapView = mapViewSupplier.get();
        BoundingBox viewport = mapView != null ? mapView.getBoundingBox() : null;
        if (viewport == null) {
            callback.accept(null);
            return;
        }

        boolean download = !isTooLarge(viewport);
        new Thread(() -> {
            PoiCatalog catalog = null;
            try {
                catalog = service.getCatalog(viewport, download);
            } catch (IOException | RuntimeException e) {
                log.warning("Cannot load POI categories: " + e);
            }
            PoiCatalog result = catalog;
            invokeLater(() -> {
                callback.accept(result);
                // a download may have brought POI data for an unchanged viewport
                lastViewport = null;
                refresh();
            });
        }, "PoiCatalog").start();
    }

    static boolean isTooLarge(BoundingBox viewport) {
        double diagonal = calculateBearing(viewport.southWest().getLongitude(), viewport.southWest().getLatitude(),
                viewport.northEast().getLongitude(), viewport.northEast().getLatitude()).getDistance();
        return diagonal > MAXIMUM_VIEWPORT_DIAGONAL_METERS;
    }

    void refresh() {
        MapView mapView = mapViewSupplier.get();
        if (mapView == null)
            return;

        if (!enabled || selectedKeys.isEmpty()) {
            generation.incrementAndGet();
            lastViewport = null;
            update(mapView, State.OFF, emptyList());
            return;
        }

        BoundingBox viewport = mapView.getBoundingBox();
        if (viewport == null || (viewport.equals(lastViewport) && selectedKeys.equals(lastKeys)))
            return;
        lastViewport = viewport;
        lastKeys = selectedKeys;

        int current = generation.incrementAndGet();
        if (isTooLarge(viewport)) {
            update(mapView, State.ZOOM_IN, emptyList());
            return;
        }

        Set<String> keys = selectedKeys;
        executor.execute(() -> {
            // panning queues a search per poll, only the last one is worth running
            if (current != generation.get())
                return;

            CategoryPoiResult result = null;
            try {
                result = service.search(viewport, keys, LIMIT_PER_GROUP);
            } catch (IOException | RuntimeException e) {
                log.warning("Cannot search POIs: " + e);
            }
            CategoryPoiResult found = result;
            invokeLater(() -> {
                MapView view = mapViewSupplier.get();
                if (current != generation.get() || view == null)
                    return;
                if (found == null)
                    update(view, State.NO_DATA, emptyList());
                else
                    update(view, found.truncated() ? State.TRUNCATED : State.SHOWN, toMarkers(found));
            });
        });
    }

    private void update(MapView mapView, State state, List<PoiMarker> markers) {
        // the poll calls this every tick while the overlay is off, only clear the map once
        if (!markers.isEmpty() || overlayShown) {
            mapView.showPoiOverlay(markers);
            overlayShown = !markers.isEmpty();
        }

        if (state != this.state || markers.size() != this.count) {
            this.state = state;
            this.count = markers.size();
            ChangeEvent event = new ChangeEvent(this);
            for (ChangeListener listener : changeListeners)
                listener.stateChanged(event);
        }
    }

    static List<PoiMarker> toMarkers(CategoryPoiResult result) {
        return result.pois().stream()
                .map(poi -> new PoiMarker(poi.position(), createLabel(poi.position()), getGroupColor(poi.group())))
                .toList();
    }

    private static String createLabel(CategorizedNavigationPosition position) {
        String category = position.getCategory();
        return category != null ? position.getDescription() + " (" + category + ")" : position.getDescription();
    }
}
