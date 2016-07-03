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
import org.mapsforge.map.util.MapViewProjection;
import org.mapsforge.map.view.MapView;
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

import static javax.swing.BorderFactory.createEtchedBorder;

/**
 * Show coordinates upon to mouse events of the {@link AwtGraphicMapView}.
 *
 * @author Christian Pesch
 */

public class MapViewCoordinateDisplayer extends MouseAdapter {
    private MapView mapView;
    private MapViewCallback mapViewCallback;
    private JWindow window;
    private JLabel label = new JLabel();
    private boolean showCoordinates;

    public void initialize(AwtGraphicMapView mapView, MapViewCallback mapViewCallback) {
        this.mapView = mapView;
        this.mapViewCallback = mapViewCallback;

        window = new JWindow(getFrame());
        JPanel contentPane = (JPanel) window.getContentPane();
        contentPane.add(label);
        contentPane.setBackground(new Color(255, 255, 204));
        contentPane.setBorder(new CompoundBorder(createEtchedBorder(), new EmptyBorder(2, 3, 2, 3)));
        label.setFont(label.getFont().deriveFont(9f));

        mapView.addMouseListener(this);
        mapView.addMouseMotionListener(this);
    }

    public void setShowCoordinates(boolean showCoordinates) {
        this.showCoordinates = showCoordinates;
    }

    private JFrame getFrame() {
        Application application = Application.getInstance();
        if (!(application instanceof SingleFrameApplication))
            return null;
        return ((SingleFrameApplication) application).getFrame();
    }

    private void display(Point locationOnScreen, int mouseX, int mouseY) {
        LatLong latLong = new MapViewProjection(mapView).fromPixels(mouseX, mouseY);
        label.setText(mapViewCallback.createCoordinates(latLong.longitude, latLong.latitude));
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
        if (showCoordinates)
            display(e.getLocationOnScreen(), e.getX(), e.getY());
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
