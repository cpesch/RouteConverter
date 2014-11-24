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

import slash.navigation.brouter.BRouter;
import slash.navigation.datasources.DataSource;
import slash.navigation.graphhopper.GraphHopper;
import slash.navigation.gui.Application;
import slash.navigation.gui.notifications.NotificationManager;
import slash.navigation.routing.BeelineRoutingService;

import javax.swing.*;
import java.io.IOException;
import java.text.MessageFormat;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * A small graphical user interface for the offline route conversion.
 *
 * @author Christian Pesch
 */

public class RouteConverterOffline extends RouteConverter {
    public static void main(String[] args) {
        launch(RouteConverterOffline.class, args);
    }

    protected String getEdition() {
        return "Offline";
    }

    protected void initializeRoutingServices() {
        getRoutingServiceFacade().addRoutingService(new BeelineRoutingService());
        DataSource brouter = getDataSourceManager().getDataSourceService().getDataSourceById("brouter");
        if (brouter != null) {
            BRouter router = new BRouter(brouter, getDataSourceManager().getDownloadManager());
            getRoutingServiceFacade().addRoutingService(router);
            log.info(String.format("Added routing service '%s'", router.getName()));
        }
        DataSource graphhopper = getDataSourceManager().getDataSourceService().getDataSourceById("graphhopper");
        if (graphhopper != null) {
            GraphHopper hopper = new GraphHopper(graphhopper, getDataSourceManager().getDownloadManager());
            getRoutingServiceFacade().addRoutingService(hopper);
            log.info(String.format("Added routing service '%s'", hopper.getName()));
        }
    }

    private NotificationManager getNotificationManager() {
        return Application.getInstance().getContext().getNotificationManager();
    }

    private Action getAction() {
        return Application.getInstance().getContext().getActionManager().get("select-maps");
    }

    protected void scanLocalMapsAndThemes() {
        try {
            getMapManager().scanDirectories();

            getNotificationManager().showNotification(RouteConverter.getBundle().getString("map-updated"), getAction());
        } catch (final IOException e) {
            invokeLater(new Runnable() {
                public void run() {
                    showMessageDialog(frame, MessageFormat.format(getBundle().getString("scan-error"), e), frame.getTitle(), ERROR_MESSAGE);
                }
            });
        }
    }

    protected void scanRemoteMapsAndThemes() {
        getMapManager().scanDatasources();
    }
}
