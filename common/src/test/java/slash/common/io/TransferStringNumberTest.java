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

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static slash.common.io.Transfer.*;

/**
 * Covers the pure string and number helpers of {@link Transfer} that the existing
 * {@code TransferTest} leaves untouched.
 *
 * @author Christian Pesch
 */
public class TransferStringNumberTest {

    @Test
    public void trimNullsOutBlanksAndTrimsWhitespace() {
        assertNull(trim(null));
        assertNull(trim("   "));
        assertEquals("x", trim("  x  "));
    }

    @Test
    public void trimWithLengthCapsTheResult() {
        assertEquals("hel", trim("hello", 3));
        assertEquals("hi", trim("hi", 10));
        assertNull(trim("   ", 3));
    }

    @Test
    public void parseIntegerStripsLeadingPlusAndTrims() {
        assertEquals(Integer.valueOf(5), parseInteger("+5"));
        assertEquals(Integer.valueOf(5), parseInteger("  5 "));
        assertNull(parseInteger(null));
    }

    @Test
    public void parseIntReturnsMinusOneOnMissingValue() {
        assertEquals(7, parseInt("7"));
        assertEquals(-1, parseInt(null));
    }

    @Test
    public void parseLongStripsLeadingPlus() {
        assertEquals(Long.valueOf(9), parseLong("+9"));
        assertNull(parseLong(null));
    }

    @Test
    public void parseShortReturnsMinusOneOnMissingValue() {
        assertEquals((short) 3, (short) parseShort("3"));
        assertEquals((short) -1, (short) parseShort(null));
    }

    @Test
    public void isEmptyForStrings() {
        assertTrue(isEmpty((String) null));
        assertTrue(isEmpty(""));
        assertFalse(isEmpty("x"));
    }

    @Test
    public void isEmptyForNumbersTreatsNullAndZeroAsEmpty() {
        assertTrue(isEmpty((Integer) null));
        assertTrue(isEmpty(Integer.valueOf(0)));
        assertFalse(isEmpty(Integer.valueOf(5)));

        assertTrue(isEmpty(Double.valueOf(Double.NaN)));
        assertTrue(isEmpty(Double.valueOf(0.0)));
        assertFalse(isEmpty(Double.valueOf(1.0)));

        assertTrue(isEmpty((BigDecimal) null));
        assertTrue(isEmpty(BigDecimal.ZERO));
    }

    @Test
    public void toDoubleMapsNullAndNaNToZero() {
        assertEquals(0.0, toDouble((Double) null), 0.0);
        assertEquals(0.0, toDouble(Double.valueOf(Double.NaN)), 0.0);
        assertEquals(2.5, toDouble(Double.valueOf(2.5)), 0.0);
        assertEquals(0.0, toDouble((Short) null), 0.0);
        assertEquals(3.0, toDouble(Short.valueOf((short) 3)), 0.0);
    }

    @Test
    public void toArrayUnboxesTheList() {
        assertArrayEquals(new int[]{1, 2, 3}, toArray(asList(1, 2, 3)));
    }

    @Test
    public void encodeUmlautsExpandsGermanCharacters() {
        assertEquals("ueoeaess", encodeUmlauts("üöäß"));
        assertEquals("UEOEAE", encodeUmlauts("ÜÖÄ"));
    }

    @Test
    public void toMixedCaseOnlyRewritesAllUppercaseInput() {
        assertEquals("Hello World", toMixedCase("HELLO WORLD"));
        assertEquals("Abc-Def", toMixedCase("ABC-DEF"));
        assertEquals("Already Mixed", toMixedCase("Already Mixed"));
    }

    @Test
    public void trimLineFeedsReplacesNewlinesWithSpaces() {
        assertEquals("a b c", trimLineFeeds("a\nb\rc"));
    }
}
