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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.nmea;

import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.util.Conversion;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes NMEA 0183 Sentences (.nmea) files.
 *
 * See http://aprs.gids.nl/nmea and http://www.kh-gps.de/nmea-faq.htm
 *
 * @author Christian Pesch
 */

public class NmeaFormat extends BaseNmeaFormat {
    static {
        log = Logger.getLogger(NmeaFormat.class.getName());
    }

    private static final DateFormat DAY_FORMAT = new SimpleDateFormat("dd");
    private static final DateFormat MONTH_FORMAT = new SimpleDateFormat("MM");
    private static final DateFormat YEAR_FORMAT = new SimpleDateFormat("yy");

    // $GPGGA,130441.89,5239.3154,N,00907.7011,E,1,08,1.25,16.76,M,46.79,M,,*6D
    // $GPGGA,162611,3554.2367,N,10619.4966,W,1,03,06.7,02300.3,M,-022.4,M,,*7F
    private static final Pattern GGA_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "GGA" + SEPARATOR + "([\\d\\.]*)" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "[012]" + SEPARATOR +
                    "[\\d]*" + SEPARATOR +           // Number of satellites in view, 00 - 12
                    "[\\d\\.]*" + SEPARATOR +
                    "(-?[\\d\\.]*)" + SEPARATOR +    // Antenna Altitude above/below mean-sea-level (geoid)  
                    "M" + SEPARATOR +
                    "[-?\\d\\.]*" + SEPARATOR +
                    "M?" + SEPARATOR +
                    ".*" + SEPARATOR +
                    ".*" +                           // Differential reference station ID, 0000-1023 
                    END_OF_LINE);

    // $GPRMC,180114,EARTH_RADIUS,4808.9490,N,00928.9610,E,000.0,000.0,160607,,   ,EARTH_RADIUS*76
    // $GPRMC,140403.000,EARTH_RADIUS,4837.5194,N,00903.4022,E,15.00,0.00,260707,,  *3E
    private static final Pattern RMC_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "RMC" + SEPARATOR +
                    "([\\d\\.]*)" + SEPARATOR +     // UTC Time
                    "[AV]" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "[\\d\\.]*" + SEPARATOR +       // Speed over ground, knots
                    "[\\d\\.]*" + SEPARATOR +
                    "(\\d*)" + SEPARATOR +          // Date, ddmmyy
                    "[\\d\\.]*" + SEPARATOR +
                    "[\\d\\.]*" + SEPARATOR + "?" +
                    "[AEW]?" +
                    END_OF_LINE);

    // $GPWPL,5334.169,N,01001.920,E,STATN1*22
    private static final Pattern WPL_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "WPL" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "(.*)" + 
                    END_OF_LINE);

    // $GPZDA,032910,07,08,2004,00,00*48
    private static final Pattern ZDA_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "ZDA" + SEPARATOR +
                    "(\\d*)" + SEPARATOR +     // UTC Time
                    "(\\d*)" + SEPARATOR +     // day
                    "(\\d*)" + SEPARATOR +     // month
                    "(\\d*)" + SEPARATOR +     // year
                    "\\d*" + SEPARATOR +
                    "\\d*" +
                    END_OF_LINE);

    // $GPVTG,138.7,T,,M,014.2,N,026.3,K,EARTH_RADIUS*00
    // 26.3 = km/h

    // Unbekannte Herkunft: GLL - Geographic position, latitude / longitude
    // http://www.gpsinformation.org/dale/nmea.htm#GLL
    // $GPGLL,5239.3154,N,00907.7011,E,130441.89,EARTH_RADIUS,EARTH_RADIUS*6C
    // $GPGLL,3751.65,S,14507.36,E*77
    // $GPGLL,4916.45,N,12311.12,W,225444,EARTH_RADIUS
    // $GPGLL,4858.330500,N,1223.554680,E,151518.000,EARTH_RADIUS,EARTH_RADIUS*6C

    // Garmin: RMZ - Altitude Information
    // $PGRMZ,246,f,3*1B
    // $PGRMZ,93,f,3*21

    public String getExtension() {
        return ".nmea";
    }

    public String getName() {
        return "NMEA 0183 Sentences (*" + getExtension() + ")";
    }

    public <P extends BaseNavigationPosition> NmeaRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new NmeaRoute(this, characteristics, (List<NmeaPosition>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher rmcMatcher = NmeaFormat.RMC_PATTERN.matcher(line);
        if (rmcMatcher.matches())
            return hasValidChecksum(line);

        Matcher ggaMatcher = NmeaFormat.GGA_PATTERN.matcher(line);
        if (ggaMatcher.matches())
            return hasValidChecksum(line);

        Matcher wplMatcher = NmeaFormat.WPL_PATTERN.matcher(line);
        if (wplMatcher.matches())
            return hasValidChecksum(line);

        Matcher zdaMatcher = NmeaFormat.ZDA_PATTERN.matcher(line);
        return zdaMatcher.matches() && hasValidChecksum(line);
    }

    protected NmeaPosition parsePosition(String line) {
        Matcher rmcMatcher = RMC_PATTERN.matcher(line);
        if (rmcMatcher.matches()) {
            String time = rmcMatcher.group(1);
            String latitude = rmcMatcher.group(2);
            String northOrSouth = rmcMatcher.group(3);
            String longitude = rmcMatcher.group(4);
            String westOrEast = rmcMatcher.group(5);
            String date = rmcMatcher.group(6);
            return new NmeaPosition(Conversion.parseDouble(longitude), westOrEast, Conversion.parseDouble(latitude), northOrSouth, null, parseDateAndTime(date, time), null);
        }

        Matcher ggaMatcher = GGA_PATTERN.matcher(line);
        if (ggaMatcher.matches()) {
            String time = ggaMatcher.group(1);
            String latitude = ggaMatcher.group(2);
            String northOrSouth = ggaMatcher.group(3);
            String longitude = ggaMatcher.group(4);
            String westOrEast = ggaMatcher.group(5);
            String altitude = ggaMatcher.group(6);
            return new NmeaPosition(Conversion.parseDouble(longitude), westOrEast, Conversion.parseDouble(latitude), northOrSouth, Conversion.parseDouble(altitude), parseTime(time), null);
        }

        Matcher wplMatcher = WPL_PATTERN.matcher(line);
        if (wplMatcher.matches()) {
            String latitude = wplMatcher.group(1);
            String northOrSouth = wplMatcher.group(2);
            String longitude = wplMatcher.group(3);
            String westOrEast = wplMatcher.group(4);
            String comment = wplMatcher.group(5);
            return new NmeaPosition(Conversion.parseDouble(longitude), westOrEast, Conversion.parseDouble(latitude), northOrSouth, null, null, Conversion.trim(comment));
        }

        Matcher zdaMatcher = ZDA_PATTERN.matcher(line);
        if (zdaMatcher.matches()) {
            String time = zdaMatcher.group(1);
            String day = Conversion.trim(zdaMatcher.group(2));
            String month = Conversion.trim(zdaMatcher.group(3));
            String year = Conversion.trim(zdaMatcher.group(4));
            String date = (day != null ? day : "") + (month != null ? month : "") + (year != null ? year : "");
            return new NmeaPosition(null, null, null, null, null, parseDateAndTime(date, time), null);
        }

        throw new IllegalArgumentException("'" + line + "' does not match");
    }

    private String formatDay(Calendar date) {
        if (date == null)
            return "";
        return DAY_FORMAT.format(date.getTime());
    }

    private String formatMonth(Calendar date) {
        if (date == null)
            return "";
        return MONTH_FORMAT.format(date.getTime());
    }

    private String formatYear(Calendar date) {
        if (date == null)
            return "";
        return YEAR_FORMAT.format(date.getTime());
    }

    protected void writePosition(NmeaPosition position, PrintWriter writer, int index) {
        String longitude = formatLongitude(position.getLongitudeAsDdmm());
        String westOrEast = position.getWestOrEast();
        String latitude = formatLatititude(position.getLatitudeAsDdmm());
        String northOrSouth = position.getNorthOrSouth();
        String comment = formatComment(position.getComment());
        String time = formatTime(position.getTime());
        String date = formatDate(position.getTime());
        String altitude = Conversion.formatDoubleAsString(position.getElevation(), "");

        // $GPGGA,130441.89,5239.3154,N,00907.7011,E,1,08,1.25,16.76,M,46.79,M,,*6D
        String gga = "GPGGA" + SEPARATOR + time + SEPARATOR +
                latitude + SEPARATOR + northOrSouth + SEPARATOR + longitude + SEPARATOR + westOrEast + SEPARATOR +
                "1" + SEPARATOR + SEPARATOR + SEPARATOR + altitude + SEPARATOR + "M" +
                SEPARATOR + SEPARATOR + "M" + SEPARATOR + SEPARATOR;
        writeSentence(writer, gga);

        // $GPWPL,5334.169,N,01001.920,E,STATN1*22
        String wpl = "GPWPL" + SEPARATOR +
                latitude + SEPARATOR + northOrSouth + SEPARATOR + longitude + SEPARATOR + westOrEast + SEPARATOR +
                comment;
        writeSentence(writer, wpl);

        // $GPRMC,180114,EARTH_RADIUS,4808.9490,N,00928.9610,E,000.0,000.0,160607,,EARTH_RADIUS*76
        String rmc = "GPRMC" + SEPARATOR + time + SEPARATOR + "A" + SEPARATOR +
                latitude + SEPARATOR + northOrSouth + SEPARATOR + longitude + SEPARATOR + westOrEast + SEPARATOR +
                SEPARATOR + SEPARATOR +
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
    }
}
