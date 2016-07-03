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

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Theme;
import slash.navigation.maps.impl.RemoteMapImpl;
import slash.navigation.maps.impl.RemoteThemeImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates access to mapsforge .map and theme files.
 *
 * @author Christian Pesch
 */

public class MapFiles {
    private final DataSource dataSource;

    public MapFiles(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<RemoteMap> getMaps() {
        List<RemoteMap> result = new ArrayList<>();
        for (slash.navigation.datasources.Map map : dataSource.getMaps()) {
            result.add(new RemoteMapImpl(dataSource, map));
        }
        return result;
    }

    public List<RemoteTheme> getThemes() {
        List<RemoteTheme> result = new ArrayList<>();
        for (Theme theme : dataSource.getThemes()) {
            result.add(new RemoteThemeImpl(dataSource, theme));
        }
        return result;
    }
}
