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

import slash.navigation.BaseNavigationPosition;
import slash.navigation.BaseRoute;
import slash.navigation.NavigationFileParser;
import slash.navigation.RouteCharacteristics;
import slash.navigation.hex.HexDecoder;
import slash.navigation.util.Conversion;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
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

public abstract class KmlFormat extends BaseKmlFormat {
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

    abstract List<KmlRoute> internalRead(InputStream inputStream) throws JAXBException;

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

    protected RouteCharacteristics parseCharacteristics(String nameToParse, RouteCharacteristics fallback) {
        RouteCharacteristics result = fallback;
        if (nameToParse != null) {
            int slashIndex = nameToParse.lastIndexOf('/');
            if (slashIndex != -1)
                nameToParse = nameToParse.substring(slashIndex + 1);
            if (nameToParse.startsWith("Waypoint"))
                result = RouteCharacteristics.Waypoints;
            else if (nameToParse.startsWith("Route"))
                result = RouteCharacteristics.Route;
            else if (nameToParse.startsWith("Track") || nameToParse.startsWith("Path"))
                result = RouteCharacteristics.Track;
        }
        return result;
    }

    protected String formatElevation(Double aDouble) {
        return aDouble != null ? aDouble.toString() : "0.0";
    }

    protected List<KmlRoute> parseRouteFromUrl(String url) {
        return parseRouteFromUrl(url, KmlRoute.class);
    }

    protected <T> List<T> parseRouteFromUrl(String url, Class<T> resultClass) {
        List<T> result = new ArrayList<T>();
        InputStream source = null;
        try {
            try {
                source = new URL(url).openStream();
                NavigationFileParser parser = new NavigationFileParser();
                boolean success = parser.read(source);
                if (success) {
                    List<BaseRoute> routes = parser.getAllRoutes();
                    for (BaseRoute route : routes) {
                        if (resultClass.isInstance(route))
                            result.add((T) route);
                    }
                }
            } finally {
                if (source != null)
                    source.close();
            }
        } catch (Exception e) {
            log.fine("Error reading " + url + ": " + e.getMessage());
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
