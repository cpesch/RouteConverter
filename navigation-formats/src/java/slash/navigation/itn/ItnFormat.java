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
import slash.navigation.RouteComments;
import slash.navigation.TextNavigationFormat;
import slash.navigation.util.Conversion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Tom Tom Rider (.itn) files.
 *
 * @author Christian Pesch
 */

public class ItnFormat extends TextNavigationFormat<ItnRoute> {
    private static final int MAXIMUM_POSITION_COUNT = 48;
    private static final char SEPARATOR_CHAR = '|';
    private static final String SEPARATOR = "\\" + SEPARATOR_CHAR;
    private static final Pattern POSITION_PATTERN = Pattern.
            compile("([+-]?\\d+)" + SEPARATOR + "([+-]?\\d+)" + SEPARATOR +
                    "(.*)" + SEPARATOR + "\\d" + SEPARATOR);
    private static final Pattern NAME_PATTERN = Pattern.
            compile("^\"([^\"]*)\"$");
    static final SimpleDateFormat TRIPMASTER_TIME = new SimpleDateFormat("HH:mm:ss");
    static final SimpleDateFormat TRIPMASTER_DATE = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    static final SimpleDateFormat PILOG_DATE = new SimpleDateFormat("yyMMdd HH:mm:ss");

    private static final String TIME = "\\d{1,2}:\\d{2}:\\d{2}";
    private static final String ELEVATION = "[-\\d\\.]+";
    private static final String REASONS = "Dur. " + TIME + "|Dauer " + TIME + "|" +
            "Abstand \\d+|Dist. \\d+|Distanz \\d+|Course \\d+|Cape \\d+|Kurs \\d+|Richtung \\d+|Waypoint|Wpt|Punkt|Pause";

    static final Pattern TRIPMASTER_1dot4_PATTERN = Pattern.compile("(" + REASONS + ") - (" + TIME + ") - (" + ELEVATION + ") m - (.+)");
    static final Pattern TRIPMASTER_SHORT_STARTEND_PATTERN = Pattern.compile(
            "(Start|Ende|Finish) : ((.+) - )?(.+) - (.+) - (" + ELEVATION + ") m - ([\\d\\.]+) km");
    static final Pattern TRIPMASTER_SHORT_WAYPOINT_PATTERN = Pattern.compile("(" + TIME + ") - (" + ELEVATION + ") m");
    static final Pattern TRIPMASTER_MIDDLE_WAYPOINT_PATTERN = Pattern.compile(
            "(\\d+:\\d+:\\d+) - (" + REASONS + ") : (.+) - (" + ELEVATION + ") m - ([\\d\\.]+) (K|k)m");
    static final Pattern TRIPMASTER_LONG_PATTERN = Pattern.compile(
            "(" + TIME + ") - ((Start : (.*)|Finish : (.*)|" + REASONS + ") : )?(.+) - " +
                    "(" + ELEVATION + ") m - ([\\d\\.]+) (K|k)m - ([\\d\\.]+) (K|k)m/h( - \\d+)?");

    private static final String DATE = "\\d{6}";
    static final Pattern PILOG_PATTERN = Pattern.compile("(" + DATE + " " + TIME + "): [+-] (.+) @(\\d+\\.\\d+)m \\((.+)\\)");
    static final Pattern LOGPOS_PATTERN = Pattern.compile("(" + DATE + " " + TIME + "): [+-] (.+) \\((.+)\\)");

    public static final int START_TYPE = 4;
    public static final int END_TYPE = 3;
    public static final int WAYPOINT = 1;

    public String getName() {
        return "Tom Tom Route (*" + getExtension() + ")";
    }

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

    public <P extends BaseNavigationPosition> ItnRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new ItnRoute(characteristics, name, (List<ItnPosition>) positions);
    }

    public List<ItnRoute> read(BufferedReader reader, Calendar startDate, String encoding) throws IOException {
        List<ItnPosition> positions = new ArrayList<ItnPosition>();

        String name = null;
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (line.length() == 0 || line.startsWith("~"))
                continue;

            if (isPosition(line)) {
                ItnPosition position = parsePosition(line);
                if (isStartDate(position.getTime()))
                    startDate = position.getTime();
                else if (startDate != null)
                    position.setStartDate(startDate);
                positions.add(position);
            } else if (isName(line)) {
                name = parseName(line);
            } else {
                return null;
            }
        }

        if (positions.size() > 0)
            return Arrays.asList(new ItnRoute(this, isTripmasterOrPilogTrack(positions) ? RouteCharacteristics.Track : RouteCharacteristics.Route, name, positions));
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

    private boolean isTripmasterOrPilogTrack(List<ItnPosition> positions) {
        for (ItnPosition position : positions) {
            if (position.getReason() == null && position.getTime() == null)
                return false;
        }
        return true;
    }

    ItnPosition parsePosition(String line) {
        Matcher lineMatcher = POSITION_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String longitude = lineMatcher.group(1);
        String latitude = lineMatcher.group(2);
        String comment = lineMatcher.group(3);
        return new ItnPosition(Conversion.parseInt(longitude), Conversion.parseInt(latitude), Conversion.trim(comment));
    }

    String parseName(String line) {
        Matcher lineMatcher = NAME_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String description = lineMatcher.group(1);
        return Conversion.trim(description);
    }

    String formatFirstOrLastName(ItnPosition position, String firstOrLast) {
        return TRIPMASTER_TIME.format(position.getTime().getTime()) + " - " + firstOrLast + " : " +
                TRIPMASTER_DATE.format(position.getTime().getTime()) + " : " +
                position.getComment() + " - " + position.getElevation() + " m - 0 km - 0 Km/h - 6";
    }

    String formatIntermediateName(ItnPosition position) {
        return TRIPMASTER_TIME.format(position.getTime().getTime()) + " - " + position.getComment() + " - " +
                position.getElevation() + " m - 0 km - 0 Km/h - 6";
    }

    public void write(ItnRoute route, PrintWriter writer, int startIndex, int endIndex, boolean numberPositionNames) {
        List<ItnPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            ItnPosition position = positions.get(i);
            String longitude = Conversion.formatIntAsString(position.getLongitudeAsInt());
            String latitude = Conversion.formatIntAsString(position.getLatitudeAsInt());
            boolean first = i == startIndex;
            boolean last = i == endIndex - 1;

            int type = ItnFormat.WAYPOINT;
            if (first)
                type = ItnFormat.START_TYPE;
            else if (last)
                type = ItnFormat.END_TYPE;

            String comment = position.getComment();
            if(route.getCharacteristics().equals(RouteCharacteristics.Track)) {
                if(first)
                    comment = formatFirstOrLastName(position, "Start");
                else if(last)
                    comment = formatFirstOrLastName(position, "Finish");
                else
                    comment = formatIntermediateName(position);
            }
            if (comment != null)
                comment = numberPositionNames ? RouteComments.numberPosition(comment, i + 1) : comment;
            if (comment != null)
                comment = comment.replaceAll(SEPARATOR, ";");
            writer.println(longitude + SEPARATOR_CHAR + latitude + SEPARATOR_CHAR + comment + SEPARATOR_CHAR + type + SEPARATOR_CHAR);
        }
    }
}
