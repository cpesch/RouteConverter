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

import jakarta.xml.bind.JAXBElement;
import slash.common.type.CompactCalendar;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.*;
import static slash.common.type.HexadecimalNumber.decodeBytes;
import static slash.common.type.ISO8601.parseDate;
import static slash.navigation.base.RouteCharacteristics.*;
import static slash.navigation.common.NavigationConversion.formatElevationAsString;
import static slash.navigation.common.NavigationConversion.formatPositionAsString;
import static slash.navigation.common.PositionParser.parsePositions;

/**
 * The base of all Google Earth formats.
 *
 * @author Christian Pesch
 */

public abstract class KmlFormat extends BaseKmlFormat {
    static final Preferences preferences = Preferences.userNodeForPackage(KmlFormat.class);

    static final String WAYPOINTS = "Waypoints";
    static final String ROUTE = "Route";
    static final String TRACK = "Track";
    static final String SPEED = "Speed [Km/h]";
    static final String MARKS = "Marks [Km]";
    static final String ROUTE_LINE_STYLE = "routeStyle";
    static final String TRACK_LINE_STYLE = "trackStyle";
    static final String WAYPOINT_STYLE = "waypointStyle";

    public String getExtension() {
        return ".kml";
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public boolean isWritingRouteCharacteristics() {
        return true;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> KmlRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new KmlRoute(this, characteristics, name, null, (List<KmlPosition>) positions);
    }

    protected KmlPosition asKmlPosition(NavigationPosition position) {
        return new KmlPosition(position.getLongitude(), position.getLatitude(), position.getElevation(), null, null, position.getDescription());
    }

    protected List<KmlPosition> asKmlPositions(List<String> strings) {
        StringBuilder buffer = new StringBuilder();
        for (String string : strings) {
            buffer.append(string);
            // to make sure the numbers are separated if they were already parsed by the XML parse
            buffer.append(' ');
        }
        List<KmlPosition> result = new ArrayList<>();
        for (NavigationPosition position : parsePositions(buffer.toString()))
            result.add(asKmlPosition(position));
        return result;
    }

    protected String createDocumentName(KmlRoute route) {
        // some kind of crude workaround since the route carries the name of the
        // plus and divided by a slash the route of the track
        String name = asRouteName(route.getName());
        if (name != null) {
            StringTokenizer tokenizer = new StringTokenizer(name, "/");
            if (tokenizer.hasMoreTokens())
                name = tokenizer.nextToken();
        }
        return name;
    }

    private String removePrefix(String name, String prefix) {
        return name != null && name.startsWith(prefix) ? name.substring(prefix.length()) : name;
    }
    protected String parseFolderName(String folderName) {
        folderName = removePrefix(folderName, WAYPOINTS);
        folderName = removePrefix(folderName, ROUTE);
        folderName = removePrefix(folderName, TRACK);
        folderName = removePrefix(folderName, ": ");
        return folderName;
    }

    protected String createFolderName(String prefix, KmlRoute route) {
        // some kind of crude workaround since the route carries the name of the
        // plus and divided by a slash the route of the track
        String name = route.getName();
        if (name != null) {
            StringTokenizer tokenizer = new StringTokenizer(name, "/");
            while (tokenizer.hasMoreTokens())
                name = tokenizer.nextToken();

            if (!name.startsWith(prefix))
                name = prefix + ": " + name;
        } else
            name = prefix;
        return name;
    }

    protected String createCoordinates(KmlPosition position, boolean separateWithSpace) {
        return formatPositionAsString(position.getLongitude()) + (separateWithSpace ? " " : ",") +
                formatPositionAsString(position.getLatitude()) + (separateWithSpace ? " " : ",") +
                formatElevationAsString(position.getElevation());
    }

    protected RouteCharacteristics parseCharacteristics(String nameToParse, String styleUrl, RouteCharacteristics fallback) {
        RouteCharacteristics result = fallback;
        if (nameToParse != null) {
            int slashIndex = nameToParse.lastIndexOf('/');
            String folder = slashIndex != -1 ? nameToParse.substring(slashIndex + 1) : nameToParse;
            if (folder.startsWith("Waypoint") || nameToParse.contains("Waypoint"))
                result = Waypoints;
            else if (folder.startsWith("Route") || nameToParse.contains("Route"))
                result = Route;
            else if (folder.startsWith("Track") || folder.startsWith("Path") || nameToParse.contains("Track"))
                result = Track;
        }
        if (styleUrl != null && styleUrl.startsWith("#")) {
            styleUrl = styleUrl.substring(1);
            result = switch (styleUrl) {
                case WAYPOINT_STYLE -> Waypoints;
                case ROUTE_LINE_STYLE -> Route;
                case TRACK_LINE_STYLE -> Track;
                default -> result;
            };
        }
        return result;
    }

    protected CompactCalendar parseTime(String time) {
        if (time != null) {
            Calendar calendar = parseDate(time);
            if (calendar != null) {
                calendar.setTimeZone(UTC);
                return fromCalendar(calendar);
            }
        }
        return null;
    }

    protected void enrichPosition(KmlPosition position, CompactCalendar time, String name, String description, CompactCalendar startDate) {
        if (!position.hasTime() && time != null)
            position.setTime(time);
        if (!position.hasTime())
            parseTime(position, description, startDate);
        if (!position.hasTime())
            parseTime(position, name, startDate);
        if (position.getDescription() == null)
            position.setDescription(name);

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

    /**
     * Appends a placemark's positions as a single waypoint (one position) or a track (more).
     * Shared across all KML versions. The only behavioural divergence is which name feeds
     * parseCharacteristics: KML 2.0/2.1/2.2-beta use the path-prefixed route name, KML 2.2 uses
     * the bare placemark name; characteristicsFromRouteName preserves that exactly.
     */
    protected void appendPlacemarkAsWaypointOrTrack(String name, String description, String placemarkName,
                                                    boolean characteristicsFromRouteName, String placemarkDescription,
                                                    String styleUrl, CompactCalendar time, List<KmlPosition> positions,
                                                    List<KmlPosition> waypoints, slash.navigation.base.ParserContext<KmlRoute> context) {
        if (positions.size() == 1) {
            KmlPosition wayPoint = positions.get(0);
            enrichPosition(wayPoint, time, placemarkName, placemarkDescription, context.getStartDate());
            waypoints.add(wayPoint);
        } else {
            String routeName = concatPath(name, asName(placemarkName));
            List<String> routeDescription = asDescription(placemarkDescription != null ? placemarkDescription : description);
            RouteCharacteristics characteristics = parseCharacteristics(characteristicsFromRouteName ? routeName : placemarkName, styleUrl, Track);
            context.appendRoute(new KmlRoute(this, characteristics, routeName, routeDescription, positions));
        }
    }

    /**
     * Prepends the accumulated single-position placemarks as one combined waypoint route.
     * Identical across all KML versions.
     */
    protected void prependWaypointsRoute(String name, String description, List<KmlPosition> waypoints,
                                         slash.navigation.base.ParserContext<KmlRoute> context) {
        if (!waypoints.isEmpty()) {
            RouteCharacteristics characteristics = parseCharacteristics(name, null, Waypoints);
            context.prependRoute(new KmlRoute(this, characteristics, name, asDescription(description), waypoints));
        }
    }

    /**
     * Filters a list of feature elements down to those whose element local name matches,
     * regardless of the JAXB-bound feature supertype (KML 2.1 binds against FeatureType,
     * KML 2.2-beta/2.2 against AbstractFeatureType) -- the lookup only needs the element's
     * QName, which JAXBElement exposes independent of its type parameter. Identical across
     * all three modern KML versions; previously duplicated verbatim in each.
     */
    @SuppressWarnings("unchecked")
    protected <T> List<JAXBElement<T>> find(List<? extends JAXBElement<?>> elements, String name, Class<T> resultClass) {
        List<JAXBElement<T>> result = new ArrayList<>();
        if (elements != null) {
            for (JAXBElement<?> element : elements) {
                if (name.equals(element.getName().getLocalPart()))
                    result.add((JAXBElement<T>) element);
            }
        }
        return result;
    }

    @FunctionalInterface
    protected interface FeatureRecursor<T> {
        void recurse(String name, T value) throws IOException;
    }

    /**
     * Recurses into each child container (KML Folder or Document), invoking recurseInto with the
     * container's resolved name and its raw (version-specific) value. nameOf may return null to
     * skip a container without recursing into it -- KML 2.2 uses this to ignore its internal
     * "speed" and "marks" folders, a behaviour the other versions don't have and don't trigger
     * (their nameOf never returns null). Loop shape identical across KML 2.1/2.2-beta/2.2;
     * the per-version divergence lives entirely in the nameOf/recurseInto lambdas the caller
     * supplies.
     */
    protected <T> void extractTracksFromContainers(List<JAXBElement<T>> containers, Function<T, String> nameOf,
                                                    FeatureRecursor<T> recurseInto) throws IOException {
        for (JAXBElement<T> container : containers) {
            T value = container.getValue();
            String containerName = nameOf.apply(value);
            if (containerName != null)
                recurseInto.recurse(containerName, value);
        }
    }

    /**
     * Extracts positions from a Point/LineString/MultiGeometry element by dispatching on the
     * element's local name rather than its JAXB-bound Java type. KML 2.1/2.2-beta/2.2 each
     * generate separate, unrelated PointType/LineStringType/MultiGeometryType classes with no
     * shared supertype, but all three expose an identical getCoordinates() shape on Point/
     * LineString -- read here via reflection so one method serves all three instead of each
     * duplicating the same instanceof chain. Geometry kinds other than Point/LineString/
     * MultiGeometry (e.g. KML 2.2's gx:Track) are left to the caller, matching the original
     * per-version code, which also silently ignored anything it didn't explicitly check for.
     *
     * multiGeometryChildrenOf supplies the version-specific MultiGeometry child accessor
     * (getGeometry() in KML 2.1, getAbstractGeometryGroup() in 2.2-beta/2.2); recurse must be
     * the caller's own full extraction method (not this one), so that version-specific geometry
     * kinds nested inside a MultiGeometry are still recognised by the caller on the way back in.
     * Operates on raw JAXBElement<?>/Object rather than a shared bound type -- the per-version
     * binding classes don't have one -- so each caller's lambdas carry a single unchecked cast at
     * the type-erasure boundary; that cast is always safe since the value really is whatever
     * binding type the caller's own JAXB unmarshalling produced.
     */
    protected List<KmlPosition> extractPositionsByElementName(JAXBElement<?> geometryElement,
                                                               Function<Object, List<? extends JAXBElement<?>>> multiGeometryChildrenOf,
                                                               Function<JAXBElement<?>, List<KmlPosition>> recurse) {
        List<KmlPosition> positions = new ArrayList<>();
        if (geometryElement == null)
            return positions;
        Object value = geometryElement.getValue();
        switch (geometryElement.getName().getLocalPart()) {
            case "Point", "LineString" -> positions.addAll(asKmlPositions(reflectGetCoordinates(value)));
            case "MultiGeometry" -> {
                for (JAXBElement<?> child : multiGeometryChildrenOf.apply(value))
                    positions.addAll(recurse.apply(child));
            }
            default -> { /* geometry kind not handled here; left to the caller, as before */ }
        }
        return positions;
    }

    @SuppressWarnings("unchecked")
    private List<String> reflectGetCoordinates(Object geometryValue) {
        try {
            return (List<String>) geometryValue.getClass().getMethod("getCoordinates").invoke(geometryValue);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Cannot read coordinates from " + geometryValue.getClass(), e);
        }
    }

    private static final Pattern TAVELLOG_DATE_PATTERN = Pattern.compile(".*Time:.*(\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}).*");
    private static final String TAVELLOG_DATE ="yyyy/MM/dd HH:mm:ss";
    private static final Pattern NAVIGON6310_TIME_AND_ELEVATION_PATTERN = Pattern.compile(".*(\\d{2}:\\d{2}:\\d{2}),([\\d\\.\\s]+)meter.*");
    private static final String NAVIGON6310_TIME = "HH:mm:ss";
    private static final Pattern BT747_TIME_AND_ELEVATION_PATTERN = Pattern.compile(".*TIME:.*>(\\d{2}-.+-\\d{2} \\d{2}:\\d{2}:\\d{2})<.*>([\\d\\.\\s]+)m<.*");
    private static final String BT747_DATE = "dd-MMMMM-yy HH:mm:ss";
    private static final Pattern QSTARTZ_DATE_AND_SPEED_PATTERN = Pattern.compile(".*Date:\\s*(\\d{4}/\\d{2}/\\d{2}).*Time:\\s*(\\d{2}:\\d{2}:\\d{2}).*Speed:\\s*([\\d\\.]+)\\s*.*", DOTALL);

    void parseTime(NavigationPosition position, String description, CompactCalendar startDate) {
        if (description != null) {
            Matcher tavelLogMatcher = TAVELLOG_DATE_PATTERN.matcher(description);
            if (tavelLogMatcher.matches()) {
                String timeString = tavelLogMatcher.group(1);
                try {
                    Date parsed = createDateFormat(TAVELLOG_DATE).parse(timeString);
                    position.setTime(fromDate(parsed));
                } catch (ParseException e) {
                    // intentionally left empty;
                }
            }
            Matcher navigonMatcher = NAVIGON6310_TIME_AND_ELEVATION_PATTERN.matcher(description);
            if (navigonMatcher.matches()) {
                String timeString = navigonMatcher.group(1);
                try {
                    Date parsed = createDateFormat(NAVIGON6310_TIME).parse(timeString);
                    position.setTime(fromDate(parsed));
                    position.setStartDate(startDate);
                } catch (ParseException e) {
                    // intentionally left empty;
                }
            }
            Matcher bt747Matcher = BT747_TIME_AND_ELEVATION_PATTERN.matcher(description);
            if (bt747Matcher.matches()) {
                String timeString = bt747Matcher.group(1);
                try {
                    Date parsed = createDateFormat(BT747_DATE, Locale.ENGLISH).parse(timeString);
                    position.setTime(fromDate(parsed));
                } catch (ParseException e) {
                    // intentionally left empty;
                }
            }
            Matcher qstarzMatcher = QSTARTZ_DATE_AND_SPEED_PATTERN.matcher(description);
            if (qstarzMatcher.matches()) {
                String dateString = qstarzMatcher.group(1);
                String timeString = qstarzMatcher.group(2);
                try {
                    Date parsed = createDateFormat(TAVELLOG_DATE).parse(dateString + " " + timeString);
                    position.setTime(fromDate(parsed));
                } catch (ParseException e) {
                    // intentionally left empty;
                }
            }
        }
    }

    private static final Pattern TAVELLOG_SPEED_PATTERN = Pattern.compile(".*Speed:\\s*(\\d+\\.\\d+).*");
    private static final Pattern WBT201LOG_SPEED_PATTERN = Pattern.compile(".*Speed=\\s*(\\d+)\\s*Km.*", DOTALL); // dot captures line terminators, too

    Double parseSpeed(String description) {
        if (description != null) {
            Matcher tavelLogMatcher = TAVELLOG_SPEED_PATTERN.matcher(description);
            if (tavelLogMatcher.matches()) {
                return parseDouble(tavelLogMatcher.group(1));
            }
            Matcher wbt201LogMatcher = WBT201LOG_SPEED_PATTERN.matcher(description);
            if (wbt201LogMatcher.matches()) {
                return parseDouble(wbt201LogMatcher.group(1));
            }
            Matcher qstarzMatcher = QSTARTZ_DATE_AND_SPEED_PATTERN.matcher(description);
            if (qstarzMatcher.matches()) {
                return parseDouble(qstarzMatcher.group(3));
            }
        }
        return null;
    }

    private static final Pattern TAVELLOG_ELEVATION_PATTERN = Pattern.compile(".*Altitude:\\s*(\\d+\\.\\d+).*");

    Double parseElevation(String description) {
        if (description != null) {
            Matcher tavelLogMatcher = TAVELLOG_ELEVATION_PATTERN.matcher(description);
            if (tavelLogMatcher.matches()) {
                return parseDouble(tavelLogMatcher.group(1));
            }
            Matcher navigonMatcher = NAVIGON6310_TIME_AND_ELEVATION_PATTERN.matcher(description);
            if (navigonMatcher.matches()) {
                return parseDouble(navigonMatcher.group(2));
            }
            Matcher bt747Matcher = BT747_TIME_AND_ELEVATION_PATTERN.matcher(description);
            if (bt747Matcher.matches()) {
                return parseDouble(bt747Matcher.group(2));
            }
        }
        return null;
    }

    protected String concatPath(String path, String fragment) {
        path = trim(path);
        fragment = trim(fragment);
        return (path != null ? path : "") +
                (fragment != null ? (path != null ? "/" : "") + fragment : "");
    }

    protected float getLineWidth() {
        return preferences.getFloat("lineWidth", 3.0f);
    }

    protected byte[] getRouteLineColor() {
        String color = preferences.get("routeLineColor", "7FFF0055");
        return decodeBytes(color);
    }

    protected byte[] getTrackLineColor() {
        String color = preferences.get("trackLineColor", "FFFF00FF");
        return decodeBytes(color);
    }

    protected boolean isWriteName() {
        return preferences.getBoolean("writeName", true);
    }

    protected boolean isWriteDesc() {
        return preferences.getBoolean("writeDesc", true);
    }
}
