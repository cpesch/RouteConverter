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
package slash.navigation.converter.gui.models;

import slash.navigation.maps.item.ItemModel;
import slash.navigation.maps.item.ItemTableModel;

import java.util.List;

/**
 * A model for managing theme styles between the ThemeStyleDialog and the MapsforgeMapView.
 *
 * @author Christian Pesch
 */

public class ThemeStyleModel {
    private static final String APPLIED_STYLE_PREFERENCE = "appliedStyle";

    private String currentTheme;
    private final ItemTableModel<ThemeStyle> availableStylesModel = new ItemTableModel<>(1);
    private final ItemModel<ThemeStyle> appliedStyleModel = new ItemModel<>(currentTheme + APPLIED_STYLE_PREFERENCE, null) {
        protected ThemeStyle stringToItem(String url) {
            return getAvailableStylesModel().getItemByUrl(url);
        }

        protected String itemToString(ThemeStyle style) {
            return style.getUrl();
        }
    };

    public void setCurrentTheme(String currentTheme) {
        this.currentTheme = currentTheme;
        // TODO how to initialize after theme change?
    }

    public void setThemeStyles(List<ThemeStyle> themeStyles) {
        getAvailableStylesModel().clear();
        for(ThemeStyle themeStyle : themeStyles)
          getAvailableStylesModel().addOrUpdateItem(themeStyle);

        appliedStyleModel.initializePreferences(currentTheme + APPLIED_STYLE_PREFERENCE, themeStyles.get(0).getUrl());
        // TODO how to initialize after theme change?
        // ThemeStyle selectedStyle = appliedStyleModel.getItem();
        // appliedStyleModel.setItem(selectedStyle != null ? selectedStyle : themeStyles.get(0));
    }

    public ItemModel<ThemeStyle> getAppliedStyleModel() {
        return appliedStyleModel;
    }

    public ItemTableModel<ThemeStyle> getAvailableStylesModel() {
        return availableStylesModel;
    }
}
