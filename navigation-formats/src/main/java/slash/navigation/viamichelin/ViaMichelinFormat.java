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

package slash.navigation.viamichelin;

import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.XmlNavigationFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.viamichelin.binding.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCalculations.asWgs84Position;
import static slash.navigation.common.NavigationConversion.formatPositionAsString;
import static slash.navigation.viamichelin.ViaMichelinUtil.marshal;
import static slash.navigation.viamichelin.ViaMichelinUtil.unmarshal;

/**
 * Reads and writes ViaMichelin (.xvm) files.
 *
 * @author Christian Pesch
 */

public class ViaMichelinFormat extends XmlNavigationFormat<ViaMichelinRoute> {

    public String getExtension() {
        return ".xvm";
    }

    public String getName() {
        return "ViaMichelin (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> ViaMichelinRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new ViaMichelinRoute(name, (List<Wgs84Position>) positions);
    }

    private String parseDescription(Poi poi) {
        String city = trim(poi.getCpCity());
        String address = trim(poi.getAddress());
        if (address != null)
            city = city != null ? city + " " + address : address;
        String name = trim(poi.getName());
        if (name != null)
            city = city != null ? city + " " + name : name;
        Description description = poi.getDescription();
        if (description != null) {
            String descriptionStr = trim(description.toString());
            if (descriptionStr != null)
                city = city != null ? city + " " + descriptionStr : descriptionStr;
        }
        return city;
    }

    private ViaMichelinRoute process(PoiList poiList) {
        String routeName = null;
        List<Wgs84Position> positions = new ArrayList<>();
        for (Object itineraryOrPoi : poiList.getItineraryOrPoi()) {
            if (itineraryOrPoi instanceof Itinerary) {
                Itinerary itinerary = (Itinerary) itineraryOrPoi;
                routeName = itinerary.getName();
                for (Step step : itinerary.getStep()) {
                    positions.add(asWgs84Position(parseDouble(step.getLongitude()), parseDouble(step.getLatitude()), step.getName()));
                }
            }
            if (itineraryOrPoi instanceof Poi) {
                Poi poi = (Poi) itineraryOrPoi;
                positions.add(asWgs84Position(parseDouble(poi.getLongitude()), parseDouble(poi.getLatitude()), parseDescription(poi)));
            }    
        }
        return new ViaMichelinRoute(routeName, positions);
    }

    public void read(InputStream source, ParserContext<ViaMichelinRoute> context) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(source)) {
            PoiList poiList = unmarshal(reader);
            context.appendRoute(process(poiList));
        }
    }


    private PoiList createPoiList(ViaMichelinRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        PoiList poiList = objectFactory.createPoiList();
        poiList.setVersion("2.0");
        Itinerary itinerary = objectFactory.createItinerary();
        itinerary.setName(asRouteName(route.getName()));
        poiList.getItineraryOrPoi().add(itinerary);
        for (Wgs84Position position : route.getPositions()) {
            Step step = objectFactory.createStep();
            step.setLongitude(formatPositionAsString(position.getLongitude()));
            step.setLatitude(formatPositionAsString(position.getLatitude()));
            step.setName(position.getDescription());
            itinerary.getStep().add(step);
        }
        return poiList;
    }

    public void write(ViaMichelinRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal(createPoiList(route), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }
}
