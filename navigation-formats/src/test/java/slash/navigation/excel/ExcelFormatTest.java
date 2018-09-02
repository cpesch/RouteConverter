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
package slash.navigation.excel;

import org.junit.Test;
import slash.common.type.CompactCalendar;

import static junit.framework.TestCase.assertEquals;
import static slash.common.TestCase.calendar;

public class ExcelFormatTest {
    @Test
    public void testSetAndGetTime() {
        CompactCalendar time = calendar(2018, 1, 21, 19, 35, 44, 33);
        ExcelPosition position = new ExcelPosition(1.0, 2.0, 3.0, 4.0, time, "five");
        assertEquals(time, position.getTime());
    }
}
