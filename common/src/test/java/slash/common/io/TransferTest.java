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

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.assertDoubleEquals;

public class TransferTest {
    @Test
    public void testCeiling() {
        assertEquals(3, Transfer.ceiling(184, 90, true));
        assertEquals(1, Transfer.ceiling(0, 1, true));
        assertEquals(3, Transfer.ceiling(184, 90, false));
        assertEquals(0, Transfer.ceiling(0, 1, false));
        assertEquals(0, Transfer.ceiling(0, 20, false));
        assertEquals(1, Transfer.ceiling(1, 20, false));
    }

    @Test
    public void testRoundFraction() {
        assertDoubleEquals(1.0, Transfer.roundFraction(1.1, 0));
        assertDoubleEquals(1.1, Transfer.roundFraction(1.1, 1));
        assertDoubleEquals(11.0, Transfer.roundFraction(11.1, 0));
        assertDoubleEquals(11.1, Transfer.roundFraction(11.1, 1));
        assertDoubleEquals(1.004, Transfer.roundFraction(1.004, 3));
        assertDoubleEquals(1.004, Transfer.roundFraction(1.0044, 3));
        assertDoubleEquals(1.005, Transfer.roundFraction(1.0045, 3));
        assertDoubleEquals(1.005, Transfer.roundFraction(1.005, 3));
    }

    @Test
    public void testFormatDoubleAsString() {
        assertEquals("0.001", Transfer.formatDoubleAsString(0.001));
        assertEquals("0.0001", Transfer.formatDoubleAsString(0.0001));
        assertEquals("0.00001", Transfer.formatDoubleAsString(0.00001));
        assertEquals("0.000001", Transfer.formatDoubleAsString(0.000001));
        assertEquals("0.0000001", Transfer.formatDoubleAsString(0.0000001));

        assertEquals("1.00000", Transfer.formatDoubleAsString(1.0, 5));
        assertEquals("1.50000", Transfer.formatDoubleAsString(1.5, 5));
        assertEquals("1.05000", Transfer.formatDoubleAsString(1.05, 5));
        assertEquals("1.00500", Transfer.formatDoubleAsString(1.005, 5));
        assertEquals("1.00005", Transfer.formatDoubleAsString(1.00005, 5));
        assertEquals("1.00000", Transfer.formatDoubleAsString(1.000005, 5));
        assertEquals("1.00000", Transfer.formatDoubleAsString(1.0000005, 5));
    }

    @Test
    public void testFormatIntAsString() {
        assertEquals("1", Transfer.formatIntAsString(1, 1));
        assertEquals("01", Transfer.formatIntAsString(1, 2));
        assertEquals("001", Transfer.formatIntAsString(1, 3));

        assertEquals("100", Transfer.formatIntAsString(100, 1));
        assertEquals("100", Transfer.formatIntAsString(100, 2));
        assertEquals("100", Transfer.formatIntAsString(100, 3));
        assertEquals("0100", Transfer.formatIntAsString(100, 4));
        assertEquals("00100", Transfer.formatIntAsString(100, 5));
    }

    @Test
    public void testWidthInDigits() {
        assertEquals(1, Transfer.widthInDigits(1));
        assertEquals(3, Transfer.widthInDigits(123));
        assertEquals(5, Transfer.widthInDigits(12345));
    }

    @Test
    public void testParseStringAsDouble() {
        assertDoubleEquals(1.0, Transfer.parseDouble("1.0"));
        assertDoubleEquals(1.0, Transfer.parseDouble("01.0"));
        assertDoubleEquals(1.0, Transfer.parseDouble("1.00"));

        assertDoubleEquals(0.00001, Transfer.parseDouble("0.00001"));
        assertDoubleEquals(0.00001, Transfer.parseDouble("0.1E-4"));
        assertDoubleEquals(0.000001, Transfer.parseDouble("0.1E-5"));
    }

    @Test
    public void testFormatDoubleAsBigDecimal() {
        assertEquals(new BigDecimal("1.0"), Transfer.formatBigDecimal(1.0, 5));
        assertEquals(new BigDecimal("1.5"), Transfer.formatBigDecimal(1.5, 5));
        assertEquals(new BigDecimal("1.05"), Transfer.formatBigDecimal(1.05, 5));
        assertEquals(new BigDecimal("1.005"), Transfer.formatBigDecimal(1.005, 5));
        assertEquals(new BigDecimal("1.00004"), Transfer.formatBigDecimal(1.00004, 5));
        assertEquals(new BigDecimal("1.000044"), Transfer.formatBigDecimal(1.000044, 5));
        assertEquals(new BigDecimal("1.000045"), Transfer.formatBigDecimal(1.000045, 5));
        assertEquals(new BigDecimal("1.00005"), Transfer.formatBigDecimal(1.00005, 5));
        assertEquals(new BigDecimal("1.000004"), Transfer.formatBigDecimal(1.000004, 5));
        assertEquals(new BigDecimal("1.000005"), Transfer.formatBigDecimal(1.000005, 5));
    }

    @Test
    public void testFormatDuration() {
        assertEquals("00:00:05", Transfer.formatDuration(5 * 1000));
        assertEquals("00:05:05", Transfer.formatDuration((5 * 60 + 5) * 1000));
        assertEquals("05:05:05", Transfer.formatDuration((5 * 60 * 60 + 5 * 60 + 5) * 1000));
        assertEquals("25:05:05", Transfer.formatDuration((25 * 60 * 60 + 5 * 60 + 5) * 1000));
        assertEquals("125:05:05", Transfer.formatDuration((125 * 60 * 60 + 5 * 60 + 5) * 1000));
    }
}