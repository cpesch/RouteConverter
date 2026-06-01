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

/**
 * Table row state for a mirror job. Wraps the snapshot {@link DataSource} and its
 * {@link Source} directly.
 *
 * @author Christian Pesch
 */
public class MirrorJobRow {
    private final SnapshotJobInfo snapshotJobInfo;
    private String status = "Ready";

    public MirrorJobRow(SnapshotJobInfo snapshotJobInfo) {
        this.snapshotJobInfo = snapshotJobInfo;
    }

    public SnapshotJobInfo getSnapshotJobInfo() {
        return snapshotJobInfo;
    }

    public DataSource getDataSource() {
        return snapshotJobInfo.dataSource();
    }

    public Source getSource() {
        return snapshotJobInfo.source();
    }

    public String getId() {
        return snapshotJobInfo.id();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
