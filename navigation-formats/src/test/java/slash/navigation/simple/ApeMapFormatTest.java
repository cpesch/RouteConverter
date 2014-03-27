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

package slash.navigation.simple;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.Wgs84Position;

import java.text.DateFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.utcCalendar;

public class ApeMapFormatTest {
    private ApeMapFormat format = new ApeMapFormat();

    @Test
    public void testGetExtension() {
        assertEquals(".trk", format.getExtension());
    }

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("(47.13336181640625,15.496421813964844,401.0,1370092005)"));
        assertTrue(format.isValidLine("(47.13031005859375,15.495400428771973,429.0,1370092161)"));
        assertTrue(format.isValidLine("(47.13031005859375,15.495400428771973,429.0,1370092161)         "));
        assertTrue(format.isValidLine("[track]"));
        assertTrue(format.isValidLine("[track]     "));
        assertTrue(format.isValidLine("(47.14454650878906,15.500686645507813)"));
        assertTrue(format.isValidLine("(47.13923645019531,15.501678466796875)"));
        assertTrue(format.isValidLine("(47.13923645019531,15.501678466796875)          "));
        assertTrue(format.isValidLine("--start--"));
        assertTrue(format.isValidLine("(47.070213317871094,15.496063232421875,437.0,1370785949)"));
        assertTrue(format.isValidLine("(47.067962646484375,15.495849609375,405.0,1370788358);#CMDNewSegment"));
        assertTrue(format.isValidLine("(47.06767654418945,15.495771408081055,407.0,1370788454)"));
        assertTrue(format.isValidLine("(47.06760787963867,15.495786666870117,407.0,1370790160)"));
        assertTrue(format.isValidLine("(47.070228576660156,15.496109962463379,407.0,1370790161);#CMDNewSegment"));
        assertTrue(format.isValidLine("(47.070186614990234,15.496094703674316,424.0,1370790212)"));
        assertTrue(format.isValidLine("(47.070186614990234,15.496094703674316,471.3,1370790216)"));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("(47.13336181640625,15.496421813964844,401.0,1370092005)"));
        assertFalse(format.isPosition("[track]"));
        assertFalse(format.isPosition("--start--"));
        assertFalse(format.isPosition("(47.14454650878906,15.500686645507813)"));
        assertFalse(format.isPosition("(47.13923645019531,15.501678466796875)"));
    }

    @Test
    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("(47.13336181640625,15.496421813964844,401.0,1370092005)", null);
        assertDoubleEquals(15.496421813964844, position.getLongitude());
        assertDoubleEquals(47.13336181640625, position.getLatitude());
        assertDoubleEquals(401.0, position.getElevation());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = utcCalendar(1370092005000L);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertNull(position.getDescription());
    }
}
