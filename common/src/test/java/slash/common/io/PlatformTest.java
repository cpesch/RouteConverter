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

import slash.common.TestCase;

public class PlatformTest extends TestCase {
    public void testIsCurrentAtLeastMinimumVersion() {
        assertTrue(Platform.isCurrentAtLeastMinimumVersion("1.6", "1.6"));
        assertTrue(Platform.isCurrentAtLeastMinimumVersion("1.6.0", "1.6.0"));
        assertTrue(Platform.isCurrentAtLeastMinimumVersion("1.6.0_14", "1.6.0_14"));

        assertTrue(Platform.isCurrentAtLeastMinimumVersion("1.6.0", "1.6"));
        assertTrue(Platform.isCurrentAtLeastMinimumVersion("1.6.0_14", "1.6"));
        assertTrue(Platform.isCurrentAtLeastMinimumVersion("1.6.0_14", "1.6.0"));
        assertTrue(Platform.isCurrentAtLeastMinimumVersion("1.6.0_15", "1.6.0_14"));

        assertFalse(Platform.isCurrentAtLeastMinimumVersion("1.6", "1.6.0"));
        assertFalse(Platform.isCurrentAtLeastMinimumVersion("1.6", "1.6.0_14"));
        assertFalse(Platform.isCurrentAtLeastMinimumVersion("1.6.0", "1.6.0_14"));
        assertFalse(Platform.isCurrentAtLeastMinimumVersion("1.6.0_13", "1.6.0_14"));
    }
}