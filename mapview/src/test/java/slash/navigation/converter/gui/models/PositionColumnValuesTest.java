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
package slash.navigation.converter.gui.models;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for {@link PositionColumnValues}.
 *
 * @author Christian Pesch
 */
public class PositionColumnValuesTest {

    @Test
    public void testSingleColumnConstructor() {
        PositionColumnValues pcv = new PositionColumnValues(3, "Hamburg");
        assertEquals(List.of(3), pcv.getColumnIndices());
        assertEquals(List.of("Hamburg"), pcv.getNextValues());
    }

    @Test
    public void testMultiColumnConstructor() {
        List<Integer> indices = Arrays.asList(1, 5, 7);
        List<Object> values = Arrays.asList("a", 42, true);
        PositionColumnValues pcv = new PositionColumnValues(indices, values);
        assertEquals(indices, pcv.getColumnIndices());
        assertEquals(values, pcv.getNextValues());
    }

    @Test
    public void testPreviousValuesInitiallyNull() {
        PositionColumnValues pcv = new PositionColumnValues(0, "initial");
        assertNull(pcv.getPreviousValues());
    }

    @Test
    public void testSetAndGetPreviousValues() {
        PositionColumnValues pcv = new PositionColumnValues(2, "value");
        List<Object> prev = Arrays.asList("old", null);
        pcv.setPreviousValues(prev);
        assertEquals(prev, pcv.getPreviousValues());
    }
}

