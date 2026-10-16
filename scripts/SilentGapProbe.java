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

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Probes the features a minimized JRE loses silently -- no exception, no
 * NoClassDefFoundError for ForceInit to catch, just wrong output on a user
 * machine:
 *  - extended charsets (jdk.charsets): a GPX/KML that declares Shift_JIS,
 *    GB2312, windows-1255, ... is rejected by the XML parser and read through
 *    the windows-1252 Garble fallback as mojibake
 *  - non-US locale data (jdk.localedata): en_GB dates format like en_US
 *    (issue #415)
 *
 * Usage: SilentGapProbe -- exits 1 and lists every missing feature.
 */
public class SilentGapProbe {
    private static final String[] CHARSETS = {
            "Shift_JIS", "EUC-JP", "GB2312", "GBK", "Big5", "EUC-KR",
            "windows-1255", "windows-1256", "windows-1258", "x-windows-874"
    };

    public static void main(String[] args) {
        List<String> missing = new ArrayList<>();
        for (String charset : CHARSETS) {
            if (!Charset.isSupported(charset))
                missing.add("charset " + charset + " (jdk.charsets)");
        }

        Date epoch = new Date(0);
        String uk = DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK).format(epoch);
        String us = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).format(epoch);
        if (uk.equals(us))
            missing.add("en_GB date format equals en_US '" + us + "' (jdk.localedata)");

        if (missing.isEmpty()) {
            System.out.println("    OK: " + CHARSETS.length + " extended charsets and en_GB locale data present");
            return;
        }
        for (String m : missing)
            System.out.println("    MISSING: " + m);
        System.exit(1);
    }
}
