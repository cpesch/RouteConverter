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

    Copyright (C) 2026 Christian Pesch. All Rights Reserved.
*/

package slash.common.nightly;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// An IT, not a unit test: push runs skip ITs, so only the nightly Hermetic ITs
// job sees this failure (rc-meta spec 00078 M6 canary).
public class NightlyCanaryIT {
    @Test
    public void answerIsFortyTwo() {
        assertEquals(42, NightlyCanary.answer());
    }
}
