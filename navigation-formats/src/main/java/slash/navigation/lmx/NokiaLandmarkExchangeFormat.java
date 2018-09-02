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

import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.XmlNavigationFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.lmx.binding.CoordinatesType;
import slash.navigation.lmx.binding.LandmarkCollectionType;
import slash.navigation.lmx.binding.LandmarkType;
import slash.navigation.lmx.binding.Lmx;
import slash.navigation.lmx.binding.ObjectFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static slash.common.io.Transfer.formatFloat;
import static slash.common.io.Transfer.formatXMLTime;
import static slash.common.io.Transfer.parseXMLTime;
import static slash.navigation.common.NavigationConversion.formatDouble;
import static slash.navigation.lmx.NokiaLandmarkExchangeUtil.marshal;
import static slash.navigation.lmx.NokiaLandmarkExchangeUtil.unmarshal;

/**
 * Reads and writes Nokia Landmark Exchange (.lmx) files.
 *
 * @author Christian Pesch
 */

public class NokiaLandmarkExchangeFormat extends XmlNavigationFormat<NokiaLandmarkExchangeRoute> {

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

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> NokiaLandmarkExchangeRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new NokiaLandmarkExchangeRoute(name, null, (List<Wgs84Position>) positions);
    }

    private Wgs84Position process(LandmarkType landmark) {
        CoordinatesType coordinates = landmark.getCoordinates();
        Double altitude = coordinates != null && coordinates.getAltitude() != null ?
                (double) coordinates.getAltitude() : null;
        return new Wgs84Position(coordinates != null ? coordinates.getLongitude() : null,
                coordinates != null ? coordinates.getLatitude() : null,
                altitude,
                null,
                coordinates != null ? parseXMLTime(coordinates.getTimeStamp()) : null,
                landmark.getName(),
                landmark);
    }

    private NokiaLandmarkExchangeRoute process(Lmx lmx) {
        List<Wgs84Position> positions = new ArrayList<>();

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
        return new NokiaLandmarkExchangeRoute(name, asDescription(description), positions, lmx);
    }

    private Lmx createLmx(NokiaLandmarkExchangeRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        Lmx lmx = route.getLmx();
        if (lmx != null) {
            if (lmx.getLandmark() != null)
                lmx.setLandmark(null);
        } else
            lmx = objectFactory.createLmx();

        LandmarkCollectionType landmarkCollectionType = lmx.getLandmarkCollection();
        if (landmarkCollectionType == null)
            landmarkCollectionType = objectFactory.createLandmarkCollectionType();
        landmarkCollectionType.setName(asRouteName(route.getName()));
        landmarkCollectionType.setDescription(asDescription(route.getDescription()));

        List<LandmarkType> landmarkTypeList = landmarkCollectionType.getLandmark();
        landmarkTypeList.clear();

        List<Wgs84Position> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);

            LandmarkType landmarkType = position.getOrigin(LandmarkType.class);
            if (landmarkType == null)
                landmarkType = objectFactory.createLandmarkType();
            landmarkType.setName(position.getDescription());

            CoordinatesType coordinatesType = landmarkType.getCoordinates();
            if (coordinatesType == null)
                coordinatesType = objectFactory.createCoordinatesType();
            coordinatesType.setAltitude(formatFloat(position.getElevation()));
            Double latitude = formatDouble(position.getLatitude(), 7);
            if (latitude != null)
                coordinatesType.setLatitude(latitude);
            Double longitude = formatDouble(position.getLongitude(), 7);
            if (longitude != null)
                coordinatesType.setLongitude(longitude);
            coordinatesType.setTimeStamp(formatXMLTime(position.getTime()));
            landmarkType.setCoordinates(coordinatesType);

            landmarkTypeList.add(landmarkType);
        }

        lmx.setLandmarkCollection(landmarkCollectionType);
        return lmx;
    }

    public void read(InputStream source, ParserContext<NokiaLandmarkExchangeRoute> context) throws Exception {
        Lmx lmx = unmarshal(source);
        context.appendRoute(process(lmx));
    }

    public void write(NokiaLandmarkExchangeRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal(createLmx(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }
}