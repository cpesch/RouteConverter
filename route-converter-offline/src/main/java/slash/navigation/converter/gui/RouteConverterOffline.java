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
import slash.navigation.converter.gui.actions.ShowMapsAndThemesAction;
import slash.navigation.converter.gui.helpers.AutomaticElevationService;
import slash.navigation.converter.gui.helpers.MapViewImpl;
import slash.navigation.converter.gui.mapview.MapViewCallbackOffline;
import slash.navigation.converter.gui.mapview.MapsforgeMapView;
import slash.navigation.datasources.DataSource;
import slash.navigation.graphhopper.GraphHopper;
import slash.navigation.gui.Application;
import slash.navigation.gui.notifications.NotificationManager;
import slash.navigation.hgt.HgtFiles;
import slash.navigation.maps.LocalMap;
import slash.navigation.maps.MapManager;
import slash.navigation.maps.RemoteResource;
import slash.navigation.routing.BeelineService;

import javax.swing.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.navigation.converter.gui.helpers.MapViewImpl.Mapsforge;
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
        launch(RouteConverterOffline.class, args);
    }

    public String getEdition() {
        return "Offline";
    }

    public List<MapViewImpl> getAvailableMapViews() {
        return singletonList(Mapsforge);
    }

    protected void initializeServices() {
        super.initializeServices();
        mapManager = new MapManager(getDataSourceManager());
        mapAfterStart = getMapManager().getDisplayedMapModel().getItem();
    }

    protected void initializeActions() {
        super.initializeActions();
        getContext().getActionManager().register("show-maps-and-themes", new ShowMapsAndThemesAction());
        JMenu viewMenu = findMenu(getContext().getMenuBar(), "view");
        if (viewMenu != null) {
            viewMenu.add(createItem("show-maps-and-themes"), 0);
            viewMenu.add(new JPopupMenu.Separator(), 1);
        }
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    protected MapViewCallbackOffline getMapViewCallback() {
        return new MapViewCallbackOfflineImpl();
    }

    protected void initializeRoutingServices() {
        BeelineService beeline = new BeelineService();
        getRoutingServiceFacade().addRoutingService(beeline);
        getRoutingServiceFacade().setPreferredRoutingService(beeline);

        BRouter router = new BRouter(getDataSourceManager().getDownloadManager());
        getRoutingServiceFacade().addRoutingService(router);

        DataSource brouterProfiles = getDataSourceManager().getDataSourceService().getDataSourceById("brouter-profiles");
        DataSource brouterSegments = getDataSourceManager().getDataSourceService().getDataSourceById("brouter-segments");
        if (brouterProfiles != null && brouterSegments != null) {
            router.setProfilesAndSegments(brouterProfiles, brouterSegments);
            getRoutingServiceFacade().setPreferredRoutingService(router);
        }

        GraphHopper hopper = new GraphHopper(getDataSourceManager().getDownloadManager());
        getRoutingServiceFacade().addRoutingService(hopper);

        DataSource graphhopper = getDataSourceManager().getDataSourceService().getDataSourceById("graphhopper");
        if (graphhopper != null)
            hopper.setDataSource(graphhopper);

        getNotificationManager().showNotification(RouteConverter.getBundle().getString("routing-updated"), getSelectMapsAction());
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

    private Action getSelectMapsAction() {
        return Application.getInstance().getContext().getActionManager().get("show-maps-and-themes");
    }

    protected void scanLocalMapsAndThemes() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    getMapManager().scanDirectories();

                    getNotificationManager().showNotification(RouteConverter.getBundle().getString("map-updated"), getSelectMapsAction());
                } catch (final IOException e) {
                    invokeLater(new Runnable() {
                        public void run() {
                            showMessageDialog(frame, MessageFormat.format(getBundle().getString("scan-error"), e), frame.getTitle(), ERROR_MESSAGE);
                        }
                    });
                }

                LocalMap mapAfterScan = getMapManager().getDisplayedMapModel().getItem();
                if (mapAfterStart != mapAfterScan && getMapView() instanceof MapsforgeMapView)
                    ((MapsforgeMapView) getMapView()).updateMapAndThemesAfterDirectoryScanning();
            }
        }, "DirectoryScanner").start();
    }

    protected void scanRemoteMapsAndThemes() {
        getMapManager().scanDatasources();

        DataSource routeconverterMaps = getDataSourceManager().getDataSourceService().getDataSourceById("routeconverter-maps");
        if (routeconverterMaps != null)
            downloadResource(routeconverterMaps, "oceans.map", "world.map");
    }

    private void downloadResource(DataSource dataSource, String... uris) {
        List<RemoteResource> resources = new ArrayList<>();
        boolean updateMap = false;
        for (String uri : uris) {
            RemoteResource resource = getMapManager().getResourcesModel().findResource(dataSource, uri);
            if (resource != null) {
                resources.add(resource);

                if (!getMapManager().getFile(resource).exists())
                    updateMap = true;
            }
        }

        if (resources.size() > 0)
            getMapManager().queueForDownload(resources);

        if (updateMap) {
            scanLocalMapsAndThemes();
            ((MapsforgeMapView) getMapView()).updateMapAndThemesAfterDirectoryScanning();
        }
    }
}
