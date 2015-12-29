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
package slash.navigation.converter.gui;

import slash.navigation.converter.gui.helpers.MapViewCallbackImpl;
import slash.navigation.gui.Application;
import slash.navigation.gui.notifications.NotificationManager;
import slash.navigation.maps.MapManager;
import slash.navigation.mapview.MapView;
import slash.navigation.mapview.mapsforge.MapViewCallbackOffline;

import java.util.ResourceBundle;

import static java.text.MessageFormat.format;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.navigation.gui.helpers.UIHelper.getFrame;

/**
 * Implements the callbacks from the {@link MapView} to the other RouteConverter services including the {@link MapManager}
 * for the RouteConverter Offline Edition.
 *
 * @author Christian Pesch
 */

public class MapViewCallbackOfflineImpl extends MapViewCallbackImpl implements MapViewCallbackOffline {
    public MapManager getMapManager() {
        return ((RouteConverterOffline)Application.getInstance()).getMapManager();
    }

    private NotificationManager getNotificationManager() {
        return Application.getInstance().getContext().getNotificationManager();
    }

    private ResourceBundle getBundle() {
        return Application.getInstance().getContext().getBundle();
    }

    public void showDownloadNotification() {
        getNotificationManager().showNotification(getBundle().getString("downloading-routing-data"), null);
    }

    public void showProcessNotification() {
        getNotificationManager().showNotification(getBundle().getString("processing-routing-data"), null);
    }

    public void showRoutingException(Exception e) {
        //noinspection ConstantConditions
        showMessageDialog(getFrame(), format(getBundle().getString("cannot-route-position-list"), e),
                getFrame().getTitle(), ERROR_MESSAGE);

    }

    public void showMapException(String mapName, Exception e) {
        //noinspection ConstantConditions
        showMessageDialog(getFrame(), format(getBundle().getString("cannot-display-map"), mapName, e),
                getFrame().getTitle(), ERROR_MESSAGE);
    }
}
