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

import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.common.UnitSystem.*;

public class UnitSystemTest {
    @Test
    public void testMetric() {
        assertDoubleEquals(1.0, Metric.distanceToDefault(1.0));
        assertDoubleEquals(1.0, Metric.distanceToUnit(1000.0));
        assertDoubleEquals(1000.0, Metric.shortDistanceToUnit(1000.0));

        assertDoubleEquals(1.2345, Metric.valueToDefault(1.2345));
        assertDoubleEquals(1.2345, Metric.valueToUnit(1.2345));
    }

    @Test
    public void testNautic() {
        assertDoubleEquals(1.8520043, Nautic.distanceToDefault(1.0));
        assertDoubleEquals(1.0000000000000002, Nautic.distanceToUnit(1852.0043));
        assertDoubleEquals(3.280839895013123, Nautic.shortDistanceToUnit(1.0));

        assertDoubleEquals(0.3048, Statute.valueToDefault(1.0));
        assertDoubleEquals(1.0, Statute.valueToUnit(0.3048));
    }

    @Test
    public void testStatute() {
        assertDoubleEquals(1.609344, Statute.distanceToDefault(1.0));
        assertDoubleEquals(1.0, Statute.distanceToUnit(1609.344));
        assertDoubleEquals(3.280839895013123, Nautic.shortDistanceToUnit(1.0));

        assertDoubleEquals(0.3048, Statute.valueToDefault(1.0));
        assertDoubleEquals(1.0, Statute.valueToUnit(0.3048));
    }
}
