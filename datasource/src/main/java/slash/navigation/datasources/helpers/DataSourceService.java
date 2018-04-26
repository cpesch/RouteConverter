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
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.Edition;
import slash.navigation.datasources.Fragment;
import slash.navigation.datasources.binding.CatalogType;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.EditionType;
import slash.navigation.datasources.impl.DataSourceImpl;
import slash.navigation.datasources.impl.EditionImpl;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.generateChecksum;
import static slash.navigation.datasources.helpers.DataSourcesUtil.asMetaDataComparablePath;
import static slash.navigation.datasources.helpers.DataSourcesUtil.unmarshal;

/**
 * Encapsulates access to a DataSources XML.
 *
 * @author Christian Pesch
 */

public class DataSourceService {
    private final List<Edition> editions = new ArrayList<>(1);
    private final List<DataSource> dataSources = new ArrayList<>(1);

    public synchronized void load(InputStream inputStream) throws JAXBException {
        CatalogType catalogType = unmarshal(inputStream);
        for (DatasourceType datasourceType : catalogType.getDatasource())
            dataSources.add(new DataSourceImpl(datasourceType));
        for (EditionType editionType : catalogType.getEdition())
            editions.add(new EditionImpl(editionType));
    }

    public synchronized void clear() {
        editions.clear();
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

    public Downloadable getDownloadable(String url) {
        for (DataSource dataSource :  getDataSources()) {
            if(!url.startsWith(dataSource.getBaseUrl()))
                continue;

            String uri = url.substring(dataSource.getBaseUrl().length());
            Downloadable downloadable = dataSource.getDownloadable(uri);
            if (downloadable != null)
                return downloadable;
        }
        return null;
    }

    public synchronized Downloadable getDownloadable(File file) throws IOException {
        String filePath = asMetaDataComparablePath(file);

        for (DataSource dataSource :  getDataSources()) {
            File directory = getApplicationDirectory(dataSource.getDirectory());
            String directoryPath = directory.getCanonicalPath();

            if (filePath.startsWith(directoryPath)) {
                String fileName = filePath.substring(directoryPath.length() + 1);

                Downloadable downloadable = dataSource.getDownloadable(fileName);
                if (downloadable != null)
                    return downloadable;

                Fragment<Downloadable> fragment = dataSource.getFragment(fileName);
                if (fragment != null)
                    return fragment.getDownloadable();

                String sha1 = generateChecksum(file);

                downloadable = dataSource.getDownloadableBySHA1(sha1);
                if (downloadable != null)
                    return downloadable;

                fragment = dataSource.getFragmentBySHA1(sha1);
                if (fragment != null)
                    return fragment.getDownloadable();
            }
        }
        return null;
    }
}
