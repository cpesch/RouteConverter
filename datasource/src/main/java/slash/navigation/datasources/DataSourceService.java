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

package slash.navigation.datasources;

import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.DatasourcesType;
import slash.navigation.datasources.impl.DataSourceImpl;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static slash.navigation.datasources.DataSourcesUtil.unmarshal;

/**
 * Encapsulates access to a DataSources XML.
 *
 * @author Christian Pesch
 */

public class DataSourceService {
    private List<DataSource> dataSources = new ArrayList<>();

    public void load(InputStream inputStream) throws JAXBException {
        DatasourcesType datasourcesType = unmarshal(inputStream);
        for(DatasourceType datasourceType : datasourcesType.getDatasource())
            dataSources.add(new DataSourceImpl(datasourceType));
    }

    public List<DataSource> getDataSources() {
        return dataSources;
    }

    public DataSource getDataSourceByUrlPrefix(String url) {
        for (DataSource dataSource : getDataSources()) {
            if (url.startsWith(dataSource.getBaseUrl()))
                return dataSource;
        }
        return null;
    }

    public DataSource getDataSourceById(String id) {
        for (DataSource dataSource : getDataSources()) {
            if (id.equals(dataSource.getId()))
                return dataSource;
        }
        return null;
    }
}
