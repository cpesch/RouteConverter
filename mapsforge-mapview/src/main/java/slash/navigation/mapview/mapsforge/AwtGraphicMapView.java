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

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.controller.LayerManagerController;
import org.mapsforge.map.controller.MapViewController;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.labels.LabelStore;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.scalebar.DefaultMapScaleBar;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.util.MapPositionUtil;
import org.mapsforge.map.util.MapViewProjection;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;
import org.mapsforge.map.view.FrameBufferHA3;

import java.awt.*;
import java.awt.geom.AffineTransform;

import static org.mapsforge.map.awt.graphics.AwtGraphicFactory.*;

/**
 * Implementation of a {@link org.mapsforge.map.view.MapView} {@link Container}.
 *
 * @author Christian Pesch, inspired by org.mapsforge.map.swing.view.MapView
 */

public class AwtGraphicMapView extends Container implements org.mapsforge.map.view.MapView {
    static final GraphicFactory GRAPHIC_FACTORY = INSTANCE;

    private final FpsCounter fpsCounter;
    private final FrameBuffer frameBuffer;
    private final FrameBufferController frameBufferController;
    private final LayerManager layerManager;
    private MapScaleBar mapScaleBar;
    private final MapViewProjection mapViewProjection;
    private final Model model;
    private final DeviceView deviceView = new DeviceView();

    AwtGraphicMapView() {
        super();

        this.model = new Model();

        this.fpsCounter = new FpsCounter(GRAPHIC_FACTORY, model.displayModel);
        this.frameBuffer = new FrameBufferHA3(this.model.frameBufferModel, this.model.displayModel, GRAPHIC_FACTORY);
        this.frameBufferController = FrameBufferController.create(frameBuffer, model);

        this.layerManager = new LayerManager(deviceView, model.mapViewPosition, GRAPHIC_FACTORY);
        this.layerManager.start();
        LayerManagerController.create(layerManager, model);

        MapViewController.create(this, model);

        this.mapScaleBar = new DefaultMapScaleBar(model.mapViewPosition, model.mapViewDimension, GRAPHIC_FACTORY, model.displayModel);

        this.mapViewProjection = new MapViewProjection(deviceView);
    }

    public void addLayer(Layer layer) {
        layerManager.getLayers().add(layer);
    }

    public void destroy() {
        layerManager.interrupt();
        frameBufferController.destroy();
        frameBuffer.destroy();
        if (mapScaleBar != null)
            mapScaleBar.destroy();
        getModel().mapViewPosition.destroy();
    }

    public void destroyAll() {
        layerManager.finish();
        for (Layer layer : layerManager.getLayers()) {
            // this delays stopping RouteConverter for a very long time since all layers have to be removed one by one
            // layerManager.getLayers().remove(layer);
            layer.onDestroy();
            if (layer instanceof TileLayer<?> tileLayer) {
                tileLayer.getTileCache().destroy();
            }
            if (layer instanceof TileRendererLayer) {
                LabelStore labelStore = ((TileRendererLayer) layer).getLabelStore();
                if (labelStore != null) {
                    labelStore.clear();
                }
            }
        }
        destroy();
        clearResourceMemoryCache();
        clearResourceFileCache();
    }

    public BoundingBox getBoundingBox() {
        return MapPositionUtil.getBoundingBox(this.model.mapViewPosition.getMapPosition(), getMapRotation(),
                this.model.displayModel.getTileSize(), getDimension(), getMapViewCenterX(), getMapViewCenterY());
    }

    public Dimension getDimension() {
        return new Dimension(toDevicePixels(getWidth()), toDevicePixels(getHeight()));
    }

    /**
     * The factor between Swing's logical coordinates and the device pixels the map is rendered in:
     * the display scale Java applies to the UI (e.g. 1.5 at 150%, 2.0 on Retina), 1.0 without one.
     */
    public double getDeviceScale() {
        GraphicsConfiguration configuration = getGraphicsConfiguration();
        return configuration != null ? configuration.getDefaultTransform().getScaleX() : 1.0;
    }

    private int toDevicePixels(int logical) {
        return (int) Math.round(logical * getDeviceScale());
    }

    /**
     * Converts a point in Swing's logical coordinates, e.g. from a {@link java.awt.event.MouseEvent},
     * into the device pixels the map projection works in.
     */
    public Point toDevicePoint(Point logical) {
        double scale = getDeviceScale();
        return new Point((int) Math.round(logical.x * scale), (int) Math.round(logical.y * scale));
    }

    public FpsCounter getFpsCounter() {
        return fpsCounter;
    }

    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    public LayerManager getLayerManager() {
        return layerManager;
    }

    public Rotation getMapRotation() {
        return model.mapViewPosition.getRotation();
    }

    public MapScaleBar getMapScaleBar() {
        return mapScaleBar;
    }

    public float getMapViewCenterX() {
        return model.mapViewPosition.getMapViewCenterX();
    }

    public float getMapViewCenterY() {
        return model.mapViewPosition.getMapViewCenterY();
    }

    public MapViewProjection getMapViewProjection() {
        return mapViewProjection;
    }

    public Model getModel() {
        return model;
    }

    public float getOffsetX() {
        return toDevicePixels(getWidth()) * (getMapViewCenterX() - 0.5f);
    }

    public float getOffsetY() {
        return toDevicePixels(getHeight()) * (getMapViewCenterY() - 0.5f);
    }

    /**
     * The {@link org.mapsforge.map.view.MapView} as mapsforge's {@link MapViewProjection} and {@link LayerManager}
     * need it: {@link #getWidth()} and {@link #getHeight()} of the {@link Container} are in Swing's logical
     * coordinates, mapsforge expects the device pixels the map is rendered in.
     */
    private class DeviceView implements org.mapsforge.map.view.MapView {
        public void addLayer(Layer layer) { AwtGraphicMapView.this.addLayer(layer); }
        public void destroy() { AwtGraphicMapView.this.destroy(); }
        public void destroyAll() { AwtGraphicMapView.this.destroyAll(); }
        public BoundingBox getBoundingBox() { return AwtGraphicMapView.this.getBoundingBox(); }
        public Dimension getDimension() { return AwtGraphicMapView.this.getDimension(); }
        public FpsCounter getFpsCounter() { return fpsCounter; }
        public FrameBuffer getFrameBuffer() { return frameBuffer; }
        public int getHeight() { return getDimension().height; }
        public LayerManager getLayerManager() { return layerManager; }
        public Rotation getMapRotation() { return AwtGraphicMapView.this.getMapRotation(); }
        public MapScaleBar getMapScaleBar() { return mapScaleBar; }
        public float getMapViewCenterX() { return AwtGraphicMapView.this.getMapViewCenterX(); }
        public float getMapViewCenterY() { return AwtGraphicMapView.this.getMapViewCenterY(); }
        public MapViewProjection getMapViewProjection() { return mapViewProjection; }
        public Model getModel() { return model; }
        public float getOffsetX() { return AwtGraphicMapView.this.getOffsetX(); }
        public float getOffsetY() { return AwtGraphicMapView.this.getOffsetY(); }
        public int getWidth() { return getDimension().width; }
        public void repaint() { AwtGraphicMapView.this.repaint(); }
        public void rotate(Rotation rotation) { AwtGraphicMapView.this.rotate(rotation); }
        public void setCenter(LatLong center) { AwtGraphicMapView.this.setCenter(center); }
        public void setMapScaleBar(MapScaleBar mapScaleBar) { AwtGraphicMapView.this.setMapScaleBar(mapScaleBar); }
        public void setMapViewCenterX(float mapViewCenterX) { AwtGraphicMapView.this.setMapViewCenterX(mapViewCenterX); }
        public void setMapViewCenterY(float mapViewCenterY) { AwtGraphicMapView.this.setMapViewCenterY(mapViewCenterY); }
        public void setZoomLevel(byte zoomLevel) { AwtGraphicMapView.this.setZoomLevel(zoomLevel); }
        public void setZoomLevelMax(byte zoomLevelMax) { AwtGraphicMapView.this.setZoomLevelMax(zoomLevelMax); }
        public void setZoomLevelMin(byte zoomLevelMin) { AwtGraphicMapView.this.setZoomLevelMin(zoomLevelMin); }
    }

    public void paint(Graphics graphics) {
        super.paint(graphics);

        Graphics2D graphics2D = (Graphics2D) graphics;
        AffineTransform logical = graphics2D.getTransform();
        // Java scales Swing by the display scale on its own, but the tiles are already rendered for
        // that scale: draw them 1:1 into device pixels instead of letting Java2D resample them
        graphics2D.setTransform(AffineTransform.getTranslateInstance(Math.round(logical.getTranslateX()),
                Math.round(logical.getTranslateY())));
        try {
            Canvas canvas = AwtGraphicFactory.createGraphicContext(graphics2D);
            frameBuffer.draw(canvas, model.mapViewPosition.getRotation());
            mapScaleBar.draw(canvas);
            fpsCounter.draw(canvas);
        } finally {
            graphics2D.setTransform(logical);
        }
    }

    public void rotate(Rotation rotation) {
        this.getModel().mapViewPosition.setRotation(rotation);
    }

    public void setCenter(LatLong center) {
        model.mapViewPosition.setCenter(center);
    }

    public void setMapScaleBar(MapScaleBar mapScaleBar) {
        if (this.mapScaleBar != null) {
            this.mapScaleBar.destroy();
        }
        this.mapScaleBar = mapScaleBar;
    }

    public void setMapViewCenterX(float mapViewCenterX) {
        this.model.mapViewPosition.setMapViewCenterX(mapViewCenterX);
    }

    public void setMapViewCenterY(float mapViewCenterY) {
        this.model.mapViewPosition.setMapViewCenterY(mapViewCenterY);
    }

    public void setZoomLevel(byte zoomLevel) {
        model.mapViewPosition.setZoomLevel(zoomLevel);
    }

    public void setZoomLevelMax(byte zoomLevelMax) {
        model.mapViewPosition.setZoomLevelMax(zoomLevelMax);
    }

    public void setZoomLevelMin(byte zoomLevelMin) {
        model.mapViewPosition.setZoomLevelMin(zoomLevelMin);
    }
}
