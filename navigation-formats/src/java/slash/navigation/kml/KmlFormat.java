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
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.text.ParseException;

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
    protected static final String[] SPEED_COLORS = {
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

    static final Pattern POSITION_PATTERN = Pattern.compile("(\\s*[[-|+]|\\d|\\.|E]*\\s*),(\\s*[[-|+]|\\d|\\.|E]*\\s*),?,?(\\s*[[-|+]|\\d|\\.|E]+\\s*)?");

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

    abstract List<KmlRoute> internalRead(InputStream source) throws IOException, JAXBException;

    boolean isPosition(String line) {
        Matcher matcher = POSITION_PATTERN.matcher(line);
        return matcher.matches();
    }

    protected KmlPosition parsePosition(String coordinates, String comment) {
        Matcher matcher = POSITION_PATTERN.matcher(coordinates);
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

    protected void enrichPosition(KmlPosition position, Calendar time, String comment, String travellogDescription) {
        if (position.getTime() == null)
            position.setTime(time);
        if (position.getComment() == null)
            position.setComment(comment);

        if (travellogDescription == null)
            return;

        Calendar logTime = parseTime(travellogDescription);
        if (position.getTime() == null)
            position.setTime(logTime);

        Double elevation = parseElevation(travellogDescription);
        if (position.getElevation() == null)
            position.setElevation(elevation);

        Double speed = parseSpeed(travellogDescription);
        if (position.getSpeed() == null)
            position.setSpeed(speed);
    }

    private static final SimpleDateFormat TAVELLOG_DATE = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final Pattern TAVELLOG_DATE_PATTERN = Pattern.compile(".*Time:.*(\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}).*");

    Calendar parseTime(String description) {
        if (description != null) {
            Matcher matcher = TAVELLOG_DATE_PATTERN.matcher(description);
            if (matcher.matches()) {
                String timeString = matcher.group(1);
                try {
                    Date date = TAVELLOG_DATE.parse(timeString);
                    Calendar time = Calendar.getInstance();
                    time.setTime(date);
                    return time;
                }
                catch (ParseException e) {
                    // intentionally left empty;
                }
            }
        }
        return null;
    }

    private static final Pattern TAVELLOG_SPEED_PATTERN = Pattern.compile(".*Speed:\\s*(\\d+\\.\\d+).*");

    Double parseSpeed(String description) {
        if (description != null) {
            Matcher matcher = TAVELLOG_SPEED_PATTERN.matcher(description);
            if (matcher.matches()) {
                return Conversion.parseDouble(matcher.group(1));
            }
        }
        return null;
    }

    private static final Pattern TAVELLOG_ELEVATION_PATTERN = Pattern.compile(".*Altitude:\\s*(\\d+\\.\\d+).*");

    Double parseElevation(String description) {
        if (description != null) {
            Matcher matcher = TAVELLOG_ELEVATION_PATTERN.matcher(description);
            if (matcher.matches()) {
                return Conversion.parseDouble(matcher.group(1));
            }
        }
        return null;
    }

    protected String concatPath(String path, String fragment) {
        path = Conversion.trim(path);
        fragment = Conversion.trim(fragment);
        String result = path != null ? path : "";
        if(fragment != null)
            result = result + "/" + fragment;
        return result;
    }

    protected List<KmlRoute> parseRouteFromUrl(String url) {
        return parseRouteFromUrl(url, KmlRoute.class);
    }

    protected <T> List<T> parseRouteFromUrl(String url, Class<T> resultClass) {
        List<T> result = new ArrayList<T>();
        try {
            NavigationFileParser parser = new NavigationFileParser();
            boolean success = parser.read(new URL(url));
            if (success) {
                List<BaseRoute> routes = parser.getAllRoutes();
                for (BaseRoute route : routes) {
                    if (resultClass.isInstance(route))
                        result.add((T) route);
                }
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
