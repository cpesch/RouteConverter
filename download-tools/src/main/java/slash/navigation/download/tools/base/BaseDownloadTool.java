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

import slash.navigation.datasources.DataSourceService;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;

import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;

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

    protected File getSnapshotDirectory() {
        return ensureDirectory(getApplicationDirectory("snapshot").getAbsolutePath());
    }

    public File getRootDirectory() {
        return ensureDirectory(new File(getSnapshotDirectory(), "root"));
    }

    protected File getEditionsDirectory() {
        return ensureDirectory(new File(getSnapshotDirectory(), "editions"));
    }

    protected File getDataSourcesDirectory() {
        return ensureDirectory(new File(getSnapshotDirectory(), "datasources"));
    }

    protected DataSourceService loadDataSources(File directory) throws JAXBException, FileNotFoundException {
        DataSourceService dataSourceService = new DataSourceService();
        File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });
        if (files != null)
            for (File file : files)
                dataSourceService.load(new FileInputStream(file));
        return dataSourceService;
    }
}
