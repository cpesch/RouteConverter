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
package slash.navigation.common;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link TransformUtil} ? WGS-84 ? GCJ-02 coordinate transform helpers.
 *
 * @author Christian Pesch
 */
public class TransformUtilTest {

    // ---- isPositionInChina ----

    @Test
    public void testPositionInsideChina() {
        // Beijing: ~116.4 E, ~39.9 N
        assertTrue(TransformUtil.isPositionInChina(116.4, 39.9));
    }

    @Test
    public void testPositionOutsideChina_WestOfLongitudeBound() {
        // Far west (e.g., London ~0.0 E)
        assertFalse(TransformUtil.isPositionInChina(0.0, 51.5));
    }

    @Test
    public void testPositionOutsideChina_EastOfLongitudeBound() {
        // Far east (e.g., 140.0 E)
        assertFalse(TransformUtil.isPositionInChina(140.0, 35.0));
    }

    @Test
    public void testPositionOutsideChina_SouthOfLatitudeBound() {
        // In valid longitude range but too far south
        assertFalse(TransformUtil.isPositionInChina(100.0, 0.5));
    }

    @Test
    public void testPositionOutsideChina_NorthOfLatitudeBound() {
        // In valid longitude range but too far north
        assertFalse(TransformUtil.isPositionInChina(100.0, 60.0));
    }

    @Test
    public void testPositionAtLongitudeBoundaryLow() {
        // longitude = 72.004 is the lower boundary (not out-of-China)
        assertTrue(TransformUtil.isPositionInChina(72.004, 30.0));
    }

    @Test
    public void testPositionJustBelowLongitudeBoundaryLow() {
        assertFalse(TransformUtil.isPositionInChina(72.003, 30.0));
    }

    @Test
    public void testPositionAtLongitudeBoundaryHigh() {
        // longitude = 137.8347 is the upper boundary (not out-of-China)
        assertTrue(TransformUtil.isPositionInChina(137.8347, 30.0));
    }

    @Test
    public void testPositionJustAboveLongitudeBoundaryHigh() {
        assertFalse(TransformUtil.isPositionInChina(137.835, 30.0));
    }

    // ---- delta ----

    @Test
    public void testDeltaReturnsTwoValues() {
        double[] d = TransformUtil.delta(39.9, 116.4);
        assertNotNull(d);
        assertEquals(2, d.length);
    }

    @Test
    public void testDeltaForBeijingIsNonZero() {
        double[] d = TransformUtil.delta(39.9, 116.4);
        // Both delta values should be non-zero for a location inside China
        assertFalse("delta[0] should be non-zero", d[0] == 0.0);
        assertFalse("delta[1] should be non-zero", d[1] == 0.0);
    }

    @Test
    public void testDeltaValuesAreSmall() {
        // WGS-84 to GCJ-02 delta is typically less than 0.01 degrees
        double[] d = TransformUtil.delta(39.9, 116.4);
        assertTrue("delta[0] should be small", Math.abs(d[0]) < 0.1);
        assertTrue("delta[1] should be small", Math.abs(d[1]) < 0.1);
    }
}

