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

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.RouteCharacteristics;
import slash.common.io.CompactCalendar;
import slash.navigation.util.Conversion;
import slash.common.io.Transfer;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes NMEA 0183 Sentences (.nmea) files.
 * <p/>
 * See http://aprs.gids.nl/nmea and http://www.kh-gps.de/nmea-faq.htm
 *
 * @author Christian Pesch
 */

public class NmeaFormat extends BaseNmeaFormat {
    static {
        log = Logger.getLogger(NmeaFormat.class.getName());
    }

    private static final NumberFormat ALTITUDE_AND_SPEED_NUMBER_FORMAT = DecimalFormat.getNumberInstance(Locale.US);
    static {
        ALTITUDE_AND_SPEED_NUMBER_FORMAT.setGroupingUsed(false);
        ALTITUDE_AND_SPEED_NUMBER_FORMAT.setMinimumFractionDigits(1);
        ALTITUDE_AND_SPEED_NUMBER_FORMAT.setMaximumFractionDigits(1);
        ALTITUDE_AND_SPEED_NUMBER_FORMAT.setMinimumIntegerDigits(1);
        ALTITUDE_AND_SPEED_NUMBER_FORMAT.setMaximumIntegerDigits(6);
    }

    private static final DateFormat DAY_FORMAT = new SimpleDateFormat("dd");
    private static final DateFormat MONTH_FORMAT = new SimpleDateFormat("MM");
    private static final DateFormat YEAR_FORMAT = new SimpleDateFormat("yy");

    static {
        DAY_FORMAT.setCalendar(Calendar.getInstance(CompactCalendar.GMT));
        MONTH_FORMAT.setCalendar(Calendar.getInstance(CompactCalendar.GMT));
        YEAR_FORMAT.setCalendar(Calendar.getInstance(CompactCalendar.GMT));
    }

    // $GPGGA,130441.89,5239.3154,N,00907.7011,E,1,08,1.25,16.76,M,46.79,M,,*6D
    // $GPGGA,162611,3554.2367,N,10619.4966,W,1,03,06.7,02300.3,M,-022.4,M,,*7F
    // $GPGGA,132713,5509.7861,N,00140.5854,W,1,07,1.0,98.9,M,,M,,*7d
    private static final Pattern GGA_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "GGA" + SEPARATOR + "([\\d\\.]*)" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "[012]" + SEPARATOR +
                    "([\\d]*)" + SEPARATOR +         // Number of satellites in view, 00 - 12
                    "[\\d\\.]*" + SEPARATOR +
                    "(-?[\\d\\.]*)" + SEPARATOR +    // Antenna Altitude above/below mean-sea-level (geoid)  
                    "M" + SEPARATOR +
                    "[-?\\d\\.]*" + SEPARATOR +
                    "M?" + SEPARATOR +
                    ".*" + SEPARATOR +
                    ".*" +                           // Differential reference station ID, 0000-1023 
                    END_OF_LINE);

    // $GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,160607,,   ,A*76
    // $GPRMC,140403.000,A,4837.5194,N,00903.4022,E,15.00,0.00,260707,,  *3E
    // $GPRMC,172103.38,V,4424.5358,N,06812.3754,W,0.000,0.000,101010,0,W,N*3A
    private static final Pattern RMC_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "RMC" + SEPARATOR +
                    "([\\d\\.]*)" + SEPARATOR +     // UTC Time
                    "[AV]" + SEPARATOR +            // Status, A=active, V=void
                    "([\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([EW])" + SEPARATOR +
                    "([\\d\\.]*)" + SEPARATOR +     // Speed over ground, knots
                    "[\\d\\.]*" + SEPARATOR +
                    "(\\d*)" + SEPARATOR +          // Date, ddmmyy
                    "[\\d\\.]*" + SEPARATOR +
                    "[\\d\\.]*" + SEPARATOR + "?" + // Magnetic variation 
                    "[AEW]?" + SEPARATOR + "?" +    // E=East, W=West
                    "[ADEMNS]?" +                   // Signal integrity
                    END_OF_LINE);

    // $GPWPL,5334.169,N,01001.920,E,STATN1*22
    private static final Pattern WPL_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "WPL" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "(.*)" + 
                    END_OF_LINE);

    // $GPZDA,032910.542,07,08,2004,00,00*48
    private static final Pattern ZDA_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "ZDA" + SEPARATOR +
                    "([\\d\\.]*)" + SEPARATOR + // UTC Time
                    "(\\d*)" + SEPARATOR +      // day
                    "(\\d*)" + SEPARATOR +      // month
                    "(\\d*)" + SEPARATOR +      // year
                    "\\d*" + SEPARATOR +
                    "\\d*" +
                    END_OF_LINE);

    // $GPVTG,0.00,T,,M,1.531,N,2.835,K,A*37
    // $GPVTG,138.7,T,,M,014.2,N,026.3,K,A*00
    private static final Pattern VTG_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "VTG" + SEPARATOR +
                    "([\\d\\.]*)" + SEPARATOR +   // true course
                    "T" + SEPARATOR +
                    "[\\d\\.]*" + SEPARATOR +     // magnetic course
                    "M" + SEPARATOR +     
                    "([\\d\\.]*)" + SEPARATOR +
                    "N" + SEPARATOR +
                    "([\\d\\.]*)" + SEPARATOR +
                    "K" + SEPARATOR +
                    "A" +
                    END_OF_LINE);

    // $GPGSA,A,3,,,,15,17,18,23,,,,,,4.7,4.4,1.5*3F
    private static final Pattern GSA_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "GSA" + SEPARATOR +
                    "A" + SEPARATOR +
                    "[123]" + SEPARATOR +        // Fix
                    "\\d*" + SEPARATOR +
                    "\\d*" + SEPARATOR +
                    "\\d*" + SEPARATOR +
                    "\\d*" + SEPARATOR +
                    "\\d*" + SEPARATOR +
                    "\\d*" + SEPARATOR +
                    "\\d*" + SEPARATOR +
                    "\\d*" + SEPARATOR +
                    "\\d*" + SEPARATOR +
                    "\\d*" + SEPARATOR +
                    "\\d*" + SEPARATOR +
                    "\\d*" + SEPARATOR +
                    "([\\d\\.]*)" + SEPARATOR +  // PDOP
                    "([\\d\\.]*)" + SEPARATOR +  // HDOP
                    "([\\d\\.]*)" +              // VDOP
                    END_OF_LINE);

    public String getExtension() {
        return ".nmea";
    }

    public String getName() {
        return "NMEA 0183 Sentences (*" + getExtension() + ")";
    }

    @SuppressWarnings({"unchecked"})
    public <P extends BaseNavigationPosition> NmeaRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new NmeaRoute(this, characteristics, (List<NmeaPosition>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher rmcMatcher = RMC_PATTERN.matcher(line);
        if (rmcMatcher.matches())
            return hasValidChecksum(line);

        Matcher ggaMatcher = GGA_PATTERN.matcher(line);
        if (ggaMatcher.matches())
            return hasValidChecksum(line);

        Matcher wplMatcher = WPL_PATTERN.matcher(line);
        if (wplMatcher.matches())
            return hasValidChecksum(line);

        Matcher zdaMatcher = ZDA_PATTERN.matcher(line);
        if (zdaMatcher.matches())
            return hasValidChecksum(line);

        Matcher vtgMatcher = VTG_PATTERN.matcher(line);
        if (vtgMatcher.matches())
            return hasValidChecksum(line);

        Matcher gsaMatcher = GSA_PATTERN.matcher(line);
        return gsaMatcher.matches() && hasValidChecksum(line);
    }

    protected NmeaPosition parsePosition(String line) {
        Matcher rmcMatcher = RMC_PATTERN.matcher(line);
        if (rmcMatcher.matches()) {
            String time = rmcMatcher.group(1);
            String latitude = rmcMatcher.group(2);
            String northOrSouth = rmcMatcher.group(3);
            String longitude = rmcMatcher.group(4);
            String westOrEast = rmcMatcher.group(5);
            Double speed = null;
            String speedStr = rmcMatcher.group(6);
            if (speedStr != null) {
                Double knots = Transfer.parseDouble(speedStr);
                if (knots != null)
                    speed = Conversion.knotsToKilometers(knots);
            }
            String date = rmcMatcher.group(7);
            return new NmeaPosition(Transfer.parseDouble(longitude), westOrEast, Transfer.parseDouble(latitude), northOrSouth,
                    null, speed, null, parseDateAndTime(date, time), null);
        }

        Matcher ggaMatcher = GGA_PATTERN.matcher(line);
        if (ggaMatcher.matches()) {
            String time = ggaMatcher.group(1);
            String latitude = ggaMatcher.group(2);
            String northOrSouth = ggaMatcher.group(3);
            String longitude = ggaMatcher.group(4);
            String westOrEast = ggaMatcher.group(5);
            String satellites = ggaMatcher.group(6);
            String altitude = ggaMatcher.group(7);
            NmeaPosition position = new NmeaPosition(Transfer.parseDouble(longitude), westOrEast, Transfer.parseDouble(latitude), northOrSouth,
                    Transfer.parseDouble(altitude), null, null, parseTime(time), null);
            position.setSatellites(Transfer.parseInt(satellites));
            return position;
        }

        Matcher wplMatcher = WPL_PATTERN.matcher(line);
        if (wplMatcher.matches()) {
            String latitude = wplMatcher.group(1);
            String northOrSouth = wplMatcher.group(2);
            String longitude = wplMatcher.group(3);
            String westOrEast = wplMatcher.group(4);
            String comment = wplMatcher.group(5);
            return new NmeaPosition(Transfer.parseDouble(longitude), westOrEast, Transfer.parseDouble(latitude), northOrSouth,
                    null, null, null, null, Transfer.trim(comment));
        }

        Matcher zdaMatcher = ZDA_PATTERN.matcher(line);
        if (zdaMatcher.matches()) {
            String time = zdaMatcher.group(1);
            String day = Transfer.trim(zdaMatcher.group(2));
            String month = Transfer.trim(zdaMatcher.group(3));
            String year = Transfer.trim(zdaMatcher.group(4));
            String date = (day != null ? day : "") + (month != null ? month : "") + (year != null ? year : "");
            return new NmeaPosition(null, null, null, null, null, null, null, parseDateAndTime(date, time), null);
        }

        Matcher vtgMatcher = VTG_PATTERN.matcher(line);
        if (vtgMatcher.matches()) {
            Double heading = Transfer.parseDouble(vtgMatcher.group(1));
            boolean knots = false;
            String speedStr = Transfer.trim(vtgMatcher.group(3));
            if (speedStr == null) {
                speedStr = Transfer.trim(vtgMatcher.group(2));
                knots = true;
            }
            Double speed = Transfer.parseDouble(speedStr);
            if (knots && speed != null)
                speed = Conversion.knotsToKilometers(speed);
            return new NmeaPosition(null, null, null, null, null, speed, heading, null, null);
        }

        Matcher gsaMatcher = GSA_PATTERN.matcher(line);
        if (gsaMatcher.matches()) {
            String pdop = gsaMatcher.group(1);
            String hdop = gsaMatcher.group(2);
            String vdop = gsaMatcher.group(3);
            NmeaPosition position = new NmeaPosition(null, null, null, null, null, null, null, null, null);
            position.setPdop(Transfer.parseDouble(pdop));
            position.setHdop(Transfer.parseDouble(hdop));
            position.setVdop(Transfer.parseDouble(vdop));
            return position;
        }

        throw new IllegalArgumentException("'" + line + "' does not match");
    }

    
    private String formatDay(CompactCalendar date) {
        if (date == null)
            return "";
        return DAY_FORMAT.format(date.getTime());
    }

    private String formatMonth(CompactCalendar date) {
        if (date == null)
            return "";
        return MONTH_FORMAT.format(date.getTime());
    }

    private String formatYear(CompactCalendar date) {
        if (date == null)
            return "";
        return YEAR_FORMAT.format(date.getTime());
    }

    private String formatAltitude(Double altitude) {
        if (altitude == null)
            return "";
        return ALTITUDE_AND_SPEED_NUMBER_FORMAT.format(altitude);
    }

    private String formatSpeed(Double speed) {
        if (speed == null)
            return "";
        return ALTITUDE_AND_SPEED_NUMBER_FORMAT.format(speed);
    }

    private String formatAccuracy(Double accuracy) {
        if (accuracy == null)
            return "";
        return ALTITUDE_AND_SPEED_NUMBER_FORMAT.format(accuracy);
    }

    protected void writePosition(NmeaPosition position, PrintWriter writer, int index) {
        String longitude = formatLongitude(position.getLongitudeAsDdmm());
        String westOrEast = position.getWestOrEast();
        String latitude = formatLatititude(position.getLatitudeAsDdmm());
        String northOrSouth = position.getNorthOrSouth();
        String satellites = position.getSatellites() != null ? Transfer.formatIntAsString(position.getSatellites()) : "";
        String comment = formatComment(position.getComment());
        String time = formatTime(position.getTime());
        String date = formatDate(position.getTime());
        String altitude = formatAltitude(position.getElevation());
        String speedKnots = position.getSpeed() != null ? formatSpeed(Conversion.kilometerToKnots(position.getSpeed())) : "";

        // $GPGGA,130441.89,5239.3154,N,00907.7011,E,1,08,1.25,16.76,M,46.79,M,,*6D
        String gga = "GPGGA" + SEPARATOR + time + SEPARATOR +
                latitude + SEPARATOR + northOrSouth + SEPARATOR + longitude + SEPARATOR + westOrEast + SEPARATOR +
                "1" + SEPARATOR + satellites + SEPARATOR + SEPARATOR + altitude + SEPARATOR + "M" +
                SEPARATOR + SEPARATOR + "M" + SEPARATOR + SEPARATOR;
        writeSentence(writer, gga);

        // $GPWPL,5334.169,N,01001.920,E,STATN1*22
        String wpl = "GPWPL" + SEPARATOR +
                latitude + SEPARATOR + northOrSouth + SEPARATOR + longitude + SEPARATOR + westOrEast + SEPARATOR +
                comment;
        writeSentence(writer, wpl);

        // $GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,160607,,A*76
        String rmc = "GPRMC" + SEPARATOR + time + SEPARATOR + "A" + SEPARATOR +
                latitude + SEPARATOR + northOrSouth + SEPARATOR + longitude + SEPARATOR + westOrEast + SEPARATOR +
                speedKnots + SEPARATOR + SEPARATOR +
                date + SEPARATOR + SEPARATOR + "A";
        writeSentence(writer, rmc);

        if(position.getTime() != null) {
            // $GPZDA,032910,07,08,2004,00,00*48
            String day = formatDay(position.getTime());
            String month = formatMonth(position.getTime());
            String year = formatYear(position.getTime());
            String zda = "GPZDA" + SEPARATOR + time + SEPARATOR + day + SEPARATOR + month + SEPARATOR + year + SEPARATOR + SEPARATOR;
            writeSentence(writer, zda);
        }

        if(position.getHeading() != null || position.getSpeed() != null) {
            String heading = formatAltitude(position.getHeading());
            String speedKm = formatSpeed(position.getSpeed());
            // $GPVTG,32.1,T,,M,1.531,N,2.835,K,A*37
            String vtg = "GPVTG" + SEPARATOR + heading + SEPARATOR + "T" + SEPARATOR + SEPARATOR + "M" + SEPARATOR +
                    speedKnots + SEPARATOR + "N" + SEPARATOR + speedKm + SEPARATOR + "K" + SEPARATOR + "A";
            writeSentence(writer, vtg);
        }

        if (position.getHdop() != null || position.getPdop() != null || position.getVdop() != null) {
            String hdop = formatAccuracy(position.getHdop());
            String pdop = formatAccuracy(position.getPdop());
            String vdop = formatAccuracy(position.getVdop());
            // $GPGSA,A,3,,,,15,17,18,23,,,,,,4.7,4.4,1.5*3F
            String gsa = "GPGSA" + SEPARATOR + "A" + SEPARATOR + "2" + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR +
                    SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR +
                    SEPARATOR + pdop + SEPARATOR + hdop + SEPARATOR + vdop;
            writeSentence(writer, gsa);
        }
    }
}
