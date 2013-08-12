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
package slash.navigation.converter.gui.mapview;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.controller.LayerManagerController;
import org.mapsforge.map.controller.MapViewController;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;

import java.awt.*;

import static org.mapsforge.map.awt.AwtGraphicFactory.INSTANCE;

/**
 * Implementation of a {@link org.mapsforge.map.view.MapView} {@link Container}.
 *
 * @author Christian Pesch, inspired by org.mapsforge.map.swing.view
 */

public class AwtGraphicMapView extends Container implements org.mapsforge.map.view.MapView {
    static final GraphicFactory GRAPHIC_FACTORY = INSTANCE;

    private final FrameBuffer frameBuffer;
    private final LayerManager layerManager;
    private final Model model;

    public AwtGraphicMapView() {
        super();

        this.model = new Model();

        this.frameBuffer = new FrameBuffer(model.frameBufferModel, GRAPHIC_FACTORY);
        FrameBufferController.create(frameBuffer, model);

        this.layerManager = new LayerManager(this, model.mapViewPosition, GRAPHIC_FACTORY);
        this.layerManager.start();
        LayerManagerController.create(layerManager, model);

        MapViewController.create(this, model);
    }

    public void destroy() {
        layerManager.interrupt();
    }

    public Dimension getDimension() {
        return new Dimension(getWidth(), getHeight());
    }

    public FpsCounter getFpsCounter() {
        throw new UnsupportedOperationException();
    }

    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    public LayerManager getLayerManager() {
        return layerManager;
    }

    public Model getModel() {
        return model;
    }

    public void paint(Graphics graphics) {
        super.paint(graphics);

        org.mapsforge.core.graphics.Canvas canvas = AwtGraphicFactory.createCanvas((Graphics2D) graphics);
        frameBuffer.draw(canvas);
    }
}
