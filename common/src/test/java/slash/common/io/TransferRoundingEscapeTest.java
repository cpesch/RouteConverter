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

import static org.junit.Assert.*;
import static slash.common.io.Transfer.*;

/**
 * Covers the rounding, escaping, XML-sanitizing and null-tolerant formatting helpers
 * of {@link Transfer}.
 *
 * @author Christian Pesch
 */
public class TransferRoundingEscapeTest {

    @Test
    public void ceilFractionRoundsUpToTheGivenDigits() {
        assertEquals(1.24, ceilFraction(1.231, 2), 0.0);
        assertEquals(2.0, ceilFraction(2.0, 2), 0.0);
    }

    @Test
    public void roundMeterToMillimeterPrecisionFloorsToFourDigits() {
        assertEquals(1.2345, roundMeterToMillimeterPrecision(1.23456), 0.0);
    }

    @Test
    public void roundMillisecondsToSecondPrecisionTruncates() {
        assertEquals(1000L, roundMillisecondsToSecondPrecision(1999L));
        assertEquals(5000L, roundMillisecondsToSecondPrecision(5000L));
    }

    @Test
    public void escapeReplacesTheEscapeCharacterOrFallsBackToDefault() {
        assertEquals("a_b_c", escape("a.b.c", '.', '_'));
        assertEquals("def", escape(null, '.', '_', "def"));
        assertEquals("", escape("   ", '.', '_'));
    }

    @Test
    public void isIsoLatin1ButReadWithUtf8DetectsReplacementCharacter() {
        assertTrue(isIsoLatin1ButReadWithUtf8("caf" + (char) 0xFFFD));
        assertFalse(isIsoLatin1ButReadWithUtf8("cafe"));
        assertFalse(isIsoLatin1ButReadWithUtf8(null));
    }

    @Test
    public void stripNonValidXMLCharactersDropsControlCharactersButKeepsTabsAndNewlines() {
        assertEquals("abc", stripNonValidXMLCharacters("a" + (char) 0x00 + "bc"));
        assertEquals("x\ty\nz", stripNonValidXMLCharacters("x\ty\nz"));
    }

    @Test
    public void formatLongAndShortAsStringTreatNullAsZero() {
        assertEquals("0", formatLongAsString(null));
        assertEquals("42", formatLongAsString(42L));
        assertEquals("0", formatShortAsString(null));
        assertEquals("7", formatShortAsString((short) 7));
    }
}
