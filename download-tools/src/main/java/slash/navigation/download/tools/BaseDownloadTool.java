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
package slash.navigation.download.tools;

import jakarta.xml.bind.JAXBException;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.helpers.DataSourceService;
import slash.navigation.rest.Credentials;
import slash.navigation.rest.SimpleCredentials;

import java.io.File;
import java.io.IOException;

import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.navigation.datasources.DataSourceManager.DATASOURCES_URI;
import static slash.navigation.datasources.DataSourceManager.loadAllDataSources;

/**
 * Base for the download tools.
 *
 * @author Christian Pesch
 */

public class BaseDownloadTool {
    protected static final String URL_ARGUMENT = "url";
    protected static final String ID_ARGUMENT = "id";
    protected static final String DATASOURCES_SERVER_ARGUMENT = "server";
    protected static final String DATASOURCES_USERNAME_ARGUMENT = "username";
    protected static final String DATASOURCES_PASSWORD_ARGUMENT = "password";
    protected static final String DATASOURCES_PASSWORD_ENVIRONMENT_VARIABLE = "PASSWORD";
    protected static final int MAXIMUM_UPDATE_COUNT = 10;

    private String url, id, dataSourcesServer, dataSourcesUserName, dataSourcesPassword;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDataSourcesServer() {
        return dataSourcesServer;
    }

    public void setDataSourcesServer(String dataSourcesServer) {
        this.dataSourcesServer = dataSourcesServer;
    }

    public void setDataSourcesUserName(String dataSourcesUserName) {
        this.dataSourcesUserName = dataSourcesUserName;
    }

    public void setDataSourcesPassword(String dataSourcesPassword) {
        if(dataSourcesPassword == null)
            dataSourcesPassword = System.getenv(DATASOURCES_PASSWORD_ENVIRONMENT_VARIABLE);
        this.dataSourcesPassword = dataSourcesPassword;
    }

    protected boolean hasDataSourcesServer() {
        return getDataSourcesServer() != null && dataSourcesUserName != null && dataSourcesPassword != null;
    }

    protected String getDataSourcesUrl() {
        return getDataSourcesServer() + DATASOURCES_URI + getId() + "/";
    }

    protected Credentials getCredentials() {
        return new SimpleCredentials(dataSourcesUserName, dataSourcesPassword.toCharArray());
    }

    protected DataSource loadDataSource(String id) throws IOException, JAXBException {
        DataSourceService service = loadAllDataSources(getDataSourcesDirectory());
        DataSource source = service.getDataSourceById(id);
        if (source == null)
            throw new IllegalArgumentException("Unknown data source: " + id);
        return source;
    }

    protected File getSnapshotDirectory() {
        String postFix = getDataSourcesServer().
                substring(getDataSourcesServer().indexOf("//") + 2, getDataSourcesServer().lastIndexOf('/')).
                replaceAll(":", "");
        return ensureDirectory(getApplicationDirectory("snapshot-" + postFix).getAbsolutePath());
    }

    File getRootDirectory() {
        return ensureDirectory(new File(getSnapshotDirectory(), "root"));
    }

    File getEditionsDirectory() {
        return ensureDirectory(new File(getSnapshotDirectory(), "editions"));
    }

    File getDataSourcesDirectory() {
        return ensureDirectory(new File(getSnapshotDirectory(), "datasources"));
    }
}
