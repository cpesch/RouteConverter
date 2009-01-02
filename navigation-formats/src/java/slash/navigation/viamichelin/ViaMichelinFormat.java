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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.viamichelin;

import slash.navigation.*;
import slash.navigation.util.Conversion;
import slash.navigation.viamichelin.binding.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 * Reads and writes ViaMichelin (.xvm) files.
 *
 * @author Christian Pesch
 */

public class ViaMichelinFormat extends XmlNavigationFormat<ViaMichelinRoute> {
    private static final Logger log = Logger.getLogger(ViaMichelinFormat.class.getName());

    public String getExtension() {
        return ".xvm";
    }

    public String getName() {
        return "ViaMichelin (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public <P extends BaseNavigationPosition> ViaMichelinRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new ViaMichelinRoute(name, (List<Wgs84Position>) positions);
    }

    private String parseComment(Poi poi) {
        String comment = Conversion.trim(poi.getCpCity());
        String address = Conversion.trim(poi.getAddress());
        if (address != null)
            comment = comment != null ? comment + " " + address : address;
        String name = Conversion.trim(poi.getName());
        if (name != null)
            comment = comment != null ? comment + " " + name : name;
        Description description = poi.getDescription();
        if (description != null) {
            String descriptionStr = Conversion.trim(description.toString());
            if (descriptionStr != null)
                comment = comment != null ? comment + " " + descriptionStr : descriptionStr;
        }
        return comment;
    }

    private ViaMichelinRoute process(PoiList poiList) {
        String routeName = null;
        List<Wgs84Position> positions = new ArrayList<Wgs84Position>();
        for (Object itineraryOrPoi : poiList.getItineraryOrPoi()) {
            if (itineraryOrPoi instanceof Itinerary) {
                Itinerary itinerary = (Itinerary) itineraryOrPoi;
                routeName = itinerary.getName();
                for (Step step : itinerary.getStep()) {
                    positions.add(new Wgs84Position(Conversion.parseDouble(step.getLongitude()), Conversion.parseDouble(step.getLatitude()), null, null, step.getName()));
                }
            }
            if (itineraryOrPoi instanceof Poi) {
                Poi poi = (Poi) itineraryOrPoi;
                positions.add(new Wgs84Position(Conversion.parseDouble(poi.getLongitude()), Conversion.parseDouble(poi.getLatitude()), null, null, parseComment(poi)));
            }    
        }
        return new ViaMichelinRoute(routeName, positions);
    }

    public List<ViaMichelinRoute> read(File source, Calendar startDate) throws IOException {
        FileReader reader = new FileReader(source);
        try {
            PoiList poiList = ViaMichelinUtil.unmarshal(reader);
            return Arrays.asList(process(poiList));
        } catch (JAXBException e) {
            log.fine("Error reading " + source + ": " + e.getMessage());
        }
        finally {
            reader.close();
        }
        return null;
    }


    private PoiList createPoiList(ViaMichelinRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        PoiList poiList = objectFactory.createPoiList();
        poiList.setVersion("2.0");
        Itinerary itinerary = objectFactory.createItinerary();
        itinerary.setName(route.getName());
        poiList.getItineraryOrPoi().add(itinerary);
        for (Wgs84Position position : route.getPositions()) {
            Step step = objectFactory.createStep();
            step.setLongitude(Conversion.formatDoubleAsString(position.getLongitude()));
            step.setLatitude(Conversion.formatDoubleAsString(position.getLatitude()));
            step.setName(position.getComment());
            itinerary.getStep().add(step);
        }
        return poiList;
    }

    public void write(ViaMichelinRoute route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        try {
            ViaMichelinUtil.marshal(createPoiList(route), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
