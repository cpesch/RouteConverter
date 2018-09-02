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
package slash.navigation.maps.tileserver.helpers;

import slash.navigation.maps.tileserver.binding.CatalogType;
import slash.navigation.maps.tileserver.binding.TileServerType;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.navigation.maps.tileserver.helpers.TileServerUtil.unmarshal;

/**
 * Encapsulates access to a TileServer XML.
 *
 * @author Christian Pesch
 */

public class TileServerService {
    private static final Logger log = Logger.getLogger(TileServerService.class.getName());
    private static final String DOT_XML = ".xml";

    private final File directory;
    private final List<TileServerType> tileServers = new ArrayList<>();
    private final List<TileServerType> overlays = new ArrayList<>();

    public TileServerService(File directory) {
        this.directory = directory;
    }

    public void initialize() {
        java.io.File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(java.io.File dir, String name) {
                return name.endsWith(DOT_XML);
            }
        });

        if (files != null) {
            for (File file : files) {
                try {
                    try (InputStream inputStream = new FileInputStream(file)) {
                        log.info("Initializing tile server definitions from " + file);
                        load(inputStream);
                    }
                } catch (IOException | JAXBException e) {
                    log.severe("Could not parse tile server definitions from " + file + ": " + getLocalizedMessage(e));
                }
            }
        }
    }

    private void load(InputStream inputStream) throws JAXBException {
        CatalogType catalogType = unmarshal(inputStream);
        tileServers.addAll(catalogType.getTileServer());
        // TODO load real overlays once the XML is different
        overlays.addAll(catalogType.getTileServer());
    }

    public List<TileServerType> getTileServers() {
        return tileServers;
    }

    public List<TileServerType> getOverlays() {
        return overlays;
    }
}
