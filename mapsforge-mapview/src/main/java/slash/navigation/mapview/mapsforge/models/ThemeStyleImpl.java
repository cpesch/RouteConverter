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
package slash.navigation.mapview.mapsforge.models;

import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import slash.navigation.maps.mapsforge.ThemeStyle;
import slash.navigation.maps.mapsforge.ThemeStyleCategory;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link ThemeStyle} based on {@link XmlRenderThemeStyleMenu} and {@link XmlRenderThemeStyleLayer}.
 *
 * @author Christian Pesch
 */
public class ThemeStyleImpl implements ThemeStyle {
    private final XmlRenderThemeStyleMenu renderThemeStyleMenu;
    private final XmlRenderThemeStyleLayer layer;

    public ThemeStyleImpl(XmlRenderThemeStyleMenu renderThemeStyleMenu, XmlRenderThemeStyleLayer layer) {
        this.renderThemeStyleMenu = renderThemeStyleMenu;
        this.layer = layer;
    }

    public String getDescription() {
        String title = layer.getTitle(Locale.getDefault().getLanguage());
        if (title != null) {
            return title;
        }
        title = layer.getTitle(renderThemeStyleMenu.getDefaultLanguage());
        if (title != null) {
            return title;
        }
        return layer.getId();
    }

    public String getUrl() {
        return layer.getId();
    }

    public Set<ThemeStyleCategory> getCategories() {
        return layer.getCategories().stream()
                .sorted()
                .map(ThemeStyleCategoryImpl::new)
                .collect(Collectors.toSet());
    }
}
