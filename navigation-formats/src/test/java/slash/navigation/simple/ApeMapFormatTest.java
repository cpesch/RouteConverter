/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package slash.navigation.simple;

import java.text.DateFormat;

import org.junit.*;

import static org.junit.Assert.*;

import slash.common.type.CompactCalendar;
import slash.navigation.base.Wgs84Position;

import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.utcCalendar;

public class ApeMapFormatTest {
    ApeMapFormat format = new ApeMapFormat();

    @Test
    public void testGetExtension() {
        assertEquals(".trk", format.getExtension());
    }

    @Test
    public void testIsValidLine() {
        System.out.println("isValidLine");
        assertTrue(format.isValidLine("(47.13336181640625,15.496421813964844,401.0,1370092005)"));
        assertTrue(format.isValidLine("(47.13031005859375,15.495400428771973,429.0,1370092161)"));
        assertTrue(format.isValidLine("(47.13031005859375,15.495400428771973,429.0,1370092161)         "));
        assertTrue(format.isValidLine("[track]"));
        assertTrue(format.isValidLine("[track]     "));
        assertTrue(format.isValidLine("(47.14454650878906,15.500686645507813)"));
        assertTrue(format.isValidLine("(47.13923645019531,15.501678466796875)"));
        assertTrue(format.isValidLine("(47.13923645019531,15.501678466796875)          "));
        assertTrue(format.isValidLine("--start--"));
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
        assertNull(position.getComment());
    }
}
