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

package slash.navigation.rest;

import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;

import static java.util.TimeZone.getTimeZone;
import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.calendar;
import static slash.navigation.rest.RFC2616.formatDate;
import static slash.navigation.rest.RFC2616.parseDate;

public class RFC2616Test {
    private static final String DATESTRING = "Sun, 06 Nov 1994 08:49:37 GMT";

    @Test
    public void testParseRfc822Updated1123() throws ParseException {
        Calendar actual = parseDate(DATESTRING);
        Calendar expected = calendar(1994, 11, 6, 9, 49, 37, 0, "GMT+1").getCalendar();
        assertEquals(getTimeZone("GMT"), actual.getTimeZone());
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testFormat() {
        Calendar expectedCalendar = calendar(1994, 11, 6, 9, 49, 37, 0, "GMT+1").getCalendar();
        String actual = formatDate(expectedCalendar.getTimeInMillis());
        assertEquals(DATESTRING, actual);
    }
}
