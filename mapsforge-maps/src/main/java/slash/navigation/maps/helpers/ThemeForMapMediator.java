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
package slash.navigation.maps.helpers;

import slash.navigation.maps.LocalMap;
import slash.navigation.maps.LocalTheme;
import slash.navigation.maps.MapManager;
import slash.navigation.maps.impl.ItemModel;
import slash.navigation.maps.impl.LocalThemesTableModel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

import static java.lang.Math.min;
import static java.util.prefs.Preferences.MAX_KEY_LENGTH;
import static slash.common.io.Files.collectFiles;
import static slash.navigation.maps.helpers.MapUtil.removePrefix;

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

    private final MapManager mapManager;

    public ThemeForMapMediator(MapManager mapManager) {
        this.mapManager = mapManager;

        getDisplayedMapModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                LocalMap map = getDisplayedMapModel().getItem();
                if(!map.isVector())
                    return;

                String themeId = preferences.get(getMapKey(map), getMapTheme(map));
                LocalTheme theme = getAvailableThemesModel().getThemeByDescription(themeId);
                if (theme != null)
                    getAppliedThemeModel().setItem(theme);
            }
        });

        getAppliedThemeModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                LocalMap map = getDisplayedMapModel().getItem();
                if(!map.isVector())
                    return;

                String themeId = getAppliedThemeModel().getItem().getDescription();
                preferences.put(getMapKey(map), themeId);
                preferences.put(getMapProviderKey(map), themeId);
            }
        });
    }

    private String getMapTheme(LocalMap map) {
        return preferences.get(getMapProviderKey(map), getFirstTheme(map));
    }

    private String getFirstTheme(LocalMap map) {
        String mapProvider = extractMapProvider(map);

        File themesDirectory = new File(mapManager.getThemesDirectory(), mapProvider);
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

    private LocalThemesTableModel getAvailableThemesModel() {
        return mapManager.getAvailableThemesModel();
    }

    private ItemModel<LocalTheme> getAppliedThemeModel() {
        return mapManager.getAppliedThemeModel();
    }

    private String extractMapProvider(LocalMap map) {
        if(!map.isVector())
            return "online";
        String prefix = removePrefix(mapManager.getMapsDirectory(), map.getFile());
        int index = prefix.indexOf("/");
        return index != -1 ? prefix.substring(0, index) : prefix;
    }

    private String getMapKey(LocalMap map) {
        String key = THEME_FOR_MAP_PREFERENCE + map.getDescription();
        return key.substring(0, min(key.length(), MAX_KEY_LENGTH));
    }

    private String getMapProviderKey(LocalMap map) {
        String key = THEME_FOR_MAP_PROVIDER_PREFERENCE + extractMapProvider(map);
        return key.substring(0, min(key.length(), MAX_KEY_LENGTH));
    }
}
