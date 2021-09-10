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

package slash.common.system;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.common.system.Platform.*;

public class PlatformTest {
    @Test
    public void testIsCurrentAtLeastMinimumVersion() {
        assertTrue(isCurrentAtLeastMinimumVersion("1.6", "1.6"));
        assertTrue(isCurrentAtLeastMinimumVersion("1.6.0", "1.6.0"));
        assertTrue(isCurrentAtLeastMinimumVersion("1.6.0_14", "1.6.0_14"));

        assertTrue(isCurrentAtLeastMinimumVersion("1.6.0", "1.6"));
        assertTrue(isCurrentAtLeastMinimumVersion("1.6.0_14", "1.6"));
        assertTrue(isCurrentAtLeastMinimumVersion("1.6.0_14", "1.6.0"));
        assertTrue(isCurrentAtLeastMinimumVersion("1.6.0_15", "1.6.0_14"));
        assertTrue(isCurrentAtLeastMinimumVersion("9.0", "1.7"));
        assertTrue(isCurrentAtLeastMinimumVersion("9.0.1", "1.7"));

        assertFalse(isCurrentAtLeastMinimumVersion("1.6", "1.6.0"));
        assertFalse(isCurrentAtLeastMinimumVersion("1.6", "1.6.0_14"));
        assertFalse(isCurrentAtLeastMinimumVersion("1.6.0", "1.6.0_14"));
        assertFalse(isCurrentAtLeastMinimumVersion("1.6.0_13", "1.6.0_14"));
    }

    @Test
    public void testIsJavaLaterThan() {
        assertTrue(isJavaLaterThan("1.7.0", 6));
        assertTrue(isJavaLaterThan("1.7.0", 7));
        assertFalse(isJavaLaterThan("1.7.0", 8));
        assertFalse(isJavaLaterThan("1.7.0", 9));
        assertFalse(isJavaLaterThan("1.7.0", 10));

        assertTrue(isJavaLaterThan("1.8.0", 7));
        assertTrue(isJavaLaterThan("1.8.0", 8));
        assertFalse(isJavaLaterThan("1.8.0", 9));
        assertFalse(isJavaLaterThan("1.8.0", 10));
        assertFalse(isJavaLaterThan("1.8.0", 11));

        assertTrue(isJavaLaterThan("9", 8));
        assertTrue(isJavaLaterThan("9", 9));
        assertFalse(isJavaLaterThan("9", 10));

        assertTrue(isJavaLaterThan("10", 8));
        assertTrue(isJavaLaterThan("10", 9));
        assertTrue(isJavaLaterThan("10.0.1", 9));
        assertTrue(isJavaLaterThan("10", 10));

        assertTrue(isJavaLaterThan("11", 10));
        assertTrue(isJavaLaterThan("11.0.1", 10));
        assertTrue(isJavaLaterThan("11", 11));

        assertTrue(isJavaLaterThan("15", 14));
        assertTrue(isJavaLaterThan("15", 15));
        assertTrue(isJavaLaterThan("16", 15));
    }

    @Test
    public void testHasJavaFX() {
        assertFalse(hasJavaFX("Oracle", "1.7.0"));
        assertTrue(hasJavaFX("Oracle", "1.8.0"));
        assertFalse(hasJavaFX("OpenJDK", "1.8.0"));
        assertTrue(hasJavaFX("Oracle", "9"));
        assertFalse(hasJavaFX("OpenJDK", "9"));
        assertTrue(hasJavaFX("Oracle", "10"));
        assertFalse(hasJavaFX("OpenJDK", "10"));
        assertTrue(hasJavaFX("Oracle", "10.0.1"));
        assertFalse(hasJavaFX("OpenJDK", "10.0.1"));
        assertFalse(hasJavaFX("Oracle", "11"));
        assertFalse(hasJavaFX("Oracle", "11.0.1"));
        assertFalse(hasJavaFX("Oracle", "12"));
    }
}
