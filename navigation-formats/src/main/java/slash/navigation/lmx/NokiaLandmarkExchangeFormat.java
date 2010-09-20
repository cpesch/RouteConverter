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

package slash.navigation.lmx;

import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.lmx.binding.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads and writes Nokia Landmark Exchange (.lmx) files.
 *
 * @author Christian Pesch
 */

public class NokiaLandmarkExchangeFormat extends GpxFormat { // TODO why is it subclassing GPX?
    private static final Logger log = Logger.getLogger(NokiaLandmarkExchangeFormat.class.getName());

    public String getExtension() {
        return ".lmx";
    }

    public String getName() {
        return "Nokia Landmark Exchange (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    private GpxPosition process(LandmarkType landmark) {
        CoordinatesType coordinates = landmark.getCoordinates();
        Double altitude = coordinates != null && coordinates.getAltitude() != null ?
                new Float(coordinates.getAltitude()).doubleValue() : null;
        return new GpxPosition(coordinates != null ? coordinates.getLongitude() : null,
                coordinates != null ? coordinates.getLatitude() : null,
                altitude,
                null,
                coordinates != null ? parseTime(coordinates.getTimeStamp()) : null,
                landmark.getName(),
                landmark);
    }

    private GpxRoute process(Lmx lmx) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();

        String name = null, description = null;
        LandmarkType aLandmark = lmx.getLandmark();
        if (aLandmark != null) {
            name = aLandmark.getName();
            description = aLandmark.getDescription();
            positions.add(process(aLandmark));
        }
        LandmarkCollectionType landmarkCollection = lmx.getLandmarkCollection();
        if (landmarkCollection != null) {
            name = landmarkCollection.getName();
            description = landmarkCollection.getDescription();
            for (LandmarkType landmark : landmarkCollection.getLandmark())
                positions.add(process(landmark));
        }
        return positions.size() > 0 ? new GpxRoute(this, RouteCharacteristics.Waypoints, name, asDescription(description), positions, lmx) : null;
    }

    private Lmx createLmx(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        Lmx lmx = route.getOrigin(Lmx.class);
        if (lmx != null) {
            if (lmx.getLandmark() != null)
                lmx.setLandmark(null);
        } else
            lmx = objectFactory.createLmx();

        LandmarkCollectionType landmarkCollectionType = lmx.getLandmarkCollection();
        if (landmarkCollectionType == null)
            landmarkCollectionType = objectFactory.createLandmarkCollectionType();
        landmarkCollectionType.setName(route.getName());
        landmarkCollectionType.setDescription(asDescription(route.getDescription()));

        List<LandmarkType> landmarkTypeList = landmarkCollectionType.getLandmark();
        landmarkTypeList.clear();

        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);

            LandmarkType landmarkType = position.getOrigin(LandmarkType.class);
            if (landmarkType == null)
                landmarkType = objectFactory.createLandmarkType();
            landmarkType.setName(position.getComment());

            CoordinatesType coordinatesType = landmarkType.getCoordinates();
            if (coordinatesType == null)
                coordinatesType = objectFactory.createCoordinatesType();
            coordinatesType.setAltitude(Transfer.formatFloat(position.getElevation()));
            coordinatesType.setLatitude(Transfer.formatPositionAsDouble(position.getLatitude()));
            coordinatesType.setLongitude(Transfer.formatPositionAsDouble(position.getLongitude()));
            coordinatesType.setTimeStamp(formatTime(position.getTime()));
            landmarkType.setCoordinates(coordinatesType);

            landmarkTypeList.add(landmarkType);
        }

        lmx.setLandmarkCollection(landmarkCollectionType);
        return lmx;
    }

    public List<GpxRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        try {
            Lmx lmx = LmxUtil.unmarshal(source);
            GpxRoute result = process(lmx);
            return result != null ? Arrays.asList(result) : null;
        } catch (JAXBException e) {
            log.fine("Error reading " + source + ": " + e.getMessage());
            return null;
        }
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            LmxUtil.marshal(createLmx(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<GpxRoute> routes, OutputStream target) throws IOException {
        throw new UnsupportedOperationException();
    }
}