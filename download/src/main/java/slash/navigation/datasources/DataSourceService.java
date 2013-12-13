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
import slash.navigation.datasources.binding.MappingType;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates access to the {@link DataSourceService}
 *
 * @author Christian Pesch
 */

public class DataSourceService {
    private DatasourcesType datasourcesType;

    public void initialize(InputStream inputStream) throws JAXBException {
        datasourcesType = DataSourcesUtil.unmarshal(inputStream);
    }

    public DatasourcesType getDatasourcesType() {
        return datasourcesType;
    }

    public DatasourceType getDataSource(String dataSourceName) {
        for (DatasourceType datasourceType : getDatasourcesType().getDatasource()) {
            if(dataSourceName.equals(datasourceType.getName()))
                return datasourceType;
        }
        return null;
    }

    public Map<String, String> getMapping(String dataSourceName) {
        DatasourceType datasourceType = getDataSource(dataSourceName);
        Map<String, String> result = new HashMap<String, String>();
        for (MappingType mapping : datasourceType.getMapping()) {
            result.put(mapping.getKey(), mapping.getUri());
        }
        return result;
    }
}
