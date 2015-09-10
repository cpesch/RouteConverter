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
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.Fragment;
import slash.navigation.download.Action;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.maps.impl.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.sort;
import static org.mapsforge.map.rendertheme.InternalRenderTheme.OSMARENDER;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.collectFiles;
import static slash.common.io.Files.printArrayToDialogString;
import static slash.navigation.download.Action.Extract;
import static slash.navigation.maps.helpers.MapUtil.extractBoundingBox;

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
            return getAvailableThemesModel().getTheme(url);
        }

        protected String itemToString(LocalTheme theme) {
            return theme.getUrl();
        }
    };

    public MapManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;

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

    private String getMapsDirectory() {
        return preferences.get(MAP_DIRECTORY_PREFERENCE, getApplicationDirectory("maps").getAbsolutePath());
    }

    private String getThemesDirectory() {
        return preferences.get(THEME_DIRECTORY_PREFERENCE, getApplicationDirectory("themes").getAbsolutePath());
    }

    public LocalMap getMap(String uri) {
        String url = new File(getMapsDirectory(), uri).toURI().toString();
        return getAvailableMapsModel().getMap(url);
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
        for (File file : mapFilesArray)
            availableMapsModel.addOrUpdateMap(new VectorMap(removePrefix(mapsDirectory, file), file.toURI().toString(), extractBoundingBox(file), file));

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
        DownloadManager downloadManager = dataSourceManager.getDownloadManager();
        List<Download> downloads = new ArrayList<>();
        for (RemoteResource resource : resources) {
            Downloadable downloadable = resource.getDownloadable();

            Action action = Action.valueOf(resource.getDataSource().getAction());
            File resourceFile = action.equals(Extract) ? getDirectory(resource) : getFile(resource);

            List<FileAndChecksum> fragments = new ArrayList<>();
            for (Fragment fragment : downloadable.getFragments()) {
                File fragmentFile = new File(resourceFile, fragment.getKey());
                fragments.add(new FileAndChecksum(fragmentFile, fragment.getLatestChecksum()));
            }

            Download download = downloadManager.queueForDownload(resource.getDataSource().getName() + ": " + downloadable.getUri(),
                    resource.getUrl(), action, null, new FileAndChecksum(resourceFile, downloadable.getLatestChecksum()), fragments);
            downloads.add(download);
        }
        downloadManager.waitForCompletion(downloads);
    }

    public File getFile(RemoteResource resource) {
        return new File(getApplicationDirectory(resource.getDataSource().getDirectory().toLowerCase()), resource.getDownloadable().getUri().toLowerCase());
    }

    private File getDirectory(RemoteResource resource) {
        String subDirectory = resource.getDataSource().getDirectory();
        if (resource instanceof RemoteMap)
            return getDirectory(getMapsDirectory(), subDirectory.substring(5), resource.getDownloadable().getUri());
        else if (resource instanceof RemoteTheme)
            return getDirectory(getThemesDirectory(), subDirectory.substring(7), resource.getDownloadable().getUri());
        return getApplicationDirectory(subDirectory);
    }

    private File getDirectory(String part1, String part2, String part3) {
        File directory = new File(part1, part2);
        return ensureDirectory(new File(directory, part3).getParentFile());
    }
}
