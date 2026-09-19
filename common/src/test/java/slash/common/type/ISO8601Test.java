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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static slash.common.TestCase.calendar;
import static slash.common.type.ISO8601.formatDate;
import static slash.common.type.ISO8601.parseDate;

public class ISO8601Test {

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
    public void testFormatDigitSymbolsArePinnedToRootLocale() throws Exception {
        // structural pin, independent of the default locale and of class-init order: the
        // shared DecimalFormat fields must stay package-private (the seam this test reads,
        // re-hiding them fails the modifiers check) and must use Locale.ROOT symbols, whose
        // zero digit is the ASCII '0'. The locale is set to ar first, so in a fresh JVM where
        // this class is the first to use ISO8601, fields taking their symbols from the
        // default locale capture '٠' and fail this. Reflection is used on purpose: it pins
        // the fields' state without depending on compile-time visibility.
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"));
            for (String name : new String[]{"XX_FORMAT", "XXX_FORMAT", "XXXX_FORMAT"}) {
                Field field = ISO8601.class.getDeclaredField(name);
                assertFalse(Modifier.isPrivate(field.getModifiers()),
                        name + " must stay package-private so this pin can hold it in place");
                field.setAccessible(true);
                DecimalFormat format = (DecimalFormat) field.get(null);
                assertEquals('0', format.getDecimalFormatSymbols().getZeroDigit());
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
