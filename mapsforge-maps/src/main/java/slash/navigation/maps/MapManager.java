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
import slash.navigation.download.Action;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.maps.models.DownloadMap;
import slash.navigation.maps.models.ItemModel;
import slash.navigation.maps.models.MapsTableModel;
import slash.navigation.maps.models.RendererMap;
import slash.navigation.maps.models.ResourcesTableModel;
import slash.navigation.maps.models.ThemeImpl;
import slash.navigation.maps.models.ThemesTableModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.io.File.separator;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static org.mapsforge.map.rendertheme.InternalRenderTheme.OSMARENDER;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.collectFiles;
import static slash.common.io.Files.printArrayToDialogString;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.Action.Extract;
import static slash.navigation.maps.helpers.MapUtil.extractBoundingBox;

/**
 * Manages {@link LocalMap}s and {@link Theme}s
 *
 * @author Christian Pesch
 */

public class MapManager {
    private static final Logger log = Logger.getLogger(MapManager.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(MapManager.class);
    public static final LocalMap SEPARATOR_TO_DOWNLOAD_MAP = new DownloadMap(null, null, null);
    public static final LocalMap DOWNLOAD_MAP = new DownloadMap(null, null, null);
    public static final Theme SEPARATOR_TO_DOWNLOAD_THEME = new ThemeImpl(null, null, null);
    public static final Theme DOWNLOAD_THEME = new ThemeImpl(null, null, null);
    private static final String MAP_DIRECTORY_PREFERENCE = "mapDirectory";
    private static final String THEME_DIRECTORY_PREFERENCE = "themeDirectory";
    private static final String DISPLAYED_MAP_PREFERENCE = "displayedMap";
    private static final String APPLIED_THEME_PREFERENCE = "appliedTheme";
    private static final String OSMARENDER_URL = "http://wiki.openstreetmap.org/wiki/Osmarender";
    private static final String OPENSTREETMAP_URL = "http://www.openstreetmap.org/";

    private final DownloadManager downloadManager;
    private MapsTableModel mapsModel = new MapsTableModel();
    private ThemesTableModel themesModel = new ThemesTableModel();
    private ResourcesTableModel resourcesModel = new ResourcesTableModel();

    private ItemModel<LocalMap> displayedMapModel = new ItemModel<LocalMap>(DISPLAYED_MAP_PREFERENCE, OPENSTREETMAP_URL) {
        protected LocalMap stringToItem(String url) {
            return getMapsModel().getMap(url);
        }

        protected String itemToString(LocalMap map) {
            return map.getUrl();
        }
    };

    private ItemModel<Theme> appliedThemeModel = new ItemModel<Theme>(APPLIED_THEME_PREFERENCE, OSMARENDER_URL) {
        protected Theme stringToItem(String url) {
            return getThemesModel().getTheme(url);
        }

        protected String itemToString(Theme theme) {
            return theme.getUrl();
        }
    };

    public MapManager(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public MapsTableModel getMapsModel() {
        return mapsModel;
    }

    public ItemModel<LocalMap> getDisplayedMapModel() {
        return displayedMapModel;
    }

    public ThemesTableModel getThemesModel() {
        return themesModel;
    }

    public ItemModel<Theme> getAppliedThemeModel() {
        return appliedThemeModel;
    }

    public ResourcesTableModel getResourcesModel() {
        return resourcesModel;
    }

    private String getMapsDirectory() {
        return preferences.get(MAP_DIRECTORY_PREFERENCE, getApplicationDirectory("maps").getAbsolutePath());
    }

    private String getThemesDirectory() {
        return preferences.get(THEME_DIRECTORY_PREFERENCE, getApplicationDirectory("themes").getAbsolutePath());
    }

    public synchronized void scanDirectories() throws IOException {
        scanMaps();
        scanThemes();
    }

    private void scanMaps() {
        long start = currentTimeMillis();

        mapsModel.clear();
        mapsModel.addOrUpdateMap(new DownloadMap("OpenStreetMap - a map of the world, created by people like you and free to use under an open license.", OPENSTREETMAP_URL, OpenStreetMapMapnik.INSTANCE));
        mapsModel.addOrUpdateMap(new DownloadMap("OpenCycleMap.org - the OpenStreetMap Cycle Map", "http://www.opencyclemap.org/", OpenCycleMap.INSTANCE));

        File mapsDirectory = ensureDirectory(getMapsDirectory());
        List<File> mapFiles = collectFiles(mapsDirectory, ".map");
        File[] mapFilesArray = mapFiles.toArray(new File[mapFiles.size()]);

        for (File file : mapFilesArray)
            mapsModel.addOrUpdateMap(new RendererMap(removePrefix(mapsDirectory, file), file.toURI().toString(), extractBoundingBox(file), file));
        mapsModel.addOrUpdateMap(SEPARATOR_TO_DOWNLOAD_MAP);
        mapsModel.addOrUpdateMap(DOWNLOAD_MAP);

        long end = currentTimeMillis();
        log.info("Collected map files " + printArrayToDialogString(mapFilesArray) + " from " + mapsDirectory + " in " + (end - start) + " milliseconds");
    }

    private void scanThemes() throws FileNotFoundException {
        long start = currentTimeMillis();

        themesModel.clear();
        themesModel.addOrUpdateTheme(new ThemeImpl("A render-theme similar to the OpenStreetMap Osmarender style", OSMARENDER_URL, OSMARENDER));

        File themesDirectory = ensureDirectory(getThemesDirectory());
        List<File> themeFiles = collectFiles(themesDirectory, ".xml");
        File[] themeFilesArray = themeFiles.toArray(new File[themeFiles.size()]);

        for (File file : themeFilesArray)
            themesModel.addOrUpdateTheme(new ThemeImpl(removePrefix(themesDirectory, file), file.toURI().toString(), new ExternalRenderTheme(file)));
        themesModel.addOrUpdateTheme(SEPARATOR_TO_DOWNLOAD_THEME);
        themesModel.addOrUpdateTheme(DOWNLOAD_THEME);

        long end = currentTimeMillis();
        log.info("Collected theme files " + printArrayToDialogString(themeFilesArray) + " from " + themesDirectory + " in " + (end-start) + " milliseconds");
    }

    private static String removePrefix(File root, File file) {
        String rootPath = root.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.startsWith(rootPath))
            filePath = filePath.substring(rootPath.length());
        else
            filePath = file.getName();
        if (filePath.startsWith(separator))
            filePath = filePath.substring(1);
        return filePath;
    }

    public void initialize() {
        List<RemoteResource> resources = new MapFilesService().getResources();
        RemoteResource[] remoteResources = resources.toArray(new RemoteResource[resources.size()]);
        sort(remoteResources, new Comparator<RemoteResource>() {
            public int compare(RemoteResource r1, RemoteResource r2) {
                return (r1.getDataSource() + r1.getUrl()).compareToIgnoreCase(r2.getDataSource() + r2.getUrl());
            }
        });
        for (RemoteResource resource : remoteResources)
            resourcesModel.addOrUpdateResource(resource);
    }

    public void queueForDownload(RemoteResource resource) {
        slash.navigation.download.datasources.File file = resource.getFile();
        Action action = resource.getFile().getUri().endsWith(".zip") ? Extract : Copy;
        File target = action.equals(Extract) ? getDirectory(resource) : getFile(resource);
        Download download = downloadManager.queueForDownload(resource.getDataSource() + ": " + file.getUri(), resource.getUrl(),
                file.getSize(), file.getChecksum(), file.getTimestamp(), action, target);
        downloadManager.waitForCompletion(asList(download));
    }

    private File getFile(RemoteResource resource) {
        return new File(getApplicationDirectory(resource.getSubDirectory()), resource.getFile().getUri());
    }

    private File getDirectory(RemoteResource resource) {
        String subDirectory = resource.getSubDirectory();
        if (resource instanceof RemoteMap)
            return ensureDirectory(getMapsDirectory() + separator + resource.getSubDirectory().substring(5));
        else if (subDirectory.startsWith("themes/"))
            return ensureDirectory(getThemesDirectory() + separator + resource.getSubDirectory().substring(7));
        return getApplicationDirectory(resource.getSubDirectory());
    }
}
