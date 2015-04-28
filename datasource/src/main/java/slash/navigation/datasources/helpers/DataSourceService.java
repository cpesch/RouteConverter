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

package slash.navigation.datasources.helpers;

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Edition;
import slash.navigation.datasources.binding.CatalogType;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.EditionType;
import slash.navigation.datasources.impl.DataSourceImpl;
import slash.navigation.datasources.impl.EditionImpl;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates access to a DataSources XML.
 *
 * @author Christian Pesch
 */

public class DataSourceService {
    private final List<Edition> editions = new ArrayList<>(1);
    private final List<DataSource> dataSources = new ArrayList<>(1);

    public synchronized void load(InputStream inputStream) throws JAXBException {
        CatalogType catalogType = DataSourcesUtil.unmarshal(inputStream);
        for (DatasourceType datasourceType : catalogType.getDatasource())
            dataSources.add(new DataSourceImpl(datasourceType));
        for (EditionType editionType : catalogType.getEdition())
            editions.add(new EditionImpl(editionType));
    }

    public synchronized void clear() {
        dataSources.clear();
    }

    public synchronized List<Edition> getEditions() {
        return editions;
    }

    public synchronized List<DataSource> getDataSources() {
        return dataSources;
    }

    public synchronized DataSource getDataSourceByUrlPrefix(String url) {
        for (DataSource dataSource : getDataSources()) {
            if (url.startsWith(dataSource.getBaseUrl()))
                return dataSource;
        }
        return null;
    }

    public synchronized DataSource getDataSourceById(String id) {
        for (DataSource dataSource : getDataSources()) {
            if (id.equals(dataSource.getId()))
                return dataSource;
        }
        return null;
    }
}
