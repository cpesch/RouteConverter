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

import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.download.actions.Checksum;
import slash.navigation.download.datasources.binding.*;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static slash.common.io.Transfer.parseTime;

/**
 * Encapsulates access to a DataSources XML.
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

    public Map<String, Fragment> getFragments(String dataSourceName) {
        DatasourceType datasourceType = getDataSource(dataSourceName);
        Map<String, Fragment> result = new HashMap<String, Fragment>();
        for (FragmentType fragmentType : datasourceType.getFragment()) {
            result.put(fragmentType.getKey(), asFragment(fragmentType));
        }
        return result;
    }

    private Fragment asFragment(FragmentType fragmentType) {
        return new Fragment(fragmentType.getKey(), fragmentType.getUri(), fragmentType.getSize(), fragmentType.getChecksum(), parseTime(fragmentType.getTimestamp()));
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
        return new File(fileType.getUri(), new Checksum(fileType.getChecksum(), fileType.getSize(), parseTime(fileType.getTimestamp())), asBoundingBox(fileType.getNorthEast(), fileType.getSouthWest()));
    }

    public Map<String, slash.navigation.download.datasources.Map> getMaps(String dataSourceName) {
        DatasourceType datasourceType = getDataSource(dataSourceName);
        Map<String, slash.navigation.download.datasources.Map> result = new HashMap<String, slash.navigation.download.datasources.Map>();
        for (MapType mapType : datasourceType.getMap()) {
            result.put(mapType.getUri(), asMap(mapType));
        }
        return result;
    }

    private slash.navigation.download.datasources.Map asMap(MapType mapType) {
        return new slash.navigation.download.datasources.Map(mapType.getUri(), mapType.getSize(), mapType.getChecksum(), parseTime(mapType.getTimestamp()), asBoundingBox(mapType.getNorthEast(), mapType.getSouthWest()));
    }

    private BoundingBox asBoundingBox(PositionType northEast, PositionType southWest) {
        if(northEast == null || southWest == null)
            return null;
        return new BoundingBox(asPosition(northEast), asPosition(southWest));
    }

    private NavigationPosition asPosition(PositionType positionType) {
        return new SimpleNavigationPosition(positionType.getLongitude(), positionType.getLatitude());
    }
}
