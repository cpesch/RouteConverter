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

package slash.navigation.simple;

import slash.navigation.util.Conversion;
import slash.navigation.*;

import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Kienzle GPS (.txt) files.
 * <p/>
 * Head: Position;X;Y;Empfänger;Land;PLZ;Ort;Strasse;Hausnummer;Planankunft;Zusatzinfos<br/>
 * Format: 118;7.0591660000;50.7527770000;PHE II;;53117;Bonn;Christian-Lassen-Str.;9;17:02;
 *
 * @author Christian Pesch
 */

public class KienzleGpsFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final String HEADER_LINE = "Position;X;Y;Empfänger;Land;PLZ;Ort;Strasse;Hausnummer;Planankunft;Zusatzinfos";
    private static final char SEPARATOR_CHAR = ';';
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm");

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "\\d+" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(.*)" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(.*)" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(\\d*)" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(.*)" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(.*)" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(.*)" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(\\d+:\\d+)" + WHITE_SPACE + SEPARATOR_CHAR +
                    ".*" +
                    END_OF_LINE);


    public String getExtension() {
        return ".txt";
    }

    public String getName() {
        return "Kienzle GPS (*" + getExtension() + ")";
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public <P extends BaseNavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return RouteCharacteristics.Route;
    }

    protected boolean isValidLine(String line) {
        return isPosition(line) || line != null && line.startsWith(HEADER_LINE);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private Calendar parseTime(String string, Calendar startDate) {
        if (string == null)
            return null;
        try {
            Date date = TIME.parse(string);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            if (startDate != null) {
                calendar.set(Calendar.YEAR, startDate.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, startDate.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, startDate.get(Calendar.DAY_OF_MONTH));
            }
            return calendar;
        } catch (ParseException e) {
            return null;
        }
    }

    protected Wgs84Position parsePosition(String line, Calendar startDate) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String longitude = lineMatcher.group(1);
        String latitude = lineMatcher.group(2);
        String organization = Conversion.trim(lineMatcher.group(3));
        String postalCode = Conversion.trim(lineMatcher.group(5));
        String city = Conversion.trim(lineMatcher.group(6));
        String street = Conversion.trim(lineMatcher.group(7));
        String houseNo = Conversion.trim(lineMatcher.group(8));
        String time = lineMatcher.group(9);
        String comment = (organization != null ? organization + ": " : "") +
                (postalCode != null ? postalCode + " " : "") +
                (city != null ? city + ", " : "") +
                (street != null ? street + " " : "") +
                (houseNo != null ? houseNo : "");
        return new Wgs84Position(Conversion.parseDouble(longitude), Conversion.parseDouble(latitude),
                null, null, parseTime(time, startDate), comment);
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        throw new UnsupportedOperationException();
    }
}