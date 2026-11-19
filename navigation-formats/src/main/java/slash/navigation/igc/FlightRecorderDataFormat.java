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
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static java.lang.Math.copySign;
import static java.lang.Math.floor;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.time.Instant.ofEpochMilli;
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

    private static final Pattern MANUFACTURER_PATTERN = Pattern.compile("^A[A-Z0-9]{3}.*");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^H[FOPS]\\s*(\\S{1,3}).*");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\s*(\\d{2})\\s*(\\d{2})\\s*(\\d{2}).*", Pattern.DOTALL);
    private static final Pattern FIX_PATTERN = Pattern.compile(
            "^B(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{3})([NS])(\\d{3})(\\d{2})(\\d{3})([WE]).(-\\d{4}|\\+\\d{4}|\\d{5})(-\\d{4}|\\+\\d{4}|\\d{5}).*");
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
        try {
            return parse(content.split("\r\n|\n|\r"));
        } catch (IgcException e) {
            log.fine("Cannot read " + getName() + ": " + e.getMessage());
            return null;
        }
    }

    private static double toDegrees(String hemisphere, int degrees, int minutes, int fraction, char positiveHemisphere) {
        return (hemisphere.charAt(0) == positiveHemisphere ? 1 : -1) *
                (degrees + (minutes * 1000 + fraction) / 1000.0 / 60);
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

    private List<GpxRoute> parse(String[] lines) throws IgcException {
        String manufacturer = null;
        TaskReader taskReader = new TaskReader();
        StringBuilder trackDescription = new StringBuilder(HEADER_MAGIC + HEADER_DELIMITER);
        String fixedTrackDescription = null;
        LocalDate date = null;
        LocalTime previousTime = null;
        List<GpxPosition> pressurePositions = new ArrayList<>(), gnssPositions = new ArrayList<>();
        boolean pressureValid = false, gnssValid = false;

        for (String line : lines) {
            if (line.isEmpty())
                continue;

            if (manufacturer == null) {
                if (!MANUFACTURER_PATTERN.matcher(line).matches())
                    return null;
                manufacturer = line.substring(1, 4);
                continue;
            }

            if (line.length() < 3 || line.charAt(0) < 'A' || line.charAt(0) > 'Z')
                throw new IgcException("bad input record: " + line);

            switch (line.charAt(0)) {
                case 'H' -> {
                    Matcher matcher = HEADER_PATTERN.matcher(line);
                    if (!matcher.matches())
                        throw new IgcException("header (H) record parse error: " + line);
                    if ("DTE".equals(matcher.group(1))) {
                        int colon = line.indexOf(':');
                        String data = colon != -1 ? line.substring(colon + 1) : line.length() > 5 ? line.substring(5) : "";
                        Matcher dateMatcher = DATE_PATTERN.matcher(data);
                        if (!dateMatcher.matches())
                            throw new IgcException("date (H) record parse error: " + line);
                        try {
                            date = LocalDate.of(toYear(Integer.parseInt(dateMatcher.group(3))),
                                    Integer.parseInt(dateMatcher.group(2)), Integer.parseInt(dateMatcher.group(1)));
                        } catch (DateTimeException e) {
                            throw new IgcException("bad date: " + line);
                        }
                    } else if (fixedTrackDescription == null && trackDescription.length() < MAXIMUM_DESCRIPTION_LENGTH) {
                        // other header data is stored in the track descriptions, as long as there is room
                        trackDescription.append(limit(line, MAXIMUM_DESCRIPTION_LENGTH - trackDescription.length()));
                        trackDescription.append(limit(HEADER_DELIMITER, MAXIMUM_DESCRIPTION_LENGTH - trackDescription.length()));
                    }
                }
                case 'B' -> {
                    if (date == null)
                        throw new IgcException("bad date");
                    if (fixedTrackDescription == null)
                        fixedTrackDescription = trackDescription.toString();
                    Matcher matcher = FIX_PATTERN.matcher(line);
                    if (!matcher.matches())
                        throw new IgcException("fix (B) record parse error: " + line);
                    double latitude = toDegrees(matcher.group(7), Integer.parseInt(matcher.group(4)),
                            Integer.parseInt(matcher.group(5)), Integer.parseInt(matcher.group(6)), 'N');
                    double longitude = toDegrees(matcher.group(11), Integer.parseInt(matcher.group(8)),
                            Integer.parseInt(matcher.group(9)), Integer.parseInt(matcher.group(10)), 'E');
                    LocalTime time;
                    try {
                        time = LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                                Integer.parseInt(matcher.group(3)));
                    } catch (DateTimeException e) {
                        throw new IgcException("bad time: " + line);
                    }
                    // increment date if we pass midnight UTC
                    if (previousTime != null && time.isBefore(previousTime))
                        date = date.plusDays(1);
                    previousTime = time;

                    CompactCalendar fixTime = toTime(date, time);
                    int pressureAltitude = Integer.parseInt(matcher.group(12).replace("+", ""));
                    int gnssAltitude = Integer.parseInt(matcher.group(13).replace("+", ""));
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
                // A (duplicate manufacturer/ID, only warned about), D, E, F, G, I, J, K are discarded
                case 'A', 'D', 'E', 'F', 'G', 'I', 'J', 'K' -> {
                }
                default -> throw new IgcException("failure reading file: " + line);
            }
        }

        if (pressurePositions.isEmpty())
            return null;

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

        void readRecord(String record) throws IgcException {
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
            if (!matcher.matches())
                throw new IgcException("task waypoint (C) record parse error: " + record);
            double latitude = toDegrees(matcher.group(4), Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)), 'N');
            double longitude = toDegrees(matcher.group(8), Integer.parseInt(matcher.group(5)),
                    Integer.parseInt(matcher.group(6)), Integer.parseInt(matcher.group(7)), 'E');
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
                    state = TaskState.turnpoint;
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
        private final CompactCalendar time;
        private final Double elevation;

        private Fix(double latitude, double longitude, CompactCalendar time, Double elevation) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.time = time;
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

    private static String formatTime(CompactCalendar time, String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(ofEpochMilli(time.getTimeInMillis()).atZone(UTC));
    }

    private static String asHeaderDescription(List<String> description) {
        if (description == null)
            return null;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < description.size(); i++) {
            buffer.append(description.get(i));
            if (i != description.size() - 1)
                buffer.append(",\n");
        }
        return buffer.toString();
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
            BigDecimal elevation = formatElevation(position.getElevation());
            fixes.add(new Fix(latitude.doubleValue(), longitude.doubleValue(), position.getTime(),
                    elevation != null ? Double.valueOf(elevation.doubleValue()) : null));
        }
        for (Fix fix : fixes) {
            if (fix.time == null)
                throw new IOException("Could not write " + getName() + ": Bad track timestamp");
        }

        String name = asRouteName(route.getName());
        boolean pressureTrack = name != null && name.startsWith(PRESSURE_TRACK_NAME);
        StringBuilder buffer = new StringBuilder();
        buffer.append("AXXXZZZRouteConverter\r\n");
        CompactCalendar date = fixes.isEmpty() ? now() : fixes.get(0).time;
        buffer.append("HFDTE").append(formatTime(date, "ddMMyy")).append("\r\n");

        String description = stripIllegalXmlCharacters(asHeaderDescription(route.getDescription()));
        if (description != null && description.startsWith(HEADER_MAGIC)) {
            // other header data has been stored in the track description
            for (String field : description.substring(HEADER_MAGIC.length()).split(HEADER_DELIMITER)) {
                if (!field.isEmpty())
                    buffer.append(field).append("\r\n");
            }
        } else
            buffer.append("HFPLTPILOT:Unknown\r\n");

        for (Fix fix : fixes) {
            int altitude = fix.elevation != null ? fix.elevation.intValue() : 0;
            buffer.append("B").append(formatTime(fix.time, "HHmmss"))
                    .append(formatLatitudeAndLongitude(fix.latitude, fix.longitude)).append("A")
                    .append(String.format(ROOT, "%05d%05d", pressureTrack ? altitude : 0, pressureTrack ? 0 : altitude))
                    .append("\r\n");
        }
        buffer.append("LXXXGenerated by ").append(getCreator()).append("\r\n");
        buffer.append("GRouteConverterSecurityRecordGuaranteedToFailVALIChecks\r\n");
        target.write(buffer.toString().getBytes(ISO_8859_1));
    }
}
