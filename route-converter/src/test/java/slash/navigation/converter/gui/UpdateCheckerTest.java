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

package slash.navigation.converter.gui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UpdateCheckerTest {
    private UpdateChecker.UpdateResult result = new UpdateChecker.UpdateResult("2.2", "1.5");

    @Test
    public void testSetParameters() {
        result.setParameters("b=c,routeconverter.version=1.3,a=b");
        assertEquals("c", result.getValue("b"));
        assertEquals("1.3", result.getValue("routeconverter.version"));
        assertEquals("b", result.getValue("a"));
        assertNull( result.getValue("c"));
    }

    @Test
    public void testParseVersion() {
        result.setParameters("b=c,routeconverter.version=1.3,java.version=2,a=b");
        assertEquals("2.2", result.getMyRouteConverterVersion());
        assertEquals("1.3", result.getLatestRouteConverterVersion());
        assertFalse(result.existsLaterRouteConverterVersion());
        assertEquals("1.5", result.getMyJavaVersion());
        assertEquals("2", result.getLatestJavaVersion());
        assertTrue(result.existsLaterJavaVersion());
    }
}
