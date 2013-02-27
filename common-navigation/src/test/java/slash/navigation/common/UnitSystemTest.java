/*
 *
 *     This file is part of RouteConverter.
 *
 *     RouteConverter is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     RouteConverter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with RouteConverter; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *     Copyright (C) 2007 Christian Pesch. All Rights Reserved.
 * /
 */

package slash.navigation.common;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static slash.navigation.common.UnitSystem.Metric;
import static slash.navigation.common.UnitSystem.Nautic;
import static slash.navigation.common.UnitSystem.Statute;

public class UnitSystemTest {
    @Test
    public void testMetric() {
        assertEquals(1.0, Metric.distanceToDefault(1.0));
        assertEquals(1.0, Metric.distanceToUnit(1.0));

        assertEquals(1.2345, Metric.valueToDefault(1.2345));
        assertEquals(1.2345, Metric.valueToUnit(1.2345));
    }

    @Test
    public void testNautic() {
        assertEquals(1.8520043, Nautic.distanceToDefault(1.0));
        assertEquals(1.0, Nautic.distanceToUnit(1.8520043));

        assertEquals(1.8520043, Nautic.valueToDefault(1.0));
        assertEquals(1.2345, Nautic.valueToUnit(1.2345));
    }


    @Test
    public void testStatute() {
        assertEquals(1.609344, Statute.distanceToDefault(1.0));
        assertEquals(1.0, Statute.distanceToUnit(1.609344));

        assertEquals(1.609344, Statute.valueToDefault(1.0));
        // feet for a meter
        assertEquals(3.280839895013123, Statute.valueToUnit(1.0));
    }
}
