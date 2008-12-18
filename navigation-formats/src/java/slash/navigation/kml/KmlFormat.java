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

package slash.navigation.kml;

import slash.navigation.*;
import slash.navigation.hex.HexDecoder;
import slash.navigation.util.Conversion;
import slash.navigation.util.InputOutput;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The base of all Google Earth formats.
 *
 * @author Christian Pesch
 */

public abstract class KmlFormat extends XmlNavigationFormat<KmlRoute> implements MultipleRoutesFormat<KmlRoute> {
    private static final Logger log = Logger.getLogger(KmlFormat.class.getName());
    protected static final Preferences preferences = Preferences.userNodeForPackage(KmlFormat.class);

    protected static final String WAYPOINTS = "Waypoints";
    protected static final String ROUTE = "Route";
    protected static final String TRACK = "Track";
    protected static final String ROUTE_LINE_STYLE = "routeStyle";
    protected static final String TRACK_LINE_STYLE = "trackStyle";
    protected static final String [] SPEED_COLORS = {
            "FF00ffff",
            "FF008080",
            "FF00ff00",
            "FF008000",
            "FFffff00",
            "FF808000",
            "FFff0000",
            "FF800000",
            "FFff00ff",
            "FF800080",
            "FF0000ff",
            "FF000080",
            "FF194c80",
            "FF000000"};
    protected static final int SPEED_SCALE = 10;
    protected static final String SPEEDBAR_URL = "http://ww.routeconverter.de/images/speedbar.png";

    private static final Pattern PATTERN = Pattern.compile("(\\s*[[-|+]|\\d|\\.|E]*\\s*),(\\s*[[-|+]|\\d|\\.|E]*\\s*),?,?(\\s*[[-|+]|\\d|\\.|E]+\\s*)?");

    public String getExtension() {
        return ".kml";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public <P extends BaseNavigationPosition> KmlRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new KmlRoute(this, characteristics, name, null, (List<KmlPosition>) positions);
    }

    boolean isPosition(String line) {
        Matcher matcher = PATTERN.matcher(line);
        return matcher.matches();
    }

    protected KmlPosition parsePosition(String coordinates, String comment) {
        Matcher matcher = PATTERN.matcher(coordinates);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + coordinates + "' does not match");
        String longitude = matcher.group(1);
        String latitude = matcher.group(2);
        String elevation = matcher.group(3);
        return new KmlPosition(Conversion.parseDouble(longitude), Conversion.parseDouble(latitude), Conversion.parseDouble(elevation), null, Conversion.trim(comment));
    }

    protected String createDocumentName(KmlRoute route) {
        // some kind of crude workaround since the route carries the name of the
        // plus and divided by a slash the route of the track
        String name = route.getName();
        if (name != null) {
            StringTokenizer tokenizer = new StringTokenizer(name, "/");
            if (tokenizer.hasMoreTokens())
                name = tokenizer.nextToken();
        }
        return name;
    }

    protected String createPlacemarkName(KmlRoute route) {
        // some kind of crude workaround since the route carries the name of the
        // plus and divided by a slash the route of the track
        String name = route.getName();
        if (name != null) {
            StringTokenizer tokenizer = new StringTokenizer(name, "/");
            while (tokenizer.hasMoreTokens())
                name = tokenizer.nextToken();
        }
        return name;
    }

    protected void addDescriptionToRoute(KmlRoute route, String descriptionString) {
        List<String> descriptionLineList = asDescription(descriptionString);
        int count = 0;
        if (descriptionLineList != null) {
            for (String descriptionLine : descriptionLineList) {
                route.getDescription().add(count++, descriptionLine);
            }
        }
    }

    protected RouteCharacteristics parseCharacteristics(String name) {
        RouteCharacteristics characteristics = RouteCharacteristics.Track;
        if (name != null) {
            int slashIndex = name.lastIndexOf('/');
            if (slashIndex != -1)
                name = name.substring(slashIndex + 1);
            if (name.startsWith("Waypoint"))
                characteristics = RouteCharacteristics.Waypoints;
            else if (name.startsWith("Route"))
                characteristics = RouteCharacteristics.Route;
            else if (name.startsWith("Track"))
                characteristics = RouteCharacteristics.Track;
        }
        return characteristics;
    }

    protected String formatElevation(Double aDouble) {
        return aDouble != null ? aDouble.toString() : "0.0";
    }

    protected List<KmlRoute> parseRouteFromUrl(String url) {
        List<KmlRoute> result = new ArrayList<KmlRoute>();
        File tempFile = null;
        try {
            tempFile = File.createTempFile("routeconverter", ".kml");
            InputOutput inout = new InputOutput(new URL(url).openStream(), new FileOutputStream(tempFile));
            inout.start();
            inout.close();
            NavigationFileParser parser = new NavigationFileParser();
            boolean success = parser.read(tempFile);
            if (success) {
                List<BaseRoute> routes = parser.getAllRoutes();
                for (BaseRoute route : routes) {
                    if (route instanceof KmlRoute)
                        result.add((KmlRoute) route);
                }
            }
        } catch (Exception e) {
            log.fine("Error reading " + url + ": " + e.getMessage());
        }
        finally {
            if (tempFile != null)
                tempFile.delete();
        }
        return result;
    }

    protected float getLineWidth() {
        return preferences.getFloat("lineWidth", 3.0f);
    }
    
    protected byte[] getRouteLineColor() {
        String color = preferences.get("routeLineColor", "7FFF0055");
        return HexDecoder.decodeBytes(color);
    }

    protected byte[] getTrackLineColor() {
        String color = preferences.get("trackLineColor", "FFFF00FF");
        return HexDecoder.decodeBytes(color);
    }
}
