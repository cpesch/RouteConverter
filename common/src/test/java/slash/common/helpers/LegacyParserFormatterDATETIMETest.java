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

package slash.common.helpers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import slash.common.type.CompactCalendar;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class LegacyParserFormatterDATETIMETest {
    private static final TimeZone ZONE_UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone ZONE_BERLIN = TimeZone.getTimeZone("Europe/Berlin");

    private DateTimeParserFormatter sut;
    private Locale testLocale;

    @Before
    public void setUp() {
        testLocale = Locale.GERMAN;
        sut = new LegacyParserFormatter(LegacyParserFormatter.ParserType.DATETIME, () -> testLocale);
    }

    @Test
    public void testFormat() {
        sut.setZone(ZONE_UTC.getID());
        Assert.assertEquals("02.03.25, 21:34:56", sut.format(cal(2025, 3, 2, 21, 34, 56)));

        sut.setZone(ZONE_BERLIN.getID());
        Assert.assertEquals("02.03.25, 22:34:56", sut.format(cal(2025, 3, 2, 21, 34, 56)));

        testLocale = Locale.ROOT;

        sut.setZone(ZONE_UTC.getID());
        Assert.assertEquals("2025-03-02 21:34:56", sut.format(cal(2025, 3, 2, 21, 34, 56)));

        sut.setZone(ZONE_BERLIN.getID());
        Assert.assertEquals("2025-03-02 22:34:56", sut.format(cal(2025, 3, 2, 21, 34, 56)));
    }

    @Test
    public void testParseUtc() throws Exception {
        sut.setZone(ZONE_UTC.getID());

        runParseTest("01.01.25, 01:02:03", cal(2025, 1, 1, 1, 2, 3), ZONE_UTC);
        runParseTest("01.01.25, 1:2:3", cal(2025, 1, 1, 1, 2, 3), ZONE_UTC);
        runParseTest("01.01.25, 15:25:35", cal(2025, 1, 1, 15, 25, 35), ZONE_UTC);
        runParseTest("01.01.25, 15:65:78", cal(2025, 1, 1, 16, 6, 18), ZONE_UTC);
        runParseTest("1.1.25, 1:2:3", cal(2025, 1, 1, 1, 2, 3), ZONE_UTC);
        runParseTest("1.1.2025, 15:25:35", cal(2025, 1, 1, 15, 25, 35), ZONE_UTC);
        runParseTest("1.1.2025, 15:65:78", cal(2025, 1, 1, 16, 6, 18), ZONE_UTC);
        runParseTest("1.1.45, 1:2:3", cal(2045, 1, 1, 1, 2, 3), ZONE_UTC);
        runParseTest("1.1.1945, 1:2:3", cal(1945, 1, 1, 1, 2, 3), ZONE_UTC);
        runParseTest("1.1.98, 1:2:3", cal(1998, 1, 1, 1, 2, 3), ZONE_UTC);

        testLocale = Locale.ROOT;

        runParseTest("2025-01-01 01:02:03", cal(2025, 1, 1, 1, 2, 3), ZONE_UTC);
        runParseTest("25-01-01 1:2:3", cal(2025, 1, 1, 1, 2, 3), ZONE_UTC);
        runParseTest("25-01-01 15:25:35", cal(2025, 1, 1, 15, 25, 35), ZONE_UTC);
        runParseTest("25-01-01 15:65:78", cal(2025, 1, 1, 16, 6, 18), ZONE_UTC);
        runParseTest("25-1-1 1:2:3", cal(2025, 1, 1, 1, 2, 3), ZONE_UTC);
        runParseTest("2025-1-1 15:25:35", cal(2025, 1, 1, 15, 25, 35), ZONE_UTC);
        runParseTest("2025-1-1 15:65:78", cal(2025, 1, 1, 16, 6, 18), ZONE_UTC);
        runParseTest("45-1-1 1:2:3", cal(2045, 1, 1, 1, 2, 3), ZONE_UTC);
        runParseTest("1945-1-1 1:2:3", cal(1945, 1, 1, 1, 2, 3), ZONE_UTC);
        runParseTest("98-1-1 1:2:3", cal(1998, 1, 1, 1, 2, 3), ZONE_UTC);
    }

    @Test
    public void testParseBerlin() throws Exception {
        sut.setZone(ZONE_BERLIN.getID());

        runParseTest("01.01.25, 02:02:03", cal(2025, 1, 1, 1, 2, 3), ZONE_BERLIN);
        runParseTest("01.01.25, 1:2:3", cal(2025, 1, 1, 0, 2, 3), ZONE_BERLIN);
        runParseTest("01.01.25, 15:25:35", cal(2025, 1, 1, 14, 25, 35), ZONE_BERLIN);
        runParseTest("01.01.25, 15:65:78", cal(2025, 1, 1, 15, 6, 18), ZONE_BERLIN);
        runParseTest("1.1.25, 1:2:3", cal(2025, 1, 1, 0, 2, 3), ZONE_BERLIN);
        runParseTest("1.1.2025, 15:25:35", cal(2025, 1, 1, 14, 25, 35), ZONE_BERLIN);
        runParseTest("1.1.2025, 15:65:78", cal(2025, 1, 1, 15, 6, 18), ZONE_BERLIN);
        runParseTest("1.1.45, 1:2:3", cal(2045, 1, 1, 0, 2, 3), ZONE_BERLIN);
        runParseTest("1.1.1945, 1:2:3", cal(1945, 1, 1, 0, 2, 3), ZONE_BERLIN);
        runParseTest("1.1.98, 1:2:3", cal(1998, 1, 1, 0, 2, 3), ZONE_BERLIN);

        testLocale = Locale.ROOT;

        runParseTest("25-01-01 02:02:03", cal(2025, 1, 1, 1, 2, 3), ZONE_BERLIN);
        runParseTest("25-01-01 1:2:3", cal(2025, 1, 1, 0, 2, 3), ZONE_BERLIN);
        runParseTest("25-01-01 15:25:35", cal(2025, 1, 1, 14, 25, 35), ZONE_BERLIN);
        runParseTest("25-01-01 15:65:78", cal(2025, 1, 1, 15, 6, 18), ZONE_BERLIN);
        runParseTest("25-1-1 1:2:3", cal(2025, 1, 1, 0, 2, 3), ZONE_BERLIN);
        runParseTest("2025-1-1 15:25:35", cal(2025, 1, 1, 14, 25, 35), ZONE_BERLIN);
        runParseTest("2025-1-1 15:65:78", cal(2025, 1, 1, 15, 6, 18), ZONE_BERLIN);
        runParseTest("45-1-1 1:2:3", cal(2045, 1, 1, 0, 2, 3), ZONE_BERLIN);
        runParseTest("1945-1-1 1:2:3", cal(1945, 1, 1, 0, 2, 3), ZONE_BERLIN);
        runParseTest("98-1-1 1:2:3", cal(1998, 1, 1, 0, 2, 3), ZONE_BERLIN);
    }

    private void runParseTest(String toParse, CompactCalendar expected, TimeZone zone) throws Exception {
        Calendar result = sut.parse(toParse, null);

        Calendar expectedCal = expected.getCalendar();
        expectedCal.setTimeZone(zone);
        Assert.assertEquals(result, expectedCal);
    }

    private static CompactCalendar cal(int year, int month, int day, int hour, int minute, int second) {
        Calendar c = Calendar.getInstance(ZONE_UTC);
        c.clear(); // prevents "remnants" such as milliseconds
        c.set(year, month - 1, day, hour, minute, second);
        return CompactCalendar.fromCalendar(c);
    }
}