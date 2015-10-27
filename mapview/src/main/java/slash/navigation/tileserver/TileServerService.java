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
package slash.navigation.tileserver;

import slash.navigation.tileserver.binding.CatalogType;
import slash.navigation.tileserver.binding.TileServerType;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates access to a TileServer XML.
 *
 * @author Christian Pesch
 */

public class TileServerService {
    private final List<TileServerType> tileServers = new ArrayList<>(1);

    public synchronized void load(InputStream inputStream) throws JAXBException {
        CatalogType catalogType = TileServerUtil.unmarshal(inputStream);
        for (TileServerType tileServerType : catalogType.getTileServer())
            tileServers.add(tileServerType);
    }

    public List<TileServerType> getTileServers() {
        return tileServers;
    }
}
