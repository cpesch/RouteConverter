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

package slash.navigation.nmea;

import slash.navigation.RouteCharacteristics;
import slash.navigation.SimpleFormat;
import slash.navigation.hex.HexDecoder;
import slash.navigation.hex.HexEncoder;
import slash.navigation.util.Conversion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The base of all NMEA-like formats.
 *
 * @author Christian Pesch
 */

public abstract class BaseNmeaFormat extends SimpleFormat<NmeaRoute> {
    protected static Logger log = Logger.getLogger(BaseNmeaFormat.class.getName());

    protected static final String SEPARATOR = ",";
    protected static final String BEGIN_OF_LINE = "^\\$GP";
    protected static final String END_OF_LINE = "\\*[0-9A-F][0-9A-F]$";

    private static final Pattern LINE_PATTERN = Pattern.compile("(^@.*|^\\$.*|" + BEGIN_OF_LINE + ".*" + END_OF_LINE + ")");

    private static final DateFormat PRECISE_DATE_AND_TIME_FORMAT = new SimpleDateFormat("ddMMyy HHmmss.SSS");
    private static final DateFormat DATE_AND_TIME_FORMAT = new SimpleDateFormat("ddMMyy HHmmss");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("ddMMyy");
    private static final DateFormat PRECISE_TIME_FORMAT = new SimpleDateFormat("HHmmss.SSS");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss");
    private static final NumberFormat LONGITUDE_NUMBER_FORMAT = DecimalFormat.getNumberInstance(Locale.US);
    private static final NumberFormat LATITUDE_NUMBER_FORMAT = DecimalFormat.getNumberInstance(Locale.US);
    static {
        LONGITUDE_NUMBER_FORMAT.setGroupingUsed(false);
        LONGITUDE_NUMBER_FORMAT.setMinimumFractionDigits(4);
        LONGITUDE_NUMBER_FORMAT.setMaximumFractionDigits(4);
        LONGITUDE_NUMBER_FORMAT.setMinimumIntegerDigits(5);
        LONGITUDE_NUMBER_FORMAT.setMaximumIntegerDigits(5);
        LATITUDE_NUMBER_FORMAT.setGroupingUsed(false);
        LATITUDE_NUMBER_FORMAT.setMinimumFractionDigits(4);
        LATITUDE_NUMBER_FORMAT.setMaximumFractionDigits(4);
        LATITUDE_NUMBER_FORMAT.setMinimumIntegerDigits(4);
        LATITUDE_NUMBER_FORMAT.setMaximumIntegerDigits(4);
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    protected int getGarbleCount() {
        return 0;
    }

    public List<NmeaRoute> read(BufferedReader reader, Calendar startDate, String encoding) throws IOException {
        List<NmeaPosition> positions = new ArrayList<NmeaPosition>();

        Calendar originalStartDate = startDate;
        int lineCount = 0;
        NmeaPosition previous = null;
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (Conversion.trim(line) == null)
                continue;

            if (isValidLine(line)) {
                if (isPosition(line)) {
                    NmeaPosition position = parsePosition(line);
                    if (isStartDate(position.getTime()))
                        startDate = position.getTime();
                    else if (startDate != null)
                        position.setStartDate(startDate);

                    if (haveDifferentLongitudeAndLatitude(previous, position)) {
                        positions.add(position);
                        previous = position;
                    } else {
                        mergePositions(previous, position, originalStartDate);
                    }
                }
            } else {
                // exception for Mobile Navigator 6: accept that the first line may be garbled
                if (lineCount++ > getGarbleCount())
                    return null;
            }
        }

        if (positions.size() > 0)
            return Arrays.asList(new NmeaRoute(this, RouteCharacteristics.Track, positions));
        else
            return null;
    }

     boolean haveDifferentLongitudeAndLatitude(NmeaPosition predecessor, NmeaPosition successor) {
        return predecessor == null ||
                (predecessor.hasCoordinates() && successor.hasCoordinates() &&
                        !(predecessor.getLongitude().equals(successor.getLongitude()) &&
                          predecessor.getLatitude().equals(successor.getLatitude())));
    }

    private void mergePositions(NmeaPosition position, NmeaPosition toBeMergedInto, Calendar originalStartDate) {
        if (position.getComment() == null)
            position.setComment(toBeMergedInto.getComment());
        if (position.getElevation() == null)
            position.setElevation(toBeMergedInto.getElevation());
        if (position.getLatitude() == null)
            position.setLatitude(toBeMergedInto.getLatitude());
        if (position.getLongitude() == null)
            position.setLongitude(toBeMergedInto.getLongitude());
        if (position.getTime() == null || isStartDateEqual(position.getTime(), originalStartDate) ||
                position.getTime().before(toBeMergedInto.getTime()))
            position.setTime(toBeMergedInto.getTime());
    }

    private boolean isStartDateEqual(Calendar calendar1, Calendar calendar2) {
        return calendar1 != null && calendar2 != null &&
                calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH) &&
                calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH);
    }

    protected boolean isValidLine(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private byte computeChecksum(String line) {
        byte result = 0;
        for (int i = 0; i < line.length(); i++) {
            result ^= line.charAt(i);
        }
        return result;
    }

    protected boolean hasValidChecksum(String line) {
        String lineForChecksum = line.substring(1, line.length() - 3);
        byte expected = computeChecksum(lineForChecksum);
        String actualStr = line.substring(line.length() - 2);
        byte[] actual = HexDecoder.decodeBytes(actualStr);
        if (actual.length != 1 || actual[0] != expected) {
            String expectedStr = HexEncoder.encodeByte(expected);
            NmeaFormat.log.severe("Checksum of '" + line + "' is invalid. Expected '" + expectedStr + "' but found '" + actualStr + "'");
            return false;
        }
        return true;
    }

    protected abstract boolean isPosition(String line);

    protected abstract NmeaPosition parsePosition(String line);

    protected Calendar parseTime(String time) {
        time = Conversion.trim(time);
        if (time == null)
            return null;
        // 130441.89
        try {
            Date parsed = PRECISE_TIME_FORMAT.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return calendar;
        } catch (ParseException e) {
            // intentionally left empty
        }
        // 130441
        try {
            Date parsed = TIME_FORMAT.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return calendar;
        } catch (ParseException e) {
            log.severe("Could not parse time '" + time + "'");
        }
        return null;
    }

    protected Calendar parseDateAndTime(String date, String time) {
        time = Conversion.trim(time);
        date = Conversion.trim(date);
        if (date == null)
            return parseTime(time);
        String dateAndTime = date + " " + time;
        // date: 160607 time: 130441.89
        try {
            Date parsed = PRECISE_DATE_AND_TIME_FORMAT.parse(dateAndTime);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return calendar;
        } catch (ParseException e) {
            // intentionally left empty
        }
        // date: 160607 time: 130441
        try {
            Date parsed = DATE_AND_TIME_FORMAT.parse(dateAndTime);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return calendar;
        } catch (ParseException e) {
            log.severe("Could not parse date and time '" + dateAndTime + "'");
        }
        return null;
    }


    protected String formatTime(Calendar time) {
        if (time == null)
            return "";
        return PRECISE_TIME_FORMAT.format(time.getTime());
    }

    protected String formatDate(Calendar date) {
        if (date == null)
            return "";
        return DATE_FORMAT.format(date.getTime());
    }

    protected String formatLongitude(Double aDouble) {
        if (aDouble == null)
            return "";
        return LONGITUDE_NUMBER_FORMAT.format(aDouble);
    }

    protected String formatLatititude(Double aDouble) {
        if (aDouble == null)
            return "";
        return LATITUDE_NUMBER_FORMAT.format(aDouble);
    }


    protected String formatComment(String comment) {
        comment = Conversion.trim(comment);
        if(comment == null)
            return "";
        return comment.replaceAll(",", ";");
    }

    protected void writeSentence(PrintWriter writer, String sentence) {
        String ggaChecksum = HexEncoder.encodeByte(computeChecksum(sentence));
        writer.println("$" + sentence + "*" + ggaChecksum);
    }

    protected abstract void writePosition(NmeaPosition position, PrintWriter writer, int index);

    protected void writeHeader(PrintWriter writer) {
    }

    protected void writeFooter(PrintWriter writer) {
    }

    public void write(NmeaRoute route, PrintWriter writer, int startIndex, int endIndex, boolean numberPositionNames) {
        writeHeader(writer);

        List<NmeaPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            NmeaPosition position = positions.get(i);
            writePosition(position, writer, i);
        }

        writeFooter(writer);
    }
}
