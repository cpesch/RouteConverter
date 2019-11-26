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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.io.Transfer.*;

public class TransferTest {
    @Test
    public void testCeiling() {
        assertEquals(3, ceiling(184, 90, true));
        assertEquals(1, ceiling(0, 1, true));
        assertEquals(3, ceiling(184, 90, false));
        assertEquals(0, ceiling(0, 1, false));
        assertEquals(0, ceiling(0, 20, false));
        assertEquals(1, ceiling(1, 20, false));
    }

    @Test
    public void testRoundFraction() {
        assertDoubleEquals(1.0, roundFraction(1.1, 0));
        assertDoubleEquals(1.1, roundFraction(1.1, 1));
        assertDoubleEquals(11.0, roundFraction(11.1, 0));
        assertDoubleEquals(11.1, roundFraction(11.1, 1));
        assertDoubleEquals(1.004, roundFraction(1.004, 3));
        assertDoubleEquals(1.004, roundFraction(1.0044, 3));
        assertDoubleEquals(1.005, roundFraction(1.0045, 3));
        assertDoubleEquals(1.005, roundFraction(1.005, 3));
    }

    @Test
    public void testFormatDoubleAsString() {
        assertEquals("0.001", formatDoubleAsString(0.001));
        assertEquals("0.0001", formatDoubleAsString(0.0001));
        assertEquals("0.00001", formatDoubleAsString(0.00001));
        assertEquals("0.000001", formatDoubleAsString(0.000001));
        assertEquals("0.0000001", formatDoubleAsString(0.0000001));

        assertEquals("1.00000", formatDoubleAsString(1.0, 5));
        assertEquals("1.50000", formatDoubleAsString(1.5, 5));
        assertEquals("1.05000", formatDoubleAsString(1.05, 5));
        assertEquals("1.00500", formatDoubleAsString(1.005, 5));
        assertEquals("1.00005", formatDoubleAsString(1.00005, 5));
        assertEquals("1.00000", formatDoubleAsString(1.000005, 5));
        assertEquals("1.00000", formatDoubleAsString(1.0000005, 5));
    }

    @Test
    public void testFormatIntAsString() {
        assertEquals("1", formatIntAsString(1, 1));
        assertEquals("01", formatIntAsString(1, 2));
        assertEquals("001", formatIntAsString(1, 3));

        assertEquals("100", formatIntAsString(100, 1));
        assertEquals("100", formatIntAsString(100, 2));
        assertEquals("100", formatIntAsString(100, 3));
        assertEquals("0100", formatIntAsString(100, 4));
        assertEquals("00100", formatIntAsString(100, 5));
    }

    @Test
    public void testWidthInDigits() {
        assertEquals(1, widthInDigits(1));
        assertEquals(3, widthInDigits(123));
        assertEquals(5, widthInDigits(12345));
    }

    private Double parseDoubleAndAssertNotNull(String aDouble) {
        Double result = parseDouble(aDouble);
        assertNotNull(result);
        return result;
    }

    @Test
    public void testParseStringAsDouble() {
        assertDoubleEquals(1.0, parseDoubleAndAssertNotNull("1.0"));
        assertDoubleEquals(1.0, parseDoubleAndAssertNotNull("01.0"));
        assertDoubleEquals(1.0, parseDoubleAndAssertNotNull("1.00"));

        assertDoubleEquals(0.00001, parseDoubleAndAssertNotNull("0.00001"));
        assertDoubleEquals(0.00001, parseDoubleAndAssertNotNull("0.1E-4"));
        assertDoubleEquals(0.000001, parseDoubleAndAssertNotNull("0.1E-5"));
    }

    @Test
    public void testFormatDuration() {
        assertEquals("00:00:05", formatDuration(5 * 1000));
        assertEquals("00:05:05", formatDuration((5 * 60 + 5) * 1000));
        assertEquals("05:05:05", formatDuration((5 * 60 * 60 + 5 * 60 + 5) * 1000));
        assertEquals(formatDuration((25 * 60 * 60 + 5 * 60 + 5) * 1000), "25:05:05");
        assertEquals("125:05:05", formatDuration((125 * 60 * 60 + 5 * 60 + 5) * 1000));
    }

    @Test
    public void testToLettersAndNumbers() {
        assertEquals("abc", toLettersAndNumbers("a b/c"));
        assertEquals("A1b2c", toLettersAndNumbers("A+1+b % 2 * c"));
    }

    @Test
    public void testToLettersAndNumbersAndSpaces() {
        assertEquals("a bc", toLettersAndNumbersAndSpaces("a b/c"));
        assertEquals("A1b 2 c", toLettersAndNumbersAndSpaces("A+1+b % 2 * c"));
    }

    @Test
    public void testEncodeFileName() {
        String original = ".A/B\\C:D.äöüß";
        String expected = "%2eA%2fB%5cC%3aD.äöüß";
        assertEquals(expected, encodeFileName(original));
        assertEquals(original, decodeUri(expected));
    }
}