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
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.ValueAndOrientation;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Locale.US;
import static slash.common.io.Transfer.*;
import static slash.common.type.CompactCalendar.createDateFormat;
import static slash.navigation.common.UnitConversion.kiloMeterToNauticMiles;
import static slash.navigation.common.UnitConversion.nauticMilesToKiloMeter;

/**
 * Reads and writes NMEA 0183 Sentences (.nmea) files.
 *
 * See http://aprs.gids.nl/nmea and http://www.kh-gps.de/nmea-faq.htm
 *
 * @author Christian Pesch
 */

public class NmeaFormat extends BaseNmeaFormat {
    private static final Preferences preferences = Preferences.userNodeForPackage(NmeaFormat.class);

    private static final NumberFormat ALTITUDE_AND_SPEED_NUMBER_FORMAT = DecimalFormat.getNumberInstance(US);
    static {
        int maximumFractionDigits = preferences.getInt("altitudeSpeedMaximumFractionDigits", 1);
        ALTITUDE_AND_SPEED_NUMBER_FORMAT.setGroupingUsed(false);
        ALTITUDE_AND_SPEED_NUMBER_FORMAT.setMinimumFractionDigits(1);
        ALTITUDE_AND_SPEED_NUMBER_FORMAT.setMaximumFractionDigits(maximumFractionDigits);
        ALTITUDE_AND_SPEED_NUMBER_FORMAT.setMinimumIntegerDigits(1);
        ALTITUDE_AND_SPEED_NUMBER_FORMAT.setMaximumIntegerDigits(6);
    }

    private static final String DAY_FORMAT = "dd";
    private static final String MONTH_FORMAT = "MM";
    private static final String YEAR_FORMAT = "yyyy";

    // $GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,160607,,   ,A*76
    // $GPRMC,140403.000,A,4837.5194,N,00903.4022,E,15.00,0.00,260707,,  *3E
    // $GPRMC,172103.38,V,4424.5358,N,06812.3754,W,0.000,0.000,101010,0,W,N*3A
    // $GNRMC,162622.00,A,4857.29112,N,00850.57680,E,0.813,251.19,160217,,,D,V*0D
    private static final Pattern RMC_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "RMC" + SEPARATOR +
                    "([\\d\\.]*)" + SEPARATOR +     // UTC Time
                    "[AV]" + SEPARATOR +            // Status, A=active, V=void
                    "([\\s\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\s\\d\\.]+)" + SEPARATOR + "([EW])" + SEPARATOR +
                    "([\\d\\.]*)" + SEPARATOR +     // Speed over ground, knots
                    "([\\d\\.]*)" + SEPARATOR +     // Course over ground, degrees
                    "(\\d*)" + SEPARATOR +          // Date, ddmmyy
                    "[\\d\\.]*" + SEPARATOR +
                    "[\\d\\.]*" + SEPARATOR + "?" + // Magnetic variation
                    "[ADEW]?" + SEPARATOR + "?" +   // E=East, W=West
                    "([ADEMNSV])?" +                // Signal integrity, N=not valid
                    END_OF_LINE);

    // $GPGGA,130441.89,5239.3154,N,00907.7011,E,1,08,1.25,16.76,M,46.79,M,,*6D
    // $GPGGA,162611,3554.2367,N,10619.4966,W,1,03,06.7,02300.3,M,-022.4,M,,*7F
    // $GPGGA,132713,5509.7861,N,00140.5854,W,1,07,1.0,98.9,M,,M,,*7d
    // $GPGGA,171247,4737.266541,N,00814.066563,E,1,,,416,M,,,*00
    private static final Pattern GGA_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "GGA" + SEPARATOR + "([\\d\\.]*)" + SEPARATOR +
                    "([\\s\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\s\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "([\\d+])" + SEPARATOR +         // Fix quality, 0=invalid
                    "([\\d]*)" + SEPARATOR +         // Number of satellites in view, 00 - 12
                    "[\\d\\.]*" + SEPARATOR +
                    "(-?[\\d\\.]*)" + SEPARATOR +    // Antenna Altitude above/below mean-sea-level (geoid)
                    "M" + SEPARATOR +
                    "[-?\\d\\.]*" + SEPARATOR +
                    "M?" + SEPARATOR +
                    ".*" + SEPARATOR + "?" +
                    ".*" +                           // Differential reference station ID, 0000-1023
                    END_OF_LINE);

    // $GPGLL,4916.45,N,12311.12,W,220433.11,A*6D
    private static final Pattern GLL_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "GLL" + SEPARATOR +
                    "([\\s\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\s\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR +      // UTC Time
                    "([AVP])" +                      // Status
                    ".*" +
                    END_OF_LINE);

    // $GNGNS,184113.00,5215.46773,N,01021.80963,E,AAAN,17,0.73,73.9,45.8,,,V*21
    private static final Pattern GNS_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "GNS" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR +     // UTC Time
                    "([\\s\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\s\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "([NADPRFEMS]+)" + SEPARATOR +  // Mode indicator, N=no fix
                    "(\\d*)" + SEPARATOR +          // Number of SVs in use, range 00?99
                    "([\\d\\.]*)" + SEPARATOR +     // HDOP
                    "([\\d\\.]*)" + SEPARATOR +     // Orthometric height in meters
                    "[\\d\\.]*" + SEPARATOR +       // Geoidal separation in meters
                    "\\d*" + SEPARATOR +            // Age of differential data
                    "\\d*" + SEPARATOR +            // Reference station ID
                    ".*" +
                    END_OF_LINE);


    // $GPWPL,5334.169,N,01001.920,E,STATN1*22
    // $GPWPL,3018.000,S,15309.000,E,Coffs Harbor (Sidney)
    private static final Pattern WPL_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "WPL" + SEPARATOR +
                    "([\\s\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\s\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "([^\\*]*)" +
                    "(\\*[0-9A-Fa-f][0-9A-Fa-f])?$");

    // $GPZDA,032910.542,07,08,2004,00,00*48
    // $GNZDA,184113.00,23,02,2017,00,00*71
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
    // $GNVTG,251.19,T,,M,0.813,N,1.506,K,D*20
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
                    "([ADEN])" +                  // Mode indicator, N=not valid
                    END_OF_LINE);

    // $GPGSA,A,3,,,,15,17,18,23,,,,,,4.7,4.4,1.5*3F
    private static final Pattern GSA_PATTERN = Pattern.
            compile(BEGIN_OF_LINE + "GSA" + SEPARATOR +
                    "[AM]" + SEPARATOR +
                    "([123])" + SEPARATOR +      // Fix, 1=Fix not available
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
    public <P extends NavigationPosition> NmeaRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new NmeaRoute(this, characteristics, (List<NmeaPosition>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher rmcMatcher = RMC_PATTERN.matcher(line);
        if (rmcMatcher.matches())
            return hasValidChecksum(line) && hasValidFix(line, rmcMatcher.group(9), "N");

        Matcher ggaMatcher = GGA_PATTERN.matcher(line);
        if (ggaMatcher.matches())
            return hasValidChecksum(line) && hasValidFix(line, ggaMatcher.group(6), "0");

        Matcher gllMatcher = GLL_PATTERN.matcher(line);
        if (gllMatcher.matches())
            return hasValidChecksum(line) && hasValidFix(line, gllMatcher.group(6), "V");

        Matcher gnsMatcher = GNS_PATTERN.matcher(line);
        if (gnsMatcher.matches())
            return hasValidChecksum(line) && hasValidFix(line, gnsMatcher.group(6), "X");

        Matcher wplMatcher = WPL_PATTERN.matcher(line);
        if (wplMatcher.matches())
            return wplMatcher.group(6) == null || hasValidChecksum(line);

        Matcher zdaMatcher = ZDA_PATTERN.matcher(line);
        if (zdaMatcher.matches())
            return hasValidChecksum(line);

        Matcher vtgMatcher = VTG_PATTERN.matcher(line);
        if (vtgMatcher.matches())
            return hasValidChecksum(line) && hasValidFix(line, vtgMatcher.group(4), "N");

        Matcher gsaMatcher = GSA_PATTERN.matcher(line);
        return gsaMatcher.matches() && hasValidChecksum(line) && hasValidFix(line, gsaMatcher.group(1), "1");
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
                Double miles = parseDouble(speedStr);
                if (miles != null)
                    speed = nauticMilesToKiloMeter(miles);
            }
            Double heading = parseDouble(rmcMatcher.group(7));
            String date = rmcMatcher.group(8);
            return new NmeaPosition(parseDouble(longitude), westOrEast, parseDouble(latitude), northOrSouth,
                    null, speed, heading, parseDateAndTime(date, time), null);
        }

        Matcher ggaMatcher = GGA_PATTERN.matcher(line);
        if (ggaMatcher.matches()) {
            String time = ggaMatcher.group(1);
            String latitude = ggaMatcher.group(2);
            String northOrSouth = ggaMatcher.group(3);
            String longitude = ggaMatcher.group(4);
            String westOrEast = ggaMatcher.group(5);
            String satellites = ggaMatcher.group(7);
            String altitude = ggaMatcher.group(8);
            NmeaPosition position = new NmeaPosition(parseDouble(longitude), westOrEast, parseDouble(latitude), northOrSouth,
                    parseDouble(altitude), null, null, parseTime(time), null);
            position.setSatellites(parseInteger(satellites));
            return position;
        }

        Matcher gllMatcher = GLL_PATTERN.matcher(line);
        if (gllMatcher.matches()) {
            String latitude = gllMatcher.group(1);
            String northOrSouth = gllMatcher.group(2);
            String longitude = gllMatcher.group(3);
            String westOrEast = gllMatcher.group(4);
            String time = gllMatcher.group(5);
            return new NmeaPosition(parseDouble(longitude), westOrEast, parseDouble(latitude), northOrSouth,
                    null, null, null, parseTime(time), null);
        }

        Matcher gnsMatcher = GNS_PATTERN.matcher(line);
        if (gnsMatcher.matches()) {
            String time = gnsMatcher.group(1);
            String latitude = gnsMatcher.group(2);
            String northOrSouth = gnsMatcher.group(3);
            String longitude = gnsMatcher.group(4);
            String westOrEast = gnsMatcher.group(5);
            String svs = gnsMatcher.group(7);
            String hdop = gnsMatcher.group(8);
            String orthometricHeight = gnsMatcher.group(9);
            NmeaPosition position = new NmeaPosition(parseDouble(longitude), westOrEast, parseDouble(latitude), northOrSouth,
                    parseDouble(orthometricHeight), null, null, parseTime(time), null);
            position.setHdop(parseDouble(hdop));
            position.setSatellites(parseInt(svs));
            return position;
        }

        Matcher wplMatcher = WPL_PATTERN.matcher(line);
        if (wplMatcher.matches()) {
            String latitude = wplMatcher.group(1);
            String northOrSouth = wplMatcher.group(2);
            String longitude = wplMatcher.group(3);
            String westOrEast = wplMatcher.group(4);
            String description = wplMatcher.group(5);
            return new NmeaPosition(parseDouble(longitude), westOrEast, parseDouble(latitude), northOrSouth,
                    null, null, null, null, trim(description));
        }

        Matcher zdaMatcher = ZDA_PATTERN.matcher(line);
        if (zdaMatcher.matches()) {
            String time = zdaMatcher.group(1);
            String day = trim(zdaMatcher.group(2));
            String month = trim(zdaMatcher.group(3));
            String year = trim(zdaMatcher.group(4));
            String date = (day != null ? day : "") + (month != null ? month : "") + (year != null ? year : "");
            return new NmeaPosition(null, null, null, null, null, null, null, parseDateAndTime(date, time), null);
        }

        Matcher vtgMatcher = VTG_PATTERN.matcher(line);
        if (vtgMatcher.matches()) {
            Double heading = parseDouble(vtgMatcher.group(1));
            boolean miles = false;
            String speedStr = trim(vtgMatcher.group(3));
            if (speedStr == null) {
                speedStr = trim(vtgMatcher.group(2));
                miles = true;
            }
            Double speed = parseDouble(speedStr);
            if (miles && speed != null)
                speed = nauticMilesToKiloMeter(speed);
            return new NmeaPosition(null, null, null, null, null, speed, heading, null, null);
        }

        Matcher gsaMatcher = GSA_PATTERN.matcher(line);
        if (gsaMatcher.matches()) {
            String pdop = gsaMatcher.group(2);
            String hdop = gsaMatcher.group(3);
            String vdop = gsaMatcher.group(4);
            NmeaPosition position = new NmeaPosition(null, null, null, null, null, null, null, null, null);
            position.setPdop(parseDouble(pdop));
            position.setHdop(parseDouble(hdop));
            position.setVdop(parseDouble(vdop));
            return position;
        }

        throw new IllegalArgumentException("'" + line + "' does not match");
    }


    private String formatDay(CompactCalendar date) {
        if (date == null)
            return "";
        return createDateFormat(DAY_FORMAT).format(date.getTime());
    }

    private String formatMonth(CompactCalendar date) {
        if (date == null)
            return "";
        return createDateFormat(MONTH_FORMAT).format(date.getTime());
    }

    private String formatYear(CompactCalendar date) {
        if (date == null)
            return "";
        return createDateFormat(YEAR_FORMAT).format(date.getTime());
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

    protected void writePosition(NmeaPosition position, PrintWriter writer) {
        ValueAndOrientation longitudeAsValueAndOrientation = position.getLongitudeAsValueAndOrientation();
        String longitude = formatLongitude(longitudeAsValueAndOrientation.getValue());
        String westOrEast = longitudeAsValueAndOrientation.getOrientation().value();
        ValueAndOrientation latitudeAsValueAndOrientation = position.getLatitudeAsValueAndOrientation();
        String latitude = formatLatitude(latitudeAsValueAndOrientation.getValue());
        String northOrSouth = latitudeAsValueAndOrientation.getOrientation().value();
        String satellites = position.getSatellites() != null ? formatIntAsString(position.getSatellites()) : "";
        String description = escape(position.getDescription(), SEPARATOR, ';', null);
        String time = formatTime(position.getTime());
        String date = formatDate(position.getTime());
        String altitude = formatAltitude(position.getElevation());
        String speedKnots = position.getSpeed() != null ? formatSpeed(kiloMeterToNauticMiles(position.getSpeed())) : "";

        // $GPGGA,130441.89,5239.3154,N,00907.7011,E,1,08,1.25,16.76,M,46.79,M,,*6D
        if(time.length() > 0 || altitude.length() > 0 || satellites.length() > 0) {
            String gga = "GPGGA" + SEPARATOR + time + SEPARATOR + latitude + SEPARATOR + northOrSouth + SEPARATOR +
                    longitude + SEPARATOR + westOrEast + SEPARATOR + "1" + SEPARATOR + satellites + SEPARATOR + SEPARATOR +
                    altitude + SEPARATOR + "M" + SEPARATOR + SEPARATOR + "M" + SEPARATOR + SEPARATOR;
            writeSentence(writer, gga);
        }

        // $GPWPL,5334.169,N,01001.920,E,STATN1*22
        if(description != null) {
            String wpl = "GPWPL" + SEPARATOR + latitude + SEPARATOR + northOrSouth + SEPARATOR + longitude + SEPARATOR +
                    westOrEast + SEPARATOR + description;
            writeSentence(writer, wpl);
        }

        // $GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,160607,,A*76
        String rmc = "GPRMC" + SEPARATOR + time + SEPARATOR + "A" + SEPARATOR + latitude + SEPARATOR +
                northOrSouth + SEPARATOR + longitude + SEPARATOR + westOrEast + SEPARATOR + speedKnots + SEPARATOR + SEPARATOR +
                date + SEPARATOR + SEPARATOR + "A";
        writeSentence(writer, rmc);

        if(position.hasTime()) {
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
            String gsa = "GPGSA" + SEPARATOR + "A" + SEPARATOR + "3" + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR +
                    SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR +
                    SEPARATOR + pdop + SEPARATOR + hdop + SEPARATOR + vdop;
            writeSentence(writer, gsa);
        }
    }
}
