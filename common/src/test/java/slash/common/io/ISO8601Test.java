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

package slash.common.io;

import slash.common.TestCase;

import java.util.Calendar;

public class ISO8601Test extends TestCase {
    public void testParse() {
        Calendar actual = ISO8601.parse("2007-03-04T14:49:05Z");
        Calendar expected = calendar(2007, 3, 4, 14, 49, 5).getCalendar();
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());
    }

    public void testFormat() {
        Calendar actual = ISO8601.parse("2007-03-04T14:49:05Z");
        Calendar expected = calendar(2007, 3, 4, 14, 49, 5).getCalendar();
        assertEquals(ISO8601.format(expected), ISO8601.format(actual));
    }
}
