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
package slash.navigation.mapview.mapsforge.helpers;

import org.mapsforge.core.model.LatLong;
import slash.navigation.gui.Application;
import slash.navigation.gui.SingleFrameApplication;
import slash.navigation.mapview.MapViewCallback;
import slash.navigation.mapview.mapsforge.AwtGraphicMapView;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Function;

import static javax.swing.BorderFactory.createEtchedBorder;

/**
 * Show coordinates, or the POI under the mouse, upon to mouse events of the {@link AwtGraphicMapView}.
 *
 * @author Christian Pesch
 */

public class MapViewCoordinateDisplayer extends MouseAdapter {
    private AwtGraphicMapView mapView;
    private MapViewCallback mapViewCallback;
    private JWindow window;
    private final JLabel label = new JLabel();
    private boolean showCoordinates;
    private Function<Point, String> poiLabeler = point -> null;

    public void initialize(AwtGraphicMapView mapView, MapViewCallback mapViewCallback) {
        this.mapView = mapView;
        this.mapViewCallback = mapViewCallback;

        window = new JWindow(getFrame());
        JPanel contentPane = (JPanel) window.getContentPane();
        contentPane.add(label);
        contentPane.setBackground(new Color(255, 255, 204));
        // the background is always light yellow, so keep the text dark in dark look-and-feels, too
        label.setForeground(Color.BLACK);
        contentPane.setBorder(new CompoundBorder(createEtchedBorder(), new EmptyBorder(2, 3, 2, 3)));
        label.setFont(label.getFont().deriveFont(9f));

        mapView.addMouseListener(this);
        mapView.addMouseMotionListener(this);
    }

    public void setShowCoordinates(boolean showCoordinates) {
        this.showCoordinates = showCoordinates;
    }

    /**
     * @param poiLabeler returns the label of the POI at a device point or {@code null} if there is none
     */
    public void setPoiLabeler(Function<Point, String> poiLabeler) {
        this.poiLabeler = poiLabeler;
    }

    private JFrame getFrame() {
        Application application = Application.getInstance();
        if (application instanceof SingleFrameApplication singleFrameApplication)
            return singleFrameApplication.getFrame();
        else
            return null;
    }

    private void displayCoordinates(Point locationOnScreen, Point devicePoint) {
        LatLong latLong = mapView.getMapViewProjection().fromPixels(devicePoint.x, devicePoint.y);
        // null while the map has no size or position yet, e.g. when the mouse moves over it during startup
        if (latLong == null) {
            hide();
            return;
        }
        display(locationOnScreen, mapViewCallback.createCoordinates(latLong.longitude, latLong.latitude));
    }

    private void display(Point locationOnScreen, String text) {
        label.setText(text);
        window.pack();
        window.setLocation(locationOnScreen.x + 16, locationOnScreen.y + 15);
        show();
    }

    private void show() {
        if (!window.isVisible())
            window.setVisible(true);
    }

    private void hide() {
        if (window.isVisible())
            window.setVisible(false);
    }

    public void mouseMoved(MouseEvent e) {
        Point devicePoint = mapView.toDevicePoint(e.getPoint());
        String poiLabel = poiLabeler.apply(devicePoint);
        if (poiLabel != null)
            display(e.getLocationOnScreen(), poiLabel);
        else if (showCoordinates)
            displayCoordinates(e.getLocationOnScreen(), devicePoint);
        else
            hide();
    }

    public void mouseEntered(MouseEvent e) {
        if(showCoordinates)
            show();
    }

    public void mouseExited(MouseEvent e) {
        hide();
    }
}
