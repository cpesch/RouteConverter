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

public class QstarzTest {
    private Kml20Format format = new Kml20Format();
    private static final String QSTARZ_DESCRIPTION = "<description>\n" +
            "      <![CDATA[Date: 2010/12/28<br />Time: 23:01:39<br />Latitude: 49.126387 N<br />Longitude: 8.613990 E<br />Speed: 0.561 km/h<br />DISTANCE:      0.66 m<br />]]>\n" +
            "</description>";

    @Test
    public void testParseTime() {
        KmlPosition position = new KmlPosition(null, null, null, null, null, null);
        CompactCalendar expectedCal = calendar(2010, 12, 28, 23, 1, 39);
        format.parseTime(position, QSTARZ_DESCRIPTION, null);
        CompactCalendar actualCal = position.getTime();
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        String actual = DateFormat.getDateTimeInstance().format(actualCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, actualCal);
    }

    @Test
    public void testParseSpeed() {
        assertDoubleEquals(0.561, format.parseSpeed(QSTARZ_DESCRIPTION));
    }
}
