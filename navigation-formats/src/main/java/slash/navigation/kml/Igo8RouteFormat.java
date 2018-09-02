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

package slash.navigation.kml;

import slash.navigation.base.ParserContext;
import slash.navigation.kml.binding22.DocumentType;
import slash.navigation.kml.binding22.FolderType;
import slash.navigation.kml.binding22.KmlType;
import slash.navigation.kml.binding22.ObjectFactory;
import slash.navigation.kml.binding22.PlacemarkType;
import slash.navigation.kml.binding22.PointType;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.prefs.Preferences;

import static java.lang.Boolean.TRUE;
import static slash.common.io.Transfer.trim;

/**
 * Reads and writes iGO8 Route (.kml) files.
 *
 * @author Christian Pesch
 */

public class Igo8RouteFormat extends Kml22Format {
    private static final Preferences preferences = Preferences.userNodeForPackage(Igo8RouteFormat.class);
    private static final String IGO_ROUTE = "iGO-Route";

    public String getName() {
        return "iGO8 Route (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumiGo8RoutePositionCount", 100);
    }

    protected void process(KmlType kmlType, ParserContext<KmlRoute> context) throws IOException {
        if (kmlType == null || kmlType.getAbstractFeatureGroup() == null)
            return;
        extractTracks(kmlType, context);

        List<KmlRoute> routes = context.getRoutes();
        context.removeRoutes();
        if (routes != null && routes.size() == 1) {
            KmlRoute route = routes.get(0);
            if (route.getName().equals(IGO_ROUTE + "/" + WAYPOINTS)) {
                route.setName(IGO_ROUTE);
                context.appendRoute(route);
            }
        }
    }

    private String trimLineFeedsAndCommas(String line) {
        line = trim(line);
        if (line != null) {
            while (line.endsWith(",") || line.endsWith("\n")) {
                line = trim(line.substring(0, line.length() - 1));
            }
        }
        return line;
    }

    private FolderType createWayPoints(KmlRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        FolderType folderType = objectFactory.createFolderType();
        folderType.setName(WAYPOINTS);
        List<KmlPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            KmlPosition position = positions.get(i);
            PlacemarkType placemarkType = objectFactory.createPlacemarkType();
            folderType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkType));
            placemarkType.setName(trimLineFeedsAndCommas(asName(isWriteName() ? position.getDescription() : null)));
            placemarkType.setDescription(trimLineFeedsAndCommas(asDesc(isWriteDesc() ? position.getDescription() : null)));
            PointType pointType = objectFactory.createPointType();
            placemarkType.setAbstractGeometryGroup(objectFactory.createPoint(pointType));
            pointType.getCoordinates().add(createCoordinates(position, false));
        }
        return folderType;
    }

    protected KmlType createKmlType(KmlRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        KmlType kmlType = objectFactory.createKmlType();
        DocumentType documentType = objectFactory.createDocumentType();
        kmlType.setAbstractFeatureGroup(objectFactory.createDocument(documentType));
        documentType.setName(IGO_ROUTE);
        documentType.setDescription(trimLineFeedsAndCommas(asDescription(route.getDescription())));
        documentType.setOpen(TRUE);

        FolderType folderType = createWayPoints(route, startIndex, endIndex);
        documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(folderType));
        return kmlType;
    }

    public void write(List<KmlRoute> routes, OutputStream target) {
        throw new UnsupportedOperationException();
    }
}