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
package slash.navigation.mapview.mapsforge;

import org.junit.Before;
import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import slash.navigation.base.BaseRoute;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.converter.gui.models.ColorModel;
import slash.navigation.converter.gui.models.PositionListsModel;
import slash.navigation.gui.models.IntegerModel;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.tileserver.TileServerMapManager;
import slash.navigation.mapview.mapsforge.renderer.NonSelectedPositionListsRenderer;
import slash.navigation.mapview.mapsforge.updater.UpdateDecoupler;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.awt.EventQueue.invokeAndWait;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.navigation.maps.mapsforge.helpers.MapUtil.toBoundingBox;

public class MapsforgeMapViewTest {
    private final MapsforgeMapView mapView = new MapsforgeMapView();
    private MapsforgeMapManager mapManager;

    @Before
    public void setUp() throws Exception {
        TileServerMapManager tileServerMapManager = mock(TileServerMapManager.class);
        when(tileServerMapManager.getAvailableMapsModel()).thenReturn(new ItemTableModel<>(1));

        mapManager = new MapsforgeMapManager(mock(), tileServerMapManager);
        mapManager.getAppliedThemeModel().setItem(mapManager.getAvailableThemesModel().getItem(0));

        MapsforgeMapViewCallback callback = mock(MapsforgeMapViewCallback.class);
        when(callback.getMapsforgeMapManager()).thenReturn(mapManager);

        Field mapViewCallback = MapsforgeMapView.class.getDeclaredField("mapViewCallback");
        mapViewCallback.setAccessible(true);
        mapViewCallback.set(mapView, callback);
    }

    @Test
    public void testBoundingBox() {
        BoundingBox from = new BoundingBox(new SimpleNavigationPosition(10.18587, 53.49249), new SimpleNavigationPosition(10.06767, 53.40451));
        org.mapsforge.core.model.BoundingBox to = mapView.asMapsforgeBoundingBox(from);
        assertNotNull(to);
        BoundingBox roundtrip = toBoundingBox(to);
        assertEquals(roundtrip, from);
    }

    @Test
    public void testThemeStylesAreDuplicatedWhenMenuContainsEquivalentLayers() throws Exception {
        XmlRenderThemeStyleMenu renderThemeStyleMenu = mock(XmlRenderThemeStyleMenu.class);
        XmlRenderThemeStyleLayer hiking = createStyleLayer("hiking", "Hiking", "paths");
        XmlRenderThemeStyleLayer hikingDuplicate = createStyleLayer("hiking", "Hiking", "paths");
        XmlRenderThemeStyleLayer cycling = createStyleLayer("cycling", "Cycling", "cycleways");

        Map<String, XmlRenderThemeStyleLayer> layers = new LinkedHashMap<>();
        layers.put("hiking", hiking);
        layers.put("hiking-alias", hikingDuplicate);
        layers.put("cycling", cycling);

        when(renderThemeStyleMenu.getLayers()).thenReturn(layers);
        when(renderThemeStyleMenu.getDefaultValue()).thenReturn("hiking");
        when(renderThemeStyleMenu.getDefaultLanguage()).thenReturn("en");
        when(renderThemeStyleMenu.getLayer("hiking")).thenReturn(hiking);

        createMenuCallback().getCategories(renderThemeStyleMenu);

        assertEquals("Equivalent styles should only appear once after a map/theme switch", 2,
                mapManager.getAvailableThemeStylesModel().getRowCount());
    }

    @Test
    public void testInvisibleThemeStylesAreNotSelectable() throws Exception {
        XmlRenderThemeStyleMenu renderThemeStyleMenu = mock(XmlRenderThemeStyleMenu.class);
        XmlRenderThemeStyleLayer hiddenParent = createStyleLayer("totm-base", "Base", false, "roads");
        XmlRenderThemeStyleLayer hiking = createStyleLayer("totm-hiking", "Hiking", true, "paths");
        XmlRenderThemeStyleLayer cycling = createStyleLayer("totm-cycling", "Cycling", true, "cycleways");

        Map<String, XmlRenderThemeStyleLayer> layers = new LinkedHashMap<>();
        layers.put("totm-base", hiddenParent);
        layers.put("totm-hiking", hiking);
        layers.put("totm-cycling", cycling);

        when(renderThemeStyleMenu.getLayers()).thenReturn(layers);
        when(renderThemeStyleMenu.getDefaultValue()).thenReturn("totm-hiking");
        when(renderThemeStyleMenu.getDefaultLanguage()).thenReturn("en");
        when(renderThemeStyleMenu.getLayer("totm-hiking")).thenReturn(hiking);

        createMenuCallback().getCategories(renderThemeStyleMenu);

        assertEquals("Only visible styles should be exposed in the theme style selector", 2,
                mapManager.getAvailableThemeStylesModel().getRowCount());
        assertNull("Hidden layers must stay internal and not become selectable styles",
                mapManager.getAvailableThemeStylesModel().getItemByUrl("totm-base"));
    }

    @Test
    public void testVisibleDefaultThemeStyleRemainsSelectedAfterFiltering() throws Exception {
        XmlRenderThemeStyleMenu renderThemeStyleMenu = mock(XmlRenderThemeStyleMenu.class);
        XmlRenderThemeStyleLayer hiddenParent = createStyleLayer("totm-base", "Base", false, "roads");
        XmlRenderThemeStyleLayer hiking = createStyleLayer("totm-hiking", "Hiking", true, "paths");
        XmlRenderThemeStyleLayer cycling = createStyleLayer("totm-cycling", "Cycling", true, "cycleways");

        Map<String, XmlRenderThemeStyleLayer> layers = new LinkedHashMap<>();
        layers.put("totm-base", hiddenParent);
        layers.put("totm-cycling", cycling);
        layers.put("totm-hiking", hiking);

        when(renderThemeStyleMenu.getLayers()).thenReturn(layers);
        when(renderThemeStyleMenu.getDefaultValue()).thenReturn("totm-hiking");
        when(renderThemeStyleMenu.getDefaultLanguage()).thenReturn("en");
        when(renderThemeStyleMenu.getLayer("totm-hiking")).thenReturn(hiking);

        createMenuCallback().getCategories(renderThemeStyleMenu);

        assertEquals("The visible default style from the theme menu should stay selected", "totm-hiking",
                mapManager.getAppliedThemeStyleModel().getItem().getUrl());
    }

    private XmlRenderThemeMenuCallback createMenuCallback() throws Exception {
        Class<?> menuCallbackClass = Class.forName(MapsforgeMapView.class.getName() + "$MenuCallback");
        Constructor<?> constructor = menuCallbackClass.getDeclaredConstructor(MapsforgeMapView.class);
        constructor.setAccessible(true);
        return (XmlRenderThemeMenuCallback) constructor.newInstance(mapView);
    }

    private XmlRenderThemeStyleLayer createStyleLayer(String id, String title, String... categories) {
        return createStyleLayer(id, title, true, categories);
    }

    private XmlRenderThemeStyleLayer createStyleLayer(String id, String title, boolean visible, String... categories) {
        XmlRenderThemeStyleLayer layer = mock(XmlRenderThemeStyleLayer.class);
        when(layer.getId()).thenReturn(id);
        when(layer.getTitle(anyString())).thenReturn(title);
        when(layer.isVisible()).thenReturn(visible);
        when(layer.getCategories()).thenReturn(new LinkedHashSet<>(Set.of(categories)));
        when(layer.getOverlays()).thenReturn(Collections.emptyList());
        return layer;
    }

    // --- background map failure modes ----------------------------------------
    // setBackgroundMap builds new MapFile(file) first; a corrupt or missing file makes the
    // mapsforge header check throw before any Swing-heavy code, and the catch must log + null
    // the layer instead of letting the exception escape on the EDT (which left a blank map with
    // nothing logged -- error report 1311).

    @Test
    public void testMapFileRejectsTruncatedGarbage() throws IOException {
        File corrupt = File.createTempFile("not-a-map", ".map");
        corrupt.deleteOnExit();
        Files.write(corrupt.toPath(), "this is not a mapsforge binary map file".getBytes());
        assertThrows(RuntimeException.class, () -> new MapFile(corrupt));
    }

    @Test
    public void testMapFileRejectsMissingFile() {
        File missing = new File(System.getProperty("java.io.tmpdir"), "missing-" + System.nanoTime() + ".map");
        assertThrows(RuntimeException.class, () -> new MapFile(missing));
    }

    @Test
    public void testSetBackgroundMapWithCorruptFileDoesNotThrowAndClearsLayer() throws Exception {
        File corrupt = File.createTempFile("not-a-map", ".map");
        corrupt.deleteOnExit();
        Files.write(corrupt.toPath(), "this is not a mapsforge binary map file".getBytes());

        mapView.setBackgroundMap(corrupt); // must not throw
        assertNull(backgroundLayerOf(mapView));
    }

    @Test
    public void testSetBackgroundMapWithMissingFileDoesNotThrowAndClearsLayer() throws Exception {
        File missing = new File(System.getProperty("java.io.tmpdir"), "missing-" + System.nanoTime() + ".map");

        mapView.setBackgroundMap(missing); // must not throw
        assertNull(backgroundLayerOf(mapView));
    }

    private static Object backgroundLayerOf(MapsforgeMapView mapView) throws Exception {
        Field field = MapsforgeMapView.class.getDeclaredField("backgroundLayer");
        field.setAccessible(true);
        return field.get(mapView);
    }

    // --- non-selected position list rebuild coalescing (issue #150 / spec 00015) --------------
    // repaintPositionListListener is the ChangeListener field that initialize() wires onto
    // RouteColorModel, RouteLineWidthModel, TrackColorModel, TrackLineWidthModel and
    // WaypointColorModel; driving it directly (instead of the full initialize() GUI bootstrap)
    // still exercises the real production listener instance and its delegation to
    // NonSelectedPositionListsRenderer.update(), whose own coalescing is unit-tested in
    // NonSelectedPositionListsRendererTest.

    private static class CountingPositionListsModel implements PositionListsModel {
        final AtomicInteger rebuilds = new AtomicInteger();
        public List<BaseRoute<?, ?>> getRoutes() { rebuilds.incrementAndGet(); return new ArrayList<>(); }
        public BaseRoute<?, ?> getSelectedRoute() { return null; }
        public void addListDataListener(ListDataListener l) { }
        public void removeListDataListener(ListDataListener l) { }
    }

    @Test
    public void testColorChangeCoalescesNonSelectedPositionListsRebuild() throws Exception {
        CountingPositionListsModel positionListsModel = new CountingPositionListsModel();
        NonSelectedPositionListsRenderer renderer = new NonSelectedPositionListsRenderer(mapView, positionListsModel,
                mock(ColorModel.class), mock(ColorModel.class), mock(IntegerModel.class), mock(IntegerModel.class),
                mock(GraphicFactory.class));
        setField(mapView, "nonSelectedPositionListsRenderer", renderer);
        setField(mapView, "updateDecoupler", mock(UpdateDecoupler.class));

        ChangeListener repaintPositionListListener = (ChangeListener) getField(mapView, "repaintPositionListListener");

        // simulate several route/track/waypoint color + line-width change events reaching the
        // same listener field within one drag, as RouteColorModel/TrackLineWidthModel/... do
        invokeAndWait(() -> {
            for (int i = 0; i < 5; i++)
                repaintPositionListListener.stateChanged(new ChangeEvent(this));
        });

        invokeAndWait(() -> { });   // drain the EDT so the deferred rebuild runs

        assertEquals("multiple color/line-width change events must coalesce into a single non-selected rebuild",
                1, positionListsModel.rebuilds.get());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = MapsforgeMapView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = MapsforgeMapView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    // --- re-fit decision after a map dimension change (issue #274) -----------------------------
    // On macOS, zoomToBounds() can run before the frame layout has settled (the profile pane below
    // the map has not taken its height yet), fitting the route against a taller-than-final map and
    // picking a zoom one level too close. shouldRefitAfterResize() is the pure decision of whether
    // a dimension change shortly afterwards should redo that fit -- extracted so it is testable
    // without a real window, mirroring testDeviceScaleFactor() below. The window is always measured
    // from the original fit (refitAfterResize() never resets the stored timestamp), and either a
    // zoom or a pan since the fit counts as user interaction that abandons it.

    @Test
    public void testRefitsWhenFitPendingAndDimensionChangedWithinWindow() {
        assertEquals(true, MapsforgeMapView.shouldRefitAfterResize(true, true, 0, 2000, false));
        assertEquals(true, MapsforgeMapView.shouldRefitAfterResize(true, true, 2000, 2000, false));
    }

    @Test
    public void testDoesNotRefitWithoutAPendingFit() {
        assertEquals(false, MapsforgeMapView.shouldRefitAfterResize(false, true, 0, 2000, false));
    }

    @Test
    public void testDoesNotRefitWhenDimensionDidNotChange() {
        assertEquals(false, MapsforgeMapView.shouldRefitAfterResize(true, false, 0, 2000, false));
    }

    @Test
    public void testDoesNotRefitOutsideTheTimeWindow() {
        assertEquals(false, MapsforgeMapView.shouldRefitAfterResize(true, true, 2001, 2000, false));
    }

    @Test
    public void testDoesNotRefitAfterUserInteraction() {
        // a fit the user has zoomed away from must not be repeated, even if still within the window
        assertEquals(false, MapsforgeMapView.shouldRefitAfterResize(true, true, 0, 2000, true));
    }

    @Test
    public void testRefitWindowIsMeasuredFromTheOriginalFitAcrossAChainOfResizes() {
        // three resizes arrive 1500ms apart; if the window were reset on every refit instead of
        // kept from the original fit, the third would still land inside a fresh 2000ms window and
        // wrongly refit again -- it must not, however many resizes arrived in between
        long refitWindowMillis = 2000;
        long firstResizeMillisSinceOriginalFit = 1500;
        long secondResizeMillisSinceOriginalFit = 3000;
        long thirdResizeMillisSinceOriginalFit = 4500;

        assertEquals(true, MapsforgeMapView.shouldRefitAfterResize(true, true, firstResizeMillisSinceOriginalFit, refitWindowMillis, false));
        assertEquals(false, MapsforgeMapView.shouldRefitAfterResize(true, true, secondResizeMillisSinceOriginalFit, refitWindowMillis, false));
        assertEquals(false, MapsforgeMapView.shouldRefitAfterResize(true, true, thirdResizeMillisSinceOriginalFit, refitWindowMillis, false));
    }

    @Test
    public void testDoesNotRefitAfterUserPanned() {
        // a pan (center changed, zoom unchanged) must abandon the pending fit just like a zoom
        // does -- refitAfterResize() folds zoomChanged || centerChanged into this single flag
        boolean zoomChanged = false;
        boolean centerChanged = true;
        assertEquals(false, MapsforgeMapView.shouldRefitAfterResize(true, true, 0, 2000, zoomChanged || centerChanged));
    }

    @Test
    public void testDeviceScaleFactor() {
        // Windows: the screen resolution already contains the display scale
        assertEquals(1.5f, MapsforgeMapView.getDeviceScaleFactor(1.5f, true, 1.5, 1f), 0.001f);
        // macOS Retina and Linux with GDK_SCALE: Java2D scales
        assertEquals(2f, MapsforgeMapView.getDeviceScaleFactor(1f, false, 2.0, 1f), 0.001f);
        // Linux desktop scaled without GDK_SCALE: only FlatLaf scales
        assertEquals(2f, MapsforgeMapView.getDeviceScaleFactor(1f, false, 1.0, 2f), 0.001f);
        assertEquals(1.5f, MapsforgeMapView.getDeviceScaleFactor(1f, false, 1.0, 1.5f), 0.001f);
        // a larger text size on top of the display scale does not enlarge the map further
        assertEquals(2f, MapsforgeMapView.getDeviceScaleFactor(1f, false, 2.0, 1.25f), 0.001f);
        // unscaled
        assertEquals(1f, MapsforgeMapView.getDeviceScaleFactor(1f, false, 1.0, 1f), 0.001f);
    }
}
