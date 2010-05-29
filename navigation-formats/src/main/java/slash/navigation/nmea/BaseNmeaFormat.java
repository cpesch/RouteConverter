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

import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleFormat;
import slash.common.hex.HexDecoder;
import slash.common.hex.HexEncoder;
import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The base of all NMEA-like formats.
 *
 * @author Christian Pesch
 */

public abstract class BaseNmeaFormat extends SimpleFormat<NmeaRoute> {
    private static final Preferences preferences = Preferences.userNodeForPackage(BaseNmeaFormat.class);
    protected static Logger log = Logger.getLogger(BaseNmeaFormat.class.getName());

    static final String SEPARATOR = ",";
    static final String BEGIN_OF_LINE = "^\\$GP";
    static final String END_OF_LINE = "\\*[0-9A-Fa-f][0-9A-Fa-f]$";

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

    protected RouteCharacteristics getCharacteristics() {
        return RouteCharacteristics.Track;
    }

    public List<NmeaRoute> read(BufferedReader reader, CompactCalendar startDate, String encoding) throws IOException {
        List<NmeaPosition> positions = new ArrayList<NmeaPosition>();

        CompactCalendar originalStartDate = startDate;
        int lineCount = 0;
        NmeaPosition previous = null;
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (Transfer.trim(line) == null)
                continue;

            if (isValidLine(line)) {
                if (isPosition(line)) {
                    NmeaPosition position = parsePosition(line);
                    if (isValidStartDate(position.getTime()))
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
            return Arrays.asList(new NmeaRoute(this, getCharacteristics(), positions));
        else
            return null;
    }

    boolean haveDifferentLongitudeAndLatitude(NmeaPosition predecessor, NmeaPosition successor) {
        return predecessor == null ||
                (predecessor.hasCoordinates() && successor.hasCoordinates() &&
                        !(predecessor.getLongitude().equals(successor.getLongitude()) &&
                                predecessor.getLatitude().equals(successor.getLatitude())));
    }

    private void mergePositions(NmeaPosition position, NmeaPosition toBeMergedInto, CompactCalendar originalStartDate) {
        if (Transfer.isEmpty(position.getComment()) && !Transfer.isEmpty(toBeMergedInto.getComment()))
            position.setComment(toBeMergedInto.getComment());
        if (Transfer.isEmpty(position.getElevation()) && !Transfer.isEmpty(toBeMergedInto.getElevation()))
            position.setElevation(toBeMergedInto.getElevation());
        if (Transfer.isEmpty(position.getSpeed()) && !Transfer.isEmpty(toBeMergedInto.getSpeed()))
            position.setSpeed(toBeMergedInto.getSpeed());
        if (Transfer.isEmpty(position.getHeading()) && !Transfer.isEmpty(toBeMergedInto.getHeading()))
            position.setHeading(toBeMergedInto.getHeading());
        if (Transfer.isEmpty(position.getLatitude()) && !Transfer.isEmpty(toBeMergedInto.getLatitude()))
            position.setLatitude(toBeMergedInto.getLatitude());
        if (Transfer.isEmpty(position.getLongitude()) && !Transfer.isEmpty(toBeMergedInto.getLongitude()))
            position.setLongitude(toBeMergedInto.getLongitude());
        if ((toBeMergedInto.getTime() != null) &&
                (position.getTime() == null || isStartDateEqual(position.getTime(), originalStartDate) ||
                        position.getTime().getCalendar().before(toBeMergedInto.getTime().getCalendar())))
            position.setTime(toBeMergedInto.getTime());
        if (Transfer.isEmpty(position.getHdop()) && !Transfer.isEmpty(toBeMergedInto.getHdop()))
            position.setHdop(toBeMergedInto.getHdop());
        if (Transfer.isEmpty(position.getPdop()) && !Transfer.isEmpty(toBeMergedInto.getPdop()))
            position.setPdop(toBeMergedInto.getPdop());
        if (Transfer.isEmpty(position.getVdop()) && !Transfer.isEmpty(toBeMergedInto.getVdop()))
            position.setVdop(toBeMergedInto.getVdop());
        if (Transfer.isEmpty(position.getSatellites()) && !Transfer.isEmpty(toBeMergedInto.getSatellites()))
            position.setSatellites(toBeMergedInto.getSatellites());
    }

    private boolean isStartDateEqual(CompactCalendar compactCalendar1, CompactCalendar compactCalendar2) {
        if (compactCalendar1 == null || compactCalendar2 == null)
            return false;
        Calendar calendar1 = compactCalendar1.getCalendar();
        Calendar calendar2 = compactCalendar2.getCalendar();
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
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
            log.severe("Checksum of '" + line + "' is invalid. Expected '" + expectedStr + "' but found '" + actualStr + "'");
            return preferences.getBoolean("ignoreInvalidChecksum", false);
        }
        return true;
    }

    protected abstract boolean isPosition(String line);

    protected abstract NmeaPosition parsePosition(String line);

    protected CompactCalendar parseTime(String time) {
        time = Transfer.trim(time);
        if (time == null)
            return null;
        // 130441.89
        try {
            Date parsed = PRECISE_TIME_FORMAT.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return CompactCalendar.fromCalendar(calendar);
        } catch (ParseException e) {
            // intentionally left empty
        }
        // 130441
        try {
            Date parsed = TIME_FORMAT.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return CompactCalendar.fromCalendar(calendar);
        } catch (ParseException e) {
            log.severe("Could not parse time '" + time + "'");
        }
        return null;
    }

    protected CompactCalendar parseDateAndTime(String date, String time) {
        time = Transfer.trim(time);
        date = Transfer.trim(date);
        if (date == null)
            return parseTime(time);
        String dateAndTime = date + " " + time;
        // date: 160607 time: 130441.89
        try {
            Date parsed = PRECISE_DATE_AND_TIME_FORMAT.parse(dateAndTime);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return CompactCalendar.fromCalendar(calendar);
        } catch (ParseException e) {
            // intentionally left empty
        }
        // date: 160607 time: 130441
        try {
            Date parsed = DATE_AND_TIME_FORMAT.parse(dateAndTime);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return CompactCalendar.fromCalendar(calendar);
        } catch (ParseException e) {
            log.severe("Could not parse date and time '" + dateAndTime + "'");
        }
        return null;
    }


    protected String formatTime(CompactCalendar time) {
        if (time == null)
            return "";
        return PRECISE_TIME_FORMAT.format(time.getTime());
    }

    protected String formatDate(CompactCalendar date) {
        if (date == null)
            return "";
        return DATE_FORMAT.format(date.getTime());
    }

    protected String formatLongitude(Double longitude) {
        if (longitude == null)
            return "";
        return LONGITUDE_NUMBER_FORMAT.format(longitude);
    }

    protected String formatLatititude(Double latitude) {
        if (latitude == null)
            return "";
        return LATITUDE_NUMBER_FORMAT.format(latitude);
    }

    protected String formatComment(String comment) {
        comment = Transfer.trim(comment);
        if (comment == null)
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

    public void write(NmeaRoute route, PrintWriter writer, int startIndex, int endIndex) {
        writeHeader(writer);

        List<NmeaPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            NmeaPosition position = positions.get(i);
            writePosition(position, writer, i);
        }

        writeFooter(writer);
    }

    protected void writeFooter(PrintWriter writer) {
    }
}
