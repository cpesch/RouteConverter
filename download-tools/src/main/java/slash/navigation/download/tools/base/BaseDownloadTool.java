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

import java.io.File;

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

    private String url, id;

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

    protected static File getSnapshotDirectory() {
        return ensureDirectory(getApplicationDirectory("snapshot").getAbsolutePath());
    }

    public static File getRootDirectory() {
        return ensureDirectory(new File(getSnapshotDirectory(), "root"));
    }

    protected static File getEditionsDirectory() {
        return ensureDirectory(new File(getSnapshotDirectory(), "editions"));
    }

    protected static File getDataSourcesDirectory() {
        return ensureDirectory(new File(getSnapshotDirectory(), "datasources"));
    }
}
