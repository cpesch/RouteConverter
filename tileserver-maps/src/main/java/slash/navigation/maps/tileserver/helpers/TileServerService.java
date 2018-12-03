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

import slash.navigation.maps.tileserver.bindingmap.MapServerType;
import slash.navigation.maps.tileserver.bindingoverlay.OverlayServerType;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;

/**
 * Encapsulates access to a TileServer XML.
 *
 * @author Christian Pesch
 */

public class TileServerService {
    private static final Logger log = Logger.getLogger(TileServerService.class.getName());
    private static final String DOT_XML = ".xml";

    private final File directory;
    private final List<MapServerType> maps = new ArrayList<>();
    private final List<OverlayServerType> overlays = new ArrayList<>();

    public TileServerService(File directory) {
        this.directory = directory;
    }

    public void initialize() {
        deleteOldDefaultFiles();

        java.io.File[] files = directory.listFiles((dir, name) -> name.endsWith(DOT_XML));
        if (files != null) {
            for (File file : files) {
                try {
                    try (InputStream inputStream = new FileInputStream(file)) {
                        loadMap(inputStream);
                        log.info("Initialized map server definitions from " + file);
                    }
                } catch (UnmarshalException e) {
                    log.fine("Could not unmarshall map server definitions from " + file + ": " + getLocalizedMessage(e));
                } catch (IOException | JAXBException e) {
                    log.severe("Could not parse map server definitions from " + file + ": " + getLocalizedMessage(e));
                }

                try {
                    try (InputStream inputStream = new FileInputStream(file)) {
                        loadOverlay(inputStream);
                        log.info("Initialized overlay server definitions from " + file);
                    }
                } catch (UnmarshalException e) {
                    log.fine("Could not unmarshall overlay server definitions from " + file + ": " + getLocalizedMessage(e));
                } catch (IOException | JAXBException e) {
                    log.severe("Could not parse overlay server definitions from " + file + ": " + getLocalizedMessage(e));
                }
            }
        }
    }

    private void deleteOldDefaultFiles() {
        java.io.File[] files = directory.listFiles((dir, name) -> name.equals("default.xml") || name.equals("default-offline.xml"));
        if (files != null)
            for (File file : files) 
                file.delete();
    }

    private void loadMap(InputStream inputStream) throws JAXBException {
        slash.navigation.maps.tileserver.bindingmap.CatalogType catalogType = MapServerUtil.unmarshal(inputStream);
        maps.addAll(catalogType.getMapServer());
    }

    private void loadOverlay(InputStream inputStream) throws JAXBException {
        slash.navigation.maps.tileserver.bindingoverlay.CatalogType catalogType = OverlayServerUtil.unmarshal(inputStream);
        overlays.addAll(catalogType.getOverlayServer());
    }

    public List<MapServerType> getMaps() {
        return maps;
    }

    public List<OverlayServerType> getOverlays() {
        return overlays;
    }
}
