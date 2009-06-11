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

package slash.navigation.itn;

import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.TextNavigationFormat;
import slash.navigation.util.Conversion;
import slash.navigation.util.RouteComments;
import slash.navigation.util.CompactCalendar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Tom Tom Route (.itn) files.
 *
 * @author Christian Pesch
 */

public abstract class TomTomRouteFormat extends TextNavigationFormat<TomTomRoute> {
    private static final int MAXIMUM_POSITION_COUNT = 48;
    private static final char SEPARATOR_CHAR = '|';
    private static final String SEPARATOR = "\\" + SEPARATOR_CHAR;
    private static final Pattern POSITION_PATTERN = Pattern.
            compile("([+-]?\\d+)" + SEPARATOR + "([+-]?\\d+)" + SEPARATOR +
                    "(.*)" + SEPARATOR + "\\d" + SEPARATOR + "?");
    private static final Pattern NAME_PATTERN = Pattern.
            compile("^\"([^\"]*)\"$");

    public static final int START_TYPE = 4;
    public static final int END_TYPE = 3;
    public static final int WAYPOINT = 1;

    public String getExtension() {
        return ".itn";
    }

    public int getMaximumFileNameLength() {
        return 18;
    }

    public int getMaximumPositionCount() {
        return MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends BaseNavigationPosition> TomTomRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new TomTomRoute(characteristics, name, (List<TomTomPosition>) positions);
    }

    protected abstract boolean isIso885915ButReadWithUtf8(String string);

    public List<TomTomRoute> read(BufferedReader reader, CompactCalendar startDate, String encoding) throws IOException {
        List<TomTomPosition> positions = new ArrayList<TomTomPosition>();

        String name = null;
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (line.length() == 0 || line.startsWith("~"))
                continue;
            // some files contain EF BB BF which is displayed as FF FE in UltraEdit
            // in UTF-8 mode we filter like this for a valid line:
            if (line.startsWith("\ufeff"))
                line = line.substring(1);
            // in ISO-8859-1 mode we filter like this:
            if (line.startsWith("\357\273\277"))
                line = line.substring(3);
            line = line.replaceAll("\u0080", "€");

            if (isPosition(line)) {
                TomTomPosition position = parsePosition(line);
                if (isValidStartDate(position.getTime()))
                    startDate = position.getTime();
                else if (startDate != null)
                    position.setStartDate(startDate);

                if (isIso885915ButReadWithUtf8(position.getComment()))
                    return null;

                positions.add(position);
            } else if (isName(line)) {
                name = parseName(line);
            } else {
                return null;
            }
        }

        if (positions.size() > 0)
            return Arrays.asList(new TomTomRoute(this, isTrack(positions) ? RouteCharacteristics.Track : RouteCharacteristics.Route, name, positions));
        else
            return null;
    }

    boolean isPosition(String line) {
        Matcher matcher = POSITION_PATTERN.matcher(line);
        return matcher.matches();
    }

    boolean isName(String line) {
        Matcher matcher = NAME_PATTERN.matcher(line);
        return matcher.matches();
    }

    private boolean isTrack(List<TomTomPosition> positions) {
        for (TomTomPosition position : positions) {
            if (position.getReason() == null && position.getTime() == null)
                return false;
        }
        return true;
    }

    TomTomPosition parsePosition(String line) {
        Matcher lineMatcher = POSITION_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String longitude = lineMatcher.group(1);
        String latitude = lineMatcher.group(2);
        String comment = lineMatcher.group(3);
        return new TomTomPosition(Conversion.parseInt(longitude), Conversion.parseInt(latitude), Conversion.trim(comment));
    }

    String parseName(String line) {
        Matcher lineMatcher = NAME_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String description = lineMatcher.group(1);
        return Conversion.trim(description);
    }

    String formatFirstOrLastName(TomTomPosition position, String firstOrLast) {
        return (position.getTime() != null ? RouteComments.TRIPMASTER_TIME.format(position.getTime().getTime()) + " - " : "") +
                firstOrLast + " : " +
                (position.getTime() != null ? RouteComments.TRIPMASTER_DATE.format(position.getTime().getTime()) + " : " : "") +
                position.getComment() +
                (position.getElevation() != null ? " - " + position.getElevation() + " m - 0 km - " +
                (position.getSpeed() != null ? position.getSpeed() : "0") + " Km/h - 6" : "");
    }

    String formatIntermediateName(TomTomPosition position) {
        return (position.getTime() != null ? RouteComments.TRIPMASTER_TIME.format(position.getTime().getTime()) + " - " : "") +
                position.getComment() +
                (position.getElevation() != null ? " - " + position.getElevation() + " m - 0 km - " +
                (position.getSpeed() != null ? position.getSpeed() : "0") + " Km/h - 6" : "");
    }

    public void write(TomTomRoute route, PrintWriter writer, int startIndex, int endIndex) {
        List<TomTomPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            TomTomPosition position = positions.get(i);
            String longitude = Conversion.formatIntAsString(position.getLongitudeAsInt());
            String latitude = Conversion.formatIntAsString(position.getLatitudeAsInt());
            boolean first = i == startIndex;
            boolean last = i == endIndex - 1;

            int type = TomTomRouteFormat.WAYPOINT;
            if (first)
                type = TomTomRouteFormat.START_TYPE;
            else if (last)
                type = TomTomRouteFormat.END_TYPE;

            String comment = position.getComment();
            if (route.getCharacteristics().equals(RouteCharacteristics.Track)) {
                if (first)
                    comment = formatFirstOrLastName(position, "Start");
                else if (last)
                    comment = formatFirstOrLastName(position, "Finish");
                else
                    comment = formatIntermediateName(position);
            }
            if (comment != null)
                comment = comment.replaceAll(SEPARATOR, ";").replaceAll("€", "\u0080");
            writer.println(longitude + SEPARATOR_CHAR + latitude + SEPARATOR_CHAR + comment + SEPARATOR_CHAR + type + SEPARATOR_CHAR);
        }
    }
}
