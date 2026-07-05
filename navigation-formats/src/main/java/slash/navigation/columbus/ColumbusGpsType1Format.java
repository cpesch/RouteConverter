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

package slash.navigation.columbus;

import slash.navigation.base.ParserContext;
import slash.navigation.base.Wgs84Position;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteComments.isDefaultDescription;
import static slash.navigation.common.NavigationConversion.formatAccuracyAsString;

/**
 * Reads and writes Columbus GPS Type 1 (.csv) files.
 *
 * Type A:
 *
 * Header: INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX
 * Format: 8     ,T,090508,075646,48.174411N,016.284588E,-235 ,0   ,0  ,3D,SPS ,1.6  ,1.3  ,0.9  ,VOX00014
 *
 * Type B:
 *
 * Header: INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,VOX
 * Format: 8     ,T,090508,075646,48.174411N,016.284588E,-235 ,0   ,0  ,VOX00014
 *
 * @author Christian Pesch
 */

public class ColumbusGpsType1Format extends ColumbusGpsFormat {
    private static final Preferences preferences = Preferences.userNodeForPackage(ColumbusGpsType1Format.class);
    private static final String HEADER_LINE = "INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX";
    private static final Pattern HEADER_PATTERN = Pattern.
            compile("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,(HEIGHT|ALTITUDE),SPEED,HEADING,(FIX MODE,VALID,PDOP,HDOP,VDOP,)?VOX");
    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    field("\\d+") +                         // 1 index
                    field("[" + VALID_TAG_VALUES + "]") +   // 2 tag
                    field("\\d*") +                         // 3 date
                    field("\\d*") +                         // 4 time
                    coordinate("[NS]") +                    // 5 latitude, 6 hemisphere
                    coordinate("[WE]") +                    // 7 longitude, 8 hemisphere
                    field("[-\\d]+") +                      // 9 height
                    field("\\d+") +                         // 10 speed
                    field("\\d+") +                         // 11 heading
                    "(" +                                   // 12 optional Type-A block
                    field("[^" + SEPARATOR + "]*") +        // 13 fix mode
                    field("[^" + SEPARATOR + "]*") +        // 14 valid
                    field("[\\d\\.]*") +                    // 15 pdop
                    field("[\\d\\.]*") +                    // 16 hdop
                    field("[\\d\\.]*") +                    // 17 vdop
                    ")?" +
                    lastField("[^" + SEPARATOR + "]*") +    // 18 vox
                    END_OF_LINE);
    private static final Set<String> VALID_FIX_MODES = new HashSet<>(asList("2D", "3D"));
    private static final Set<String> VALID_VALID = new HashSet<>(asList("SPS", "DGPS"));

    public String getName() {
        return "Columbus GPS Type 1 (*" + getExtension() + ")";
    }

    protected Pattern getLinePattern() {
        return LINE_PATTERN;
    }


    private boolean hasValidField(String line, String field, Set<String> validValues) {
        if (field != null && !validValues.contains(field)) {
            log.severe("Field for '" + line + "' is invalid. Contains '" + field + "' but expecting '" + validValues + "'");
            return preferences.getBoolean("ignoreInvalidFix", false);
        }
        return true;
    }

    protected boolean hasValidFix(String line, Matcher matcher) {
        return hasValidField(line, trim(matcher.group(13)), VALID_FIX_MODES) &&
                hasValidField(line, trim(matcher.group(14)), VALID_VALID);
    }

    protected String getHeader() {
        return HEADER_LINE;
    }

    protected Pattern getHeaderPattern() {
        return HEADER_PATTERN;
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        boolean isTypeA = trim(lineMatcher.group(14)) != null;
        Wgs84Position position = parseCommonPosition(lineMatcher, context,
                parseDateAndTime(lineMatcher.group(3), lineMatcher.group(4)), 18, isTypeA);
        position.setPdop(parseDouble(lineMatcher.group(15)));
        position.setHdop(parseDouble(lineMatcher.group(16)));
        position.setVdop(parseDouble(lineMatcher.group(17)));
        return position;
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String speed = fillWithZeros(position.getSpeed() != null ? formatIntAsString(position.getSpeed().intValue()) : "0", 4);
        String heading = fillWithZeros(position.getHeading() != null ? formatIntAsString(position.getHeading().intValue()) : "0", 3);
        String pdop = fillWithZeros(position.getPdop() != null ? formatAccuracyAsString(position.getPdop()) : "", 5);
        String hdop = fillWithZeros(position.getHdop() != null ? formatAccuracyAsString(position.getHdop()) : "", 5);
        String vdop = fillWithZeros(position.getVdop() != null ? formatAccuracyAsString(position.getVdop()) : "", 5);
        String description = !isDefaultDescription(position.getDescription()) ? position.getDescription() : "";

        writer.println(formatCommonPrefix(position, index) + SEPARATOR +
                speed + SEPARATOR +
                heading + SEPARATOR +
                "3D" + SEPARATOR +
                "SPS" + SEPARATOR +
                pdop + SEPARATOR +
                hdop + SEPARATOR +
                vdop + SEPARATOR +
                fillWithZeros(escape(description, SEPARATOR, ';'), 8));
    }
}
