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

package slash.navigation.routing;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static slash.navigation.routing.TravelRestrictions.NO_RESTRICTIONS;

public class TravelRestrictionsTest {

    @Test
    public void testNoRestrictionsAllFalse() {
        assertFalse(NO_RESTRICTIONS.avoidBridges());
        assertFalse(NO_RESTRICTIONS.avoidFerries());
        assertFalse(NO_RESTRICTIONS.avoidMotorways());
        assertFalse(NO_RESTRICTIONS.avoidTolls());
        assertFalse(NO_RESTRICTIONS.avoidTunnels());
    }

    @Test
    public void testCustomRestrictions() {
        TravelRestrictions r = new TravelRestrictions(true, false, true, false, true);
        assert r.avoidBridges();
        assertFalse(r.avoidFerries());
        assert r.avoidMotorways();
        assertFalse(r.avoidTolls());
        assert r.avoidTunnels();
    }

    @Test
    public void testEqualsAndHashCodeSameValues() {
        TravelRestrictions a = new TravelRestrictions(true, false, false, true, false);
        TravelRestrictions b = new TravelRestrictions(true, false, false, true, false);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualDifferentValues() {
        TravelRestrictions a = new TravelRestrictions(true, false, false, false, false);
        TravelRestrictions b = new TravelRestrictions(false, false, false, false, false);
        assertNotEquals(a, b);
    }

    @Test
    public void testNoRestrictionsSameAsAllFalse() {
        assertEquals(NO_RESTRICTIONS, new TravelRestrictions(false, false, false, false, false));
    }
}

