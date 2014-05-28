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
package slash.navigation.datasources.impl;

import slash.navigation.datasources.*;
import slash.navigation.datasources.binding.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Implementation of a {@link DataSource} based on a {@link DatasourceType}.
 *
 * @author Christian Pesch
 */

public class DataSourceImpl implements DataSource {
    private final DatasourceType datasourceType;

    public DataSourceImpl(DatasourceType datasourceType) {
        this.datasourceType = datasourceType;
    }

    public String getId() {
        return datasourceType.getId();
    }

    public String getName() {
        return datasourceType.getName();
    }

    public String getBaseUrl() {
        return datasourceType.getBaseUrl();
    }

    public String getDirectory() {
        return datasourceType.getDirectory();
    }

    public List<File> getFiles() {
        List<File> result = new ArrayList<>();
        for (FileType fileType : datasourceType.getFile())
            result.add(new FileImpl(fileType, this));
        return result;
    }

    public List<Map> getMaps() {
        List<Map> result = new ArrayList<>();
        for (MapType mapType : datasourceType.getMap())
            result.add(new MapImpl(mapType, this));
        return result;
    }

    public List<Theme> getThemes() {
        List<Theme> result = new ArrayList<>();
        for (ThemeType themeType : datasourceType.getTheme())
            result.add(new ThemeImpl(themeType, this));
        return result;
    }

    private java.util.Map<String, Fragment> fragmentMap = null;

    private void putFragments(List<? extends Downloadable> downloadables) {
        for(Downloadable downloadable : downloadables) {
            for(Fragment fragment : downloadable.getFragments())
                fragmentMap.put(fragment.getKey(), fragment);
        }
    }

    public synchronized Fragment getFragment(String key) {
        if(fragmentMap == null) {
            fragmentMap = new HashMap<>();
            putFragments(getFiles());
            putFragments(getMaps());
            putFragments(getThemes());
        }
        return fragmentMap.get(key);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSource dataSource = (DataSource) o;

        return getBaseUrl().equals(dataSource.getBaseUrl());
    }

    public int hashCode() {
        int result;
        result = getBaseUrl().hashCode();
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[baseUrl=" + getBaseUrl() + "]";
    }
}
