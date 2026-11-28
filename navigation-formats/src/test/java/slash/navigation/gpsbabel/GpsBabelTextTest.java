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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.navigation.gpsbabel.GpsBabelText.canEncodeCp1252;
import static slash.navigation.gpsbabel.GpsBabelText.csvLineParse;
import static slash.navigation.gpsbabel.GpsBabelText.decodeCp1252;
import static slash.navigation.gpsbabel.GpsBabelText.decodeUtf8OrCp1252;
import static slash.navigation.gpsbabel.GpsBabelText.encodeCp1252;
import static slash.navigation.gpsbabel.GpsBabelText.stripBom;
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

    @Test
    public void cleanFieldReplacesDelimitersLineBreaksTabAndIllegalXmlCharactersByASpace() {
        assertEquals("a b c d e f g h", GpsBabelText.cleanField("a,b;c\r\nd\te\0f\uFFFEg\uD800h", ",;"));
        assertEquals("a b  c", GpsBabelText.cleanField("a\nb\n\nc", ""));
        assertEquals("a;b", GpsBabelText.cleanField("a;b", ","));
        assertEquals("", GpsBabelText.cleanField(null, ","));
        assertEquals("\uD83D\uDE00 x", GpsBabelText.cleanField("\uD83D\uDE00 x", ","));
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++)
            result[i] = (byte) values[i];
        return result;
    }

    @Test
    public void testStripBom() {
        assertArrayEquals(bytes('a'), stripBom(bytes(0xEF, 0xBB, 0xBF, 'a')));
        assertArrayEquals(bytes(), stripBom(bytes(0xEF, 0xBB, 0xBF)));
        assertArrayEquals(bytes(0xEF, 0xBB, 'a'), stripBom(bytes(0xEF, 0xBB, 'a')));
        assertArrayEquals(bytes(0xFF, 0xFE, 'a'), stripBom(bytes(0xFF, 0xFE, 'a')));
        assertArrayEquals(bytes(), stripBom(bytes()));
    }

    @Test
    public void testDecodeUtf8OrCp1252() {
        assertEquals("M\u00fcller \u20ac", decodeUtf8OrCp1252("M\u00fcller \u20ac".getBytes(UTF_8)));
        assertEquals("M\u00fcller \u20ac", decodeUtf8OrCp1252(bytes('M', 0xFC, 'l', 'l', 'e', 'r', ' ', 0x80)));
        assertEquals("ab", decodeUtf8OrCp1252(bytes(0xEF, 0xBB, 0xBF, 'a', 'b')));
        assertEquals("\u00fc", decodeUtf8OrCp1252(bytes(0xEF, 0xBB, 0xBF, 0xFC)));
        assertEquals("plain", decodeUtf8OrCp1252("plain".getBytes(UTF_8)));
        assertEquals("", decodeUtf8OrCp1252(bytes()));
    }

    @Test
    public void testCp1252HolesRoundTrip() {
        byte[] all = new byte[256];
        for (int i = 0; i < all.length; i++)
            all[i] = (byte) i;
        String text = decodeCp1252(all, 0, all.length);
        assertEquals(256, text.length());
        assertEquals('\u0081', text.charAt(0x81));
        assertEquals('\u009D', text.charAt(0x9D));
        assertEquals('\u20AC', text.charAt(0x80));
        assertEquals((char) 0x1A, text.charAt(0x1A));
        assertArrayEquals(all, encodeCp1252(text));
        assertTrue(canEncodeCp1252(text));
        assertEquals("\u0090", decodeCp1252(bytes('x', 0x90, 'y'), 1, 1));
    }

    @Test
    public void testEncodeCp1252ReplacesOtherCharacters() {
        assertArrayEquals(bytes('a', '?', 0x81, '?', 'b'), encodeCp1252("a\u017C\u0081\uD83D\uDE00b"));
        assertFalse(canEncodeCp1252("\u017C\u00F3\u0142\u0107"));
        assertFalse(canEncodeCp1252("\uD83D\uDE00"));
        assertTrue(canEncodeCp1252("M\u00fcller \u20ac \u0081"));
    }
}
