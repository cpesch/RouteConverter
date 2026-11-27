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

    Copyright (C) 2004 Chris Jones (GPSBabel igc.cc)
    Translated to Java from GPSBabel igc.cc, igc.h (master 9c1e336) for RouteConverter, 2026; modified.
*/

package slash.navigation.igc;

import slash.common.type.CompactCalendar;
import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.base.UnreadableFileException;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static java.lang.Math.copySign;
import static java.lang.Math.floor;
import static java.lang.Math.floorDiv;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneOffset.UTC;
import static java.util.Locale.ROOT;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.common.type.CompactCalendar.now;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.common.NavigationConversion.formatElevation;
import static slash.navigation.common.NavigationConversion.formatPosition;

/**
 * Reads and writes FAI/IGC Flight Recorder Data (.igc) files.
 *
 * Reading salvages record by record (spec 00083 D19, D23): an unreadable line is skipped. A file with a date record
 * or a fix record of which nothing can be read - the date is missing or invalid, no fix is readable - raises an
 * {@link UnreadableFileException} that says why; a file without both, or without fixes but without damage, is
 * declined quietly. Writing is as exact as the format permits (D18, D25): fixes carry
 * whole seconds (rounded), altitudes are clamped to the 5 character field and the header carries the full set of
 * records required by the FAI Technical Specification. Limits of the format: a B record carries only the time of the
 * day, so the date of every later fix is derived from the first fix and tracks longer than 24 hours or unsorted
 * tracks do not round trip; two digit years of the date records are read as 1970-2069.
 *
 * @author Christian Pesch
 */

public class FlightRecorderDataFormat extends GpxBackedNavigationFormat {
    private static final Logger log = Logger.getLogger(FlightRecorderDataFormat.class.getName());

    private static final String HEADER_MAGIC = "IGCHDRS";
    private static final String HEADER_DELIMITER = "~";
    private static final String DATE_MAGIC = "IGCDATE";
    private static final String PRESSURE_TRACK_NAME = "PRESALTTRK";
    private static final String GNSS_TRACK_NAME = "GNSSALTTRK";
    private static final int MAXIMUM_DESCRIPTION_LENGTH = 1024;
    private static final int MAXIMUM_RECORD_TEXT_LENGTH = 78;
    private static final int MAXIMUM_MESSAGE_LENGTH = 120;
    private static final String UTF8_BYTE_ORDER_MARK = "\u00ef\u00bb\u00bf";
    private static final char CTRL_Z = '\u001a';
    // a step back in time of more than 12 hours is the passing of midnight, everything smaller is jitter
    private static final int MIDNIGHT_ROLLOVER_SECONDS = 12 * 60 * 60;
    private static final int MINIMUM_ALTITUDE = -9999, MAXIMUM_ALTITUDE = 99999;

    private static final Pattern HEADER_PATTERN = Pattern.compile("^H[FOPS]\\s*(\\S{1,3}).*");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\s*(\\d{2})\\s*(\\d{2})\\s*(\\d{2}).*", Pattern.DOTALL);
    private static final Pattern FIX_PATTERN = Pattern.compile(
            "^B(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{3})([NS])(\\d{3})(\\d{2})(\\d{3})([WE])(.)(-\\d{4}|\\+\\d{4}|\\d{5})(-\\d{4}|\\+\\d{4}|\\d{5}).*");
    private static final Pattern TASK_ID_PATTERN = Pattern.compile(
            "^C(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{6})(.{4})(\\d{2})(.*)");
    private static final Pattern TASK_POINT_PATTERN = Pattern.compile(
            "^C(\\d{2})(\\d{2})(\\d{3})([NS])(\\d{3})(\\d{2})(\\d{3})([WE])(.*)");
    private static final Pattern LOG_BOOK_PATTERN = Pattern.compile("^L\\s*(\\S{1,3}).*");

    private final Gpx10Format gpxFormat = new Gpx10Format();

    public String getName() {
        return "FAI/IGC Flight Recorder Data (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".igc";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    // read

    private static class IgcException extends Exception {
        IgcException(String message) {
            super(message);
        }
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        String content = new String(source.readAllBytes(), ISO_8859_1);
        // a UTF-8 byte order mark written by editors shows up as three Latin-1 characters
        if (content.startsWith(UTF8_BYTE_ORDER_MARK))
            content = content.substring(UTF8_BYTE_ORDER_MARK.length());
        return parse(content.split("\r\n|\n|\r"));
    }

    private static double toDegrees(String hemisphere, int degrees, int minutes, int fraction, char positiveHemisphere) {
        return (hemisphere.charAt(0) == positiveHemisphere ? 1 : -1) *
                (degrees + (minutes * 1000 + fraction) / 1000.0 / 60);
    }

    // degrees, minutes below 60 and not more than the maximum
    private static boolean isValidCoordinate(int degrees, int minutes, int fraction, int maximumDegrees) {
        return minutes < 60 && degrees * 60000L + minutes * 1000L + fraction <= maximumDegrees * 60000L;
    }

    private static boolean isZeroCoordinate(double latitude, double longitude) {
        return abs(latitude) < 0.0001 && abs(longitude) < 0.0001;
    }

    private static int toYear(int year) {
        return year < 70 ? year + 2000 : year + 1900;
    }

    private static CompactCalendar toTime(LocalDate date, LocalTime time) {
        return fromMillis(date.atTime(time).toInstant(UTC).toEpochMilli());
    }

    private static String limit(String string, int length) {
        return string.length() > length ? string.substring(0, length) : string;
    }

    // leading blanks and DOS end-of-file markers (Ctrl-Z) are no part of a record
    private static String normalize(String line) {
        int start = 0, end = line.length();
        while (start < end && (line.charAt(start) == ' ' || line.charAt(start) == '\t' || line.charAt(start) == CTRL_Z))
            start++;
        while (end > start && line.charAt(end - 1) == CTRL_Z)
            end--;
        return line.substring(start, end);
    }

    private static boolean isDateRecord(String line) {
        Matcher matcher = HEADER_PATTERN.matcher(line);
        return matcher.matches() && "DTE".equals(matcher.group(1));
    }

    private static LocalDate parseDate(String line) throws IgcException {
        int colon = line.indexOf(':');
        String data = colon != -1 ? line.substring(colon + 1) : line.length() > 5 ? line.substring(5) : "";
        Matcher matcher = DATE_PATTERN.matcher(data);
        if (!matcher.matches())
            throw new IgcException("date (H) record parse error: " + line);
        try {
            return LocalDate.of(toYear(Integer.parseInt(matcher.group(3))),
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(1)));
        } catch (DateTimeException e) {
            throw new IgcException("bad date: " + line);
        }
    }

    /**
     * IGC has no magic number: a date record or a fix that matches the B record layout tells that a file is meant
     * to be an IGC file, so that a file without a usable date is an error with a reason instead of a decline (D19).
     */
    private static String findDateProblem(String[] lines) {
        String fix = null;
        for (String raw : lines) {
            String line = normalize(raw);
            if (line.length() >= 3 && isDateRecord(line)) {
                try {
                    parseDate(line);
                } catch (IgcException e) {
                    return "its date (HFDTE) record is invalid: " + limit(e.getMessage(), MAXIMUM_MESSAGE_LENGTH);
                }
            }
            if (fix == null && line.startsWith("B") && FIX_PATTERN.matcher(line).matches())
                fix = line;
        }
        return fix != null ? "it has fix (B) records but no date (HFDTE) record" : null;
    }

    // fixes in front of the first valid date record belong to that date
    private static LocalDate findFirstDate(String[] lines) {
        for (String raw : lines) {
            String line = normalize(raw);
            if (line.length() >= 3 && isDateRecord(line)) {
                try {
                    return parseDate(line);
                } catch (IgcException e) {
                    // look at the next date record
                }
            }
        }
        return null;
    }

    private List<GpxRoute> parse(String[] lines) throws UnreadableFileException {
        TaskReader taskReader = new TaskReader();
        StringBuilder trackDescription = new StringBuilder(HEADER_MAGIC + HEADER_DELIMITER);
        String fixedTrackDescription = null;
        LocalDate date = findFirstDate(lines);
        if (date == null) {
            String problem = findDateProblem(lines);
            if (problem != null)
                throw new UnreadableFileException("IGC file cannot be read: " + problem);
            return null;
        }
        LocalTime previousTime = null;
        List<GpxPosition> pressurePositions = new ArrayList<>(), gnssPositions = new ArrayList<>();
        boolean pressureValid = false, gnssValid = false;
        int skipped = 0;
        String firstSkipped = null;

        for (String raw : lines) {
            String line = normalize(raw);
            if (line.isEmpty())
                continue;

            try {
                // anything that is not a record - too short, no upper case letter - is skipped
                if (line.length() < 3 || line.charAt(0) < 'A' || line.charAt(0) > 'Z')
                    throw new IgcException("bad input record: " + line);

                switch (line.charAt(0)) {
                    case 'H' -> {
                        Matcher matcher = HEADER_PATTERN.matcher(line);
                        if (!matcher.matches())
                            throw new IgcException("header (H) record parse error: " + line);
                        if ("DTE".equals(matcher.group(1))) {
                            date = parseDate(line);
                        } else if (fixedTrackDescription == null && trackDescription.length() < MAXIMUM_DESCRIPTION_LENGTH) {
                            // other header data is stored in the track descriptions, as long as there is room
                            trackDescription.append(limit(line, MAXIMUM_DESCRIPTION_LENGTH - trackDescription.length()));
                            trackDescription.append(limit(HEADER_DELIMITER, MAXIMUM_DESCRIPTION_LENGTH - trackDescription.length()));
                        }
                    }
                    case 'B' -> {
                        if (fixedTrackDescription == null)
                            fixedTrackDescription = trackDescription.toString();
                        Matcher matcher = FIX_PATTERN.matcher(line);
                        if (!matcher.matches())
                            throw new IgcException("fix (B) record parse error: " + line);
                        int latitudeDegrees = Integer.parseInt(matcher.group(4)), latitudeMinutes = Integer.parseInt(matcher.group(5)),
                                latitudeFraction = Integer.parseInt(matcher.group(6));
                        int longitudeDegrees = Integer.parseInt(matcher.group(8)), longitudeMinutes = Integer.parseInt(matcher.group(9)),
                                longitudeFraction = Integer.parseInt(matcher.group(10));
                        if (!isValidCoordinate(latitudeDegrees, latitudeMinutes, latitudeFraction, 90) ||
                                !isValidCoordinate(longitudeDegrees, longitudeMinutes, longitudeFraction, 180))
                            throw new IgcException("coordinate out of range: " + line);
                        double latitude = toDegrees(matcher.group(7), latitudeDegrees, latitudeMinutes, latitudeFraction, 'N');
                        double longitude = toDegrees(matcher.group(11), longitudeDegrees, longitudeMinutes, longitudeFraction, 'E');
                        LocalTime time;
                        try {
                            time = LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                                    Integer.parseInt(matcher.group(3)));
                        } catch (DateTimeException e) {
                            throw new IgcException("bad time: " + line);
                        }
                        // increment date if we pass midnight UTC; smaller steps back in time are jitter of the logger
                        if (previousTime != null && previousTime.toSecondOfDay() - time.toSecondOfDay() > MIDNIGHT_ROLLOVER_SECONDS)
                            date = date.plusDays(1);
                        previousTime = time;

                        // a fix without GPS reception at 0/0 is no position
                        if (matcher.group(12).equals("V") && latitude == 0.0 && longitude == 0.0)
                            continue;

                        CompactCalendar fixTime = toTime(date, time);
                        int pressureAltitude = Integer.parseInt(matcher.group(13).replace("+", ""));
                        int gnssAltitude = Integer.parseInt(matcher.group(14).replace("+", ""));
                        if (pressureAltitude != 0)
                            pressureValid = true;
                        if (gnssAltitude != 0)
                            gnssValid = true;
                        pressurePositions.add(new GpxPosition(longitude, latitude,
                                pressureAltitude != 0 ? Double.valueOf(pressureAltitude) : null, null, fixTime, null));
                        gnssPositions.add(new GpxPosition(longitude, latitude,
                                gnssAltitude != 0 ? Double.valueOf(gnssAltitude) : null, null, fixTime, null));
                    }
                    case 'C' -> taskReader.readRecord(line);
                    case 'L' -> {
                        Matcher matcher = LOG_BOOK_PATTERN.matcher(line);
                        if (!matcher.matches())
                            throw new IgcException("log book (L) record parse error: " + line);
                        // post-flight declaration
                        if ("PFC".equals(matcher.group(1)))
                            taskReader.readRecord(line.length() > 4 ? line.substring(4) : "");
                    }
                    // A (manufacturer/ID, not needed), D, E, F, G, I, J, K are discarded
                    case 'A', 'D', 'E', 'F', 'G', 'I', 'J', 'K' -> {
                    }
                    default -> throw new IgcException("unknown record: " + line);
                }
            } catch (IgcException e) {
                skipped++;
                if (firstSkipped == null)
                    firstSkipped = e.getMessage();
            }
        }
        if (skipped > 0)
            log.fine("Skipped " + skipped + " unreadable lines reading " + getName() + ", first: " + firstSkipped);

        if (pressurePositions.isEmpty()) {
            // nothing to salvage from a damaged file; a file without fixes is no error
            if (skipped > 0 && taskReader.getRoutes().isEmpty())
                throw new UnreadableFileException("IGC file cannot be read: it has no readable fix (B) record, " +
                        skipped + " unreadable lines, the first: " + limit(firstSkipped, MAXIMUM_MESSAGE_LENGTH));
            return null;
        }

        List<GpxRoute> result = new ArrayList<>(taskReader.getRoutes());
        // include pressure altitude track only if it has useful altitude data, GNSS altitude track only if it has
        // useful altitude data or if it is the only track available
        List<String> description = asDescription(stripIllegalXmlCharacters(fixedTrackDescription));
        if (pressureValid)
            result.add(new GpxRoute(gpxFormat, Track, PRESSURE_TRACK_NAME, description, pressurePositions));
        if (gnssValid || !pressureValid)
            result.add(new GpxRoute(gpxFormat, Track, GNSS_TRACK_NAME, description, gnssPositions));
        return result;
    }

    private enum TaskState {id, takeoff, start, turnpoint, finish, landing}

    private class TaskReader {
        private final List<GpxRoute> routes = new ArrayList<>();
        private TaskState state = TaskState.id;
        private int turnPointCount, turnPointIndex;
        private List<GpxPosition> positions;
        private CompactCalendar creation;

        List<GpxRoute> getRoutes() {
            return routes;
        }

        // a damaged record drops the task it belongs to, the next task declaration starts afresh
        void readRecord(String record) throws IgcException {
            try {
                readTaskRecord(record);
            } catch (IgcException e) {
                if (state != TaskState.id && !routes.isEmpty())
                    routes.remove(routes.size() - 1);
                state = TaskState.id;
                throw e;
            }
        }

        private void readTaskRecord(String record) throws IgcException {
            if (state == TaskState.id) {
                Matcher matcher = TASK_ID_PATTERN.matcher(record);
                if (!matcher.matches())
                    throw new IgcException("task id (C) record parse error: " + record);
                try {
                    creation = toTime(LocalDate.of(toYear(Integer.parseInt(matcher.group(3))),
                                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(1))),
                            LocalTime.of(Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)),
                                    Integer.parseInt(matcher.group(6))));
                } catch (DateTimeException e) {
                    throw new IgcException("bad date time: " + record);
                }
                turnPointCount = Integer.parseInt(matcher.group(9));
                String description = DATE_MAGIC + matcher.group(7) + ": " + limit(matcher.group(10), MAXIMUM_RECORD_TEXT_LENGTH);
                positions = new ArrayList<>();
                routes.add(new GpxRoute(gpxFormat, Route, stripIllegalXmlCharacters(matcher.group(8)),
                        asDescription(stripIllegalXmlCharacters(description)), positions));
                state = TaskState.takeoff;
                return;
            }

            Matcher matcher = TASK_POINT_PATTERN.matcher(record);
            if (!matcher.matches()) {
                // the declaration of the next task while the previous one is incomplete keeps the points read so far
                if (TASK_ID_PATTERN.matcher(record).matches()) {
                    state = TaskState.id;
                    readTaskRecord(record);
                    return;
                }
                throw new IgcException("task waypoint (C) record parse error: " + record);
            }
            int latitudeDegrees = Integer.parseInt(matcher.group(1)), latitudeMinutes = Integer.parseInt(matcher.group(2)),
                    latitudeFraction = Integer.parseInt(matcher.group(3));
            int longitudeDegrees = Integer.parseInt(matcher.group(5)), longitudeMinutes = Integer.parseInt(matcher.group(6)),
                    longitudeFraction = Integer.parseInt(matcher.group(7));
            if (!isValidCoordinate(latitudeDegrees, latitudeMinutes, latitudeFraction, 90) ||
                    !isValidCoordinate(longitudeDegrees, longitudeMinutes, longitudeFraction, 180))
                throw new IgcException("task waypoint (C) coordinate out of range: " + record);
            double latitude = toDegrees(matcher.group(4), latitudeDegrees, latitudeMinutes, latitudeFraction, 'N');
            double longitude = toDegrees(matcher.group(8), longitudeDegrees, longitudeMinutes, longitudeFraction, 'E');
            String text = limit(matcher.group(9), MAXIMUM_RECORD_TEXT_LENGTH);

            // name the waypoint according to the order of the task record
            String shortName;
            switch (state) {
                case takeoff -> {
                    shortName = "TAKEOFF";
                    state = TaskState.start;
                }
                case start -> {
                    shortName = "START";
                    turnPointIndex = 0;
                    // a task without turn points continues with the finish
                    state = turnPointCount > 0 ? TaskState.turnpoint : TaskState.finish;
                }
                case turnpoint -> {
                    if (++turnPointIndex == turnPointCount)
                        state = TaskState.finish;
                    shortName = String.format(ROOT, "TURN%02d", turnPointIndex);
                }
                case finish -> {
                    shortName = "FINISH";
                    state = TaskState.landing;
                }
                default -> {
                    shortName = "LANDING";
                    state = TaskState.id;
                }
            }

            // zero latitude and longitude indicates an unknown waypoint
            if (isZeroCoordinate(latitude, longitude))
                return;
            positions.add(new GpxPosition(longitude, latitude, null, null, creation,
                    stripIllegalXmlCharacters(asDescription(shortName, text.isEmpty() ? null : text))));
        }
    }

    // write

    private static class Fix {
        private final double latitude, longitude;
        private final long seconds;
        private final Double elevation;

        private Fix(double latitude, double longitude, long seconds, Double elevation) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.seconds = seconds;
            this.elevation = elevation;
        }
    }

    private static long roundHalfAwayFromZero(double value) {
        return (long) copySign(floor(abs(value) + 0.5), value);
    }

    private static String formatLatitudeAndLongitude(double latitude, double longitude) {
        long latitudeMilliMinutes = roundHalfAwayFromZero(latitude * 60000.0);
        long longitudeMilliMinutes = roundHalfAwayFromZero(longitude * 60000.0);
        return String.format(ROOT, "%02d%05d%c%03d%05d%c",
                abs(latitudeMilliMinutes) / 60000L, abs(latitudeMilliMinutes) % 60000L, latitudeMilliMinutes < 0 ? 'S' : 'N',
                abs(longitudeMilliMinutes) / 60000L, abs(longitudeMilliMinutes) % 60000L, longitudeMilliMinutes < 0 ? 'W' : 'E');
    }

    // a B record has 5 characters for an altitude: -9999 to 99999, unknown is 0
    private static int formatAltitude(Double elevation) {
        if (elevation == null || elevation.isNaN())
            return 0;
        return (int) max(MINIMUM_ALTITUDE, min(MAXIMUM_ALTITUDE, roundHalfAwayFromZero(elevation)));
    }

    // B records have whole seconds: the sub second part is rounded
    private static long toSeconds(CompactCalendar time) {
        return floorDiv(time.getTimeInMillis() + 500L, 1000L);
    }

    private static String formatTime(long seconds, String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(ofEpochSecond(seconds).atZone(UTC));
    }

    private static String asHeaderDescription(List<String> description) {
        if (description == null)
            return null;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < description.size(); i++) {
            buffer.append(description.get(i));
            if (i != description.size() - 1)
                buffer.append(", ");
        }
        return buffer.toString();
    }

    // H records are one line of printable characters
    private static String sanitizeHeaderField(String field) {
        StringBuilder buffer = new StringBuilder(field.length());
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c >= ' ' && c != 0x7f)
                buffer.append(c);
        }
        return buffer.toString().trim();
    }

    // the records required by the FAI Technical Specification (A3.2.4) besides HFDTE, in order of the specification
    private Map<String, String> requiredHeaderRecords() {
        String version = getCreator().substring("RouteConverter ".length());
        Map<String, String> required = new LinkedHashMap<>();
        required.put("FXA", "HFFXA035");
        required.put("PLT", "HFPLTPILOTINCHARGE:Unknown");
        required.put("CM2", "HFCM2CREW2:NIL");
        required.put("GTY", "HFGTYGLIDERTYPE:Unknown");
        required.put("GID", "HFGIDGLIDERID:Unknown");
        required.put("DTM", "HFDTM100GPSDATUM:WGS-1984");
        required.put("RFW", "HFRFWFIRMWAREVERSION:" + version);
        required.put("RHW", "HFRHWHARDWAREVERSION:NIL");
        required.put("FTY", "HFFTYFRTYPE:RouteConverter," + version);
        required.put("GPS", "HFGPSRECEIVER:NIL");
        required.put("PRS", "HFPRSPRESSALTSENSOR:NIL");
        required.put("ALG", "HFALGALTGPS:GEO");
        required.put("ALP", "HFALPALTPRESSURE:ISA");
        return required;
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        List<Fix> fixes = new ArrayList<>();
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            BigDecimal latitude = formatPosition(position.getLatitude());
            BigDecimal longitude = formatPosition(position.getLongitude());
            if (latitude == null || longitude == null)
                continue;
            // igc.cc wr_header/wr_fix_record: "Bad track timestamp". GPSBabel 1.5.4 instead stamps the epoch (HFDTE010170,
            // B000000), which invents 1970 fixes in a flight log - maintainer 2026-11-19: prefer correct behaviour
            if (position.getTime() == null)
                throw new IOException("Could not write " + getName() + ": Bad track timestamp");
            BigDecimal elevation = formatElevation(position.getElevation());
            fixes.add(new Fix(latitude.doubleValue(), longitude.doubleValue(), toSeconds(position.getTime()),
                    elevation != null ? Double.valueOf(elevation.doubleValue()) : null));
        }

        String name = asRouteName(route.getName());
        boolean pressureTrack = name != null && name.startsWith(PRESSURE_TRACK_NAME);
        StringBuilder buffer = new StringBuilder();
        buffer.append("AXXXZZZRouteConverter\r\n");
        long dateSeconds = fixes.isEmpty() ? toSeconds(now()) : fixes.get(0).seconds;
        buffer.append("HFDTEDATE:").append(formatTime(dateSeconds, "ddMMyy")).append(",01\r\n");

        // header records stored in the track description are written in place of the defaults
        Map<String, String> records = requiredHeaderRecords();
        Set<String> replaced = new HashSet<>();
        List<String> others = new ArrayList<>();
        String description = stripIllegalXmlCharacters(asHeaderDescription(route.getDescription()));
        if (description != null && description.startsWith(HEADER_MAGIC)) {
            for (String field : description.substring(HEADER_MAGIC.length()).split(HEADER_DELIMITER)) {
                field = sanitizeHeaderField(field);
                if (field.isEmpty())
                    continue;
                Matcher matcher = HEADER_PATTERN.matcher(field);
                String code = matcher.matches() ? matcher.group(1) : null;
                if (code != null && records.containsKey(code) && replaced.add(code))
                    records.put(code, field);
                else
                    others.add(field);
            }
        }
        for (String record : records.values())
            buffer.append(record).append("\r\n");
        for (String other : others)
            buffer.append(other).append("\r\n");

        for (Fix fix : fixes) {
            int altitude = formatAltitude(fix.elevation);
            buffer.append("B").append(formatTime(fix.seconds, "HHmmss"))
                    .append(formatLatitudeAndLongitude(fix.latitude, fix.longitude)).append("A")
                    .append(String.format(ROOT, "%05d%05d", pressureTrack ? altitude : 0, pressureTrack ? 0 : altitude))
                    .append("\r\n");
        }
        buffer.append("LXXXGenerated by ").append(getCreator()).append("\r\n");
        buffer.append("GRouteConverterSecurityRecordGuaranteedToFailVALIChecks\r\n");
        target.write(buffer.toString().getBytes(ISO_8859_1));
    }
}
