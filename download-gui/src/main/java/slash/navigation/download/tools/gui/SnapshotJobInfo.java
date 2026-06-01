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
package slash.navigation.download.tools.gui;

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Source;

import java.nio.file.Path;

/**
 * Snapshot-derived metadata for a mirror job row.
 *
 * @author Christian Pesch
 */
public record SnapshotJobInfo(DataSource dataSource, Source source,
                              int fileCount, int mapCount, int themeCount, int downloadableCount, long totalSize,
                              Path snapshotFile) {

    public String id() {
        return dataSource.getId();
    }

    public String name() {
        return dataSource.getName();
    }

    public String baseUrl() {
        return dataSource.getBaseUrl();
    }

    public String mirrorUrl() {
        if (source != null && source.getUrl() != null && !source.getUrl().isBlank())
            return source.getUrl();
        return dataSource.getBaseUrl();
    }
}
