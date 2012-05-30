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

package slash.navigation.kml;

import org.junit.Test;
import slash.common.type.CompactCalendar;

import java.text.DateFormat;

import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;

public class Bt747Test {
    private Kml20Format format = new Kml20Format();
    private static final String BT747_NAME = "TIME: 10:11:56; &lt;table width=400&gt;&lt;tr&gt;&lt;td&gt;Index:&lt;/td&gt;&lt;td&gt;7643&lt;/td&gt;&lt;/tr&gt;&lt;tr&gt;&lt;td&gt;Zeit:&lt;/td&gt;&lt;td&gt;05-September-10 10:11:56&lt;/td&gt;&lt;/tr&gt;&lt;tr&gt;&lt;td&gt;Geographische Breite:&lt;/td&gt;&lt;td&gt;49.385769 N&lt;/td&gt;&lt;/tr&gt;&lt;tr&gt;&lt;td&gt;Geografische L&amp;auml;nge:&lt;/td&gt;&lt;td&gt;8.572565 E&lt;/td&gt;&lt;/tr&gt;&lt;tr&gt;&lt;td&gt;H&amp;ouml;he:&lt;/td&gt;&lt;td&gt;102.109 m&lt;/td&gt;&lt;/tr&gt;&lt;/table&gt;".replaceAll("&gt;", ">").replaceAll("&lt;", "<");

    @Test
    public void testParseTime() {
        KmlPosition position = new KmlPosition(null, null, null, null, null, null);
        CompactCalendar expectedCal = calendar(2010, 9, 5, 10, 11, 56);
        format.parseTime(position, BT747_NAME, null);
        CompactCalendar actualCal = position.getTime();
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        String actual = DateFormat.getDateTimeInstance().format(actualCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, actualCal);
    }

    @Test
    public void testParseElevation() {
        assertDoubleEquals(102.109, format.parseElevation(BT747_NAME));
    }
}
