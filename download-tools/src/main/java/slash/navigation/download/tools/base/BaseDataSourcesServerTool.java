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
package slash.navigation.download.tools.base;

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceService;
import slash.navigation.rest.Credentials;
import slash.navigation.rest.SimpleCredentials;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;

import static slash.navigation.datasources.DataSourceManager.loadDataSources;

/**
 * Base for the datasource server accessing tools.
 *
 * @author Christian Pesch
 */

public class BaseDataSourcesServerTool extends BaseDownloadTool {
    protected static final String DATASOURCES_SERVER_ARGUMENT = "server";
    protected static final String DATASOURCES_USERNAME_ARGUMENT = "username";
    protected static final String DATASOURCES_PASSWORD_ARGUMENT = "password";

    private String datasourcesServer, datasourcesUserName, datasourcesPassword;

    public void setDatasourcesServer(String datasourcesServer) {
        this.datasourcesServer = datasourcesServer;
    }

    public void setDatasourcesUserName(String datasourcesUserName) {
        this.datasourcesUserName = datasourcesUserName;
    }

    public void setDatasourcesPassword(String datasourcesPassword) {
        this.datasourcesPassword = datasourcesPassword;
    }

    protected boolean hasDataSourcesServer() {
        return datasourcesServer != null && datasourcesUserName != null && datasourcesPassword != null;
    }

    protected String getDataSourcesUrl() {
        return datasourcesServer + "v1/datasources/" + getId() + "/";
    }

    protected Credentials getCredentials() {
        return new SimpleCredentials(datasourcesUserName, datasourcesPassword);
    }

    protected DataSource loadDataSource(String id) throws FileNotFoundException, JAXBException {
        DataSourceService service = loadDataSources(getDataSourcesDirectory());
        DataSource source = service.getDataSourceById(id);
        if (source == null)
            throw new IllegalArgumentException("Unknown data source: " + id);
        return source;
    }
}
