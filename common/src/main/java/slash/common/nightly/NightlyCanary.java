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

/**
 * Canary for the nightly failure-triage loop (rc-meta spec 00078 M6): its
 * integration test fails on purpose until the factory builder fixes this
 * method, proving nightly failure -> issue -> auto-approval -> builder PR.
 * Remove both files once the proof is done.
 */
public class NightlyCanary {
    /**
     * The answer to the ultimate question; see NightlyCanaryIT.
     */
    public static int answer() {
        return 41;
    }
}
