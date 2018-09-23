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

import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
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

import java.awt.*;

import static org.mapsforge.map.awt.graphics.AwtGraphicFactory.INSTANCE;
import static org.mapsforge.map.awt.graphics.AwtGraphicFactory.clearResourceFileCache;
import static org.mapsforge.map.awt.graphics.AwtGraphicFactory.clearResourceMemoryCache;

/**
 * Implementation of a {@link org.mapsforge.map.view.MapView} {@link Container}.
 *
 * @author Christian Pesch, inspired by org.mapsforge.map.swing.view.MapView
 */

public class AwtGraphicMapView extends Container implements org.mapsforge.map.view.MapView {
    static final GraphicFactory GRAPHIC_FACTORY = INSTANCE;

    private final FrameBuffer frameBuffer;
    private final FrameBufferController frameBufferController;
    private final LayerManager layerManager;
    private final FpsCounter fpsCounter;
    private MapScaleBar mapScaleBar;
    private final MapViewProjection mapViewProjection;
    private final Model model;

    public AwtGraphicMapView() {
        super();

        this.model = new Model();

        this.fpsCounter = new FpsCounter(GRAPHIC_FACTORY, model.displayModel);
        this.frameBuffer = new FrameBuffer(model.frameBufferModel, model.displayModel, GRAPHIC_FACTORY);
        this.frameBufferController = FrameBufferController.create(frameBuffer, model);

        this.layerManager = new LayerManager(this, model.mapViewPosition, GRAPHIC_FACTORY);
        this.layerManager.start();
        LayerManagerController.create(layerManager, model);

        MapViewController.create(this, model);

        this.mapScaleBar = new DefaultMapScaleBar(model.mapViewPosition, model.mapViewDimension, GRAPHIC_FACTORY, model.displayModel);

        this.mapViewProjection = new MapViewProjection(this);
    }

    public void addLayer(Layer layer) {
        this.layerManager.getLayers().add(layer);
    }

    public void removeLayer(Layer layer) {
        this.layerManager.getLayers().remove(layer);
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
        for (Layer layer : layerManager.getLayers()) {
            // this delays stopping RouteConverter for a very long time since all layers have to be removed one by one
            // removeLayer(layer);
            layer.onDestroy();
            if (layer instanceof TileLayer) {
                ((TileLayer) layer).getTileCache().destroy();
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
        return MapPositionUtil.getBoundingBox(model.mapViewPosition.getMapPosition(), getDimension(),
                model.displayModel.getTileSize());
    }

    public Dimension getDimension() {
        return new Dimension(getWidth(), getHeight());
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

    public MapScaleBar getMapScaleBar() {
        return mapScaleBar;
    }

    public MapViewProjection getMapViewProjection() {
        return mapViewProjection;
    }

    public Model getModel() {
        return model;
    }

    public void paint(Graphics graphics) {
        super.paint(graphics);

        GraphicContext graphicContext = AwtGraphicFactory.createGraphicContext(graphics);
        frameBuffer.draw(graphicContext);
        fpsCounter.draw(graphicContext);
        mapScaleBar.draw(graphicContext);
    }

    public void setCenter(LatLong center) {
        model.mapViewPosition.setCenter(center);
    }

    public void setMapScaleBar(MapScaleBar mapScaleBar) {
        this.mapScaleBar.destroy();
        this.mapScaleBar = mapScaleBar;
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
