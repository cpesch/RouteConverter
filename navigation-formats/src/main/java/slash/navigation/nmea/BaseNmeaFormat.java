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

import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleFormat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Locale.US;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.*;
import static slash.common.type.HexadecimalNumber.decodeBytes;
import static slash.common.type.HexadecimalNumber.encodeByte;
import static slash.navigation.base.RouteCharacteristics.Track;

/**
 * The base of all NMEA-like formats.
 *
 * @author Christian Pesch
 */

public abstract class BaseNmeaFormat extends SimpleFormat<NmeaRoute> {
    private static final Preferences preferences = Preferences.userNodeForPackage(BaseNmeaFormat.class);
    protected final Logger log;

    static final char SEPARATOR = ',';
    static final String BEGIN_OF_LINE = "^\\$G[NP]";
    static final String END_OF_LINE = "\\*[0-9A-Fa-f][0-9A-Fa-f]$";

    private static final Pattern LINE_PATTERN = Pattern.compile("(^@.*|^\\$.*|" + BEGIN_OF_LINE + ".*" + END_OF_LINE + ")");

    private static final String DATE_AND_PRECISE_TIME_FORMAT = "ddMMyy HHmmss.SSS";
    private static final String PRECISE_DATE_AND_TIME_FORMAT = "ddMMyyyy HHmmss";
    private static final String DATE_AND_TIME_FORMAT = "ddMMyy HHmmss";
    private static final String DATE_FORMAT = "ddMMyy";
    private static final String PRECISE_TIME_FORMAT = "HHmmss.SSS";
    private static final String TIME_FORMAT = "HHmmss";
    private static final NumberFormat LONGITUDE_NUMBER_FORMAT = DecimalFormat.getNumberInstance(US);
    private static final NumberFormat LATITUDE_NUMBER_FORMAT = DecimalFormat.getNumberInstance(US);
    static {
        int maximumFractionDigits = preferences.getInt("positionMaximumFractionDigits", 4);
        LONGITUDE_NUMBER_FORMAT.setGroupingUsed(false);
        LONGITUDE_NUMBER_FORMAT.setMinimumFractionDigits(4);
        LONGITUDE_NUMBER_FORMAT.setMaximumFractionDigits(maximumFractionDigits);
        LONGITUDE_NUMBER_FORMAT.setMinimumIntegerDigits(5);
        LONGITUDE_NUMBER_FORMAT.setMaximumIntegerDigits(5);
        LATITUDE_NUMBER_FORMAT.setGroupingUsed(false);
        LATITUDE_NUMBER_FORMAT.setMinimumFractionDigits(4);
        LATITUDE_NUMBER_FORMAT.setMaximumFractionDigits(maximumFractionDigits);
        LATITUDE_NUMBER_FORMAT.setMinimumIntegerDigits(4);
        LATITUDE_NUMBER_FORMAT.setMaximumIntegerDigits(4);
    }

    public BaseNmeaFormat() {
        this.log = Logger.getLogger(getClass().getName());
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    protected int getGarbleCount() {
        return 0;
    }

    protected RouteCharacteristics getCharacteristics() {
        return Track;
    }

    public void read(BufferedReader reader, String encoding, ParserContext<NmeaRoute> context) throws IOException {
        List<NmeaPosition> positions = new ArrayList<>();

        CompactCalendar startDate = context.getStartDate();
        CompactCalendar originalStartDate = startDate;
        int lineCount = 0;
        NmeaPosition previous = null;
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (trim(line) == null)
                continue;

            if (isValidLine(line)) {
                if (isPosition(line)) {
                    NmeaPosition position = parsePosition(line);
                    boolean validStartDate = isValidStartDate(position.getTime());
                    if (validStartDate)
                        startDate = position.getTime();
                    else
                        position.setStartDate(startDate);

                    if (haveDifferentLongitudeAndLatitude(previous, position) || haveDifferentTime(previous, position) && !validStartDate) {
                        positions.add(position);
                        previous = position;
                    } else if (previous != null) {
                        mergePositions(previous, position, originalStartDate);
                    }
                }
            } else {
                log.info(format("Found garbage for format %s: %s", getName(), line));

                // exception for Mobile Navigator 6: accept that the first line may be garbled
                if (lineCount++ > getGarbleCount())
                    throw new IllegalArgumentException(format("Format %s contains more than %d lines of garble; exiting", getName(), getGarbleCount()));
            }
        }

        if (positions.size() > 0)
            context.appendRoute(createRoute(getCharacteristics(), null, positions));
    }

    boolean haveDifferentLongitudeAndLatitude(NmeaPosition predecessor, NmeaPosition successor) {
        return predecessor == null ||
                (predecessor.hasCoordinates() && successor.hasCoordinates() &&
                        !(predecessor.getLongitudeAsValueAndOrientation().equals(successor.getLongitudeAsValueAndOrientation()) &&
                                predecessor.getLatitudeAsValueAndOrientation().equals(successor.getLatitudeAsValueAndOrientation())));
    }

    boolean haveDifferentTime(NmeaPosition predecessor, NmeaPosition successor) {
        if(predecessor == null)
            return true;
        if(!predecessor.hasTime() || !successor.hasTime())
            return false;
        CompactCalendar predecessorTime = predecessor.getTime();
        CompactCalendar successorTime = successor.getTime();
        return predecessorTime.hasDateDefined() && successorTime.hasDateDefined() &&
                !predecessorTime.equals(successorTime);
    }

    private void mergePositions(NmeaPosition position, NmeaPosition toBeMergedInto, CompactCalendar originalStartDate) {
        if (isEmpty(position.getDescription()) && !isEmpty(toBeMergedInto.getDescription()))
            position.setDescription(toBeMergedInto.getDescription());
        if (isEmpty(position.getElevation()) && !isEmpty(toBeMergedInto.getElevation()))
            position.setElevation(toBeMergedInto.getElevation());
        if (isEmpty(position.getSpeed()) && !isEmpty(toBeMergedInto.getSpeed()))
            position.setSpeed(toBeMergedInto.getSpeed());
        if (isEmpty(position.getHeading()) && !isEmpty(toBeMergedInto.getHeading()))
            position.setHeading(toBeMergedInto.getHeading());
        if (isEmpty(position.getLatitude()) && !isEmpty(toBeMergedInto.getLatitude()))
            position.setLatitudeAsValueAndOrientation(toBeMergedInto.getLatitudeAsValueAndOrientation());
        if (isEmpty(position.getLongitude()) && !isEmpty(toBeMergedInto.getLongitude()))
            position.setLongitudeAsValueAndOrientation(toBeMergedInto.getLongitudeAsValueAndOrientation());
        if (toBeMergedInto.hasTime() &&
                (!position.hasTime() || isStartDateEqual(position.getTime(), originalStartDate) ||
                        position.getTime().getCalendar().before(toBeMergedInto.getTime().getCalendar())))
            position.setTime(toBeMergedInto.getTime());
        if (isEmpty(position.getHdop()) && !isEmpty(toBeMergedInto.getHdop()))
            position.setHdop(toBeMergedInto.getHdop());
        if (isEmpty(position.getPdop()) && !isEmpty(toBeMergedInto.getPdop()))
            position.setPdop(toBeMergedInto.getPdop());
        if (isEmpty(position.getVdop()) && !isEmpty(toBeMergedInto.getVdop()))
            position.setVdop(toBeMergedInto.getVdop());
        if (isEmpty(position.getSatellites()) && !isEmpty(toBeMergedInto.getSatellites()))
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
        byte[] actual = decodeBytes(actualStr);
        if (actual.length != 1 || actual[0] != expected) {
            String expectedStr = encodeByte(expected);
            log.severe("Checksum of '" + line + "' is invalid. Expected '" + expectedStr + "' but found '" + actualStr + "'");
            return preferences.getBoolean("ignoreInvalidChecksum", false);
        }
        return true;
    }

    protected boolean hasValidFix(String line, String field, String valueThatIndicatesNoFix) {
        if (field != null && field.equals(valueThatIndicatesNoFix)) {
            log.severe("Fix for '" + line + "' is invalid. Contains '" + valueThatIndicatesNoFix + "'");
            return preferences.getBoolean("ignoreInvalidFix", false);
        }
        return true;
    }

    protected abstract boolean isPosition(String line);

    protected abstract NmeaPosition parsePosition(String line);

    protected CompactCalendar parseTime(String time) {
        time = trim(time);
        if (time == null)
            return null;
        // 130441.89
        try {
            Date parsed = createDateFormat(PRECISE_TIME_FORMAT).parse(time);
            return fromDate(parsed);
        } catch (ParseException e) {
            // intentionally left empty
        }
        // 130441
        return parseDate(time, TIME_FORMAT);
    }

    protected CompactCalendar parseDateAndTime(String date, String time) {
        time = trim(time);
        date = trim(date);
        if (date == null)
            return parseTime(time);
        // workaround for broken CoPilot on Samsung Galaxy S5
        if(date.length() == 5)
            date = "0" + date;
        String dateAndTime = date + " " + time;
        // date: 160607 time: 130441.89
        try {
            Date parsed = createDateFormat(DATE_AND_PRECISE_TIME_FORMAT).parse(dateAndTime);
            return fromDate(parsed);
        } catch (ParseException e) {
            // intentionally left empty
        }
        // date: 160607 time: 130441
        try {
            Date parsed = createDateFormat(DATE_AND_TIME_FORMAT).parse(dateAndTime);
            return fromDate(parsed);
        } catch (ParseException e) {
            // intentionally left empty
        }
        // date: 16062007 time: 130441
        return parseDate(dateAndTime, PRECISE_DATE_AND_TIME_FORMAT);
    }


    protected String formatTime(CompactCalendar time) {
        if (time == null)
            return "";
        return createDateFormat(PRECISE_TIME_FORMAT).format(time.getTime());
    }

    protected String formatDate(CompactCalendar date) {
        if (date == null)
            return "";
        return createDateFormat(DATE_FORMAT).format(date.getTime());
    }

    protected String formatLongitude(Double longitude) {
        if (longitude == null)
            return "";
        return LONGITUDE_NUMBER_FORMAT.format(longitude);
    }

    protected String formatLatitude(Double latitude) {
        if (latitude == null)
            return "";
        return LATITUDE_NUMBER_FORMAT.format(latitude);
    }

    protected void writeSentence(PrintWriter writer, String sentence) {
        String ggaChecksum = encodeByte(computeChecksum(sentence));
        writer.println("$" + sentence + "*" + ggaChecksum);
    }

    protected abstract void writePosition(NmeaPosition position, PrintWriter writer);

    protected void writeHeader(PrintWriter writer) {
    }

    public void write(NmeaRoute route, PrintWriter writer, int startIndex, int endIndex) {
        writeHeader(writer);

        List<NmeaPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            NmeaPosition position = positions.get(i);
            writePosition(position, writer);
        }

        writeFooter(writer);
    }

    protected void writeFooter(PrintWriter writer) {
    }
}
