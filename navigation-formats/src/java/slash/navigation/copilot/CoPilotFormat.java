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

package slash.navigation.copilot;

import slash.navigation.*;
import slash.navigation.util.Conversion;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * The base of all CoPilot formats.
 *
 * @author Christian Pesch
 */

public abstract class CoPilotFormat extends SimpleFormat<Wgs84Route> {
    protected static final String START_TRIP = "Start Trip";
    protected static final String END_TRIP = "End Trip";
    protected static final String START_STOP = "Start Stop";
    protected static final String END_STOP = "End Stop";
    protected static final String START_STOP_OPT = "Start StopOpt";
    protected static final String END_STOP_OPT = "End StopOpt";

    protected static final char NAME_VALUE_SEPARATOR = '=';
    protected static final Pattern NAME_VALUE_PATTERN = Pattern.compile("(.+?)" + NAME_VALUE_SEPARATOR + "(.+|)");
    protected static final double INTEGER_FACTOR = 1000000.0;

    protected static final String CREATOR = "Creator";
    protected static final String LONGITUDE = "Longitude";
    protected static final String LATITUDE = "Latitude";
    protected static final String STATE = "State";
    protected static final String ZIP = "Zip";
    protected static final String CITY = "City";
    protected static final String COUNTY = "County";
    protected static final String ADDRESS = "Address";


    public String getExtension() {
        return ".trp";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public <P extends BaseNavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    public List<Wgs84Route> read(BufferedReader reader, Calendar startDate, String encoding) throws IOException {
         List<Wgs84Position> positions = new ArrayList<Wgs84Position>();
         Map<String, String> map = new HashMap<String, String>();

         while (true) {
             String line = reader.readLine();
             if (line == null)
                 break;
             if (Conversion.trim(line) == null)
                 continue;

             if (line.startsWith(END_TRIP) || line.startsWith(END_STOP_OPT)) {
             } else if (line.startsWith(START_TRIP) || line.startsWith(START_STOP) || line.startsWith(START_STOP_OPT)) {
                 map.clear();
             } else if (line.startsWith(END_STOP)) {
                 Wgs84Position position = parsePosition(map);
                 positions.add(position);
                 map.clear();
             } else if (isNameValue(line)) {
                 String name = parseName(line);
                 String value = parseValue(line);
                 map.put(name, value);
             } else {
                 return null;
             }
         }

         if (positions.size() > 0)
             return Arrays.asList(new Wgs84Route(this, RouteCharacteristics.Route, positions));
         else
             return null;
     }

    boolean isNameValue(String line) {
        Matcher matcher = NAME_VALUE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private String parseName(String line) {
        Matcher matcher = NAME_VALUE_PATTERN.matcher(line);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        return matcher.group(1);
    }

    private String parseValue(String line) {
        Matcher matcher = NAME_VALUE_PATTERN.matcher(line);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        return matcher.group(2);
    }

    Wgs84Position parsePosition(Map<String, String> map) {
        Integer latitude = Conversion.parseInt(map.get(LATITUDE));
        Integer longitude = Conversion.parseInt(map.get(LONGITUDE));
        String state = Conversion.trim(map.get(STATE));
        String zip = Conversion.trim(map.get(ZIP));
        String city = Conversion.trim(map.get(CITY));
        String county = Conversion.trim(map.get(COUNTY));
        String address = Conversion.trim(map.get(ADDRESS));
        String comment = (state != null ? state + (zip != null ? "-" : " ") : "") +
                (zip != null ? zip + " " : "") + (city != null ? city : "") +
                (county != null ? ", " + county : "") + (address != null ? ", " + address : "");
        return new Wgs84Position(longitude != null ? longitude / INTEGER_FACTOR : null,
                latitude != null ? latitude / INTEGER_FACTOR : null,
                null, null, Conversion.trim(comment));
    }

}
