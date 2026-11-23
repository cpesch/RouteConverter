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

package slash.navigation.gpsbabel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.navigation.gpsbabel.GpsBabelText.csvLineParse;
import static slash.navigation.gpsbabel.GpsBabelText.humanToDec;

public class GpsBabelTextTest {

    private void assertLatLon(double lat, double lon, double[] actual) {
        assertEquals(lat, actual[0], 1e-9);
        assertEquals(lon, actual[1], 1e-9);
    }

    @Test
    public void testHumanToDecHemisphereBefore() {
        assertEquals(48.1173, humanToDec("N48 07.038", 1)[0], 1e-9);
        assertTrue(Double.isNaN(humanToDec("N48 07.038", 1)[1]));
        assertEquals(-11.516666666, humanToDec("W011 31.000", 2)[1], 1e-8);
        assertTrue(Double.isNaN(humanToDec("W011 31.000", 2)[0]));
    }

    @Test
    public void testHumanToDecDegreesMinutesSecondsHemisphereAfter() {
        double expected = 48 + 7 / 60.0 + 2.3 / 3600.0;
        assertEquals(expected, humanToDec("48°07'02.3\"N", 1)[0], 1e-9);
        assertEquals(expected, humanToDec("48°07'02.3\"N", 0)[0], 1e-9);
        assertEquals(-expected, humanToDec("48 7 2.3 S", 0)[0], 1e-9);
    }

    @Test
    public void testHumanToDecDecimal() {
        assertEquals(48.1173, humanToDec("48.1173", 1)[0], 1e-9);
        assertEquals(48.1173, humanToDec("48.1173", 2)[1], 1e-9);
        assertEquals(-48.1173, humanToDec("-48.1173", 1)[0], 1e-9);
        assertEquals(48.1173, humanToDec("48,1173", 1)[0], 1e-9);
        assertTrue(Double.isNaN(humanToDec("48.1173", 0)[0]));
        assertTrue(Double.isNaN(humanToDec("48.1173", 0)[1]));
    }

    @Test
    public void testHumanToDecWithDegreeSignAndHemisphereAfter() {
        assertEquals(52.1234567, humanToDec("52.1234567ºN", 1)[0], 1e-9);
        assertEquals(52.1234567, humanToDec("52.1234567°N", 0)[0], 1e-9);
    }

    @Test
    public void testHumanToDecBoth() {
        assertLatLon(41 + 9.12 / 60.0, -(85 + 9.36 / 60.0), humanToDec("N 41° 09.12' W 085° 09.36'", 0));
        assertLatLon(41 + 9.12 / 60.0, 85 + 9.36 / 60.0, humanToDec("41° 09.12' N 085° 09.36' E", 0));
        assertLatLon(48.1173, 11.5, humanToDec("48.1173 N 11.5 E", 0));
    }

    @Test
    public void testHumanToDecNmeaStyle() {
        assertEquals(48.1173, humanToDec("4807.038N", 1)[0], 1e-9);
    }

    @Test
    public void testHumanToDecNothing() {
        double[] result = humanToDec("", 0);
        assertTrue(Double.isNaN(result[0]));
        assertTrue(Double.isNaN(result[1]));
    }

    @Test
    public void testCsvLineParse() {
        assertEquals(asList("a", "b", "", "c,d"), csvLineParse("a,b,,\"c,d\"", ",", "\""));
    }

    @Test
    public void testCsvLineParseEmptyFieldsKept() {
        assertEquals(asList("a", ""), csvLineParse("a,", ",", "\""));
        assertEquals(asList("", "a"), csvLineParse(",a", ",", "\""));
        assertEquals(asList(""), csvLineParse("", ",", "\""));
        assertEquals(asList("", "", ""), csvLineParse(",,", ",", "\""));
    }

    @Test
    public void testCsvLineParseCommaSpaceDelimiter() {
        // like GPSBabel: the delimiter is just the comma, callers trim the blank
        assertEquals(asList("a", " b", " c"), csvLineParse("a, b, c", ", ", "\""));
    }

    @Test
    public void testCsvLineParseEnclosureAndEscapedEnclosure() {
        assertEquals(asList("x", "say hi", ""), csvLineParse("x,\"say hi\",\"\"", ",", "\""));
        assertEquals(asList("a;b", "c"), csvLineParse("\"a;b\";c", ";", "\""));
    }

    @Test
    public void testCsvLineParseMultiCharacterDelimiter() {
        List<String> fields = csvLineParse("a||b||c", "||", "\"");
        assertEquals(asList("a", "b", "c"), fields);
    }

    @Test
    public void testCsvLineParseWhitespaceDelimiter() {
        assertEquals(asList("a ", "b\t", "c"), csvLineParse("a b\tc", "\\w", "\""));
    }
}
