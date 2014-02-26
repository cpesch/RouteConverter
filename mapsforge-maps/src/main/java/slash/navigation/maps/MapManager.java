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
import slash.navigation.download.DownloadManager;
import slash.navigation.maps.models.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static org.mapsforge.map.rendertheme.InternalRenderTheme.OSMARENDER;
import static slash.common.io.Files.collectFiles;
import static slash.common.io.Files.printArrayToDialogString;

/**
 * Manages {@link Map}s and {@link Theme}s
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

    private final DownloadManager downloadManager;
    private MapsTableModel mapsModel = new MapsTableModel();
    private ThemesTableModel themesModel = new ThemesTableModel();
    private ResourcesTableModel resourcesModel = new ResourcesTableModel();

    private ItemModel<Map> displayedMapModel = new ItemModel<Map>(DISPLAYED_MAP_PREFERENCE, OPENSTREETMAP_URL) {
        protected Map stringToItem(String url) {
            for(Map map : getMapsModel().getMaps()) {
                if(map.getUrl().equals(url))
                    return map;
            }
            return null;
        }

        protected String itemToString(Map map) {
            return map.getUrl();
        }
    };

    private ItemModel<Theme> appliedThemeModel = new ItemModel<Theme>(APPLIED_THEME_PREFERENCE, OSMARENDER_URL) {
        protected Theme stringToItem(String url) {
            for(Theme theme : getThemesModel().getThemes()) {
                if(theme.getUrl().equals(url))
                    return theme;
            }
            return null;
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

    public ItemModel<Map> getDisplayedMapModel() {
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

    private java.io.File getDirectory(String preferenceName, String defaultDirectoryName) {
        String directoryName = preferences.get(preferenceName,
                new java.io.File(System.getProperty("user.home"), ".routeconverter/" + defaultDirectoryName).getAbsolutePath());
        java.io.File directory = new java.io.File(directoryName);
        if (!directory.exists()) {
            if (!directory.mkdirs())
                throw new IllegalArgumentException(format("Cannot create '%s' directory '%s'", defaultDirectoryName, directory));
        }
        return directory;
    }

    private java.io.File getMapsDirectory() {
        return getDirectory(MAP_DIRECTORY_PREFERENCE, "maps");
    }

    private java.io.File getThemesDirectory() {
        return getDirectory(THEME_DIRECTORY_PREFERENCE, "themes");
    }

    public void scanDirectories() throws IOException {
        mapsModel.addOrUpdateMap(new DownloadMap("OpenStreetMap - a map of the world, created by people like you and free to use under an open license.", OPENSTREETMAP_URL, OpenStreetMapMapnik.INSTANCE));
        mapsModel.addOrUpdateMap(new DownloadMap("OpenCycleMap.org - the OpenStreetMap Cycle Map", "http://www.opencyclemap.org/", OpenCycleMap.INSTANCE));

        File mapsDirectory = getMapsDirectory();
        List<File> mapFiles = collectFiles(mapsDirectory, ".map");
        File[] mapFilesArray = mapFiles.toArray(new File[mapFiles.size()]);
        log.info("Collected map files " + printArrayToDialogString(mapFilesArray) + " from " + mapsDirectory);

        for (File file : mapFilesArray)
            mapsModel.addOrUpdateMap(new RendererMap(removePrefix(mapsDirectory, file), file.toURI().toString(), file));

        themesModel.addOrUpdateTheme(new ThemeImpl("A render-theme similar to the OpenStreetMap Osmarender style", OSMARENDER_URL, OSMARENDER));

        File themesDirectory = getThemesDirectory();
        List<File> themeFiles = collectFiles(themesDirectory, ".xml");
        File[] themeFilesArray = themeFiles.toArray(new File[themeFiles.size()]);
        log.info("Collected theme files " + printArrayToDialogString(themeFilesArray) + " from " + themesDirectory);

        for (File file : themeFilesArray)
            themesModel.addOrUpdateTheme(new ThemeImpl(removePrefix(themesDirectory, file), file.toURI().toString(), new ExternalRenderTheme(file)));
    }

    private static String removePrefix(File root, File file) {
        String rootPath = root.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.startsWith(rootPath))
            filePath = filePath.substring(rootPath.length());
        else
            filePath = file.getName();
        if (filePath.startsWith(File.separator))
            filePath = filePath.substring(1);
        return filePath;
    }

    public void initialize() {
        MapFilesService mapFilesService = new MapFilesService(downloadManager);
        for (MapFiles mapFiles : mapFilesService.getMapFiles()) {
            List<Resource> resources = mapFiles.getResources();
            for (Resource resource : resources)
                resourcesModel.addOrUpdateResource(resource);
        }
    }
}
