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

package slash.navigation.util;

import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.Wgs84Position;
import slash.navigation.itn.TomTomPosition;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.formatIntAsString;
import static slash.common.io.Transfer.trim;

/**
 * Helpers for processing of comments of positions
 *
 * @author Christian Pesch
 */
public abstract class RouteComments {
    private static final Preferences preferences = Preferences.userNodeForPackage(RouteComments.class);
    private static final String MAXIMUM_ROUTE_NAME_LENGTH_PREFERENCE = "maximumRouteNameLength";

    public static String shortenRouteName(BaseRoute route) {
        String result = "?";
        if (route != null) {
            if (route.getName() != null)
                result = route.getName();
            int maximumRouteNameLength = preferences.getInt(MAXIMUM_ROUTE_NAME_LENGTH_PREFERENCE, 50);
            if (result.length() > maximumRouteNameLength)
                result = result.substring(0, maximumRouteNameLength) + "...";
        }
        return result;
    }

    public static String createRouteName(List<? extends BaseNavigationPosition> positions) {
        if (positions.size() > 0)
            return positions.get(0).getComment() + " to " + positions.get(positions.size() - 1).getComment();
        else
            return "?";
    }

    public static void commentRouteName(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        if (route.getName() == null) {
            route.setName(createRouteName(route.getPositions()));
        }
    }

    @SuppressWarnings("unchecked")
    public static String createRouteDescription(BaseRoute route) {
        String name = trim(route.getName());
        List<String> description = route.getDescription();
        StringBuilder buffer = new StringBuilder();
        if (name != null)
            buffer.append(name);
        if (description != null) {
            for (String line : description)
                buffer.append(line);
        }
        return buffer.toString();
    }

    private static final String POSITION = "Position";
    private static final Pattern POSITION_PATTERN = Pattern.compile("(.*)" + POSITION + ".*(\\d+)(.*)");

    private static String getPositionComment(int index) {
        return POSITION + " " + (index + 1);
    }

    public static void commentPositions(List<? extends BaseNavigationPosition> positions) {
        for (int i = 0; i < positions.size(); i++) {
            BaseNavigationPosition position = positions.get(i);
            String original = position.getComment();
            String modified = getPositionComment(position, i);
            if (original == null || !original.equals(modified))
                position.setComment(modified);
        }
    }

    private static String getPositionComment(BaseNavigationPosition position, int index) {
        if (position.getComment() == null || "(null)".equals(position.getComment())) {
            return getPositionComment(index);
        } else {
            Matcher matcher = POSITION_PATTERN.matcher(position.getComment());
            if (matcher.matches()) {
                String prefix = trim(matcher.group(1));
                String postfix = trim(matcher.group(3));
                return (prefix != null ? prefix : "") + getPositionComment(index) + (postfix != null ? postfix : "");
            }
        }
        return position.getComment();
    }

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\s*(\\d*)(.*)");

    public static String getNumberedPosition(BaseNavigationPosition position, int index,
                                             int digitCount, NumberPattern numberPattern) {
        String number = formatIntAsString((index + 1), digitCount);
        String comment = getPositionComment(position, index);
        Matcher matcher = NUMBER_PATTERN.matcher(comment);
        String description = matcher.matches() ? matcher.group(2) : comment;
        return formatNumberedPosition(numberPattern, number, trim(description));
    }

    public static String formatNumberedPosition(NumberPattern numberPattern, String number, String description) {
        switch (numberPattern) {
            case DESCRIPTION_ONLY:
                return description;
            case NUMBER_ONLY:
                return number;
            case NUMBER_DIRECTLY_FOLLOWED_BY_DESCRIPTION:
                return description != null ? number + description : number;
            case NUMBER_SPACE_THEN_DESCRIPTION:
                return  description != null ? number + " " + description : number;
            default:
                throw new IllegalArgumentException("Number pattern " + numberPattern + " is not supported");
        }
    }

    @SuppressWarnings("unchecked")
    public static void commentRoutePositions(List<? extends BaseRoute> routes) {
        if(routes.size() < 2)
            return;

        Map<LongitudeAndLatitude, String> comments = new HashMap<LongitudeAndLatitude, String>();
        Map<LongitudeAndLatitude, Double> elevations = new HashMap<LongitudeAndLatitude, Double>();
        Map<LongitudeAndLatitude, CompactCalendar> times = new HashMap<LongitudeAndLatitude, CompactCalendar>();
        Map<LongitudeAndLatitude, Double> speeds = new HashMap<LongitudeAndLatitude, Double>();

        for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : routes) {
            for (BaseNavigationPosition position : route.getPositions()) {
                if (!position.hasCoordinates())
                    continue;

                if (position.getComment() != null) {
                    LongitudeAndLatitude lal = new LongitudeAndLatitude(position);
                    if (comments.get(lal) == null) {
                        comments.put(lal, position.getComment());
                    }
                }

                if (position.getElevation() != null) {
                    LongitudeAndLatitude lal = new LongitudeAndLatitude(position);
                    if (elevations.get(lal) == null) {
                        elevations.put(lal, position.getElevation());
                    }
                }

                if (position.getSpeed() != null) {
                    LongitudeAndLatitude lal = new LongitudeAndLatitude(position);
                    if (speeds.get(lal) == null) {
                        speeds.put(lal, position.getSpeed());
                    }
                }

                if (position.getTime() != null) {
                    LongitudeAndLatitude lal = new LongitudeAndLatitude(position);
                    if (times.get(lal) == null) {
                        times.put(lal, position.getTime());
                    }
                }
            }
        }

        for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : routes) {
            for (BaseNavigationPosition position : route.getPositions()) {
                if (!position.hasCoordinates())
                    continue;

                if (position.getComment() == null) {
                    LongitudeAndLatitude lal = new LongitudeAndLatitude(position);
                    String comment = comments.get(lal);
                    if (comment != null) {
                        position.setComment(comment);
                    }
                }

                if (position.getElevation() == null) {
                    LongitudeAndLatitude lal = new LongitudeAndLatitude(position);
                    Double elevation = elevations.get(lal);
                    if (elevation != null) {
                        position.setElevation(elevation);
                    }
                }

                if (position.getSpeed() == null) {
                    LongitudeAndLatitude lal = new LongitudeAndLatitude(position);
                    Double speed = speeds.get(lal);
                    if (speed != null) {
                        position.setSpeed(speed);
                    }
                }

                if (position.getTime() == null) {
                    LongitudeAndLatitude lal = new LongitudeAndLatitude(position);
                    CompactCalendar time = times.get(lal);
                    if (time != null) {
                        position.setTime(time);
                    }
                }
            }
        }

        for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : routes) {
            commentPositions(route.getPositions());
        }
    }


    private static class LongitudeAndLatitude {
        public final double longitude, latitude;

        public LongitudeAndLatitude(BaseNavigationPosition position) {
            this.longitude = position.getLongitude();
            this.latitude = position.getLatitude();
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final LongitudeAndLatitude that = (LongitudeAndLatitude) o;

            return Double.compare(that.latitude, latitude) == 0 &&
                    Double.compare(that.longitude, longitude) == 0;
        }

        public int hashCode() {
            int result;
            long temp;
            temp = longitude != +0.0d ? Double.doubleToLongBits(longitude) : 0L;
            result = (int) (temp ^ (temp >>> 32));
            temp = latitude != +0.0d ? Double.doubleToLongBits(latitude) : 0L;
            result = 29 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }


    public static final SimpleDateFormat TRIPMASTER_TIME = new SimpleDateFormat("HH:mm:ss");
    public static final SimpleDateFormat TRIPMASTER_DATE = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    static {
        TRIPMASTER_TIME.setTimeZone(CompactCalendar.UTC);
        TRIPMASTER_DATE.setTimeZone(CompactCalendar.UTC);
    }

    private static final String TIME = "\\d{1,2}:\\d{2}:\\d{2}";
    private static final String DATE = "\\d{2}/\\d{2}/\\d{4}";
    private static final String DATE_WITHOUT_SEPARATOR = "\\d{6}";
    private static final String DOUBLE = "[-\\d\\.]+";
    private static final String TRIPMASTER_REASONS = "Dur. " + TIME + "|Dauer " + TIME + "|" +
            "Abstand \\d+|Dist. \\d+|Distanz \\d+|Km " + DOUBLE + "|" +
            "Course \\d+|Cape \\d+|Kurs \\d+|Richtung \\d+|" +
            "Waypoint|Wpt|Punkt|Pause";

    private static final Pattern TRIPMASTER_HEADING_PATTERN = Pattern.compile("(Course|Cape|Kurs|Richtung) (\\d+)");
    private static final Pattern TRIPMASTER_14_PATTERN = Pattern.compile("(" + TRIPMASTER_REASONS + ") - (" + TIME + ") - (" + DOUBLE + ") m - (.+)");
    private static final Pattern TRIPMASTER_18_SHORT_STARTEND_PATTERN = Pattern.compile(
            "(Start|Ende|Finish) : ((.+) - )?(.+) - (.+) - (" + DOUBLE + ") m - (" + DOUBLE + ") (K|k)m");
    private static final Pattern TRIPMASTER_18_SHORT_WAYPOINT_PATTERN = Pattern.compile("(" + TIME + ") - (" + DOUBLE + ") m");
    private static final Pattern TRIPMASTER_25_SHORT_STARTEND_PATTERN = Pattern.compile(
            "(" + TIME + ") - ((Start|Ende|Finish) : (" + DATE + ") (" +  TIME + ")) - (" + DOUBLE + ") m");
    private static final Pattern TRIPMASTER_25_SHORT_WAYPOINT_PATTERN = Pattern.compile(
            "(" + TIME + ") - (.+) - (" + DOUBLE + ") m");
    private static final Pattern TRIPMASTER_MIDDLE_PATTERN = Pattern.compile(
            "(" + TIME + ") - (" + TRIPMASTER_REASONS + ")(\\s?:\\s.+)? - (" + DOUBLE + ") m - (" + DOUBLE + ") (K|k)m");
    private static final Pattern TRIPMASTER_LONG_NO_REASON_PATTERN = Pattern.compile(
            "(" + TIME + ") - (.+) - (" + DOUBLE + ") m - (" + DOUBLE + ") (K|k)m - (" + DOUBLE + ") (K|k)m/h( - \\d+)?");
    private static final Pattern TRIPMASTER_LONG_PATTERN = Pattern.compile(
            "(" + TIME + ") - (Start : (.*?)|Finish : (.*?)|" + TRIPMASTER_REASONS + ")(\\s?:\\s.+)? - " +
            "(" + DOUBLE + ") m - (" + DOUBLE + ") (K|k)m - (" + DOUBLE + ") (K|k)m/h( - \\d+)?");
    private static final Pattern ROUTECONVERTER_STARTEND_PATTERN = Pattern.compile(
            "((Start|Ende|Finish) : (.+) : (" + DATE + ") (" +  TIME + ")) - (" + DOUBLE + ") m - (" + DOUBLE + ") (K|k)m/h - (" + DOUBLE + ") deg.*");
    private static final Pattern ROUTECONVERTER_INTERMEDIATE_PATTERN = Pattern.compile(
            "(.+) : (" + TIME + ") - (" + DOUBLE + ") m - (" + DOUBLE + ") (K|k)m/h - (" + DOUBLE + ") deg.*");

    /**
     * logpos encoding of the comment:
     * + looks like a planned position with a verbose comment
     * + Rottstuecker (Wiesloch); K4174 Horrenberger Strasse @166.6m (s=60 d=34)
     * - looks like a tracked position with a verbose comment
     * - Rottstuecker (Wiesloch); K4174 Horrenberger Strasse @162.6m (s=66 d=6)
     * * is a coordinate comment
     * * 1000462:4889518 @365.8m (s=1 d=193)
     * = seems to be written if the position does not change for a time period
     * = 1000466:4889529 (@365.8m 090314 07:36:52 - 090314 08:02:04)
     */
    private static final String COMMENT_SEPARATOR = "(\\+|-|\\*|=)";
    private static final SimpleDateFormat LOGPOS_DATE = new SimpleDateFormat("yyMMdd HH:mm:ss");
    static {
        LOGPOS_DATE.setTimeZone(CompactCalendar.UTC);
    }
    private static final Pattern LOGPOS_1_PATTERN = Pattern.compile("(" + DATE_WITHOUT_SEPARATOR + " " + TIME + "): " +
            COMMENT_SEPARATOR + " (.+) \\(?@(" + DOUBLE + "|\\?)m \\(?((s=(\\d+) d=(\\d+))?.*)\\)");
    private static final Pattern LOGPOS_2_PATTERN = Pattern.compile("(" + DATE_WITHOUT_SEPARATOR + " " + TIME + "): " +
            COMMENT_SEPARATOR + " (.+) \\((s=(\\d+) d=(\\d+))\\)");

    private static final String TTTRACKLOG_NUMBER = "\\d+\\.?\\d?";
    private static final String TTTRACKLOG_REASONS = "Start|End|" +
            TTTRACKLOG_NUMBER + " \\w+ Pause( (" + TTTRACKLOG_NUMBER + ")m)?|" +
            "v=(" + TTTRACKLOG_NUMBER + ") alt=(" + TTTRACKLOG_NUMBER + ")";
    private static final Pattern TTTRACKLOG_PATTERN = Pattern.compile("(\\d{2}:\\d{2}:?\\d{0,2}) " +
            "(" + TTTRACKLOG_REASONS + ") .*");


    private static CompactCalendar parse(String string, DateFormat dateFormat) {
        if (string == null)
            return null;
        try {
            Date parsed = dateFormat.parse(string);
            return CompactCalendar.fromDate(parsed);
        } catch (ParseException e) {
            return null;
        }
    }

    private static CompactCalendar parseTripmaster14Time(String string) {
        return parse(string, TRIPMASTER_TIME);
    }

    private static CompactCalendar parseTripmaster18Date(String string) {
        return parse(string, TRIPMASTER_DATE);
    }

    public static Double parseTripmasterHeading(String string) {
        Matcher matcher = TRIPMASTER_HEADING_PATTERN.matcher(string);
        if (matcher.matches()) {
            return Transfer.parseDouble(matcher.group(2));
        }
        return null;
    }

    private static CompactCalendar parseLogposDate(String string) {
        return parse(string, LOGPOS_DATE);
    }

    private static CompactCalendar parseTTTracklogTime(String string) {
        if (string.length() == 5)
            string += ":00";
        return parseTripmaster14Time(string);
    }

    private static Double parseDouble(String string) {
        Double aDouble = Transfer.parseDouble(string);
        return !Transfer.isEmpty(aDouble) ? aDouble : null;
    }

    public static void parseComment(BaseNavigationPosition position, String comment) {
        Matcher matcher = TRIPMASTER_14_PATTERN.matcher(comment);
        if (matcher.matches()) {
            position.setTime(parseTripmaster14Time(matcher.group(2)));
            position.setElevation(parseDouble(matcher.group(3)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                String reason = trim(matcher.group(1));
                tomTomPosition.setReason(reason);
                tomTomPosition.setHeading(parseTripmasterHeading(reason));
                tomTomPosition.setCity(trim(matcher.group(4)));
            }

            if  (position instanceof Wgs84Position) {
                Wgs84Position wgs84Position = (Wgs84Position) position;
                String reason = trim(matcher.group(1));
                wgs84Position.setHeading(parseTripmasterHeading(reason));
            }
        }

        matcher = TRIPMASTER_18_SHORT_STARTEND_PATTERN.matcher(comment);
        if (matcher.matches()) {
            String dateStr = trim(matcher.group(4));
            String timeStr = trim(matcher.group(5));
            position.setTime(parseTripmaster18Date(dateStr + " " + timeStr));
            if (position.getTime() == null)
                position.setTime(parseTripmaster14Time(timeStr));
            position.setElevation(parseDouble(matcher.group(6)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                String city = trim(matcher.group(3));
                if (city == null) {
                    city = dateStr;
                    dateStr = null;
                }
                tomTomPosition.setReason(trim(matcher.group(1)) + " : " + (dateStr != null ? dateStr + " - " : "") + timeStr);
                tomTomPosition.setCity(city);
            }
        }

        matcher = TRIPMASTER_18_SHORT_WAYPOINT_PATTERN.matcher(comment);
        if (matcher.matches()) {
            position.setTime(parseTripmaster14Time(matcher.group(1)));
            position.setElevation(parseDouble(matcher.group(2)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                tomTomPosition.setReason("Waypoint");
                tomTomPosition.setCity(null);
            }

        }

        matcher = TRIPMASTER_25_SHORT_WAYPOINT_PATTERN.matcher(comment);
        if (matcher.matches()) {
            position.setTime(parseTripmaster14Time(trim(matcher.group(1))));
            position.setElevation(parseDouble(matcher.group(3)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                String reason = trim(matcher.group(2));
                tomTomPosition.setReason(reason);
                tomTomPosition.setHeading(parseTripmasterHeading(reason));
                tomTomPosition.setCity(null);
            }

            if  (position instanceof Wgs84Position) {
                Wgs84Position wgs84Position = (Wgs84Position) position;
                String reason = trim(matcher.group(2));
                wgs84Position.setHeading(parseTripmasterHeading(reason));
            }
        }

        matcher = TRIPMASTER_25_SHORT_STARTEND_PATTERN.matcher(comment);
        if (matcher.matches()) {
            String dateStr = trim(matcher.group(4));
            String timeStr = trim(matcher.group(5));
            position.setTime(parseTripmaster18Date(dateStr + " " + timeStr));
            position.setElevation(parseDouble(matcher.group(6)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                String reason = trim(matcher.group(2));
                tomTomPosition.setReason(reason);
                tomTomPosition.setCity(null);
            }
        }

        matcher = TRIPMASTER_MIDDLE_PATTERN.matcher(comment);
        if (matcher.matches()) {
            position.setTime(parseTripmaster14Time(trim(matcher.group(1))));
            position.setElevation(parseDouble(matcher.group(4)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                String city = trim(matcher.group(3));
                if (city != null && city.startsWith(": "))
                    city = trim(city.substring(2, city.length()));
                String reason = trim(matcher.group(2));
                if (reason == null)
                    reason = city;
                tomTomPosition.setCity(city);
                tomTomPosition.setReason(reason);
            }
        }

        matcher = TRIPMASTER_LONG_NO_REASON_PATTERN.matcher(comment);
        if (matcher.matches()) {
            position.setTime(parseTripmaster14Time(trim(matcher.group(1))));
            position.setSpeed(parseDouble(matcher.group(6)));
            position.setElevation(parseDouble(matcher.group(3)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                String city = trim(matcher.group(2));
                if (city != null && city.startsWith(": "))
                    city = trim(city.substring(2, city.length()));
                tomTomPosition.setCity(city);
                tomTomPosition.setReason(city);
            }
        }

        matcher = TRIPMASTER_LONG_PATTERN.matcher(comment);
        if (matcher.matches()) {
            position.setTime(parseTripmaster18Date(matcher.group(3)));
            if (position.getTime() == null)
                position.setTime(parseTripmaster14Time(matcher.group(1)));
            position.setSpeed(parseDouble(matcher.group(9)));
            position.setElevation(parseDouble(matcher.group(6)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                String city = trim(matcher.group(5));
                if (city != null && city.startsWith(": "))
                    city = trim(city.substring(2, city.length()));
                String reason = trim(matcher.group(2));
                if (city == null)
                    city = reason;
                tomTomPosition.setCity(city);
                tomTomPosition.setReason(reason);
            }
        }

        matcher = LOGPOS_2_PATTERN.matcher(comment);
        if (matcher.matches()) {
            position.setTime(parseLogposDate(matcher.group(1)));
            position.setSpeed(parseDouble(matcher.group(5)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                tomTomPosition.setReason(trim(matcher.group(4)));
                tomTomPosition.setCity(trim(matcher.group(3)));
                tomTomPosition.setHeading(parseDouble(matcher.group(6)));
            }
        }

        matcher = LOGPOS_1_PATTERN.matcher(comment);
        if (matcher.matches()) {
            position.setTime(parseLogposDate(matcher.group(1)));
            Double elevation;
            try {
                elevation = parseDouble(matcher.group(4));
            } catch (NumberFormatException e) {
                elevation = null;
            }
            position.setElevation(elevation);
            position.setSpeed(parseDouble(matcher.group(7)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                tomTomPosition.setReason(trim(matcher.group(5)));
                tomTomPosition.setCity(trim(matcher.group(3)));
                tomTomPosition.setHeading(parseDouble(matcher.group(8)));
            }
        }

        matcher = TTTRACKLOG_PATTERN.matcher(comment);
        if (matcher.matches()) {
            position.setTime(parseTTTracklogTime(matcher.group(1)));
            position.setSpeed(parseDouble(matcher.group(5)));
            Double elevation = parseDouble(matcher.group(6));
            if(elevation == null)
                elevation = parseDouble(matcher.group(4)); // pause with elevation
            position.setElevation(elevation);
            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                tomTomPosition.setReason(trim(matcher.group(2)));
            }
        }

        matcher = ROUTECONVERTER_STARTEND_PATTERN.matcher(comment);
        if (matcher.matches()) {
            String dateStr = trim(matcher.group(4));
            String timeStr = trim(matcher.group(5));
            position.setTime(parseTripmaster18Date(dateStr + " " + timeStr));
            position.setElevation(parseDouble(matcher.group(6)));
            position.setSpeed(parseDouble(matcher.group(7)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                String reason = trim(matcher.group(2));
                tomTomPosition.setReason(reason);
                String city = trim(matcher.group(3));
                tomTomPosition.setCity(city);
                tomTomPosition.setHeading(parseDouble(matcher.group(9)));
            }
        }

        matcher = ROUTECONVERTER_INTERMEDIATE_PATTERN.matcher(comment);
        if (matcher.matches()) {
            String timeStr = trim(matcher.group(2));
            position.setTime(parseTripmaster14Time(timeStr));
            position.setElevation(parseDouble(matcher.group(3)));
            position.setSpeed(parseDouble(matcher.group(4)));

            if (position instanceof TomTomPosition) {
                TomTomPosition tomTomPosition = (TomTomPosition) position;
                String city = trim(matcher.group(1));
                tomTomPosition.setCity(city);
                tomTomPosition.setHeading(parseDouble(matcher.group(6)));
            }
        }
    }
}
