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
package slash.navigation.maps.mapsforge;

import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import slash.common.filtering.FilteringTableModel;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.Download;
import slash.navigation.maps.item.ItemModel;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.mapsforge.helpers.ActiveTileMapPredicate;
import slash.navigation.maps.mapsforge.helpers.ThemeForMapMediator;
import slash.navigation.maps.mapsforge.helpers.TileServerToTileMapMediator;
import slash.navigation.maps.mapsforge.impl.MapFilesService;
import slash.navigation.maps.mapsforge.impl.TileMap;
import slash.navigation.maps.mapsforge.impl.VectorMap;
import slash.navigation.maps.mapsforge.impl.VectorTheme;
import slash.navigation.maps.mapsforge.models.JoinedTableModel;
import slash.navigation.maps.mapsforge.models.OpenStreetMap;
import slash.navigation.maps.mapsforge.models.TileMapTableModel;
import slash.navigation.maps.tileserver.TileServerMapManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.sort;
import static org.mapsforge.map.rendertheme.InternalRenderTheme.DEFAULT;
import static org.mapsforge.map.rendertheme.InternalRenderTheme.OSMARENDER;
import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.collectFiles;
import static slash.common.io.Files.printArrayToDialogString;
import static slash.navigation.maps.mapsforge.helpers.MapUtil.extractBoundingBox;
import static slash.navigation.maps.mapsforge.helpers.MapUtil.removePrefix;
import static slash.navigation.maps.mapsforge.models.OpenStreetMap.OPENSTREETMAP_URL;

/**
 * Manages {@link LocalMap}s and {@link LocalTheme}s
 *
 * @author Christian Pesch
 */

public class MapsforgeMapManager {
    private static final Logger log = Logger.getLogger(MapsforgeMapManager.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(MapsforgeMapManager.class);
    private static final String MAP_DIRECTORY_PREFERENCE = "mapDirectory";
    private static final String THEME_DIRECTORY_PREFERENCE = "themeDirectory";
    private static final String DISPLAYED_MAP_PREFERENCE = "displayedMap";
    private static final String APPLIED_THEME_PREFERENCE = "appliedTheme";
    private static final String DEFAULT_URL = "http://wiki.openstreetmap.org/wiki/Default";
    private static final String OSMARENDER_URL = "http://wiki.openstreetmap.org/wiki/Osmarender";
    private static final String DOT_MAP = ".map";

    private final DataSourceManager dataSourceManager;
    private ItemTableModel<TileMap> availableOnlineMapsModel = new TileMapTableModel();
    private ItemTableModel<LocalMap> availableOfflineMapsModel = new ItemTableModel<>(1);
    private JoinedTableModel<LocalMap> availableMapsModel = new JoinedTableModel<>(availableOfflineMapsModel,
            new FilteringTableModel<>(availableOnlineMapsModel, new ActiveTileMapPredicate()));
    private ItemTableModel<LocalTheme> availableThemesModel = new ItemTableModel<>(1);
    private ItemTableModel<RemoteMap> downloadableMapsModel = new ItemTableModel<>(3);
    private ItemTableModel<RemoteTheme> downloadableThemesModel = new ItemTableModel<>(3);

    private ItemModel<LocalMap> displayedMapModel = new ItemModel<LocalMap>(DISPLAYED_MAP_PREFERENCE,  OPENSTREETMAP_URL) {
        protected LocalMap stringToItem(String url) {
            return getAvailableMapsModel().getItemByUrl(url);
        }

        protected String itemToString(LocalMap map) {
            return map.getUrl();
        }
    };

    private ItemModel<LocalTheme> appliedThemeModel = new ItemModel<LocalTheme>(APPLIED_THEME_PREFERENCE, OSMARENDER_URL) {
        protected LocalTheme stringToItem(String url) {
            return getAvailableThemesModel().getItemByUrl(url);
        }

        protected String itemToString(LocalTheme theme) {
            return theme.getUrl();
        }
    };

    private ThemeForMapMediator themeForMapMediator;
    private TileServerToTileMapMediator tileServerToTileMapMediator;

    public MapsforgeMapManager(DataSourceManager dataSourceManager, TileServerMapManager tileServerMapManager) {
        this.dataSourceManager = dataSourceManager;

        themeForMapMediator = new ThemeForMapMediator(this);
        tileServerToTileMapMediator = new TileServerToTileMapMediator(tileServerMapManager.getAvailableMapsModel(), availableOnlineMapsModel);
        initializeOpenStreetMap();
        initializeBuiltinThemes();
    }

    public void dispose() {
        themeForMapMediator.dispose();
        themeForMapMediator = null;
        tileServerToTileMapMediator.dispose();
        tileServerToTileMapMediator = null;
    }

    public JoinedTableModel<LocalMap> getAvailableMapsModel() {
        return availableMapsModel;
    }

    public ItemTableModel<TileMap> getAvailableOnlineMapsModel() {
        return availableOnlineMapsModel;
    }

    public ItemTableModel<LocalMap> getAvailableOfflineMapsModel() {
        return availableOfflineMapsModel;
    }

    public ItemTableModel<RemoteMap> getDownloadableMapsModel() {
        return downloadableMapsModel;
    }

    public ItemModel<LocalMap> getDisplayedMapModel() {
        return displayedMapModel;
    }

    public ItemTableModel<LocalTheme> getAvailableThemesModel() {
        return availableThemesModel;
    }

    public ItemTableModel<RemoteTheme> getDownloadableThemesModel() {
        return downloadableThemesModel;
    }

    public ItemModel<LocalTheme> getAppliedThemeModel() {
        return appliedThemeModel;
    }

    public String getMapsPath() {
        return preferences.get(MAP_DIRECTORY_PREFERENCE, "");
    }

    public void setMapsPath(String path) {
        preferences.put(MAP_DIRECTORY_PREFERENCE, path);
    }

    public String getThemePath() {
        return preferences.get(THEME_DIRECTORY_PREFERENCE, "");
    }

    public void setThemePath(String path) {
        preferences.put(THEME_DIRECTORY_PREFERENCE, path);
    }

    private File getDirectory(String preferencesPath, String directoryName) {
        java.io.File f = new java.io.File(preferencesPath);
        if (f.exists())
            return f;
        return ensureDirectory(getApplicationDirectory(directoryName).getAbsolutePath());
    }

    public File getMapsDirectory() {
        return getDirectory(getMapsPath(), "maps");
    }

    public File getThemesDirectory() {
        return getDirectory(getThemePath(), "themes");
    }

    private void initializeOpenStreetMap() {
        availableOnlineMapsModel.addOrUpdateItem(new OpenStreetMap());
    }

    private void initializeBuiltinThemes() {
        availableThemesModel.addOrUpdateItem(new VectorTheme("OpenStreetMap Default", DEFAULT_URL, DEFAULT));
        availableThemesModel.addOrUpdateItem(new VectorTheme("OpenStreetMap Osmarender", OSMARENDER_URL, OSMARENDER));
    }

    private void checkFile(File file) throws FileNotFoundException {
        if (!file.exists())
            throw new FileNotFoundException("file does not exist: " + file.getAbsolutePath());
        else if (!file.isFile())
            throw new FileNotFoundException("not a file: " + file.getAbsolutePath());
        else if (!file.canRead())
            throw new FileNotFoundException("cannot read file: " + file.getAbsolutePath());
    }

    public synchronized void scanMaps() throws IOException {
        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                availableOfflineMapsModel.clear();
                initializeOpenStreetMap();
            }
        });

        long start = currentTimeMillis();

        final File mapsDirectory = getMapsDirectory();
        List<File> mapFiles = collectFiles(mapsDirectory, DOT_MAP);
        File[] mapFilesArray = mapFiles.toArray(new File[0]);
        for (final File file : mapFilesArray) {
            // avoid directory with world.map
            if(file.getParent().endsWith("routeconverter"))
                continue;

            checkFile(file);
            invokeInAwtEventQueue(new Runnable() {
                public void run() {
                    availableOfflineMapsModel.addOrUpdateItem(new VectorMap(removePrefix(mapsDirectory, file), file.toURI().toString(), extractBoundingBox(file), file));
                }
            });
        }

        long end = currentTimeMillis();
        log.info(format("Collected %d map files %s from %s in %d milliseconds",
                mapFilesArray.length, printArrayToDialogString(mapFilesArray, false), mapsDirectory, (end - start)));
    }

    public synchronized void scanThemes() throws IOException {
        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                availableThemesModel.clear();
                initializeBuiltinThemes();
            }
        });

        long start = currentTimeMillis();

        final File themesDirectory = getThemesDirectory();
        List<File> themeFiles = collectFiles(themesDirectory, ".xml");
        File[] themeFilesArray = themeFiles.toArray(new File[0]);
        for (final File file : themeFilesArray) {
            checkFile(file);
            final ExternalRenderTheme renderTheme = new ExternalRenderTheme(file);
            invokeInAwtEventQueue(new Runnable() {
                public void run() {
                    availableThemesModel.addOrUpdateItem(new VectorTheme(removePrefix(themesDirectory, file), file.toURI().toString(), renderTheme));
                }
            });
        }

        long end = currentTimeMillis();
        log.info(format("Collected %d theme files %s from %s in %d milliseconds",
                themeFilesArray.length, printArrayToDialogString(themeFilesArray,false), themesDirectory, (end - start)));
    }

    public void scanDatasources() {
        MapFilesService mapFilesService = new MapFilesService(dataSourceManager);
        mapFilesService.initialize();

        List<RemoteMap> maps = mapFilesService.getMaps();
        RemoteMap[] remoteMaps = maps.toArray(new RemoteMap[0]);
        sort(remoteMaps, new Comparator<RemoteResource>() {
            public int compare(RemoteResource r1, RemoteResource r2) {
                return (r1.getDataSource() + r1.getUrl()).compareToIgnoreCase(r2.getDataSource() + r2.getUrl());
            }
        });
        for (RemoteMap remoteMap : remoteMaps)
            downloadableMapsModel.addOrUpdateItem(remoteMap);

        List<RemoteTheme> themes = mapFilesService.getThemes();
        RemoteTheme[] remoteThemes = themes.toArray(new RemoteTheme[0]);
        sort(remoteThemes, new Comparator<RemoteResource>() {
            public int compare(RemoteResource r1, RemoteResource r2) {
                return (r1.getDataSource() + r1.getUrl()).compareToIgnoreCase(r2.getDataSource() + r2.getUrl());
            }
        });
        for (RemoteTheme remoteTheme : remoteThemes)
            downloadableThemesModel.addOrUpdateItem(remoteTheme);
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
