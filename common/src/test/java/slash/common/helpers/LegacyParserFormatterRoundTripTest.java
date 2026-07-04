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

import org.junit.Test;
import slash.common.type.CompactCalendar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.fail;
import static slash.common.helpers.LegacyParserFormatter.ParserType.DATE;
import static slash.common.helpers.LegacyParserFormatter.ParserType.DATETIME;
import static slash.common.helpers.LegacyParserFormatter.ParserType.TIME;

/**
 * Guards the round-trip invariant: whatever {@link LegacyParserFormatter#format} produces
 * for a given locale, {@link LegacyParserFormatter#parse} must accept back.
 * <p>
 * This broke when the parser was rebuilt via {@code new SimpleDateFormat(toLocalizedPattern())}:
 * a localized pattern handed to a constructor that expects a non-localized one, with the
 * formatter's locale dropped. In the table's date/time cells that surfaced as "Could not
 * recognize ... does not match the required format" for a value the app had just displayed.
 *
 * @author Christian Pesch
 */
public class LegacyParserFormatterRoundTripTest {
    private static final CompactCalendar SAMPLE = CompactCalendar.fromMillisAndTimeZone(1717840000000L, "UTC");

    @Test
    public void everyLocaleRoundTripsItsOwnOutput() {
        List<String> failures = new ArrayList<>();
        for (LegacyParserFormatter.ParserType type : new LegacyParserFormatter.ParserType[]{TIME, DATE, DATETIME}) {
            for (Locale locale : Locale.getAvailableLocales()) {
                LegacyParserFormatter formatter = new LegacyParserFormatter(type, () -> locale);
                formatter.setZone("UTC");

                String formatted;
                try {
                    formatted = formatter.format(SAMPLE);
                } catch (RuntimeException e) {
                    // formatter cannot render this locale at all -- not what we guard here
                    continue;
                }

                try {
                    formatter.parse(formatted, SAMPLE);
                } catch (Exception e) {
                    failures.add(type + " / " + locale + " formatted '" + formatted + "' but parse failed: " + e);
                }
            }
        }
        if (!failures.isEmpty())
            fail("formatted output that could not be parsed back (" + failures.size() + "):\n" + String.join("\n", failures));
    }
}
