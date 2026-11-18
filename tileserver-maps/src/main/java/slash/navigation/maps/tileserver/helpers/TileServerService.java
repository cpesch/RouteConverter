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

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private static final String MAP_SERVERS_XML = "mapservers.xml";
    private static final String OVERLAY_SERVERS_XML = "overlayservers.xml";

    private final File directory;
    private final List<MapServerType> maps = new ArrayList<>();
    private final List<OverlayServerType> overlays = new ArrayList<>();
    private final Set<String> mapIds = new HashSet<>();
    private final Set<String> overlayIds = new HashSet<>();

    public TileServerService(File directory) {
        this.directory = directory;
    }

    public void initialize() {
        maps.clear();
        overlays.clear();
        mapIds.clear();
        overlayIds.clear();

        // only search in the root directory of the theme to avoid confusion
        File[] files = directory.listFiles((dir, name) -> name.endsWith(DOT_XML));
        if (files != null) {
            for (File file : orderFiles(files)) {
                try {
                    try (InputStream inputStream = new FileInputStream(file)) {
                        loadMap(inputStream, file);
                        log.info("Initialized map server definitions from " + file);
                    }
                } catch (UnmarshalException e) {
                    log.fine("Could not unmarshall map server definitions from " + file + ": " + getLocalizedMessage(e));
                } catch (IOException | JAXBException e) {
                    log.severe("Could not parse map server definitions from " + file + ": " + getLocalizedMessage(e));
                }

                try {
                    try (InputStream inputStream = new FileInputStream(file)) {
                        loadOverlay(inputStream, file);
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

    private static List<File> orderFiles(File[] files) {
        File mapServersFile = null;
        File overlayServersFile = null;
        List<File> rest = new ArrayList<>();
        for (File file : files) {
            switch (file.getName()) {
                case MAP_SERVERS_XML -> mapServersFile = file;
                case OVERLAY_SERVERS_XML -> overlayServersFile = file;
                default -> rest.add(file);
            }
        }
        rest.sort(Comparator.comparing(File::getName));

        List<File> ordered = new ArrayList<>();
        if (mapServersFile != null)
            ordered.add(mapServersFile);
        if (overlayServersFile != null)
            ordered.add(overlayServersFile);
        ordered.addAll(rest);
        return ordered;
    }

    private void loadMap(InputStream inputStream, File file) throws JAXBException {
        slash.navigation.maps.tileserver.bindingmap.CatalogType catalogType = MapServerUtil.unmarshal(inputStream);
        for (MapServerType mapServerType : catalogType.getMapServer()) {
            String id = mapServerType.getId();
            if (mapIds.add(id))
                maps.add(mapServerType);
            else
                log.info("Ignoring map server definition '" + id + "' from " + file + ": id already defined");
        }
    }

    private void loadOverlay(InputStream inputStream, File file) throws JAXBException {
        slash.navigation.maps.tileserver.bindingoverlay.CatalogType catalogType = OverlayServerUtil.unmarshal(inputStream);
        for (OverlayServerType overlayServerType : catalogType.getOverlayServer()) {
            String id = overlayServerType.getId();
            if (overlayIds.add(id))
                overlays.add(overlayServerType);
            else
                log.info("Ignoring overlay server definition '" + id + "' from " + file + ": id already defined");
        }
    }

    public List<MapServerType> getMaps() {
        return maps;
    }

    public List<OverlayServerType> getOverlays() {
        return overlays;
    }
}
