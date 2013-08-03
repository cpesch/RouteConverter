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
package slash.navigation.base;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static slash.common.TestCase.calendar;

public class TextNavigationFormatTest {

    @Test
    public void testIsValidStartDate() {
        TextNavigationFormat format = mock(TextNavigationFormat.class, CALLS_REAL_METHODS);
        assertTrue(format.isValidStartDate(calendar(2013, 8, 2, 20, 48, 45)));
        assertFalse(format.isValidStartDate(calendar(1970, 1, 1, 2, 3, 45)));
    }
}
