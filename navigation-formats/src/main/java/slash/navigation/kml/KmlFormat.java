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

import slash.common.hex.HexDecoder;
import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.base.NavigationFileParser;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.googlemaps.GoogleMapsPosition;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
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
    static final Preferences preferences = Preferences.userNodeForPackage(KmlFormat.class);

    static final String WAYPOINTS = "Waypoints";
    static final String ROUTE = "Route";
    static final String TRACK = "Track";
    static final String ROUTE_LINE_STYLE = "routeStyle";
    static final String TRACK_LINE_STYLE = "trackStyle";

    public String getExtension() {
        return ".kml";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public boolean isWritingRouteCharacteristics() {
        return true;
    }

    @SuppressWarnings("unchecked")
    public <P extends BaseNavigationPosition> KmlRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new KmlRoute(this, characteristics, name, null, (List<KmlPosition>) positions);
    }

    abstract List<KmlRoute> internalRead(InputStream source, CompactCalendar startDate) throws IOException, JAXBException;

    protected KmlPosition asKmlPosition(GoogleMapsPosition position) {
        return new KmlPosition(position.getLongitude(), position.getLatitude(), position.getElevation(), null, null, position.getComment());
    }

    protected List<KmlPosition> asKmlPositions(List<String> strings) {
        StringBuffer buffer = new StringBuffer();
        for (String string : strings) {
            buffer.append(string);
            // to make sure the numbers are separated if they were already parsed by the XML parse
            buffer.append(' ');
        }
        List<KmlPosition> result = new ArrayList<KmlPosition>();
        for (GoogleMapsPosition position : GoogleMapsPosition.parsePositions(buffer.toString()))
            result.add(asKmlPosition(position));
        return result;
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

    protected void enrichPosition(KmlPosition position, Calendar time, String name, String description, CompactCalendar startDate) {
        if (position.getTime() == null && time != null)
            position.setTime(CompactCalendar.fromCalendar(time));
        if (position.getComment() == null)
            position.setComment(name);

        if (position.getTime() == null) {
            CompactCalendar logTime = parseTime(description);
            if (logTime != null)
                position.setTime(logTime);
            else {
                logTime = parseTime(name);
                if (logTime != null) {
                    position.setTime(logTime);
                    position.setStartDate(startDate);
                }
            }
        }

        if (position.getElevation() == null) {
            Double elevation = parseElevation(description);
            if (elevation == null)
                elevation = parseElevation(name);
            position.setElevation(elevation);
        }

        if (position.getSpeed() == null) {
            Double speed = parseSpeed(description);
            position.setSpeed(speed);
        }
    }

    private static final Pattern TAVELLOG_DATE_PATTERN = Pattern.compile(".*Time:.*(\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}).*");
    private static final SimpleDateFormat TAVELLOG_DATE = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    static {
        TAVELLOG_DATE.setTimeZone(CompactCalendar.UTC);
    }

    private static final Pattern NAVIGON6310_TIME_AND_ELEVATION_PATTERN = Pattern.compile(".*(\\d{2}:\\d{2}:\\d{2}),([\\d\\.\\s]+)meter.*");
    private static final SimpleDateFormat NAVIGON6310_TIME = new SimpleDateFormat("HH:mm:ss");
    static {
        NAVIGON6310_TIME.setTimeZone(CompactCalendar.UTC);
    }

    CompactCalendar parseTime(String description) {
        if (description != null) {
            Matcher tavelLogMatcher = TAVELLOG_DATE_PATTERN.matcher(description);
            if (tavelLogMatcher.matches()) {
                String timeString = tavelLogMatcher.group(1);
                try {
                    Date parsed = TAVELLOG_DATE.parse(timeString);
                    return CompactCalendar.fromDate(parsed);
                }
                catch (ParseException e) {
                    // intentionally left empty;
                }
            }
            Matcher navigonMatcher = NAVIGON6310_TIME_AND_ELEVATION_PATTERN.matcher(description);
            if (navigonMatcher.matches()) {
                String timeString = navigonMatcher.group(1);
                try {
                    Date parsed = NAVIGON6310_TIME.parse(timeString);
                    return CompactCalendar.fromDate(parsed);
                }
                catch (ParseException e) {
                    // intentionally left empty;
                }
            }
        }
        return null;
    }

    private static final Pattern TAVELLOG_SPEED_PATTERN = Pattern.compile(".*Speed:\\s*(\\d+\\.\\d+).*");
    private static final Pattern WBT201LOG_SPEED_PATTERN = Pattern.compile(".*Speed=\\s*(\\d+)\\s*Km.*", Pattern.DOTALL); // dot captures line terminators, too

    Double parseSpeed(String description) {
        if (description != null) {
            Matcher tavelLogMatcher = TAVELLOG_SPEED_PATTERN.matcher(description);
            if (tavelLogMatcher.matches()) {
                return Transfer.parseDouble(tavelLogMatcher.group(1));
            }
            Matcher wbt201LogMatcher = WBT201LOG_SPEED_PATTERN.matcher(description);
            if (wbt201LogMatcher.matches()) {
                return Transfer.parseDouble(wbt201LogMatcher.group(1));
            }
        }
        return null;
    }

    private static final Pattern TAVELLOG_ELEVATION_PATTERN = Pattern.compile(".*Altitude:\\s*(\\d+\\.\\d+).*");

    Double parseElevation(String description) {
        if (description != null) {
            Matcher tavelLogMatcher = TAVELLOG_ELEVATION_PATTERN.matcher(description);
            if (tavelLogMatcher.matches()) {
                return Transfer.parseDouble(tavelLogMatcher.group(1));
            }
            Matcher navigonMatcher = NAVIGON6310_TIME_AND_ELEVATION_PATTERN.matcher(description);
            if (navigonMatcher.matches()) {
                return Transfer.parseDouble(navigonMatcher.group(2));
            }
        }
        return null;
    }

    protected String concatPath(String path, String fragment) {
        path = Transfer.trim(path);
        fragment = Transfer.trim(fragment);
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
                        result.add(resultClass.cast(route));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.fine("Error reading url " + url + ": " + e.getMessage());
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
