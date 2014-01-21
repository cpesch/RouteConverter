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

package slash.navigation.download.datasources;

import slash.navigation.download.datasources.binding.FragmentType;
import slash.navigation.download.datasources.binding.DatasourceType;
import slash.navigation.download.datasources.binding.DatasourcesType;
import slash.navigation.download.datasources.binding.FileType;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates access to the {@link DataSourceService}
 *
 * @author Christian Pesch
 */

public class DataSourceService {
    private List<DatasourceType> datasourceTypes = new ArrayList<DatasourceType>();

    public void load(InputStream inputStream) throws JAXBException {
        DatasourcesType datasourcesType = DataSourcesUtil.unmarshal(inputStream);
        datasourceTypes.addAll(datasourcesType.getDatasource());
    }

    public List<DatasourceType> getDatasourceTypes() {
        return datasourceTypes;
    }

    public DatasourceType getDataSource(String dataSourceName) {
        for (DatasourceType datasourceType : getDatasourceTypes()) {
            if (dataSourceName.equals(datasourceType.getName()))
                return datasourceType;
        }
        return null;
    }

    public Map<String, Fragment> getArchives(String dataSourceName) {
        DatasourceType datasourceType = getDataSource(dataSourceName);
        Map<String, Fragment> result = new HashMap<String, Fragment>();
        for (FragmentType fragmentType : datasourceType.getFragment()) {
            result.put(fragmentType.getKey(), asFragment(fragmentType));
        }
        return result;
    }

    private Fragment asFragment(FragmentType fragmentType) {
        return new Fragment(fragmentType.getKey(), fragmentType.getUri(), fragmentType.getSize(), fragmentType.getChecksum());
    }

    public Map<String, File> getFiles(String dataSourceName) {
        DatasourceType datasourceType = getDataSource(dataSourceName);
        Map<String, File> result = new HashMap<String, File>();
        for (FileType fileType : datasourceType.getFile()) {
            result.put(fileType.getUri(), asFile(fileType));
        }
        return result;
    }

    private File asFile(FileType fileType) {
        return new File(fileType.getUri(), fileType.getSize(), fileType.getChecksum());
    }
}
