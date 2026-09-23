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

package slash.common.type;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static slash.common.TestCase.calendar;
import static slash.common.type.ISO8601.formatDate;
import static slash.common.type.ISO8601.parseDate;

public class ISO8601Test {

    // ---- characterisation of behaviour the java.time migration must preserve (rc/RouteConverter#190) ----
    //
    // Written and confirmed green against the hand-rolled implementation BEFORE the migration, so
    // they pin the existing contract rather than the new output. Offsets are deliberately NOT pinned
    // here: negative offsets and DST are corrected by the migration and are covered separately.

    private static Calendar utc(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        Calendar result = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        result.clear();
        result.set(year, month - 1, day, hour, minute, second);
        result.set(Calendar.MILLISECOND, millisecond);
        return result;
    }

    @Test
    public void testFormatEpoch() {
        assertEquals("1970-01-01T00:00:00Z", formatDate(utc(1970, 1, 1, 0, 0, 0, 0), false));
        assertEquals("1970-01-01T00:00:00.000Z", formatDate(utc(1970, 1, 1, 0, 0, 0, 0), true));
    }

    @Test
    public void testFormatBefore1970() {
        assertEquals("1969-07-20T20:17:40Z", formatDate(utc(1969, 7, 20, 20, 17, 40, 0), false));
        assertEquals("1969-07-20T20:17:40.000Z", formatDate(utc(1969, 7, 20, 20, 17, 40, 0), true));
    }

    @Test
    public void testFormatLeapDay() {
        assertEquals("2024-02-29T12:00:00Z", formatDate(utc(2024, 2, 29, 12, 0, 0, 0), false));
        assertEquals("2024-02-29T12:00:00.000Z", formatDate(utc(2024, 2, 29, 12, 0, 0, 0), true));
    }

    @Test
    public void testFormatMillisecondsArePaddedToThreeDigits() {
        assertEquals("2026-01-02T03:04:05.007Z", formatDate(utc(2026, 1, 2, 3, 4, 5, 7), true));
        assertEquals("2026-01-02T03:04:05.123Z", formatDate(utc(2026, 1, 2, 3, 4, 5, 123), true));
    }

    @Test
    public void testFormatWithoutMillisecondsDropsThem() {
        assertEquals("2026-01-02T03:04:05Z", formatDate(utc(2026, 1, 2, 3, 4, 5, 123), false));
    }

    @Test
    public void testFormatYearIsAlwaysFourDigits() {
        assertEquals("0042-01-01T00:00:00Z", formatDate(utc(42, 1, 1, 0, 0, 0, 0), false));
    }

    @Test
    public void testParseAndFormatRoundTripInUtc() {
        String[] texts = {"1970-01-01T00:00:00Z", "1969-07-20T20:17:40Z", "2024-02-29T12:00:00Z",
                "2026-01-02T03:04:05Z", "0042-01-01T00:00:00Z"};
        for (String text : texts) {
            Calendar parsed = parseDate(text);
            assertNotNull(parsed, text);
            assertEquals(text, formatDate(parsed, false), text);
        }
    }

    @Test
    public void testParseAcceptsSpaceInsteadOfT() {
        Calendar withT = parseDate("2026-09-23T08:05:03Z");
        Calendar withSpace = parseDate("2026-09-23 08:05:03Z");
        assertNotNull(withT);
        assertNotNull(withSpace);
        assertEquals(withT.getTimeInMillis(), withSpace.getTimeInMillis());
    }

    @Test
    public void testParseAcceptsFewerThanThreeFractionalDigits() {
        Calendar actual = parseDate("2026-09-23T08:05:03.5Z");
        assertNotNull(actual);
        assertEquals(500, actual.get(Calendar.MILLISECOND));
    }

    @Test
    public void testParseAcceptsEasternArabicDigits() {
        // files written by builds affected by the DecimalFormat locale defect exist in the wild;
        // parsing must keep accepting them (#189, #190 decision 3)
        Calendar actual = parseDate("\u0662\u0660\u0662\u0666-\u0660\u0669-\u0662\u0663T\u0660\u0668:\u0660\u0665:\u0660\u0663Z");
        assertNotNull(actual);
        assertEquals("2026-09-23T08:05:03Z", formatDate(actual, false));
    }

    @Test
    public void testParseHandlesTheEraBoundary() {
        Calendar beforeChrist = parseDate("-0001-01-01T00:00:00Z");
        assertNotNull(beforeChrist);
        assertEquals("-0001-01-01T00:00:00Z", formatDate(beforeChrist, false));

        Calendar yearZero = parseDate("0000-01-01T00:00:00Z");
        assertNotNull(yearZero);
        assertEquals("0000-01-01T00:00:00Z", formatDate(yearZero, false));
    }

    @Test
    public void testParseReturnsNullForUnparseableInput() {
        assertNull(parseDate(null));
        assertNull(parseDate(""));
        assertNull(parseDate("garbage"));
        assertNull(parseDate("2026-13-01T00:00:00Z"));
        assertNull(parseDate("2026/09/23T08:05:03Z"));
    }

    // ---- zone offsets, corrected by the java.time migration (#190) ----

    private static Calendar at(String zone, int year, int month, int day, int hour, int minute) {
        Calendar result = Calendar.getInstance(TimeZone.getTimeZone(zone));
        result.clear();
        result.set(year, month - 1, day, hour, minute, 0);
        return result;
    }

    @Test
    public void testFormatNegativeOffsetWritesASingleMinusSign() {
        // before the migration this produced the unparseable "+-05:00"
        assertEquals("2026-06-01T12:00:00-05:00", formatDate(at("GMT-05:00", 2026, 6, 1, 12, 0), false));
        assertEquals("2026-06-01T12:00:00-11:00", formatDate(at("Pacific/Niue", 2026, 6, 1, 12, 0), false));
    }

    @Test
    public void testFormatPositiveOffsetIsUnchanged() {
        assertEquals("2026-06-01T12:00:00+05:30", formatDate(at("GMT+05:30", 2026, 6, 1, 12, 0), false));
        assertEquals("2026-06-01T12:00:00+13:00", formatDate(at("Pacific/Tongatapu", 2026, 6, 1, 12, 0), false));
    }

    @Test
    public void testFormatUsesTheOffsetInEffectAtThatInstantNotTheRawOffset() {
        // Berlin is +01:00 in winter and +02:00 in summer; the raw offset is +01:00 all year
        assertEquals("2026-01-15T12:00:00+01:00", formatDate(at("Europe/Berlin", 2026, 1, 15, 12, 0), false));
        assertEquals("2026-07-15T12:00:00+02:00", formatDate(at("Europe/Berlin", 2026, 7, 15, 12, 0), false));
    }

    @Test
    public void testOffsetsRoundTripToTheSameInstant() {
        String[] zones = {"GMT-05:00", "Pacific/Niue", "GMT+05:30", "Pacific/Tongatapu", "Europe/Berlin"};
        for (String zone : zones) {
            Calendar original = at(zone, 2026, 7, 15, 12, 0);
            Calendar parsed = parseDate(formatDate(original, false));
            assertNotNull(parsed, zone);
            assertEquals(original.getTimeInMillis(), parsed.getTimeInMillis(), zone);
        }
    }

    @Test
    public void testUtcStillWritesZ() {
        assertEquals("2026-07-15T12:00:00Z", formatDate(utc(2026, 7, 15, 12, 0, 0, 0), false));
    }

    @Test
    public void testParseGMT() {
        Calendar actual = parseDate("2007-03-04T14:49:05Z");
        Calendar expected = calendar(2007, 3, 4, 14, 49, 5).getCalendar();
        assert actual != null;
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testParseTimeZoneSeparatedByT() {
        Calendar actual = parseDate("2007-03-04T14:49:05T03:00");
        Calendar expected = calendar(2007, 3, 4, 11, 49, 5).getCalendar();
        assert actual != null;
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testParseTimeZoneSeparatedByPlus() {
        Calendar actual = parseDate("2007-03-04T14:49:05+03:00");
        Calendar expected = calendar(2007, 3, 4, 11, 49, 5).getCalendar();
        assert actual != null;
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testParseTimeZoneSeparatedByMinus() {
        Calendar actual = parseDate("2007-03-04T14:49:05-03:00");
        Calendar expected = calendar(2007, 3, 4, 17, 49, 5).getCalendar();
        assert actual != null;
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testFormatGMT() {
        String string = "2007-03-04T14:49:05Z";
        Calendar actual = parseDate(string);
        Calendar expected = calendar(2007, 3, 4, 14, 49, 5).getCalendar();
        assertEquals(string, formatDate(actual, false));
        assertEquals(formatDate(expected, false), formatDate(actual, false));
        assertEquals(formatDate(expected, true), formatDate(actual, true));
    }

    @Test
    public void testFormatTimeZone() {
        String string = "2007-03-04T14:49:05+03:30";
        Calendar actual = parseDate(string);
        Calendar expected = calendar(2007, 3, 4, 14, 49, 5).getCalendar();
        String[] ids = TimeZone.getAvailableIDs((3 * 3600 + 30 * 60) * 1000);
        expected.setTimeZone(TimeZone.getTimeZone(ids[0]));
        assertEquals(string, formatDate(actual, false));
    }

    @Test
    public void testFormatWithMilliSeconds1() {
        Calendar actual = parseDate("2010-09-18T03:13:32.2Z");
        Calendar expected = calendar(2010, 9, 18, 3, 13, 32, 200).getCalendar();
        assertEquals(formatDate(expected, true), formatDate(actual, true));
    }

    @Test
    public void testFormatWithMilliSeconds2() {
        Calendar actual = parseDate("2010-09-18T03:13:32.29Z");
        Calendar expected = calendar(2010, 9, 18, 3, 13, 32, 290).getCalendar();
        assertEquals(formatDate(expected, true), formatDate(actual, true));
    }

    @Test
    public void testFormatWithMilliSeconds3() {
        Calendar actual = parseDate("2010-09-18T03:13:32.293Z");
        Calendar expected = calendar(2010, 9, 18, 3, 13, 32, 293).getCalendar();
        assertEquals(formatDate(expected, true), formatDate(actual, true));
    }

    @Test
    public void testFormatWithSpaces() {
        Calendar actual = parseDate("2010-09-18 03:13:32Z");
        Calendar expected = calendar(2010, 9, 18, 3, 13, 32).getCalendar();
        assertEquals(formatDate(expected, true), formatDate(actual, true));
    }

    @Test
    public void testFormatWithoutTimezone() {
        Calendar actual = parseDate("2010-09-18 03:13:32");
        Calendar expected = calendar(2010, 9, 18, 3, 13, 32).getCalendar();
        assertEquals(formatDate(expected, true), formatDate(actual, true));
    }

    @Test
    public void testParseNullReturnsNull() {
        assertNull(parseDate(null));
    }

    @Test
    public void testParseMalformedReturnsNull() {
        assertNull(parseDate("not-a-date"));
        assertNull(parseDate("2007/03/04T14:49:05Z"));   // wrong date delimiter
        assertNull(parseDate("2007-03-04X14:49:05Z"));   // wrong date/time delimiter
        assertNull(parseDate("2007-03-04T14-49-05Z"));   // wrong time delimiter
        assertNull(parseDate("2007-03-04T14:49:05X"));   // unknown timezone designator
        assertNull(parseDate("2007-13-40T14:49:05Z"));   // out of range with lenient=false
    }

    @Test
    public void testFormatNullThrows() {
        try {
            formatDate((Calendar) null, false);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            formatDate((slash.common.type.CompactCalendar) null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void testFormatCompactCalendarConvenienceOverload() {
        assertEquals("2007-03-04T14:49:05Z", formatDate(calendar(2007, 3, 4, 14, 49, 5)));
    }

    @Test
    public void testParseAndFormatRoundTripAcrossTheEraBoundary() {
        // year 0000 denotes 1 BCE: parse sets the BC era, format renders it back to 0000
        Calendar actual = parseDate("0000-01-01T00:00:00Z");
        assertNotNull(actual);
        assertEquals("0000-01-01T00:00:00Z", formatDate(actual, false));
    }

    @Test
    public void testFormatUsesAsciiDigitsWhateverTheDefaultLocaleAndClassInitOrder() {
        // replaces the reflective pin on the former DecimalFormat fields (#189). The migration
        // passes Locale.ROOT to String.format on every call instead of capturing symbols in a
        // static formatter at class-init, so this no longer depends on which test loads ISO8601
        // first - the earlier structural test existed only because that ordering mattered.
        Locale previousLocale = Locale.getDefault();
        try {
            for (String tag : new String[]{"ar", "ar-EG", "fa-IR", "hi-IN", "en-US"}) {
                Locale.setDefault(Locale.forLanguageTag(tag));
                assertEquals("2026-01-02T03:04:05Z", formatDate(utc(2026, 1, 2, 3, 4, 5, 0), false), tag);
                assertEquals("2026-01-02T03:04:05.007Z", formatDate(utc(2026, 1, 2, 3, 4, 5, 7), true), tag);
            }
        } finally {
            Locale.setDefault(previousLocale);
        }
    }

    @Test
    public void testFormatUsesAsciiDigitsUnderAnArabicLocale() {
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"));
            assertEquals("2026-09-19T08:05:03Z", formatDate(calendar(2026, 9, 19, 8, 5, 3)));
        } finally {
            Locale.setDefault(previousLocale);
        }
    }

    @Test
    public void testFormatUsesAsciiDigitsUnderAPersianLocale() {
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("fa-IR"));
            assertEquals("2026-09-19T08:05:03Z", formatDate(calendar(2026, 9, 19, 8, 5, 3)));
        } finally {
            Locale.setDefault(previousLocale);
        }
    }

    @Test
    public void testFormatStillParsesBackAfterAnArabicLocaleRoundTrip() {
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"));
            String formatted = formatDate(calendar(2026, 9, 19, 8, 5, 3));
            assertEquals("2026-09-19T08:05:03Z", formatted);
            Calendar parsed = parseDate(formatted);
            assertNotNull(parsed);
            assertEquals(formatted, formatDate(parsed, false));
        } finally {
            Locale.setDefault(previousLocale);
        }
    }
}
