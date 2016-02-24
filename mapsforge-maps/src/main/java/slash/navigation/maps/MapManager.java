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
package slash.navigation.maps;

import org.mapsforge.map.layer.download.tilesource.OpenCycleMap;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.Download;
import slash.navigation.maps.helpers.ThemeForMapMediator;
import slash.navigation.maps.impl.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.sort;
import static org.mapsforge.map.rendertheme.InternalRenderTheme.OSMARENDER;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.collectFiles;
import static slash.common.io.Files.printArrayToDialogString;
import static slash.navigation.maps.helpers.MapUtil.extractBoundingBox;
import static slash.navigation.maps.helpers.MapUtil.removePrefix;

/**
 * Manages {@link LocalMap}s and {@link LocalTheme}s
 *
 * @author Christian Pesch
 */

public class MapManager {
    private static final Logger log = Logger.getLogger(MapManager.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(MapManager.class);
    private static final String MAP_DIRECTORY_PREFERENCE = "mapDirectory";
    private static final String THEME_DIRECTORY_PREFERENCE = "themeDirectory";
    private static final String DISPLAYED_MAP_PREFERENCE = "displayedMap";
    private static final String APPLIED_THEME_PREFERENCE = "appliedTheme";
    private static final String OSMARENDER_URL = "http://wiki.openstreetmap.org/wiki/Osmarender";
    private static final String OPENSTREETMAP_URL = "http://www.openstreetmap.org/";

    private final DataSourceManager dataSourceManager;
    private LocalMapsTableModel availableMapsModel = new LocalMapsTableModel();
    private LocalThemesTableModel availableThemesModel = new LocalThemesTableModel();
    private RemoteMapsTableModel downloadableMapsModel = new RemoteMapsTableModel();
    private RemoteThemesTableModel downloadableThemesModel = new RemoteThemesTableModel();

    private ItemModel<LocalMap> displayedMapModel = new ItemModel<LocalMap>(DISPLAYED_MAP_PREFERENCE, OPENSTREETMAP_URL) {
        protected LocalMap stringToItem(String url) {
            return getAvailableMapsModel().getMap(url);
        }

        protected String itemToString(LocalMap map) {
            return map.getUrl();
        }
    };

    private ItemModel<LocalTheme> appliedThemeModel = new ItemModel<LocalTheme>(APPLIED_THEME_PREFERENCE, OSMARENDER_URL) {
        protected LocalTheme stringToItem(String url) {
            return getAvailableThemesModel().getThemeByUrl(url);
        }

        protected String itemToString(LocalTheme theme) {
            return theme.getUrl();
        }
    };

    public MapManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;

        new ThemeForMapMediator(this);
        initializeOnlineMaps();
        initializeBuiltinThemes();
    }

    public LocalMapsTableModel getAvailableMapsModel() {
        return availableMapsModel;
    }

    public RemoteMapsTableModel getDownloadableMapsModel() {
        return downloadableMapsModel;
    }

    public ItemModel<LocalMap> getDisplayedMapModel() {
        return displayedMapModel;
    }

    public LocalThemesTableModel getAvailableThemesModel() {
        return availableThemesModel;
    }

    public RemoteThemesTableModel getDownloadableThemesModel() {
        return downloadableThemesModel;
    }

    public ItemModel<LocalTheme> getAppliedThemeModel() {
        return appliedThemeModel;
    }

    public File getMapsDirectory() {
        return new File(preferences.get(MAP_DIRECTORY_PREFERENCE, getApplicationDirectory("maps").getAbsolutePath()));
    }

    public File getThemesDirectory() {
        return new File(preferences.get(THEME_DIRECTORY_PREFERENCE, getApplicationDirectory("themes").getAbsolutePath()));
    }

    private void initializeOnlineMaps() {
        availableMapsModel.clear();
        availableMapsModel.addOrUpdateMap(new OnlineMap("OpenStreetMap", OPENSTREETMAP_URL, OpenStreetMapMapnik.INSTANCE));
        availableMapsModel.addOrUpdateMap(new OnlineMap("OpenCycleMap", "http://www.opencyclemap.org/", OpenCycleMap.INSTANCE));
    }

    private void initializeBuiltinThemes() {
        availableThemesModel.clear();
        availableThemesModel.addOrUpdateTheme(new VectorTheme("OpenStreetMap Osmarender", OSMARENDER_URL, OSMARENDER));
    }

    public synchronized void scanMaps() throws IOException {
        initializeOnlineMaps();

        long start = currentTimeMillis();

        File mapsDirectory = ensureDirectory(getMapsDirectory());
        List<File> mapFiles = collectFiles(mapsDirectory, ".map");
        File[] mapFilesArray = mapFiles.toArray(new File[mapFiles.size()]);
        for (File file : mapFilesArray) {
            if(file.getParent().endsWith("routeconverter"))
                continue;

            availableMapsModel.addOrUpdateMap(new VectorMap(removePrefix(mapsDirectory, file), file.toURI().toString(), extractBoundingBox(file), file));
        }

        long end = currentTimeMillis();
        log.info(format("Collected %d map files %s from %s in %d milliseconds",
                mapFilesArray.length, printArrayToDialogString(mapFilesArray), mapsDirectory, (end - start)));
    }

    public synchronized void scanThemes() throws IOException {
        initializeBuiltinThemes();

        long start = currentTimeMillis();

        File themesDirectory = ensureDirectory(getThemesDirectory());
        List<File> themeFiles = collectFiles(themesDirectory, ".xml");
        File[] themeFilesArray = themeFiles.toArray(new File[themeFiles.size()]);
        for (File file : themeFilesArray)
            availableThemesModel.addOrUpdateTheme(new VectorTheme(removePrefix(themesDirectory, file), file.toURI().toString(), new ExternalRenderTheme(file)));

        long end = currentTimeMillis();
        log.info(format("Collected %d theme files %s from %s in %d milliseconds",
                themeFilesArray.length, printArrayToDialogString(themeFilesArray), themesDirectory, (end - start)));
    }

    public void scanDatasources() {
        MapFilesService mapFilesService = new MapFilesService(dataSourceManager);
        mapFilesService.initialize();

        List<RemoteMap> maps = mapFilesService.getMaps();
        RemoteMap[] remoteMaps = maps.toArray(new RemoteMap[maps.size()]);
        sort(remoteMaps, new Comparator<RemoteResource>() {
            public int compare(RemoteResource r1, RemoteResource r2) {
                return (r1.getDataSource() + r1.getUrl()).compareToIgnoreCase(r2.getDataSource() + r2.getUrl());
            }
        });
        for (RemoteMap remoteMap : remoteMaps)
            downloadableMapsModel.addOrUpdateMap(remoteMap);

        List<RemoteTheme> themes = mapFilesService.getThemes();
        RemoteTheme[] remoteThemes = themes.toArray(new RemoteTheme[themes.size()]);
        sort(remoteThemes, new Comparator<RemoteResource>() {
            public int compare(RemoteResource r1, RemoteResource r2) {
                return (r1.getDataSource() + r1.getUrl()).compareToIgnoreCase(r2.getDataSource() + r2.getUrl());
            }
        });
        for (RemoteTheme remoteTheme : remoteThemes)
            downloadableThemesModel.addOrUpdateTheme(remoteTheme);
    }

    public void queueForDownload(List<? extends RemoteResource> resources) {
        List<Download> downloads = new ArrayList<>();
        for (RemoteResource resource : resources) {
            Downloadable downloadable = resource.getDownloadable();
            DataSource dataSource = resource.getDataSource();

            downloads.add(dataSourceManager.queueForDownload(dataSource, downloadable));
        }
        dataSourceManager.getDownloadManager().waitForCompletion(downloads);
    }
}
