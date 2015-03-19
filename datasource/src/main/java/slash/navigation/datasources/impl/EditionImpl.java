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

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Edition;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.EditionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of an {@link Edition} based on a {@link EditionType}.
 *
 * @author Christian Pesch
 */

public class EditionImpl implements Edition {
    private final EditionType editionType;

    public EditionImpl(EditionType editionType) {
        this.editionType = editionType;
    }

    public String getId() {
        return editionType.getId();
    }

    public String getName() {
        return editionType.getName();
    }

    public String getHref() {
        return editionType.getHref();
    }

    public List<DataSource> getDataSources() {
        List<DataSource> result = new ArrayList<>();
        for (DatasourceType datasourceType : editionType.getDatasource())
            result.add(new DataSourceImpl(datasourceType));
        return result;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Edition dataSource = (Edition) o;

        return getId().equals(dataSource.getId());
    }

    public int hashCode() {
        int result;
        result = getId().hashCode();
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[id=" + getId() + "]";
    }
}
