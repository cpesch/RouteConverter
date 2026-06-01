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

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Snapshot catalog summary and lookup data.
 *
 * @author Christian Pesch
 */
public class SnapshotCatalogData {
    private final Path snapshotRoot;
    private final Path dataSourcesDirectory;
    private final int editionCount;
    private final int dataSourceCount;
    private final Map<String, SnapshotJobInfo> jobInfoById;

    public SnapshotCatalogData(Path snapshotRoot, Path dataSourcesDirectory, int editionCount, int dataSourceCount,
                               Map<String, SnapshotJobInfo> jobInfoById) {
        this.snapshotRoot = snapshotRoot;
        this.dataSourcesDirectory = dataSourcesDirectory;
        this.editionCount = editionCount;
        this.dataSourceCount = dataSourceCount;
        this.jobInfoById = new LinkedHashMap<>(jobInfoById);
    }

    public Path getSnapshotRoot() {
        return snapshotRoot;
    }

    public Path getDataSourcesDirectory() {
        return dataSourcesDirectory;
    }

    public int getEditionCount() {
        return editionCount;
    }

    public int getDataSourceCount() {
        return dataSourceCount;
    }

    public SnapshotJobInfo getJobInfo(String id) {
        return jobInfoById.get(id);
    }

    public Collection<SnapshotJobInfo> getJobInfos() {
        return jobInfoById.values();
    }
}

