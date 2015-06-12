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
package slash.navigation.maps.impl;

import slash.navigation.datasources.DataSource;
import slash.navigation.maps.LocalMap;
import slash.navigation.maps.LocalTheme;
import slash.navigation.maps.MapManager;
import slash.navigation.maps.RemoteResource;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;

/**
 * Acts as a {@link TableModel} for the available resources of {@link LocalMap}s and {@link LocalTheme}s of the {@link MapManager}.
 *
 * @author Christian Pesch
 */

public class ResourcesTableModel extends AbstractTableModel {
    private List<RemoteResource> resources = new ArrayList<>();

    public List<RemoteResource> getResources() {
        return resources;
    }

    public void setResources(List<RemoteResource> resources) {
        this.resources = resources;
        fireTableDataChanged();
    }

    public int getRowCount() {
        return resources.size();
    }

    public int getColumnCount() {
        return 3;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getResource(rowIndex);
    }

    public RemoteResource getResource(int rowIndex) {
        return resources.get(rowIndex);
    }

    public RemoteResource findResource(DataSource datasource, String uri) {
        String url = datasource.getBaseUrl() + uri;
        for(RemoteResource resource : resources) {
            if(url.equals(resource.getUrl()))
                return resource;
        }
        return null;
    }

    private void addResource(RemoteResource resource) {
        if (!resources.add(resource))
            throw new IllegalArgumentException("Resource " + resource + " not added to " + resources);

        final int index = resources.indexOf(resource);
        if (index == -1)
            throw new IllegalArgumentException("Resource " + resource + " not found in " + resources);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsInserted(index, index);
            }
        });
    }

    void updateResource(RemoteResource resource) {
        final int index = resources.indexOf(resource);
        if (index == -1)
            throw new IllegalArgumentException("Resource " + resource + " not found in " + resources);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsUpdated(index, index);
            }
        });
    }

    public void addOrUpdateResource(RemoteResource resource) {
        int index = resources.indexOf(resource);
        if (index == -1)
            addResource(resource);
        else
            updateResource(resource);
    }

    private void removeResource(RemoteResource resource) {
        final int index = resources.indexOf(resource);
        if (index == -1)
            throw new IllegalArgumentException("Resource " + resource + " not found in " + resources);

        if (!resources.remove(resource))
            throw new IllegalArgumentException("Resource " + resource + " not removed from " + resources);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsDeleted(index, index);
            }
        });
    }
}