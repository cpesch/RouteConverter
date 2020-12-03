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
package slash.navigation.maps.mapsforge.helpers;

import slash.navigation.maps.item.ItemModel;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.LocalTheme;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

import static java.lang.Math.min;
import static java.util.prefs.Preferences.MAX_KEY_LENGTH;
import static slash.common.io.Files.collectFiles;
import static slash.navigation.maps.mapsforge.helpers.MapUtil.removePrefix;

/**
 * Manages which {@link LocalTheme} is to be used for which {@link LocalMap}
 *
 * @author Christian Pesch
 */

public class ThemeForMapMediator {
    private static final Preferences preferences = Preferences.userNodeForPackage(ThemeForMapMediator.class);
    private static final String THEME_FOR_MAP_PREFERENCE = "map";
    private static final String THEME_FOR_MAP_PROVIDER_PREFERENCE = "mapProvider";
    private static final String DOT_XML = ".xml";

    private final MapsforgeMapManager mapManager;
    private ChangeListener mapListener, themeListener;

    public ThemeForMapMediator(MapsforgeMapManager mapManager) {
        this.mapManager = mapManager;

        mapListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                LocalMap map = getDisplayedMapModel().getItem();
                if (!map.getType().isThemed())
                    return;

                String themeId = preferences.get(getMapKey(map), getMapTheme(map));
                LocalTheme theme = getAvailableThemesModel().getItemByDescription(themeId);
                if (theme != null)
                    getAppliedThemeModel().setItem(theme);
            }
        };
        getDisplayedMapModel().addChangeListener(mapListener);

        themeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                LocalMap map = getDisplayedMapModel().getItem();
                if (!map.getType().isThemed())
                    return;

                String themeId = getAppliedThemeModel().getItem().getDescription();
                preferences.put(getMapKey(map), themeId);
                preferences.put(getMapProviderKey(map), themeId);
            }
        };
        getAppliedThemeModel().addChangeListener(themeListener);
    }

    public void dispose() {
        getDisplayedMapModel().removeChangeListener(mapListener);
        mapListener = null;
        getAppliedThemeModel().removeChangeListener(themeListener);
        themeListener = null;
    }

    private String getMapTheme(LocalMap map) {
        return preferences.get(getMapProviderKey(map), getFirstTheme(map));
    }

    private String getFirstTheme(LocalMap map) {
        File themesDirectory = new File(mapManager.getThemesDirectory(), map.getProvider());
        if (themesDirectory.exists()) {
            List<File> themes = collectFiles(mapManager.getThemesDirectory(), DOT_XML);
            if(themes.size() > 0)
                return removePrefix(mapManager.getThemesDirectory(), themes.get(0));
        }
        return getAppliedThemeModel().getItem().getDescription();
    }

    private ItemModel<LocalMap> getDisplayedMapModel() {
        return mapManager.getDisplayedMapModel();
    }

    private ItemTableModel<LocalTheme> getAvailableThemesModel() {
        return mapManager.getAvailableThemesModel();
    }

    private ItemModel<LocalTheme> getAppliedThemeModel() {
        return mapManager.getAppliedThemeModel();
    }

    private String getMapKey(LocalMap map) {
        String key = THEME_FOR_MAP_PREFERENCE + map.getDescription();
        return key.substring(0, min(key.length(), MAX_KEY_LENGTH));
    }

    private String getMapProviderKey(LocalMap map) {
        String key = THEME_FOR_MAP_PROVIDER_PREFERENCE + map.getProvider();
        return key.substring(0, min(key.length(), MAX_KEY_LENGTH));
    }
}
