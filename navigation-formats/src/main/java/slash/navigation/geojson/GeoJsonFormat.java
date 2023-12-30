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
package slash.navigation.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.*;
import slash.common.type.CompactCalendar;
import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static slash.common.io.Transfer.*;
import static slash.common.type.ISO8601.formatDate;
import static slash.common.type.ISO8601.parseDate;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Reads and writes GeoJSON (.json) files.
 *
 * @author Christian Pesch
 */

public class GeoJsonFormat extends SimpleFormat<Wgs84Route> {
    private static final Logger log = Logger.getLogger(GeoJsonFormat.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(GeoJsonFormat.class);
    private static final String ADDRESS = "address";
    private static final String DATE = "date";
    private static final String LOCATION = "location";
    private static final String NAME = "name";

    public String getExtension() {
        return ".json";
    }

    public String getName() {
        return "GeoJSON (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws IOException {
        read(source, UTF8_ENCODING, context);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        GeoJsonObject object = new ObjectMapper().readValue(reader, GeoJsonObject.class);
        if (object instanceof FeatureCollection featureCollection) {
            List<Wgs84Position> positions = process(featureCollection);
            if (!positions.isEmpty()) {
                context.appendRoute(new Wgs84Route(this, Waypoints, "FeatureCollection", positions));
            }
        } else
            log.warning("Reading GeoJSON object " + object + " is not supported.");
    }

    private CompactCalendar parseTime(String string) {
        Calendar time = parseDate(string);
        return time != null ? CompactCalendar.fromCalendar(time) : null;
    }

    private List<Wgs84Position> process(FeatureCollection featureCollection) {
        List<Wgs84Position> result = new ArrayList<>();
        for (Feature feature : featureCollection.getFeatures()) {
            GeoJsonObject geometry = feature.getGeometry();
            if (geometry instanceof Point point) {
                String name = trim(feature.getProperty(NAME));
                String address = trim(feature.getProperty(ADDRESS));

                // Bewertung.json uses a location object
                Map<String, String> location = feature.getProperty(LOCATION);
                if (location != null) {
                    name = trim(location.get(NAME));
                    address = trim(location.get(ADDRESS));
                }

                Double elevation = point.getCoordinates().getAltitude();
                if (isEmpty(elevation))
                    elevation = null;

                CompactCalendar time = parseTime(trim(feature.getProperty(DATE)));

                result.add(new Wgs84Position(point.getCoordinates().getLongitude(),
                        point.getCoordinates().getLatitude(), elevation,
                        null, time, asDescription(name, address)));
            } else
                log.warning("Reading Geometry object " + geometry + " is not supported.");
        }
        return result;
    }

    private Feature createFeature(Wgs84Position position) {
        Feature feature = new Feature();

        Map<String, Object> properties = new HashMap<>();
        if (position.getDescription() != null) {
            String name = asName(position.getDescription());
            if (name != null)
                properties.put(NAME, name);
            String desc = asDesc(position.getDescription());
            if (desc != null)
                properties.put(ADDRESS, desc);
        }
        if (position.getTime() != null) {
            String date = formatDate(position.getTime());
            properties.put(DATE, date);
        }
        feature.setProperties(properties);

        LngLatAlt coordinates = new LngLatAlt();
        if (position.getElevation() != null)
            coordinates.setAltitude(position.getElevation());
        if (position.getLatitude() != null)
            coordinates.setLatitude(position.getLatitude());
        if (position.getLongitude() != null)
            coordinates.setLongitude(position.getLongitude());

        Point point = new Point();
        point.setCoordinates(coordinates);
        feature.setGeometry(point);

        return feature;
    }

    public void write(Wgs84Route route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(route, target, UTF8_ENCODING, startIndex, endIndex);
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) throws IOException {
        FeatureCollection featureCollection = new FeatureCollection();
        List<Wgs84Position> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            featureCollection.add(createFeature(position));
        }
        ObjectMapper mapper = new ObjectMapper();
        if (preferences.getBoolean("prettyPrintXml", true))
            mapper.enable(INDENT_OUTPUT);
        mapper.writeValue(writer, featureCollection);
    }
}
