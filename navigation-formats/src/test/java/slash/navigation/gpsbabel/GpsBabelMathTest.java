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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static slash.navigation.gpsbabel.GpsBabelMath.*;

public class GpsBabelMathTest {

    @Test
    public void testSiRound() {
        assertEquals(3, siRound(2.5));
        assertEquals(-3, siRound(-2.5));
        assertEquals(-2, siRound(-2.4));
        assertEquals(2, siRound(2.4));
        assertEquals(0, siRound(0.0));
    }

    @Test
    public void testDdmm2Degrees() {
        assertEquals(48.1173, ddmm2degrees(4807.038), 1e-9);
        assertEquals(0.0, ddmm2degrees(0.0), 0.0);
    }

    @Test
    public void testDegrees2DdmmRoundTrip() {
        assertEquals(4807.038, degrees2ddmm(48.1173), 1e-9);
        assertEquals(48.1173, ddmm2degrees(degrees2ddmm(48.1173)), 1e-9);
        assertEquals(11.5, ddmm2degrees(degrees2ddmm(11.5)), 1e-9);
    }

    @Test
    public void testSemicircles() {
        assertEquals(0, degreesToSemicircles(0.0));
        assertEquals(1073741824, degreesToSemicircles(90.0));
        assertEquals(-1073741824, degreesToSemicircles(-90.0));
        assertEquals(0.0, semicirclesToDegrees(0), 0.0);
        assertEquals(90.0, semicirclesToDegrees(1073741824), 0.0);
        assertEquals(-180.0, semicirclesToDegrees(Integer.MIN_VALUE), 0.0);
    }

    @Test
    public void testSemicirclesRoundTrip() {
        double[] values = {0.0, 90.0, -90.0, 180.0 - 1e-6, -180.0, 48.1173, -11.516667};
        for (double value : values)
            assertEquals(value, semicirclesToDegrees(degreesToSemicircles(value)), 1e-7);
    }

    @Test
    public void testSemicirclesRoundHalfAwayFromZero() {
        double half = 180.0 / 2147483648.0 / 2.0;
        assertEquals(1, degreesToSemicircles(half * 1.0000001));
        assertEquals(-1, degreesToSemicircles(-half * 1.0000001));
        assertEquals(0, degreesToSemicircles(half * 0.9999999));
    }

    @Test
    public void testMonthLookup() {
        assertEquals(5, monthLookup("jun"));
        assertEquals(0, monthLookup("JAN"));
        assertEquals(11, monthLookup("Dec"));
        assertEquals(-1, monthLookup("XYZ"));
        assertEquals(-1, monthLookup("June"));
        assertEquals(-1, monthLookup(""));
        assertEquals(-1, monthLookup(null));
    }
}
