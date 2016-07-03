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

import slash.navigation.common.BoundingBox;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Map;
import slash.navigation.maps.RemoteMap;

/**
 * The implementation of a {@link RemoteMap}.
 *
 * @author Christian Pesch
 */
public class RemoteMapImpl extends RemoteResourceImpl implements RemoteMap {
    public RemoteMapImpl(DataSource datasource, Map map) {
        super(datasource, map);
    }

    public BoundingBox getBoundingBox() {
        Map map = Map.class.cast(getDownloadable());
        return map.getBoundingBox();
    }
}
