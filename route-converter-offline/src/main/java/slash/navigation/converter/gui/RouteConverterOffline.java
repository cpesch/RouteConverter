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
import slash.navigation.converter.gui.actions.ShowMapsAction;
import slash.navigation.converter.gui.actions.ShowThemesAction;
import slash.navigation.converter.gui.helpers.AutomaticElevationService;
import slash.navigation.converter.gui.helpers.MapViewImplementation;
import slash.navigation.datasources.DataSource;
import slash.navigation.graphhopper.GraphHopper;
import slash.navigation.gui.Application;
import slash.navigation.gui.notifications.NotificationManager;
import slash.navigation.hgt.HgtFiles;
import slash.navigation.maps.LocalMap;
import slash.navigation.maps.MapManager;
import slash.navigation.mapview.MapView;
import slash.navigation.mapview.mapsforge.MapViewCallbackOffline;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;
import slash.navigation.routing.Beeline;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import static java.util.Collections.singletonList;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.navigation.converter.gui.helpers.MapViewImplementation.Mapsforge;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.gui.helpers.JMenuHelper.createItem;
import static slash.navigation.gui.helpers.JMenuHelper.findMenu;

/**
 * A small graphical user interface for the offline route conversion.
 *
 * @author Christian Pesch
 */

public class RouteConverterOffline extends RouteConverter {
    private MapManager mapManager;
    private LocalMap mapAfterStart;

    public static void main(String[] args) {
        launch(RouteConverterOffline.class, new String[]{ RouteConverter.class.getPackage().getName() + ".Untranslated", RouteConverter.class.getName()}, args);
    }

    public String getEdition() {
        return "RouteConverter Offline Edition";
    }

    public String getEditionId() {
        return "offline";
    }

    public List<MapViewImplementation> getAvailableMapViews() {
        return singletonList(Mapsforge);
    }

    protected void initializeServices() {
        super.initializeServices();
        mapManager = new MapManager(getDataSourceManager());
        mapAfterStart = getMapManager().getDisplayedMapModel().getItem();
    }

    protected void initializeActions() {
        super.initializeActions();
        getContext().getActionManager().register("show-maps", new ShowMapsAction());
        getContext().getActionManager().register("show-themes", new ShowThemesAction());
        JMenu viewMenu = findMenu(getContext().getMenuBar(), "view");
        if (viewMenu != null) {
            viewMenu.add(createItem("show-maps"), 0);
            viewMenu.add(createItem("show-themes"), 1);
            viewMenu.add(new JPopupMenu.Separator(), 2);
        }
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    protected MapViewCallbackOffline getMapViewCallback() {
        return new MapViewCallbackOfflineImpl();
    }

    protected void initializeRoutingServices() {
        Beeline beeline = new Beeline();
        getRoutingServiceFacade().addRoutingService(beeline);
        getRoutingServiceFacade().setPreferredRoutingService(beeline);

        BRouter router = new BRouter(getDownloadManager());
        getRoutingServiceFacade().addRoutingService(router);

        GraphHopper hopper = new GraphHopper(getDownloadManager());
        getRoutingServiceFacade().addRoutingService(hopper);

        configureRoutingServices();

        getNotificationManager().showNotification(RouteConverter.getBundle().getString("routing-updated"),
                Application.getInstance().getContext().getActionManager().get("show-downloads"));
    }

    protected void updateRoutingServices() {
        configureRoutingServices();
    }

    private void configureRoutingServices() {
        DataSource brouterProfiles = getDataSourceManager().getDataSourceService().getDataSourceById("brouter-profiles");
        DataSource brouterSegments = getDataSourceManager().getDataSourceService().getDataSourceById("brouter-segments-4");
        if (brouterProfiles != null && brouterSegments != null) {
            BRouter router = getRoutingServiceFacade().getRoutingService(BRouter.class);
            router.setProfilesAndSegments(brouterProfiles, brouterSegments);
        }

        DataSource graphhopper = getDataSourceManager().getDataSourceService().getDataSourceById("graphhopper");
        if (graphhopper != null) {
            GraphHopper hopper = getRoutingServiceFacade().getRoutingService(GraphHopper.class);
            hopper.setDataSource(graphhopper);
        }
    }

    protected void initializeElevationServices() {
        AutomaticElevationService automaticElevationService = new AutomaticElevationService(getElevationServiceFacade());
        getElevationServiceFacade().addElevationService(automaticElevationService);
        getElevationServiceFacade().setPreferredElevationService(automaticElevationService);

        getHgtFilesService().initialize();
        for (HgtFiles hgtFile : getHgtFilesService().getHgtFiles()) {
            getElevationServiceFacade().addElevationService(hgtFile);
        }
    }

    private NotificationManager getNotificationManager() {
        return Application.getInstance().getContext().getNotificationManager();
    }

    protected void scanLocalMapsAndThemes() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    getMapManager().scanMaps();
                    getMapManager().scanThemes();

                    getNotificationManager().showNotification(RouteConverter.getBundle().getString("map-updated"),
                            Application.getInstance().getContext().getActionManager().get("show-maps"));
                } catch (final IOException e) {
                    invokeLater(new Runnable() {
                        public void run() {
                            showMessageDialog(frame, MessageFormat.format(getBundle().getString("scan-error"), e), frame.getTitle(), ERROR_MESSAGE);
                        }
                    });
                }

                LocalMap mapAfterScan = getMapManager().getDisplayedMapModel().getItem();
                if (mapAfterStart != mapAfterScan) {
                    MapView mapView = getMapView();
                    if (mapView instanceof MapsforgeMapView)
                        ((MapsforgeMapView) mapView).updateMapAndThemesAfterDirectoryScanning();
                }
            }
        }, "DirectoryScanner").start();
    }

    protected void scanRemoteMapsAndThemes() {
        getMapManager().scanDatasources();

        final File file = new File(getApplicationDirectory("maps/routeconverter"), "world.map");
        getDownloadManager().executeDownload("RouteConverter Background Map", "http://static.routeconverter.com/maps/world.map", Copy, file, new Runnable() {
            public void run() {
                invokeLater(new Runnable() {
                    public void run() {
                        MapView mapView = getMapView();
                        if (mapView instanceof MapsforgeMapView)
                            ((MapsforgeMapView) mapView).setBackgroundMap(file);
                    }
                });
            }
        });
    }
}
